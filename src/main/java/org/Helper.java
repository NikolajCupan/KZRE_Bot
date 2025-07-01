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

        @Override
        public String toString() {
            return "[" + this.type.toString() + ": " + this.value + "]";
        }
    }

    public static boolean isDecimalNumber(String string) {
        try {
            Double.parseDouble(string);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public static boolean isWholeNumber(String string) {
        try {
            Integer.parseInt(string);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
