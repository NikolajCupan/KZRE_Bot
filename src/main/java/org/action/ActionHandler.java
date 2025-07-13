package org.action;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.Helper;
import org.Modifier;
import org.ProcessingContext;
import org.parser.ChatCommand;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ActionHandler {
    public enum GlobalActionModifier { VERBOSE, DEBUG }

    protected static final Map<Enum<?>, Modifier<? extends Enum<?>, ? extends Number>> ACTION_MODIFIERS =
            new HashMap<>();

    static {
        ActionHandler.ACTION_MODIFIERS.put(
                ActionHandler.GlobalActionModifier.VERBOSE,
                new Modifier<>(Helper.EmptyEnum.class, null, false, false, false, true, null, null)
        );
        ActionHandler.ACTION_MODIFIERS.put(
                ActionHandler.GlobalActionModifier.DEBUG,
                new Modifier<>(Helper.EmptyEnum.class, null, false, false, false, true, null, null)
        );
    }

    protected abstract Class<? extends Enum<?>> getActionModifierEnumClass();
    public abstract Action getAction();
    public abstract void executeAction(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext);

    public Set<String> getActionPossibleModifiers() {
        Set<String> possibleModifiers = new HashSet<>();
        for (GlobalActionModifier enumerator : GlobalActionModifier.class.getEnumConstants()) {
            possibleModifiers.add(enumerator.toString());
        }

        Class<? extends Enum<?>> actionModifierEnumClassDerived = this.getActionModifierEnumClass();
        for (Enum<?> enumerator : actionModifierEnumClassDerived.getEnumConstants()) {
            possibleModifiers.add(enumerator.toString());
        }

        return possibleModifiers;
    }

    public Enum<?> getActionModifierEnumerator(String strModifier) {
        try {
            return Enum.valueOf(GlobalActionModifier.class, strModifier);
        } catch (IllegalArgumentException ignored) {
            return Enum.valueOf((Class)this.getActionModifierEnumClass(), strModifier);
        }
    }

    public Modifier<? extends Enum<?>, ? extends Number> getModifier(Enum<?> actionModifier) {
        return ActionHandler.ACTION_MODIFIERS.get(actionModifier);
    }

    @Override
    public String toString() {
        return this.getAction().toString();
    }
}
