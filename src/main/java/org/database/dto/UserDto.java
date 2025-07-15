package org.database.dto;

import jakarta.persistence.*;
import org.hibernate.Session;

@Entity
@Table(name = UserDto.USER_TABLE_NAME)
public class UserDto {
    public static final String USER_TABLE_NAME = "user";

    public static final String SNOWFLAKE_USER_COLUMN_NAME = "snowflake_user";

    @Id
    @Column(name = UserDto.SNOWFLAKE_USER_COLUMN_NAME, unique = true, nullable = false)
    private String snowflakeUser;

    public UserDto() {}

    public UserDto(String snowflakeUser) {
        this.snowflakeUser = snowflakeUser;
    }

    public static UserDto getUser(String snowflakeUser, Session session) {
        String sql = "SELECT * FROM " + UserDto.USER_TABLE_NAME + " WHERE "
                + UserDto.SNOWFLAKE_USER_COLUMN_NAME + " = :p_snowflakeUser";

        return session.createNativeQuery(sql, UserDto.class)
                .setParameter("p_snowflakeUser", snowflakeUser)
                .getSingleResultOrNull();
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
