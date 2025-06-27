package org;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.action.Quote;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.text.MessageFormat;

public class Main {
    public static Dotenv DOTENV;
    public static SessionFactory SESSION_FACTORY;

    public static void main(String[] args) {
        Main.DOTENV = Dotenv.load();

        Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
        configuration.setProperty("hibernate.hikari.dataSource.url", MessageFormat.format("jdbc:mysql://{0}:{1}/{2}",
                Main.DOTENV.get("DATABASE_HOST"), Main.DOTENV.get("DATABASE_PORT"), Main.DOTENV.get("DATABASE_NAME")));
        configuration.setProperty("hibernate.hikari.dataSource.user", Main.DOTENV.get("DATABASE_USER"));
        configuration.setProperty("hibernate.hikari.dataSource.password", Main.DOTENV.get("DATABASE_PASSWORD"));
        Main.SESSION_FACTORY = configuration.buildSessionFactory();

        JDA api = JDABuilder.createDefault(Main.DOTENV.get("TOKEN")).enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();

        MessagesListener messagesListener = new MessagesListener();
        api.addEventListener(messagesListener);
        messagesListener.registerActionHandler(new Quote());
    }
}
