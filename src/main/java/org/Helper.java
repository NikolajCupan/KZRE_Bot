package org;

public class Helper {
    public static class TypedValue {
        public enum Type { STRING, WHOLE_NUMBER, DECIMAL_NUMBER, ENUMERATOR, NULL }

        private final Type type;
        private final String value;

        public TypedValue(Type type, String value) {
            this.type = type;
            this.value = value;
        }

        public Type getType() {
            return this.type;
        }

        public String getValue() {
            return this.value;
        }

        @Override
        public String toString() {
            return "[" + this.type.toString() + ": " + this.value + "]";
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

    public static<T> boolean isDecimalNumber(T testedValue) {
        try {
            Double.parseDouble(testedValue.toString());
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public static<T> boolean isWholeNumber(T testedValue) {
        try {
            Integer.parseInt(testedValue.toString());
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public static void failIfBlank(String value, String message) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
