package org;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import io.github.cdimascio.dotenv.Dotenv;
import reactor.core.publisher.Mono;

public class Main {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();

        DiscordClient client = DiscordClient.create(dotenv.get("TOKEN"));
        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) -> Mono.empty());
        login.block();
    }
}
