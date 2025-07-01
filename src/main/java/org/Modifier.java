package org;

import java.util.HashSet;
import java.util.Set;

public class Modifier<T extends Enum<T>, U extends Enum<U>> {
    private final T modifier;

    private final Set<String> possibleArguments;
    private final Class<U> possibleArgumentsType;

    private final String defaultArgument;
    private final Helper.TypedValue.Type defaultArgumentType;

    private final boolean argumentCanBeAnyString;
    private final boolean argumentCanBeDecimalNumber;
    private final boolean argumentCanBeWholeNumber;

    public <V> Modifier(
            T modifier,
            Class<U> enumClass,
            V defaultArgument,
            boolean argumentCanBeAnyString,
            boolean argumentCanBeDecimalNumber,
            boolean argumentCanBeWholeNumber
    ) {
        this.modifier = modifier;

        this.possibleArguments = new HashSet<>();
        for (Enum<?> possibleArgument : enumClass.getEnumConstants()) {
            this.possibleArguments.add(possibleArgument.toString());
        }
        this.possibleArgumentsType = enumClass;

        this.defaultArgument = defaultArgument == null ? "" : String.valueOf(defaultArgument);
        if (defaultArgument == null) {
            this.defaultArgumentType = Helper.TypedValue.Type.NULL;
        } else if (defaultArgument instanceof Enum<?>) {
            if (!Helper.enumeratorIsFromEnum(enumClass, (Enum<?>)defaultArgument)) {
                throw new IllegalArgumentException("If default argument is an enumerator, it must be from the possible arguments enum");
            }

            this.defaultArgumentType = Helper.TypedValue.Type.ENUMERATOR;
        } else if (Helper.isWholeNumber(defaultArgument)) {
            this.defaultArgumentType = Helper.TypedValue.Type.WHOLE_NUMBER;
        } else if (Helper.isDecimalNumber(defaultArgument)) {
            this.defaultArgumentType = Helper.TypedValue.Type.DECIMAL_NUMBER;
        } else {
            this.defaultArgumentType = Helper.TypedValue.Type.STRING;
        }

        this.argumentCanBeAnyString = argumentCanBeAnyString;
        this.argumentCanBeDecimalNumber = argumentCanBeDecimalNumber;
        this.argumentCanBeWholeNumber = argumentCanBeWholeNumber;
    }

    public Helper.TypedValue getDefaultArgument() {
        return new Helper.TypedValue(this.defaultArgumentType, this.defaultArgument);
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
        if (this.possibleArguments.contains(chatArgument)) {
            return Helper.TypedValue.Type.ENUMERATOR;
        } else if (this.argumentCanBeWholeNumber && Helper.isWholeNumber(chatArgument)) {
            return Helper.TypedValue.Type.WHOLE_NUMBER;
        } else if (this.argumentCanBeDecimalNumber && Helper.isDecimalNumber(chatArgument)) {
            return Helper.TypedValue.Type.DECIMAL_NUMBER;
        } else {
            return Helper.TypedValue.Type.STRING;
        }
    }

    public T getModifier() {
        return this.modifier;
    }

    public Class<U> getPossibleArgumentsType() {
        return this.possibleArgumentsType;
    }
}
