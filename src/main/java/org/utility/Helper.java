package org.utility;

import java.math.RoundingMode;
import java.text.NumberFormat;

public class Helper {
    private static final NumberFormat NUMBER_FORMAT;

    static {
        NUMBER_FORMAT = NumberFormat.getInstance();
        Helper.NUMBER_FORMAT.setRoundingMode(RoundingMode.HALF_UP);
    }

    public static double formatDecimalNumber(double value, int decimalPlaces) {
        Helper.NUMBER_FORMAT.setMaximumFractionDigits(decimalPlaces);
        return Double.parseDouble(Helper.NUMBER_FORMAT.format(value));
    }

    public enum EmptyEnum {}
}
