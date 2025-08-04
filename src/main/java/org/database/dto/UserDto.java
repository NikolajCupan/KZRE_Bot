package org.database.dto;

import jakarta.persistence.*;
import org.hibernate.Session;

@Entity
@Table(name = UserDto.USER_TABLE_NAME)
public class UserDto {
    public static final String USER_TABLE_NAME = "user";

    public static final String SNOWFLAKE_USER_COLUMN_NAME = "snowflake_user";
    public static final String BOT_COLUMN_NAME = "bot";

    @Id
    @Column(name = UserDto.SNOWFLAKE_USER_COLUMN_NAME, unique = true, nullable = false)
    private String snowflakeUser;

    @Column(name = UserDto.BOT_COLUMN_NAME, nullable = false)
    public boolean bot;

    public UserDto() {}

    public UserDto(String snowflakeUser, boolean bot) {
        this.snowflakeUser = snowflakeUser;
        this.bot = bot;
    }

    public static void refreshUser(String snowflakeUser, boolean isBot, Session session) {
        if (!UserDto.userExists(snowflakeUser, session)) {
            UserDto newUser = new UserDto(snowflakeUser, isBot);
            session.persist(newUser);
        }
    }

    public static boolean userExists(String snowflakeUser, Session session) {
        String sql = "SELECT * FROM " + UserDto.USER_TABLE_NAME + " WHERE "
                + UserDto.SNOWFLAKE_USER_COLUMN_NAME + " = :p_snowflakeUser";

        return !session.createNativeQuery(sql, UserDto.class)
                .setParameter("p_snowflakeUser", snowflakeUser)
                .getResultList()
                .isEmpty();
    }
}
