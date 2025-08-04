package org.database.dto;

import jakarta.persistence.*;
import org.hibernate.Session;

import java.util.List;

@Entity
@Table(name = GuildEmojiDto.GUILD_EMOJI_TABLE_NAME)
@IdClass(GuildEmojiDto.GuildEmojiDtoPK.class)
public class GuildEmojiDto {
    public static final String GUILD_EMOJI_TABLE_NAME = "guild_emoji";

    public static final String ID_EMOJI_COLUMN_NAME = "id_emoji";
    public static final String SNOWFLAKE_GUILD_COLUMN_NAME = "snowflake_guild";
    public static final String SNOWFLAKE_EMOJI_COLUMN_NAME = "snowflake_emoji";
    public static final String ANIMATED_COLUMN_NAME = "animated";

    @Id
    @Column(name = GuildEmojiDto.ID_EMOJI_COLUMN_NAME, nullable = false)
    private long idEmoji;

    @Id
    @Column(name = GuildEmojiDto.SNOWFLAKE_GUILD_COLUMN_NAME, nullable = false)
    private String snowflakeGuild;

    @Column(name = GuildEmojiDto.SNOWFLAKE_EMOJI_COLUMN_NAME, nullable = false)
    private String snowflakeEmoji;

    @Column(name = GuildEmojiDto.ANIMATED_COLUMN_NAME, nullable = false)
    private boolean animated;

    public GuildEmojiDto(long idEmoji, String snowflakeGuild, String snowflakeEmoji, boolean animated) {
        this.idEmoji = idEmoji;
        this.snowflakeGuild = snowflakeGuild;
        this.snowflakeEmoji = snowflakeEmoji;
        this.animated = animated;
    }

    public static boolean guildEmojiExists(String emoji, String snowflakeGuild, Session session) {
        List<Long> emojiDtosIds = EmojiDto.getEmojiDtosByEmoji(emoji, session).stream()
                .map(EmojiDto::getIdEmoji)
                .toList();
        String sql = "SELECT * FROM " + GuildEmojiDto.GUILD_EMOJI_TABLE_NAME + " WHERE "
                + GuildEmojiDto.SNOWFLAKE_GUILD_COLUMN_NAME + " = :p_snowflakeGuild AND "
                + GuildEmojiDto.ID_EMOJI_COLUMN_NAME + " IN :p_idsEmojis";

        return !session.createNativeQuery(sql, GuildEmojiDto.class)
                .setParameter("p_snowflakeGuild", snowflakeGuild)
                .setParameter("p_idsEmojis", emojiDtosIds)
                .getResultList()
                .isEmpty();
    }

    public static GuildEmojiDto getGuildEmojiDtoByEmoji(String emoji, String snowflakeGuild, Session session) {
        List<Long> emojiDtosIds = EmojiDto.getEmojiDtosByEmoji(emoji, session).stream()
                .map(EmojiDto::getIdEmoji)
                .toList();
        String sql = "SELECT * FROM " + GuildEmojiDto.GUILD_EMOJI_TABLE_NAME + " WHERE "
                + GuildEmojiDto.SNOWFLAKE_GUILD_COLUMN_NAME + " = :p_snowflakeGuild AND "
                + GuildEmojiDto.ID_EMOJI_COLUMN_NAME + " IN :p_idsEmojis";

        return session.createNativeQuery(sql, GuildEmojiDto.class)
                .setParameter("p_snowflakeGuild", snowflakeGuild)
                .setParameter("p_idsEmojis", emojiDtosIds)
                .getSingleResult();
    }

    public long getIdEmoji() {
        return this.idEmoji;
    }

    protected record GuildEmojiDtoPK(long idEmoji, String snowflakeGuild) {
        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }

            if (other instanceof GuildEmojiDtoPK(long otherIdEmoji, String otherSnowflakeGuild)) {
                return this.idEmoji == otherIdEmoji && this.snowflakeGuild.equals(otherSnowflakeGuild);
            }

            return false;
        }
    }
}
