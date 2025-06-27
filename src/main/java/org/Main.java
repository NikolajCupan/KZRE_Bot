package org;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.action.Quote;

public class Main {
    public static final Dotenv DOTENV = Dotenv.load();

    public static void main(String[] args) {
        JDA api = JDABuilder.createDefault(Main.DOTENV.get("TOKEN")).enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();

        MessagesListener messagesListener = new MessagesListener();
        api.addEventListener(messagesListener);

        messagesListener.registerActionHandler(new Quote());
    }
}
