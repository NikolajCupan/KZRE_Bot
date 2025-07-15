package org.action;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.parsing.Modifier;
import org.utility.Helper;
import org.utility.ProcessingContext;
import org.parsing.ChatCommand;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ActionHandler {
    protected static final Map<Enum<?>, Modifier<? extends Enum<?>, ? extends Number>> ACTION_MODIFIERS =
            new HashMap<>();

    static {
        ActionHandler.ACTION_MODIFIERS.put(
                ActionHandler.GlobalActionModifier.VERBOSE,
                new Modifier<>(Helper.EmptyEnum.class, null, false, false, false, true, null, null)
        );
    }

    public abstract void executeAction(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext);
    protected abstract Class<? extends Enum<?>> getModifierEnumClass();

    public Set<String> getPossibleModifiers() {
        Set<String> possibleModifiers = new HashSet<>();
        for (GlobalActionModifier enumerator : GlobalActionModifier.class.getEnumConstants()) {
            possibleModifiers.add(enumerator.toString());
        }

        Class<? extends Enum<?>> actionModifierEnumClassDerived = this.getModifierEnumClass();
        for (Enum<?> enumerator : actionModifierEnumClassDerived.getEnumConstants()) {
            possibleModifiers.add(enumerator.toString());
        }

        return possibleModifiers;
    }

    public Enum<?> getModifierEnumerator(String strModifier) {
        try {
            return Enum.valueOf(GlobalActionModifier.class, strModifier);
        } catch (IllegalArgumentException ignored) {
            //noinspection unchecked, rawtypes
            return Enum.valueOf((Class)this.getModifierEnumClass(), strModifier);
        }
    }

    public Modifier<? extends Enum<?>, ? extends Number> getModifier(Enum<?> actionModifier) {
        return ActionHandler.ACTION_MODIFIERS.get(actionModifier);
    }

    public enum GlobalActionModifier { VERBOSE }
}
