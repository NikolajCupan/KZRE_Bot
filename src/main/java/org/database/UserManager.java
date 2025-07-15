package org.database;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.Main;
import org.database.dto.UserDto;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;

public class UserManager {
    public UserManager() {}

    public void refreshUser(@NotNull MessageReceivedEvent event) {
        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            String snowflake = event.getMessage().getAuthor().getId();
            if (!UserDto.userExists(snowflake, session)) {
                UserDto newUser = new UserDto(snowflake);
                session.persist(newUser);
            }
        } finally {
            transaction.commit();
            session.close();
        }
    }
}
