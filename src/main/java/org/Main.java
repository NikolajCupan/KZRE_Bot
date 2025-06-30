package org;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.action.Quote;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.text.MessageFormat;

public class Main {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static Dotenv DOTENV;
    public static SessionFactory DATABASE_SESSION_FACTORY;

    public static void main(String[] args) {
        Main.DOTENV = Dotenv.load();

        Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
        configuration.setProperty("hibernate.hikari.dataSource.url", MessageFormat.format("jdbc:mysql://{0}:{1}/{2}",
                Main.DOTENV.get("DATABASE_HOST"), Main.DOTENV.get("DATABASE_PORT"), Main.DOTENV.get("DATABASE_NAME")));
        configuration.setProperty("hibernate.hikari.dataSource.user", Main.DOTENV.get("DATABASE_USER"));
        configuration.setProperty("hibernate.hikari.dataSource.password", Main.DOTENV.get("DATABASE_PASSWORD"));
        Main.DATABASE_SESSION_FACTORY = configuration.buildSessionFactory();
        Main.initializeDatabase();

        JDA api = JDABuilder.createDefault(Main.DOTENV.get("TOKEN")).enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();

        MessagesListener messagesListener = new MessagesListener();
        api.addEventListener(messagesListener);
        messagesListener.registerActionHandler(new Quote());
    }

    public static void initializeDatabase() {
        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        session.doWork(connection -> {
            ScriptRunner scriptRunner = new ScriptRunner(connection);
            scriptRunner.setSendFullScript(false);
            scriptRunner.setStopOnError(true);

            try {
                scriptRunner.runScript(new FileReader(Constants.RESOURCES_PATH.resolve("schema.sql").toString()));
            } catch (Exception ignore) {
                Main.LOGGER.error("Could not run database initialization script");
            }
        });

        transaction.commit();
        session.close();
    }
}
