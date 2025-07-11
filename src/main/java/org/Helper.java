package org;

import org.exception.InvalidArgumentException;
import org.exception.MissingArgumentException;

import java.text.MessageFormat;

public class Helper {
    public static class TypedValue {
        public enum Type { STRING, WHOLE_NUMBER, DECIMAL_NUMBER, ENUMERATOR, SWITCH, NULL }
        public enum Resolution { MODIFIER_MISSING, ARGUMENT_MISSING, ARGUMENT_INVALID, ARGUMENT_VALID }

        private final Helper.TypedValue.Type type;
        private final Helper.TypedValue.Resolution resolution;
        private final String usedValue;
        private final String rawValue;
        private final Class<? extends Enum<?>> enumClass;

        private boolean used;

        public TypedValue(TypedValue.Type type, TypedValue.Resolution resolution, String usedValue, String rawValue, Class<? extends Enum<?>> enumClass) {
            this.type = type;
            this.resolution = resolution;
            this.usedValue = usedValue;
            this.rawValue = rawValue;
            this.enumClass = enumClass;

            this.used = false;
        }

        public Helper.TypedValue.Type getType() {
            return this.type;
        }

        public Helper.TypedValue.Resolution getResolution() {
            return this.resolution;
        }

        public String getUsedValue() {
            return this.usedValue;
        }

        public String getRawValue() {
            return this.rawValue;
        }

        public Class<? extends Enum<?>> getEnumClass() {
            return this.enumClass;
        }

        public boolean isUsed() {
            return this.used;
        }

        public void setUsed() {
            this.used = true;
        }

        public String getStateMessage(String modifierName, boolean includeDefaultValueUsage) {
            String message = "";
            switch (this.resolution) {
                case MODIFIER_MISSING -> message = MessageFormat.format("Modifier \"{0}\" was not found", modifierName);
                case ARGUMENT_MISSING -> message = MessageFormat.format("Argument for modifier \"{0}\" was not found", modifierName);
                case ARGUMENT_INVALID -> message = MessageFormat.format("Argument \"{0}\" for modifier \"{1}\" is not valid", this.rawValue, modifierName);
                case ARGUMENT_VALID -> throw new IllegalStateException(MessageFormat.format("Cannot generate state message when resolution is set to\"{0}\"", this.resolution));
            };

            if (includeDefaultValueUsage && this.type != Type.NULL) {
                message += MessageFormat.format(", default value \"{0}\" was used instead", this.usedValue);
            }

            return message;
        }
    }

    public enum EmptyEnum {}

    public static<T extends Enum<?>, U extends Enum<?>> boolean enumeratorIsFromEnum(Class<T> enumClass, U testedEnumerator) {
        for (Enum<?> enumerator : enumClass.getEnumConstants()) {
            if (enumerator.name().equals(testedEnumerator.name())) {
                return true;
            }
        }

        return false;
    }

    public static<T> boolean isDecimal(T testedValue) {
        try {
            Double.parseDouble(testedValue.toString());
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public static<T> boolean isWholeNumber(T testedValue) {
        try {
            Long.parseLong(testedValue.toString());
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public static<T> boolean isLongInRange(Object testedValue, T minInclusive, T maxInclusive) {
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

    public static<T> boolean isDoubleInRange(Object testedValue, T minInclusive, T maxInclusive) {
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

    public static void failIfBlank(String value, String message) {
        if (value.isBlank()) {
            throw new MissingArgumentException(message);
        }
    }

    public static void failIfOutOfRange(long value, long minInclusive, long maxInclusive, String message) {
        if (minInclusive > maxInclusive) {
            throw new IllegalArgumentException("Min value must be lower or equal to max value");
        }

        if (value < minInclusive || value > maxInclusive) {
            throw new InvalidArgumentException(message);
        }
    }
}
