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
                new Modifier<>(ActionModifier.TYPE, List.of(TypeArgument.GET, TypeArgument.NEW_QUOTE, TypeArgument.NEW_TAG), TypeArgument.GET, Helper.TypedValue.Type.ENUMERATOR, false, false, false)
        );
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.TAG,
                new Modifier<>(ActionModifier.TAG, List.of(), null, Helper.TypedValue.Type.NULL, true, false, false)
        );
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.ORDER,
                new Modifier<>(ActionModifier.ORDER, List.of(OrderArgument.RANDOM, OrderArgument.NEWEST, OrderArgument.OLDEST), OrderArgument.RANDOM, Helper.TypedValue.Type.ENUMERATOR, false, false, false)
        );
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.COUNT,
                new Modifier<>(ActionModifier.COUNT, List.of(CountArgument.ALL), 5, Helper.TypedValue.Type.WHOLE_NUMBER, false, false, true)
        );
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.VALUE,
                new Modifier<>(ActionModifier.VALUE, List.of(), null, Helper.TypedValue.Type.NULL, true, false, false)
        );
    }

    @Override
    public Action getAction() {
        return Quote.action;
    }

    @Override
    public void executeAction(MessageReceivedEvent event, ChatCommand chatCommand) {
        MessageChannel channel = event.getChannel();

        System.out.println(ActionModifier.TYPE + " " + chatCommand.getArgument(Quote.ACTION_MODIFIERS.get(ActionModifier.TYPE)));
        System.out.println(ActionModifier.TAG + " " + chatCommand.getArgument(Quote.ACTION_MODIFIERS.get(ActionModifier.TAG)));
        System.out.println(ActionModifier.ORDER + " " + chatCommand.getArgument(Quote.ACTION_MODIFIERS.get(ActionModifier.ORDER)));
        System.out.println(ActionModifier.COUNT + " " + chatCommand.getArgument(Quote.ACTION_MODIFIERS.get(ActionModifier.COUNT)));
        System.out.println(ActionModifier.VALUE + " " + chatCommand.getArgument(Quote.ACTION_MODIFIERS.get(ActionModifier.VALUE)));

        channel.sendMessage("test").queue();
    }
}
