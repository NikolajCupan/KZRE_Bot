package org.database.dto;

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

    public static void refreshGuild(String snowflakeGuild, Session session) {
        if (!GuildDto.guildExists(snowflakeGuild, session)) {
            GuildDto newGuild = new GuildDto(snowflakeGuild);
            session.persist(newGuild);
        }
    }

    public static boolean guildExists(String snowflakeGuild, Session session) {
        String sql = "SELECT * FROM " + GuildDto.GUILD_TABLE_NAME + " WHERE "
                + GuildDto.SNOWFLAKE_GUILD_COLUMN_NAME + " = :p_snowflakeGuild";

        return !session.createNativeQuery(sql, GuildDto.class)
                .setParameter("p_snowflakeGuild", snowflakeGuild)
                .getResultList()
                .isEmpty();
    }
}
