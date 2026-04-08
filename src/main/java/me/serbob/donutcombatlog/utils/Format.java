package me.serbob.donutcombatlog.utils;

import java.text.DecimalFormat;

public class Format {
    private static final DecimalFormat df = new DecimalFormat("#,##0.00");

    public static String formatPrice(double price) {
        if (price >= 1.0E12) {
            return df.format(price / 1.0E12) + "t";
        }
        if (price >= 1.0E9) {
            return df.format(price / 1.0E9) + "b";
        }
        if (price >= 1_000_000.0) {
            return df.format(price / 1_000_000.0) + "m";
        }
        if (price >= 1_000.0) {
            return df.format(price / 1_000.0) + "k";
        }
        if (price == (long) price) {
            return String.format("%d", (long) price);
        }
        return df.format(price);
    }
}
