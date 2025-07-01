package org;

import java.util.List;

public class Modifier<T extends Enum<?>> {
    private final T modifier;
    private final List<String> possibleArguments;

    private final String defaultArgument;
    private final Helper.TypedValue.Type defaultArgumentType;

    private final boolean argumentCanBeAnyString;
    private final boolean argumentCanBeDecimalNumber;
    private final boolean argumentCanBeWholeNumber;

    public <U> Modifier(T modifier, List<String> possibleArguments, U defaultArgument,
            Helper.TypedValue.Type defaultArgumentType, boolean argumentCanBeAnyString,
            boolean argumentCanBeDecimalNumber, boolean argumentCanBeWholeNumber) {
        this.modifier = modifier;
        this.possibleArguments = possibleArguments;

        this.defaultArgument = defaultArgument == null ? "" : String.valueOf(defaultArgument);
        this.defaultArgumentType = defaultArgumentType;

        this.argumentCanBeAnyString = argumentCanBeAnyString;
        this.argumentCanBeDecimalNumber = argumentCanBeDecimalNumber;
        this.argumentCanBeWholeNumber = argumentCanBeWholeNumber;
    }

    public T getModifier() {
        return this.modifier;
    }

    public Helper.TypedValue getDefaultArgument() {
        return new Helper.TypedValue(this.defaultArgumentType, String.valueOf(this.defaultArgument));
    }

    public boolean isPossibleArgument(String chatArgument) {
        if (this.argumentCanBeAnyString) {
            return true;
        } else if (this.argumentCanBeDecimalNumber && Helper.isDecimalNumber(chatArgument)) {
            return true;
        } else if (this.argumentCanBeWholeNumber && Helper.isWholeNumber(chatArgument)) {
            return true;
        }

        return this.possibleArguments.contains(chatArgument);
    }

    public Helper.TypedValue.Type getChatArgumentType(String chatArgument) {
        if (this.argumentCanBeWholeNumber && Helper.isWholeNumber(chatArgument)) {
            return Helper.TypedValue.Type.WHOLE_NUMBER;
        } else if (this.argumentCanBeDecimalNumber && Helper.isDecimalNumber(chatArgument)) {
            return Helper.TypedValue.Type.DECIMAL_NUMBER;
        } else {
            return Helper.TypedValue.Type.STRING;
        }
    }
}
