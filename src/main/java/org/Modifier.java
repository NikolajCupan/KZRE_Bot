package org;

import java.util.HashSet;
import java.util.Set;

public class Modifier<T extends Enum<T>, U extends Number & Comparable<U>> {
    private final Set<String> possibleArguments;
    private final Class<T> possibleArgumentsEnumClass;

    private final String defaultArgument;
    private final Helper.TypedValue.Type defaultArgumentType;

    private final boolean argumentCanBeAnyString;
    private final boolean argumentCanBeDecimalNumber;
    private final boolean argumentCanBeWholeNumber;

    private final boolean isSwitchModifier;
    private final U minInclusive;
    private final U maxInclusive;

    public <V> Modifier(
            Class<T> possibleArgumentsEnumClass,
            V defaultArgument,
            boolean argumentCanBeAnyString,
            boolean argumentCanBeDecimalNumber,
            boolean argumentCanBeWholeNumber,
            boolean isSwitchModifier,
            U minInclusive,
            U maxInclusive
    ) {
        this.possibleArguments = new HashSet<>();
        for (Enum<T> possibleArgument : possibleArgumentsEnumClass.getEnumConstants()) {
            this.possibleArguments.add(possibleArgument.toString());
        }
        this.possibleArgumentsEnumClass = possibleArgumentsEnumClass;


        this.defaultArgument = defaultArgument == null ? "" : String.valueOf(defaultArgument);
        if (defaultArgument == null) {
            if (isSwitchModifier) {
                this.defaultArgumentType = Helper.TypedValue.Type.SWITCH;
            } else {
                this.defaultArgumentType = Helper.TypedValue.Type.NULL;
            }
        } else if (defaultArgument instanceof Enum<?>) {
            if (!Helper.enumeratorIsFromEnum(possibleArgumentsEnumClass, (Enum<?>)defaultArgument)) {
                throw new IllegalArgumentException("If default argument is an enumerator, it must be from the possible arguments enum");
            }

            this.defaultArgumentType = Helper.TypedValue.Type.ENUMERATOR;
        } else if (Helper.isWholeNumber(defaultArgument)) {
            if (!(minInclusive instanceof Long) || !(defaultArgument instanceof Long)) {
                throw new IllegalArgumentException("If default argument is a whole number, both it and the range values must be of type Long");
            } else if (!Helper.isLongInRange(defaultArgument, minInclusive, maxInclusive)) {
                throw new IllegalArgumentException("Default argument is out of specified range");
            }

            this.defaultArgumentType = Helper.TypedValue.Type.WHOLE_NUMBER;
        } else if (Helper.isDecimal(defaultArgument)) {
            if (!(minInclusive instanceof Double) || !(defaultArgument instanceof Double)) {
                throw new IllegalArgumentException("If default argument is a decimal number, both it and the range values must be of type Double");
            } else if (!Helper.isDoubleInRange(defaultArgument, minInclusive, maxInclusive)) {
                throw new IllegalArgumentException("Default argument is out of specified range");
            }

            this.defaultArgumentType = Helper.TypedValue.Type.DECIMAL_NUMBER;
        } else {
            this.defaultArgumentType = Helper.TypedValue.Type.STRING;
        }


        this.argumentCanBeAnyString = argumentCanBeAnyString;
        this.argumentCanBeDecimalNumber = argumentCanBeDecimalNumber;
        this.argumentCanBeWholeNumber = argumentCanBeWholeNumber;
        this.isSwitchModifier = isSwitchModifier;


        if (minInclusive != null && minInclusive.compareTo(maxInclusive) > 0) {
            throw new IllegalArgumentException("Min value must be lower or equal to max value");
        }
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;


        if (this.isSwitchModifier
                && (this.argumentCanBeAnyString || this.argumentCanBeDecimalNumber || this.argumentCanBeWholeNumber)) {
            throw new IllegalArgumentException("Switch modifier cannot use any string / decimal number / whole number options");
        }
        if (this.isSwitchModifier
                && (this.possibleArgumentsEnumClass != Helper.EmptyEnum.class || this.possibleArgumentsEnumClass.getEnumConstants().length != 0)) {
            throw new IllegalArgumentException("Switch modifier must use EmptyEnum for possible arguments type");
        }
    }

    public Class<T> getPossibleArgumentsEnumClass() {
        return this.possibleArgumentsEnumClass;
    }

    public String getDefaultArgument() {
        return this.defaultArgument;
    }

    public Helper.TypedValue.Type getDefaultArgumentType() {
        return this.defaultArgumentType;
    }

    public boolean isSwitchModifier() {
        return this.isSwitchModifier;
    }

    public boolean isPossibleArgument(String chatArgument) {
        if (this.argumentCanBeAnyString) {
            return true;
        } else if (this.argumentCanBeDecimalNumber && Helper.isDecimal(chatArgument)
                && Helper.isDoubleInRange(chatArgument, this.minInclusive, this.maxInclusive)) {
            return true;
        } else if (this.argumentCanBeWholeNumber && Helper.isWholeNumber(chatArgument)
                && Helper.isLongInRange(chatArgument, this.minInclusive, this.maxInclusive)) {
            return true;
        }

        return this.possibleArguments.contains(chatArgument);
    }

    public Helper.TypedValue.Type getChatArgumentType(String chatArgument) {
        if (this.possibleArguments.contains(chatArgument)) {
            return Helper.TypedValue.Type.ENUMERATOR;
        } else if (this.argumentCanBeWholeNumber && Helper.isWholeNumber(chatArgument)
                && Helper.isLongInRange(chatArgument, this.minInclusive, this.maxInclusive)) {
            return Helper.TypedValue.Type.WHOLE_NUMBER;
        } else if (this.argumentCanBeDecimalNumber && Helper.isDecimal(chatArgument)
                && Helper.isDoubleInRange(chatArgument, this.minInclusive, this.maxInclusive)) {
            return Helper.TypedValue.Type.DECIMAL_NUMBER;
        } else {
            return Helper.TypedValue.Type.STRING;
        }
    }
}
