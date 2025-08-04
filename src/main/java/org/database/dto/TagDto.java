package org.database.dto;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import jakarta.persistence.*;
import org.database.DtoWithDistance;
import org.database.Persistable;
import org.hibernate.Session;
import org.utility.Constants;
import org.utility.ProcessingContext;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public static final String SNOWFLAKE_TAG_AUTHOR_COLUMN_NAME = "snowflake_tag_author";
    public static final String SNOWFLAKE_GUILD_COLUMN_NAME = "snowflake_guild";
    public static final String TAG_COLUMN_NAME = "tag";
    public static final String DATE_CREATED_COLUMN_NAME = "date_created";
    public static final String DATE_MODIFIED_COLUMN_NAME = "date_modified";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = TagDto.ID_TAG_COLUMN_NAME, unique = true, nullable = false)
    private long idTag;

    @Column(name = TagDto.SNOWFLAKE_TAG_AUTHOR_COLUMN_NAME, nullable = false)
    private String snowflakeTagAuthor;

    @Column(name = TagDto.SNOWFLAKE_GUILD_COLUMN_NAME, nullable = false)
    private String snowflakeGuild;

    @Column(name = TagDto.TAG_COLUMN_NAME, nullable = false)
    private String tag;

    @Column(name = TagDto.DATE_CREATED_COLUMN_NAME, nullable = false, insertable = false, updatable = false)
    private LocalDateTime dateCreated;

    @Column(name = TagDto.DATE_MODIFIED_COLUMN_NAME, nullable = false, insertable = false)
    private LocalDateTime dateModified;

    public TagDto() {}

    public TagDto(String snowflakeTagAuthor, String snowflakeGuild, String tag) {
        this.snowflakeTagAuthor = snowflakeTagAuthor;
        this.snowflakeGuild = snowflakeGuild;
        this.tag = tag;
    }

    public static boolean tagExists(String newTag, String snowflakeGuild, Session session) {
        String sql = "SELECT * FROM " + TagDto.TAG_TABLE_NAME + " WHERE "
                + TagDto.SNOWFLAKE_GUILD_COLUMN_NAME + " = :p_snowflakeGuild AND "
                + TagDto.TAG_COLUMN_NAME + " = :p_tag LIMIT 1";

        return session.createNativeQuery(sql, TagDto.class)
                .setParameter("p_snowflakeGuild", snowflakeGuild)
                .setParameter("p_tag", newTag)
                .getSingleResultOrNull() != null;
    }

    public static List<String> filterOutExistingTags(Set<String> tagsToCheck, String snowflakeGuild, Session session) {
        String sql = "SELECT * FROM " + TagDto.TAG_TABLE_NAME + " WHERE "
                + TagDto.SNOWFLAKE_GUILD_COLUMN_NAME + " = :p_snowflakeGuild AND "
                + TagDto.TAG_COLUMN_NAME + " IN :p_tagsToCheck";
        List<TagDto> existingTags = session.createNativeQuery(sql, TagDto.class)
                .setParameter("p_snowflakeGuild", snowflakeGuild)
                .setParameter("p_tagsToCheck", tagsToCheck)
                .getResultList();

        Set<String> nonExistentTags = new HashSet<>(tagsToCheck);
        existingTags.forEach(existingTagDto -> nonExistentTags.remove(existingTagDto.getTag()));

        return nonExistentTags.stream().toList();
    }

    public static List<TagDto.TagDistance> findSimilarTags(String newTag, String snowflakeGuild, Session session) {
        String sql = "SELECT * FROM " + TagDto.TAG_TABLE_NAME + " WHERE "
                + TagDto.SNOWFLAKE_GUILD_COLUMN_NAME + " = :p_snowflakeGuild";
        List<TagDto> tags = session.createNativeQuery(sql, TagDto.class)
                .setParameter("p_snowflakeGuild", snowflakeGuild)
                .getResultList();

        NormalizedLevenshtein levenshtein = new NormalizedLevenshtein();
        return tags.stream()
                .map(tag -> new TagDto.TagDistance(tag, levenshtein.distance(tag.getTag(), newTag)))
                .filter(pair -> pair.distance <= Constants.LEVENSHTEIN_DISTANCE_WARNING_THRESHOLD)
                .toList();
    }

    public static List<TagDto> mapStringTagsToDtos(Collection<String> tags, String snowflakeGuild, Session session) {
        String sql = "SELECT * FROM " + TagDto.TAG_TABLE_NAME + " WHERE "
                + TagDto.SNOWFLAKE_GUILD_COLUMN_NAME + " = :p_snowflakeGuild AND "
                + TagDto.TAG_COLUMN_NAME + " IN :p_tags";
        List<TagDto> tagDtos = session.createNativeQuery(sql, TagDto.class)
                .setParameter("p_snowflakeGuild", snowflakeGuild)
                .setParameter("p_tags", tags)
                .getResultList();

        assert tagDtos.size() == tags.size();
        return tagDtos;
    }

    @Override
    public void persist(ProcessingContext processingContext, Session session) {
        session.persist(this);

        processingContext.addMessages(
                MessageFormat.format("New tag \"{0}\" was successfully created", this.tag),
                ProcessingContext.MessageType.SUCCESS_RESULT
        );
    }

    @Override
    public void cancelPersist(ProcessingContext processingContext) {
        processingContext.addMessages(
                MessageFormat.format("Creation of tag \"{0}\" was canceled", this.tag),
                ProcessingContext.MessageType.INFO_RESULT
        );
    }

    public long getIdTag() {
        return this.idTag;
    }

    public String getTag() {
        return this.tag;
    }

    public record TagDistance(TagDto tagDto, double distance) implements DtoWithDistance {
        @Override
        public String getName() {
            return "tag";
        }

        @Override
        public String getValue() {
            return this.tagDto.getTag();
        }

        @Override
        public double getDistance() {
            return this.distance;
        }
    }
}
