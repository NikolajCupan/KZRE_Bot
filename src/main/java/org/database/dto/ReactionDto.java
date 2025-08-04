package org.database.dto;

import jakarta.persistence.*;

@Entity
@Table(name = ReactionDto.REACTION_TABLE_NAME)
@IdClass(ReactionDto.ReactionDtoPK.class)
public class ReactionDto {
    public static final String REACTION_TABLE_NAME = "reaction";

    public static final String SNOWFLAKE_MESSAGE_COLUMN_NAME = "snowflake_message";
    public static final String SNOWFLAKE_AUTHOR_COLUMN_NAME = "snowflake_author";
    public static final String ID_EMOJI_COLUMN_NAME = "id_emoji";

    @Id
    @Column(name = ReactionDto.SNOWFLAKE_MESSAGE_COLUMN_NAME, nullable = false)
    private String snowflakeMessage;

    @Id
    @Column(name = ReactionDto.SNOWFLAKE_AUTHOR_COLUMN_NAME, nullable = false)
    private String snowflakeAuthor;

    @Id
    @Column(name = ReactionDto.ID_EMOJI_COLUMN_NAME, nullable = false)
    private long idEmoji;

    public ReactionDto(String snowflakeMessage, String snowflakeAuthor, long idEmoji) {
        this.snowflakeMessage = snowflakeMessage;
        this.snowflakeAuthor = snowflakeAuthor;
        this.idEmoji = idEmoji;
    }

    protected record ReactionDtoPK(String snowflakeMessage, String snowflakeAuthor, long idEmoji) {
        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }

            if (other instanceof ReactionDtoPK(String otherSnowflakeMessage, String otherSnowflakeAuthor, long otherIdEmoji)) {
                return this.snowflakeMessage.equals(otherSnowflakeMessage) && this.snowflakeAuthor.equals(otherSnowflakeAuthor)
                        && this.idEmoji == otherIdEmoji;
            }

            return false;
        }
    }
}
