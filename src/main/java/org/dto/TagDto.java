package org.dto;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = TagDto.TAG_TABLE_NAME,
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {TagDto.SNOWFLAKE_GUILD_COLUMN_NAME, TagDto.TAG_COLUMN_NAME})
        }
)
public class TagDto {
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
}
