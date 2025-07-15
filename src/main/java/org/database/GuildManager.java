package org.database;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.Main;
import org.database.dto.GuildDto;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;

public class GuildManager {
    public GuildManager() {}

    public void refreshGuild(@NotNull MessageReceivedEvent event) {
        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            String snowflake = event.getMessage().getGuild().getId();
            if (!GuildDto.guildExists(snowflake, session)) {
                GuildDto newGuild = new GuildDto(snowflake);
                session.persist(newGuild);
            }
        } finally {
            transaction.commit();
            session.close();
        }
    }
}
