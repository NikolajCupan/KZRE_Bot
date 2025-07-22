package org.database.dto;

import com.dynatrace.hash4j.hashing.Hashing;
import com.dynatrace.hash4j.similarity.ElementHashProvider;
import com.dynatrace.hash4j.similarity.SimilarityHashPolicy;
import com.dynatrace.hash4j.similarity.SimilarityHasher;
import com.dynatrace.hash4j.similarity.SimilarityHashing;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.hibernate.Session;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utility.Constants;
import org.utility.Helper;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.ToLongFunction;

@Entity
@Table(
        name = QuoteDto.QUOTE_TABLE_NAME,
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {QuoteDto.SNOWFLAKE_GUILD_COLUMN_NAME, QuoteDto.QUOTE_HASH_COLUMN_NAME})
        }
)
public class QuoteDto {
    public static final String QUOTE_TABLE_NAME = "quote";

    public static final String ID_QUOTE_COLUMN_NAME = "id_quote";
    public static final String SNOWFLAKE_AUTHOR_COLUMN_NAME = "snowflake_author";
    public static final String SNOWFLAKE_GUILD_COLUMN_NAME = "snowflake_guild";
    public static final String QUOTE_COLUMN_NAME = "quote";
    public static final String QUOTE_HASH_COLUMN_NAME = "quote_hash";
    public static final String QUOTE_SIMHASH_COLUMN_NAME = "quote_simhash";
    public static final String DATE_CREATED_COLUMN_NAME = "date_created";
    public static final String DATE_MODIFIED_COLUMN_NAME = "date_modified";

    private static final Logger LOGGER = LoggerFactory.getLogger(QuoteDto.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = QuoteDto.ID_QUOTE_COLUMN_NAME, unique = true, nullable = false)
    private long idQuote;

    @Column(name = QuoteDto.SNOWFLAKE_AUTHOR_COLUMN_NAME, nullable = false)
    private String snowflakeAuthor;

    @Column(name = QuoteDto.SNOWFLAKE_GUILD_COLUMN_NAME, nullable = false)
    private String snowflakeGuild;

    @Column(name = QuoteDto.QUOTE_COLUMN_NAME, nullable = false)
    private String quote;

    @Column(name = QuoteDto.QUOTE_HASH_COLUMN_NAME, nullable = false, insertable = false)
    private byte[] quoteHash;

    @Column(name = QuoteDto.QUOTE_SIMHASH_COLUMN_NAME, nullable = false)
    private byte[] quoteSimhash;

    @Column(name = QuoteDto.DATE_CREATED_COLUMN_NAME, nullable = false, insertable = false, updatable = false)
    private LocalDateTime dateCreated;

    @Column(name = QuoteDto.DATE_MODIFIED_COLUMN_NAME, nullable = false, insertable = false)
    private LocalDateTime dateModified;

    public static boolean quoteExists(String newQuote, String snowflakeGuild, Session session) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (Exception ignore) {
            QuoteDto.LOGGER.error("Could not initialize SHA-256 instance");
            return false;
        }

        byte[] newQuoteHash = messageDigest.digest(newQuote.getBytes());

        String sql = "SELECT * FROM " + QuoteDto.QUOTE_TABLE_NAME + " WHERE "
                + QuoteDto.SNOWFLAKE_GUILD_COLUMN_NAME + " = :p_snowflakeGuild AND "
                + QuoteDto.QUOTE_HASH_COLUMN_NAME + " = :p_quoteHash";
        return session.createNativeQuery(sql, QuoteDto.class)
                .setParameter("p_snowflakeGuild", snowflakeGuild)
                .setParameter("p_quoteHash", newQuoteHash)
                .getSingleResultOrNull() != null;
    }

    public static List<Pair<Double, QuoteDto>> findSimilarQuotes(String newQuote, String snowflakeGuild, Session session) {
        ToLongFunction<String> hashFunction = s -> Hashing.komihash5_0().hashCharsToLong(s);
        SimilarityHashPolicy policy = SimilarityHashing.superMinHash(1024, 1);
        SimilarityHasher hasher = policy.createHasher();

        List<String> tokens = Helper.normalizeAndTokenizeString(newQuote);
        byte[] newQuoteSimHash = hasher.compute(ElementHashProvider.ofCollection(tokens, hashFunction));


        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Tuple> criteria = builder.createQuery(Tuple.class);
        Root<QuoteDto> root = criteria.from(QuoteDto.class);

        Path<Long> idQuotePath = root.get(Helper.snakeCaseToCamelCase(QuoteDto.ID_QUOTE_COLUMN_NAME));
        Path<byte[]> quoteSimhashPath = root.get(Helper.snakeCaseToCamelCase(QuoteDto.QUOTE_SIMHASH_COLUMN_NAME));
        CompoundSelection<Tuple> selection = builder.tuple(quoteSimhashPath, idQuotePath);

        criteria.select(selection);
        List<Tuple> simhashes = session.createQuery(criteria).getResultList();
        Map<Long, Double> similarQuotesIds = new HashMap<>();

        for (Tuple tuple : simhashes) {
            long idQuote = tuple.get(idQuotePath);
            byte[] simhash = tuple.get(quoteSimhashPath);

            double distance = 1.0 - policy.getFractionOfEqualComponents(newQuoteSimHash, simhash);
            if (distance <= Constants.SIMHASH_DISTANCE_WARNING_THRESHOLD) {
                similarQuotesIds.put(idQuote, distance);
            }
        }

        String sql = "SELECT * FROM " + QuoteDto.QUOTE_TABLE_NAME + " WHERE "
                + QuoteDto.SNOWFLAKE_GUILD_COLUMN_NAME + " = :p_snowflakeGuild AND "
                + QuoteDto.ID_QUOTE_COLUMN_NAME + " IN :p_idsQuotes";
        List<QuoteDto> quoteDtos = session.createNativeQuery(sql, QuoteDto.class)
                .setParameter("p_snowflakeGuild", snowflakeGuild)
                .setParameter("p_idsQuotes", similarQuotesIds.keySet().stream().toList())
                .getResultList();

        return quoteDtos.stream()
                .map(quote -> new Pair<>(similarQuotesIds.get(quote.getIdQuote()), quote))
                .toList();
    }

    public long getIdQuote() {
        return this.idQuote;
    }
}
