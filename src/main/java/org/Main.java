package org;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.action.util.ActionExecution;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utility.Constants;

import java.io.FileReader;
import java.text.MessageFormat;
import java.util.*;

public class Main {
    public static final List<String> COMMAND_LINE_ARGUMENTS;
    public static final Dotenv DOTENV;
    public static final SessionFactory DATABASE_SESSION_FACTORY;
    public static final JDA JDA_API;

    private static final Logger LOGGER;

    static {
        COMMAND_LINE_ARGUMENTS = new ArrayList<>();
        DOTENV = Dotenv.load();

        Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
        configuration.setProperty(
                "hibernate.hikari.dataSource.url",
                MessageFormat.format("jdbc:mysql://{0}:{1}/{2}", Main.DOTENV.get("DATABASE_HOST"), Main.DOTENV.get("DATABASE_PORT"), Main.DOTENV.get("DATABASE_NAME"))
        );
        configuration.setProperty("hibernate.hikari.dataSource.user", Main.DOTENV.get("DATABASE_USER"));
        configuration.setProperty("hibernate.hikari.dataSource.password", Main.DOTENV.get("DATABASE_PASSWORD"));
        DATABASE_SESSION_FACTORY = configuration.buildSessionFactory();
        JDA_API = JDABuilder.createDefault(Main.DOTENV.get("TOKEN"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();

        LOGGER = LoggerFactory.getLogger(Main.class);
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(Main.DOTENV.get("TIME_ZONE")));

        Main.parseCommandLineArguments(args);
        Main.initializeDatabase();

        Main.JDA_API.addEventListener(new ActionExecution());
    }

    private static void parseCommandLineArguments(String[] arguments) {
        Collections.addAll(Main.COMMAND_LINE_ARGUMENTS, arguments);
    }

    private static void initializeDatabase() {
        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            session.doWork(connection -> {
                ScriptRunner scriptRunner = new ScriptRunner(connection);
                scriptRunner.setSendFullScript(false);
                scriptRunner.setStopOnError(true);

                try {
                    scriptRunner.runScript(new FileReader(Constants.RESOURCES_PATH.resolve("schema.sql").toString()));
                } catch (Exception exception) {
                    Main.LOGGER.error("Could not run database initialization script: \"{}\"", exception.getMessage());
                }
            });
        } finally {
            transaction.commit();
            session.close();
        }
    }
}
