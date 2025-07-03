package org;

import jakarta.persistence.Table;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.dto.UserDto;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;

public class UserHandler {
    public UserHandler() {}

    public void refreshUser(@NotNull MessageReceivedEvent event) {
        Session session = Main.DATABASE_SESSION_FACTORY.openSession();

        String snowflake = event.getMessage().getAuthor().getId();

        String userTableName = UserDto.class.getAnnotation(Table.class).name();
        String sql = "SELECT * FROM " + userTableName + " WHERE snowflake_user = :snowflake";
        boolean userNotFound = session.createNativeQuery(sql, UserDto.class)
                .setParameter("snowflake", snowflake)
                .getResultList()
                .isEmpty();

        if (userNotFound) {
            Transaction transaction = session.beginTransaction();

            UserDto newUser = new UserDto();
            newUser.setSnowflakeUser(snowflake);
            session.persist(newUser);

            transaction.commit();
        }

        session.close();
    }
}
