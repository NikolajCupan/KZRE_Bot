package org.dto;

import jakarta.persistence.*;
import org.hibernate.Session;

@Entity
@Table(name = GuildDto.GUILD_TABLE_NAME)
public class GuildDto {
    public static final String GUILD_TABLE_NAME = "guild";

    public static final String SNOWFLAKE_GUILD_COLUMN_NAME = "snowflake_guild";

    @Id
    @Column(name = GuildDto.SNOWFLAKE_GUILD_COLUMN_NAME, unique = true, nullable = false)
    private String snowflakeGuild;

    public GuildDto() {}

    public GuildDto(String snowflakeGuild) {
        this.snowflakeGuild = snowflakeGuild;
    }

    public static GuildDto getGuild(Session session, String snowflake) {
        String sql = "SELECT * FROM " + GuildDto.GUILD_TABLE_NAME + " WHERE "
                + GuildDto.SNOWFLAKE_GUILD_COLUMN_NAME + " = :p_snowflake";

        return session.createNativeQuery(sql, GuildDto.class)
                .setParameter("p_snowflake", snowflake)
                .getSingleResultOrNull();
    }

    public static boolean guildExists(Session session, String snowflake) {
        String sql = "SELECT * FROM " + GuildDto.GUILD_TABLE_NAME + " WHERE "
                + GuildDto.SNOWFLAKE_GUILD_COLUMN_NAME + " = :p_snowflake";

        return !session.createNativeQuery(sql, GuildDto.class)
                .setParameter("p_snowflake", snowflake)
                .getResultList()
                .isEmpty();
    }
}
