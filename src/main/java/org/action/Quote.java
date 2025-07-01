package org.action;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.Helper;
import org.Modifier;
import org.parser.ChatCommand;

import java.util.EnumMap;
import java.util.Map;

public class Quote extends ActionHandler {
    private enum ActionModifier { TYPE, TAG, ORDER, COUNT, VALUE }

    private enum TypeArgument { GET, NEW_QUOTE, NEW_TAG }
    private enum OrderArgument { RANDOM, NEWEST, OLDEST }
    private enum CountArgument { ALL }

    private static final Action action = Action.QUOTE;
    private static final Map<ActionModifier, Modifier<? extends Enum<?>>> ACTION_MODIFIERS =
            new EnumMap<>(ActionModifier.class);

    static {
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.TYPE,
                new Modifier<>(ActionModifier.TYPE, TypeArgument.class, TypeArgument.GET, false, false, false)
        );
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.TAG,
                new Modifier<>(ActionModifier.TAG, Helper.EmptyEnum.class, null, true, false, false)
        );
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.ORDER,
                new Modifier<>(ActionModifier.ORDER, OrderArgument.class, OrderArgument.RANDOM, false, false, false)
        );
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.COUNT,
                new Modifier<>(ActionModifier.COUNT, CountArgument.class, 5, false, false, true)
        );
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.VALUE,
                new Modifier<>(ActionModifier.VALUE, Helper.EmptyEnum.class, null, true, false, false)
        );
    }

    @Override
    public Action getAction() {
        return Quote.action;
    }

    @Override
    public void executeAction(MessageReceivedEvent event, ChatCommand chatCommand) {
        MessageChannel channel = event.getChannel();
        channel.sendMessage("test").queue();
    }
}
