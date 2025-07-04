package org.dto;

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

    public static UserDto getUser(Session session, String snowflake) {
        String sql = "SELECT * FROM " + UserDto.USER_TABLE_NAME + " WHERE "
                + UserDto.SNOWFLAKE_USER_COLUMN_NAME + " = :p_snowflake";

        return session.createNativeQuery(sql, UserDto.class)
                .setParameter("p_snowflake", snowflake)
                .getSingleResultOrNull();
    }

    public static boolean userExists(Session session, String snowflake) {
        String sql = "SELECT * FROM " + UserDto.USER_TABLE_NAME + " WHERE "
                + UserDto.SNOWFLAKE_USER_COLUMN_NAME + " = :p_snowflake";

        return !session.createNativeQuery(sql, UserDto.class)
                .setParameter("p_snowflake", snowflake)
                .getResultList()
                .isEmpty();
    }
}
