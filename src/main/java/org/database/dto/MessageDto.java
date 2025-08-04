package org.database.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.Session;

@Entity
@Table(name = MessageDto.MESSAGE_TABLE_NAME)
public class MessageDto {
    public static final String MESSAGE_TABLE_NAME = "message";

    public static final String SNOWFLAKE_MESSAGE_COLUMN_NAME = "snowflake_message";
    public static final String SNOWFLAKE_CHANNEL_COLUMN_NAME = "snowflake_channel";
    public static final String MESSAGE_COLUMN_NAME = "message";

    @Id
    @Column(name = MessageDto.SNOWFLAKE_MESSAGE_COLUMN_NAME, unique = true, nullable = false)
    private String snowflakeMessage;

    @Column(name = MessageDto.SNOWFLAKE_CHANNEL_COLUMN_NAME, nullable = false)
    private String snowflakeChannel;

    @Column(name = MessageDto.MESSAGE_COLUMN_NAME, nullable = false)
    private String message;

    public MessageDto(String snowflakeMessage, String snowflakeChannel, String message) {
        this.snowflakeMessage = snowflakeMessage;
        this.snowflakeChannel = snowflakeChannel;
        this.message = message;
    }

    public static void purgeChannelData(String snowflakeChannel, String snowflakeGuild, Session session) {
        MessageDto.purgeReactions(snowflakeChannel, session);
        MessageDto.purgeEmojis(snowflakeGuild, session);

        String sql = "DELETE FROM " + MessageDto.MESSAGE_TABLE_NAME + " WHERE "
                + MessageDto.SNOWFLAKE_CHANNEL_COLUMN_NAME + " = :p_snowflakeChannel";
        session.createNativeQuery(sql, MessageDto.class)
                .setParameter("p_snowflakeChannel", snowflakeChannel)
                .executeUpdate();
    }

    private static void purgeReactions(String snowflakeChannel, Session session) {
        String innerSql = "SELECT " + MessageDto.SNOWFLAKE_MESSAGE_COLUMN_NAME + " FROM " + MessageDto.MESSAGE_TABLE_NAME
                + " WHERE " + MessageDto.SNOWFLAKE_CHANNEL_COLUMN_NAME + " = :p_snowflakeChannel";
        String sql = "DELETE FROM " + ReactionDto.REACTION_TABLE_NAME + " WHERE "
                + ReactionDto.SNOWFLAKE_MESSAGE_COLUMN_NAME + " IN (" + innerSql + ")";

        session.createNativeQuery(sql, ReactionDto.class)
                .setParameter("p_snowflakeChannel", snowflakeChannel)
                .executeUpdate();
    }

    private static void purgeEmojis(String snowflakeGuild, Session session) {
        String innerSql = "SELECT " + ReactionDto.ID_EMOJI_COLUMN_NAME + " FROM " + ReactionDto.REACTION_TABLE_NAME;

        String guildEmojiSql = "DELETE FROM " + GuildEmojiDto.GUILD_EMOJI_TABLE_NAME + " WHERE "
                + GuildEmojiDto.SNOWFLAKE_GUILD_COLUMN_NAME + " = :p_snowflakeGuild AND "
                + GuildEmojiDto.ID_EMOJI_COLUMN_NAME + " NOT IN (" + innerSql + ")";
        String emojiSql = "DELETE FROM " + EmojiDto.EMOJI_TABLE_NAME + " WHERE "
                + EmojiDto.ID_EMOJI_COLUMN_NAME + " NOT IN (" + innerSql + ")";

        session.createNativeQuery(guildEmojiSql, GuildEmojiDto.class)
                .setParameter("p_snowflakeGuild", snowflakeGuild)
                .executeUpdate();
        session.createNativeQuery(emojiSql, EmojiDto.class).executeUpdate();
    }
}
