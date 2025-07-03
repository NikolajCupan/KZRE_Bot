package org.dto;

import jakarta.persistence.*;

@Entity
@Table(name = "user")
public class UserDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user", nullable = false)
    private long idUser;

    @Column(name = "snowflake_user", nullable = false)
    private String snowflakeUser;

    public UserDto() {}

    public UserDto(long idUser, String snowflakeUser) {
        this.idUser = idUser;
        this.snowflakeUser = snowflakeUser;
    }

    public void setSnowflakeUser(String snowflakeUser) {
        this.snowflakeUser = snowflakeUser;
    }
}
