package org.utility;

import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import org.exception.InvalidActionArgumentException;

import java.math.RoundingMode;
import java.text.MessageFormat;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

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

        return Stream.of(resultString.split(" "))
                .map(element -> element.replaceAll("[^A-Za-z0-9]", ""))
                .filter(element -> !element.isBlank())
                .toList();
    }

    public static<T> String stringifyCollection(Collection<T> collection) {
        return Helper.stringifyCollection(collection, T::toString, true);
    }

    public static<T> String stringifyCollection(
            Collection<T> collection, Function<T, String> stringifyMethod, boolean addQuotes
    ) {
        if (collection == null || collection.isEmpty()) {
            return "[]";
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');

        collection.forEach(element -> {
            if (addQuotes) stringBuilder.append('\"');
            stringBuilder.append(stringifyMethod.apply(element));
            if (addQuotes) stringBuilder.append('\"');
            stringBuilder.append(", ");
        });

        stringBuilder.setLength(stringBuilder.length() - 2);
        stringBuilder.append(']');

        return stringBuilder.toString();
    }

    public static String extractIdFromChannelMention(String value) {
        if (value.length() <= 3 || !value.startsWith("<#") || !value.endsWith(">")) {
            throw new InvalidActionArgumentException(
                    MessageFormat.format("Invalid channel \"{0}\" provided", value)
            );
        }

        return value.substring(2, value.length() - 1);
    }

    public static boolean isGuildEmoji(MessageReaction reaction) {
        return reaction.getEmoji() instanceof CustomEmoji;
    }

    public enum EmptyEnum {}
}
