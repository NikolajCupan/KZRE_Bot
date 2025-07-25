package org.utility;

import java.math.RoundingMode;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;

public class Helper {
    public static double formatDecimalNumber(double value, int decimalPlaces) {
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setRoundingMode(RoundingMode.HALF_UP);
        numberFormat.setMaximumFractionDigits(decimalPlaces);

        return Double.parseDouble(numberFormat.format(value));
    }

    public static String snakeCaseToCamelCase(String value) {
        String result = value;
        while(result.contains("_")) {
            result = result.replaceFirst(
                    "_[a-z]",
                    String.valueOf(Character.toUpperCase(result.charAt(result.indexOf("_") + 1)))
            );
        }

        return result;
    }

    public static List<String> normalizeAndTokenizeString(String value) {
        String resultString = value.toLowerCase();
        resultString = Normalizer.normalize(resultString, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");

        return List.of(resultString.split(" ")).stream()
                .map(element -> element.replaceAll("[^A-Za-z0-9]", ""))
                .filter(element -> !element.isBlank())
                .toList();
    }

    public static<T> String stringifyCollection(Collection<T> collection) {
        return Helper.stringifyCollection(collection, true);
    }

    public static<T> String stringifyCollection(Collection<T> collection, boolean addQuotes) {
        if (collection == null || collection.isEmpty()) {
            return "[]";
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');

        collection.forEach(element -> {
            if (addQuotes) stringBuilder.append('\"');
            stringBuilder.append(element.toString());
            if (addQuotes) stringBuilder.append('\"');
            stringBuilder.append(", ");
        });

        stringBuilder.setLength(stringBuilder.length() - 2);
        stringBuilder.append(']');

        return stringBuilder.toString();
    }

    public enum EmptyEnum {}
}
