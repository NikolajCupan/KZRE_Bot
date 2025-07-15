package org.utility;

import org.exception.InvalidActionArgumentException;

import java.text.MessageFormat;

public class Check {
    public static<T extends Enum<?>, U extends Enum<?>> boolean enumeratorIsFromEnum(
            Class<T> enumClass, U testedEnumerator, boolean throwIfFalse
    ) {
        return Check.throwIfFalse(
                Check.enumeratorIsFromEnum(enumClass, testedEnumerator), throwIfFalse, "Enumerator is not from enum", null
        );
    }

    public static<T> boolean isWholeNumber(T testedValue, boolean throwIfFalse, String valueName) {
        return Check.throwIfFalse(
                Check.isWholeNumber(testedValue), throwIfFalse, "is not a whole number", valueName
        );
    }

    public static<T> boolean isDecimalNumber(T testedValue, boolean throwIfFalse, String valueName) {
        return Check.throwIfFalse(
                Check.isDecimalNumber(testedValue), throwIfFalse, "is not a decimal number", valueName
        );
    }

    public static<T extends Number, U extends Number> boolean isInRange(
            T testedValue, U minInclusive, U maxInclusive, boolean throwIfFalse, String valueName
    ) {
        if (testedValue instanceof Byte || testedValue instanceof Short || testedValue instanceof Integer || testedValue instanceof Long) {
            return Check.throwIfFalse(
                    Check.isLongInRange(testedValue, minInclusive.longValue(), maxInclusive.longValue()),
                    throwIfFalse, MessageFormat.format("is not in range <{0}; {1}>", minInclusive, maxInclusive), valueName
            );
        } else {
            return Check.throwIfFalse(
                    Check.isDoubleInRange(testedValue, minInclusive.doubleValue(), maxInclusive.doubleValue()),
                    throwIfFalse, MessageFormat.format("is not in range <{0}; {1}>", minInclusive, maxInclusive), valueName
            );
        }
    }

    public static<T> boolean isLongInRange(
            Object testedValue, T minInclusive, T maxInclusive, boolean throwIfFalse, String valueName
    ) {
        return Check.throwIfFalse(
                Check.isLongInRange(testedValue, minInclusive, maxInclusive),
                throwIfFalse, MessageFormat.format("Long value is not in range <{0}; {1}>", minInclusive, maxInclusive), valueName
        );
    }

    public static<T> boolean isDoubleInRange(
            Object testedValue, T minInclusive, T maxInclusive, boolean throwIfFalse, String valueName
    ) {
        return Check.throwIfFalse(
                Check.isDoubleInRange(testedValue, minInclusive, maxInclusive),
                throwIfFalse, MessageFormat.format("Double value is not in range <{0}; {1}>", minInclusive, maxInclusive), valueName
        );
    }

    private static<T extends Enum<?>, U extends Enum<?>> boolean enumeratorIsFromEnum(Class<T> enumClass, U testedEnumerator) {
        for (Enum<?> enumerator : enumClass.getEnumConstants()) {
            if (enumerator.name().equals(testedEnumerator.name())) {
                return true;
            }
        }

        return false;
    }

    private static<T> boolean isWholeNumber(T testedValue) {
        try {
            Long.parseLong(testedValue.toString());
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static<T> boolean isDecimalNumber(T testedValue) {
        try {
            Double.parseDouble(testedValue.toString());
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static<T> boolean isLongInRange(Object testedValue, T minInclusive, T maxInclusive) {
        if (!(minInclusive instanceof Long) || !(maxInclusive instanceof Long)) {
            throw new IllegalArgumentException("Arguments must be type Long");
        }

        long testedValueCasted = Long.parseLong(String.valueOf(testedValue));
        long minValueCasted = (long)minInclusive;
        long maxValueCasted = (long)maxInclusive;

        if (minValueCasted > maxValueCasted) {
            throw new IllegalArgumentException("Min value must be lower or equal to max value");
        }

        return testedValueCasted >= minValueCasted && testedValueCasted <= maxValueCasted;
    }

    private static<T> boolean isDoubleInRange(Object testedValue, T minInclusive, T maxInclusive) {
        if (!(minInclusive instanceof Double) || !(maxInclusive instanceof Double)) {
            throw new IllegalArgumentException("Arguments must be type Double");
        }

        double testedValueCasted = Double.parseDouble(String.valueOf(testedValue));
        double minValueCasted = (double)minInclusive;
        double maxValueCasted = (double)maxInclusive;

        if (minValueCasted > maxValueCasted) {
            throw new IllegalArgumentException("Min value must be lower or equal to max value");
        }

        return testedValueCasted >= minValueCasted && testedValueCasted <= maxValueCasted;
    }

    private static boolean throwIfFalse(boolean result, boolean throwIfFalse, String message, String valueName) {
        if (!result && throwIfFalse) {
            String finalMessage = MessageFormat.format("{0} {1}", (valueName != null && !valueName.isBlank()) ? valueName : "Value", message);
            throw new InvalidActionArgumentException(finalMessage);
        }

        return result;
    }
}
