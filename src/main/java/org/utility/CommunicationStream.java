package org.utility;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.awt.*;
import java.util.List;

public class CommunicationStream {
    public static void returnResponse(MessageChannel channel, String result) {
        if (result != null && !result.isBlank()) {
            channel.sendMessage(result).queue();
        }
    }

    public static void returnResponseVerbose(MessageChannel channel, EmbedBuilder result) {
        if (!result.isEmpty()) {
            channel.sendMessageEmbeds(result.build()).queue();
        }
    }

    public static String processRequestResult(ProcessingContext processingContext) {
        if (processingContext.hasMessageOfType(ProcessingContext.MessageType.PARSING_ERROR)) {
            return processingContext.getMessages(List.of(ProcessingContext.MessageType.PARSING_ERROR))
                    .getFirst().message();
        } else if (processingContext.hasMessageOfType(ProcessingContext.MessageType.ERROR)) {
            return processingContext.getMessages(List.of(ProcessingContext.MessageType.ERROR))
                    .getFirst().message();
        } else {
            List<ProcessingContext.Message> messages = processingContext.getMessages(List.of(ProcessingContext.MessageType.INFO_RESULT, ProcessingContext.MessageType.SUCCESS_RESULT));
            if (!messages.isEmpty()) {
                return messages.getFirst().message();
            }
        }

        return null;
    }

    public static EmbedBuilder processRequestResultVerbose(ProcessingContext processingContext) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.BLACK);

        if (processingContext.hasMessageOfType(ProcessingContext.MessageType.PARSING_ERROR)) {
            processingContext.getMessages(List.of(ProcessingContext.MessageType.PARSING_ERROR)).forEach(element ->
                    embedBuilder.addField(element.messageType().toString(), element.message(), false)
            );
            return embedBuilder;
        } else if (processingContext.hasMessageOfType(ProcessingContext.MessageType.ERROR)) {
            processingContext.getMessages(List.of(ProcessingContext.MessageType.ERROR)).forEach(element ->
                    embedBuilder.addField(element.messageType().toString(), element.message(), false)
            );
            return embedBuilder;
        }


        processingContext.getMessages(List.of(ProcessingContext.MessageType.INFO_RESULT, ProcessingContext.MessageType.SUCCESS_RESULT)).forEach(element ->
                embedBuilder.addField(element.messageType().toString(), element.message(), false)
        );

        processingContext.getMessages(List.of(ProcessingContext.MessageType.PARSING_WARNING, ProcessingContext.MessageType.WARNING, ProcessingContext.MessageType.FORCE_SWITCH_WARNING)).forEach(element ->
                embedBuilder.addField(element.messageType().toString(), element.message(), false)
        );

        return embedBuilder;
    }
}
