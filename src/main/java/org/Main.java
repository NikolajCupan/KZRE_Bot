package org;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.action.Quote;

public class Main extends ListenerAdapter {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();

        JDA api = JDABuilder.createDefault(dotenv.get("TOKEN")).enableIntents(GatewayIntent.MESSAGE_CONTENT).build();
        api.addEventListener(new MessagesListener());

        MessagesListener.registerActionHandler(new Quote());
    }
}
