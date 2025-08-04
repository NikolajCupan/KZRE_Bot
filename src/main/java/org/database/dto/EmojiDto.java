package org.database.dto;

import jakarta.persistence.*;
import org.hibernate.Session;

import java.util.List;

@Entity
@Table(name = EmojiDto.EMOJI_TABLE_NAME)
public class EmojiDto {
    public static final String EMOJI_TABLE_NAME = "emoji";

    public static final String ID_EMOJI_COLUMN_NAME = "id_emoji";
    public static final String EMOJI_COLUMN_NAME = "emoji";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = EmojiDto.ID_EMOJI_COLUMN_NAME, unique = true, nullable = false)
    private long idEmoji;

    @Column(name = EmojiDto.EMOJI_COLUMN_NAME, nullable = false)
    private String emoji;

    public EmojiDto(String emoji) {
        this.emoji = emoji;
    }

    public static boolean emojiExists(String emoji, Session session) {
        String sql = "SELECT * FROM " + EmojiDto.EMOJI_TABLE_NAME + " WHERE "
                + EmojiDto.EMOJI_COLUMN_NAME + " = :p_emoji";

        return !session.createNativeQuery(sql, EmojiDto.class)
                .setParameter("p_emoji", emoji)
                .getResultList()
                .isEmpty();
    }

    public static EmojiDto getEmojiDtoByIdEmoji(long idEmoji, Session session) {
        String sql = "SELECT * FROM " + EmojiDto.EMOJI_TABLE_NAME + " WHERE "
                + EmojiDto.ID_EMOJI_COLUMN_NAME + " = :p_idEmoji";

        return session.createNativeQuery(sql, EmojiDto.class)
                .setParameter("p_idEmoji", idEmoji)
                .getSingleResultOrNull();
    }

    public static List<EmojiDto> getEmojiDtosByEmoji(String emoji, Session session) {
        String sql = "SELECT * FROM " + EmojiDto.EMOJI_TABLE_NAME + " WHERE "
                + EmojiDto.EMOJI_COLUMN_NAME + " = :p_emoji";

        return session.createNativeQuery(sql, EmojiDto.class)
                .setParameter("p_emoji", emoji)
                .getResultList();
    }

    public long getIdEmoji() {
        return this.idEmoji;
    }
}
