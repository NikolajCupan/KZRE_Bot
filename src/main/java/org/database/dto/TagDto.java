package org.database.dto;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import jakarta.persistence.*;
import org.database.Persistable;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.utility.Constants;
import org.utility.ProcessingContext;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = TagDto.TAG_TABLE_NAME,
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {TagDto.SNOWFLAKE_GUILD_COLUMN_NAME, TagDto.TAG_COLUMN_NAME})
        }
)
public class TagDto implements Persistable {
    public static final String TAG_TABLE_NAME = "tag";

    public static final String ID_TAG_COLUMN_NAME = "id_tag";
    public static final String SNOWFLAKE_AUTHOR_COLUMN_NAME = "snowflake_author";
    public static final String SNOWFLAKE_GUILD_COLUMN_NAME = "snowflake_guild";
    public static final String TAG_COLUMN_NAME = "tag";
    public static final String DATE_CREATED_COLUMN_NAME = "date_created";
    public static final String DATE_MODIFIED_COLUMN_NAME = "date_modified";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = TagDto.ID_TAG_COLUMN_NAME, unique = true, nullable = false)
    private long idTag;

    @Column(name = TagDto.SNOWFLAKE_AUTHOR_COLUMN_NAME, nullable = false)
    private String snowflakeAuthor;

    @Column(name = TagDto.SNOWFLAKE_GUILD_COLUMN_NAME, nullable = false)
    private String snowflakeGuild;

    @Column(name = TagDto.TAG_COLUMN_NAME, nullable = false)
    private String tag;

    @Column(name = TagDto.DATE_CREATED_COLUMN_NAME, nullable = false, insertable = false, updatable = false)
    private LocalDateTime dateCreated;

    @Column(name = TagDto.DATE_MODIFIED_COLUMN_NAME, nullable = false, insertable = false)
    private LocalDateTime dateModified;

    public TagDto() {}

    public TagDto(String snowflakeAuthor, String snowflakeGuild, String tag) {
        this.snowflakeAuthor = snowflakeAuthor;
        this.snowflakeGuild = snowflakeGuild;
        this.tag = tag;
    }

    public static List<TagDto> findSimilarTags(String newTag, String snowflakeGuild, Session session) {
        String sql = "SELECT * FROM " + TagDto.TAG_TABLE_NAME + " WHERE "
                + TagDto.SNOWFLAKE_GUILD_COLUMN_NAME + " = :p_snowflakeGuild";
        List<TagDto> tags = session.createNativeQuery(sql, TagDto.class)
                .setParameter("p_snowflakeGuild", snowflakeGuild)
                .getResultList();

        NormalizedLevenshtein levenshtein = new NormalizedLevenshtein();
        return tags.stream()
                .filter(tag -> levenshtein.distance(tag.getTag(), newTag) <= Constants.LEVENSHTEIN_DISTANCE_WARNING_THRESHOLD)
                .toList();
    }

    @Override
    public void persist(ProcessingContext processingContext, Session session) {
        try {
            session.persist(this);

            processingContext.addMessages(
                    MessageFormat.format("New tag \"{0}\" was successfully created", this.getTag()),
                    ProcessingContext.MessageType.SUCCESS_RESULT
            );
        } catch (ConstraintViolationException exception) {
            processingContext.addMessages(
                    MessageFormat.format("Tag \"{0}\" already exists", this.getTag()),
                    ProcessingContext.MessageType.ERROR
            );
        }
    }

    @Override
    public void rejectPersist(ProcessingContext processingContext) {
        processingContext.addMessages(
                MessageFormat.format("Creation of tag \"{0}\" was rejected", this.getTag()),
                ProcessingContext.MessageType.INFO_RESULT
        );
    }

    public String getTag() {
        return this.tag;
    }
}
