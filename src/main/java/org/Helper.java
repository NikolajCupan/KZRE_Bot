package org;

import org.jetbrains.annotations.NotNull;

public class Helper {
    public record TypedValue(Helper.TypedValue.Type type, String value) {
        public enum Type { STRING, WHOLE_NUMBER, DECIMAL_NUMBER, ENUMERATOR, NULL }

        @NotNull
        @Override
        public String toString() {
            return "[" + this.type.toString() + ": " + this.value + "]";
        }

        public String valueFirstWord() {
            if (this.value.isBlank()) {
                return "";
            }

            int spaceIndex = this.value.indexOf(' ');
            if (spaceIndex == -1) {
                return this.value;
            }

            return this.value.substring(0, spaceIndex);
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
