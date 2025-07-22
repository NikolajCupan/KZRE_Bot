package org.utility;

import org.exception.InvalidActionArgumentException;
import java.text.MessageFormat;

public class Check {
    public static boolean isBooleanFalse(boolean testedValue, boolean throwIfFalse, String customValueName, String customMessage) {
        return Check.throwIfFalse(
                Check.isBooleanFalse(testedValue), throwIfFalse, customValueName, "cannot be true", customMessage
        );
    }

    public static boolean isNotBlank(String testedValue, boolean throwIfFalse, String customValueName, String customMessage) {
        return Check.throwIfFalse(
                Check.isNotBlank(testedValue), throwIfFalse, customValueName, "cannot be blank", customMessage
        );
    }

    public static<T extends Enum<?>, U extends Enum<?>> boolean enumeratorIsFromEnum(
            U testedValue, Class<T> enumClass, boolean throwIfFalse, String customValueName, String customMessage
    ) {
        return Check.throwIfFalse(
                Check.enumeratorIsFromEnum(testedValue, enumClass), throwIfFalse,
                customValueName, "Enumerator is not from enum", customMessage
        );
    }

    public static<T> boolean isWholeNumber(T testedValue, boolean throwIfFalse, String customValueName, String customMessage) {
        return Check.throwIfFalse(
                Check.isWholeNumber(testedValue), throwIfFalse, customValueName, "is not a whole number", customMessage
        );
    }

    public static<T> boolean isDecimalNumber(T testedValue, boolean throwIfFalse, String customValueName, String customMessage) {
        return Check.throwIfFalse(
                Check.isDecimalNumber(testedValue), throwIfFalse, customValueName, "is not a decimal number", customMessage
        );
    }

    public static<T extends Number, U extends Number> boolean isInRange(
            T testedValue, U minInclusive, U maxInclusive, boolean throwIfFalse, String customValueName, String customMessage
    ) {
        if (testedValue instanceof Byte || testedValue instanceof Short || testedValue instanceof Integer || testedValue instanceof Long) {
            return Check.throwIfFalse(
                    Check.isLongInRange(testedValue, minInclusive.longValue(), maxInclusive.longValue()), throwIfFalse,
                    customValueName, MessageFormat.format("is not in range <{0}; {1}>", minInclusive, maxInclusive), customMessage
            );
        } else {
            return Check.throwIfFalse(
                    Check.isDoubleInRange(testedValue, minInclusive.doubleValue(), maxInclusive.doubleValue()), throwIfFalse,
                    customValueName, MessageFormat.format("is not in range <{0}; {1}>", minInclusive, maxInclusive), customMessage
            );
        }
    }

    public static<T> boolean isLongInRange(
            Object testedValue, T minInclusive, T maxInclusive, boolean throwIfFalse, String customValueName, String customMessage
    ) {
        return Check.throwIfFalse(
                Check.isLongInRange(testedValue, minInclusive, maxInclusive), throwIfFalse,
                customValueName, MessageFormat.format("is not in range <{0}; {1}>", minInclusive, maxInclusive), customMessage
        );
    }

    public static<T> boolean isDoubleInRange(
            Object testedValue, T minInclusive, T maxInclusive, boolean throwIfFalse, String customValueName, String customMessage
    ) {
        return Check.throwIfFalse(
                Check.isDoubleInRange(testedValue, minInclusive, maxInclusive), throwIfFalse,
                customValueName, MessageFormat.format("is not in range <{0}; {1}>", minInclusive, maxInclusive), customMessage
        );
    }

    private static boolean isBooleanFalse(boolean testedValue) {
        return !testedValue;
    }

    private static boolean isNotBlank(String testedValue) {
        return !testedValue.isBlank();
    }

    private static<T extends Enum<?>, U extends Enum<?>> boolean enumeratorIsFromEnum(U testedValue, Class<T> enumClass) {
        for (Enum<?> enumerator : enumClass.getEnumConstants()) {
            if (enumerator.name().equals(testedValue.name())) {
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

    private static boolean throwIfFalse(boolean testResult, boolean throwIfFalse, String customValueName, String defaultMessage, String customMessage) {
        if (!testResult && throwIfFalse) {
            String finalValueName = (customValueName != null && !customValueName.isBlank()) ? customValueName : "Value";
            String finalMessage = (customMessage != null && !customMessage.isBlank()) ? customMessage : defaultMessage;

            throw new InvalidActionArgumentException(finalValueName + " " + finalValueName);
        }

        return testResult;
    }
}
