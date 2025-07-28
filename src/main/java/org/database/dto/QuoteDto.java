package org.database.dto;

import com.dynatrace.hash4j.hashing.Hashing;
import com.dynatrace.hash4j.similarity.ElementHashProvider;
import com.dynatrace.hash4j.similarity.SimilarityHashPolicy;
import com.dynatrace.hash4j.similarity.SimilarityHasher;
import com.dynatrace.hash4j.similarity.SimilarityHashing;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.database.DtoWithDistance;
import org.database.Persistable;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utility.Constants;
import org.utility.Helper;
import org.utility.ProcessingContext;

import java.security.MessageDigest;
import java.text.MessageFormat;
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
public class QuoteDto implements Persistable {
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

    @Transient
    private List<TagDto> tagDtos;

    public QuoteDto() {}

    public QuoteDto(String snowflakeAuthor, String snowflakeGuild, String quote, List<TagDto> tagDtos) {
        this.snowflakeAuthor = snowflakeAuthor;
        this.snowflakeGuild = snowflakeGuild;
        this.quote = quote;
        this.quoteSimhash = QuoteDto.generateSimhash(quote);

        this.tagDtos = tagDtos;
    }

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

    public static List<QuoteDto.QuoteDistance> findSimilarQuotes(String newQuote, String snowflakeGuild, Session session) {
        byte[] newQuoteSimhash = QuoteDto.generateSimhash(newQuote);
        List<QuoteDto.QuoteSimhashWrapper> existingSimhashes = QuoteDto.getExistingSimhashes(session);
        Map<Long, Double> similarQuotesIds = new HashMap<>();

        SimilarityHashPolicy policy = QuoteDto.generateHashPolicy();

        for (QuoteDto.QuoteSimhashWrapper simhashWrapper : existingSimhashes) {
            double distance = 1.0 - policy.getFractionOfEqualComponents(newQuoteSimhash, simhashWrapper.simhash());
            if (distance <= Constants.SIMHASH_DISTANCE_WARNING_THRESHOLD) {
                similarQuotesIds.put(simhashWrapper.idQuote(), distance);
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
                .map(quote -> new QuoteDto.QuoteDistance(quote, similarQuotesIds.get(quote.getIdQuote())))
                .toList();
    }

    private static List<QuoteDto.QuoteSimhashWrapper> getExistingSimhashes(Session session) {
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<QuoteDto.QuoteSimhashWrapper> criteria = builder.createQuery(QuoteDto.QuoteSimhashWrapper.class);
        Root<QuoteDto> root = criteria.from(QuoteDto.class);

        Path<Long> idQuotePath = root.get(Helper.snakeCaseToCamelCase(QuoteDto.ID_QUOTE_COLUMN_NAME));
        Path<byte[]> quoteSimhashPath = root.get(Helper.snakeCaseToCamelCase(QuoteDto.QUOTE_SIMHASH_COLUMN_NAME));
        CompoundSelection<QuoteDto.QuoteSimhashWrapper> selection = builder.construct(QuoteDto.QuoteSimhashWrapper.class, idQuotePath, quoteSimhashPath);

        criteria.select(selection);
        return session.createQuery(criteria).getResultList();
    }

    private static byte[] generateSimhash(String value) {
        ToLongFunction<String> hashFunction = s -> Hashing.komihash5_0().hashCharsToLong(s);
        SimilarityHashPolicy policy = QuoteDto.generateHashPolicy();
        SimilarityHasher hasher = policy.createHasher();

        List<String> tokens = Helper.normalizeAndTokenizeString(value);
        return hasher.compute(ElementHashProvider.ofCollection(tokens, hashFunction));
    }

    private static SimilarityHashPolicy generateHashPolicy() {
        return SimilarityHashing.superMinHash(1024, 1);
    }

    @Override
    public void persist(ProcessingContext processingContext, Session session) {
        assert !this.tagDtos.isEmpty();

        session.persist(this);
        this.tagDtos.forEach(tagDto -> session.persist(new QuoteTagDto(tagDto.getIdTag(), this.idQuote)));

        String stringifiedTags = Helper.stringifyCollection(this.tagDtos, TagDto::getTag, true);
        String tagsMessage = "with " + (this.tagDtos.size() > 1 ? "tags " : "tag ") + stringifiedTags;

        processingContext.addMessages(
                MessageFormat.format("New quote \"{0}\" {1} was successfully created", this.quote, tagsMessage),
                ProcessingContext.MessageType.SUCCESS_RESULT
        );
    }

    @Override
    public void cancelPersist(ProcessingContext processingContext) {
        processingContext.addMessages(
                MessageFormat.format("Creation of quote \"{0}\" was canceled", this.quote),
                ProcessingContext.MessageType.INFO_RESULT
        );
    }

    public void loadTagDtos(Session session) {
        assert this.tagDtos == null;

        String innerSql = "SELECT " + QuoteTagDto.ID_TAG_COLUMN_NAME + " FROM "
                + QuoteTagDto.QUOTE_TAG_TABLE_NAME + " WHERE  " + QuoteTagDto.ID_QUOTE_COLUMN_NAME
                + " = :p_idQuote";
        String sql = "SELECT * FROM " + TagDto.TAG_TABLE_NAME + " WHERE "
                + TagDto.ID_TAG_COLUMN_NAME + " IN (" + innerSql + ")";
        this.tagDtos = session.createNativeQuery(sql, TagDto.class)
                .setParameter("p_idQuote", this.idQuote)
                .getResultList();
    }

    public long getIdQuote() {
        return this.idQuote;
    }

    public String getQuote() {
        return this.quote;
    }

    public List<TagDto> getTagDtos() {
        return this.tagDtos;
    }

    public record QuoteDistance(QuoteDto quoteDto, double distance) implements DtoWithDistance {
        @Override
        public String getName() {
            return "quote";
        }

        @Override
        public String getValue() {
            return this.quoteDto.getQuote();
        }

        @Override
        public double getDistance() {
            return this.distance;
        }
    }

    private record QuoteSimhashWrapper(long idQuote, byte[] simhash) {}
}
