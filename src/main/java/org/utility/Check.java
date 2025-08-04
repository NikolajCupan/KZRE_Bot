package org.utility;

import org.exception.InvalidActionArgumentException;
import java.text.MessageFormat;
import java.util.Collection;

public class Check {
    public static<T> boolean isEmpty(Collection<T> testedCollection, boolean throwIfTestFails, String customValueName) {
        return Check.throwIfFalse(
                Check.isEmpty(testedCollection), throwIfTestFails, customValueName, "found: " + Helper.stringifyCollection(testedCollection), null
        );
    }

    public static boolean isBooleanTrue(boolean testedValue, boolean throwIfTestFails, String customValueName, String customMessage) {
        return Check.throwIfFalse(
                Check.isBooleanTrue(testedValue), throwIfTestFails, customValueName, "cannot be false", customMessage
        );
    }

    public static boolean isBooleanFalse(boolean testedValue, boolean throwIfTestFails, String customValueName, String customMessage) {
        return Check.throwIfFalse(
                Check.isBooleanFalse(testedValue), throwIfTestFails, customValueName, "cannot be true", customMessage
        );
    }

    public static boolean isNotBlank(String testedValue, boolean throwIfTestFails, String customValueName, String customMessage) {
        return Check.throwIfFalse(
                Check.isNotBlank(testedValue), throwIfTestFails, customValueName, "cannot be blank", customMessage
        );
    }

    public static<T extends Enum<?>, U extends Enum<?>> boolean enumeratorIsFromEnum(
            U testedValue, Class<T> enumClass, boolean throwIfTestFails, String customValueName, String customMessage
    ) {
        return Check.throwIfFalse(
                Check.enumeratorIsFromEnum(testedValue, enumClass), throwIfTestFails,
                customValueName, "Enumerator is not from enum", customMessage
        );
    }

    public static<T> boolean isWholeNumber(T testedValue, boolean throwIfTestFails, String customValueName, String customMessage) {
        return Check.throwIfFalse(
                Check.isWholeNumber(testedValue), throwIfTestFails, customValueName, "is not a whole number", customMessage
        );
    }

    public static<T> boolean isDecimalNumber(T testedValue, boolean throwIfTestFails, String customValueName, String customMessage) {
        return Check.throwIfFalse(
                Check.isDecimalNumber(testedValue), throwIfTestFails, customValueName, "is not a decimal number", customMessage
        );
    }

    public static<T extends Number, U extends Number> boolean isInRange(
            T testedValue, U minInclusive, U maxInclusive, boolean throwIfTestFails, String customValueName, String customMessage
    ) {
        if (testedValue instanceof Byte || testedValue instanceof Short || testedValue instanceof Integer || testedValue instanceof Long) {
            return Check.throwIfFalse(
                    Check.isLongInRange(testedValue, minInclusive.longValue(), maxInclusive.longValue()), throwIfTestFails,
                    customValueName, MessageFormat.format("is not in range <{0}; {1}>", minInclusive, maxInclusive), customMessage
            );
        } else {
            return Check.throwIfFalse(
                    Check.isDoubleInRange(testedValue, minInclusive.doubleValue(), maxInclusive.doubleValue()), throwIfTestFails,
                    customValueName, MessageFormat.format("is not in range <{0}; {1}>", minInclusive, maxInclusive), customMessage
            );
        }
    }

    public static<T> boolean isLongInRange(
            Object testedValue, T minInclusive, T maxInclusive, boolean throwIfTestFails, String customValueName, String customMessage
    ) {
        return Check.throwIfFalse(
                Check.isLongInRange(testedValue, minInclusive, maxInclusive), throwIfTestFails,
                customValueName, MessageFormat.format("is not in range <{0}; {1}>", minInclusive, maxInclusive), customMessage
        );
    }

    public static<T> boolean isDoubleInRange(
            Object testedValue, T minInclusive, T maxInclusive, boolean throwIfTestFails, String customValueName, String customMessage
    ) {
        return Check.throwIfFalse(
                Check.isDoubleInRange(testedValue, minInclusive, maxInclusive), throwIfTestFails,
                customValueName, MessageFormat.format("is not in range <{0}; {1}>", minInclusive, maxInclusive), customMessage
        );
    }

    private static<T> boolean isEmpty(Collection<T> testedCollection) {
        return testedCollection.isEmpty();
    }

    private static boolean isBooleanTrue(boolean testedValue) {
        return testedValue;
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

    private static boolean throwIfFalse(boolean testResult, boolean throwIfTestFails, String customValueName, String defaultMessage, String customMessage) {
        if (!testResult && throwIfTestFails) {
            String finalValueName = (customValueName != null) ? customValueName : "Value";
            String finalMessage = (customMessage != null) ? customMessage : defaultMessage;

            throw new InvalidActionArgumentException(finalValueName + " " + finalMessage);
        }

        return testResult;
    }
}
