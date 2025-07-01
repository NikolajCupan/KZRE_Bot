package org.action;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.Helper;
import org.Modifier;
import org.parser.ChatCommand;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Quote extends ActionHandler {
    private enum ActionModifier {
        TYPE, TAG, ORDER, COUNT, VALUE
    }

    private static final Action action = Action.QUOTE;
    private static final Map<ActionModifier, Modifier<? extends Enum<?>>> ACTION_MODIFIERS = new EnumMap<>(
            ActionModifier.class);

    static {
        Quote.ACTION_MODIFIERS.put(ActionModifier.TYPE, new Modifier<>(ActionModifier.TYPE,
                List.of("GET", "NEW_QUOTE", "NET_TAG"), "GET", Helper.TypedValue.Type.STRING, false, false, false));
        Quote.ACTION_MODIFIERS.put(ActionModifier.TAG,
                new Modifier<>(ActionModifier.TAG, List.of(), null, Helper.TypedValue.Type.NULL, true, false, false));
        Quote.ACTION_MODIFIERS.put(ActionModifier.COUNT, new Modifier<>(ActionModifier.COUNT, List.of("ALL"), 5,
                Helper.TypedValue.Type.WHOLE_NUMBER, false, false, true));
    }

    @Override
    public Action getAction() {
        return Quote.action;
    }

    @Override
    public void executeAction(MessageReceivedEvent event, ChatCommand chatCommand) {
        MessageChannel channel = event.getChannel();

        System.out.println(chatCommand.getArgument(Quote.ACTION_MODIFIERS.get(ActionModifier.TYPE)));
        System.out.println(chatCommand.getArgument(Quote.ACTION_MODIFIERS.get(ActionModifier.TAG)));
        System.out.println(chatCommand.getArgument(Quote.ACTION_MODIFIERS.get(ActionModifier.COUNT)));

        channel.sendMessage("test").queue();
    }
}
