package org.action;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.Modifier;
import org.ProcessingContext;
import org.parser.ChatCommand;

public abstract class ActionHandler {
    public abstract Action getAction();
    public abstract Enum<?> getActionModifierEnumerator(String strModifier);
    public abstract Modifier<? extends Enum<?>, ? extends Number> getModifier(Enum<?> actionModifier);
    public abstract Class<? extends Enum<?>> getActionModifierEnum();
    public abstract void executeAction(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext);

    @Override
    public String toString() {
        return this.getAction().toString();
    }
}
