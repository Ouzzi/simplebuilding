package com.simplebuilding.modules.simplestats.data;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

/** Number, distance and time text for statistics. Pure Java; units are symbols (m, km, h, min, s, d) so no lookup is needed. */
public final class StatFormat {
    private StatFormat() {}

    /** Locale of a Minecraft language code such as {@code de_de}; unknown codes give English. */
    public static Locale locale(String code) {
        if (code == null || code.isBlank()) return Locale.US;
        Locale locale = Locale.forLanguageTag(code.replace('_', '-'));
        return locale.getLanguage().isEmpty() ? Locale.US : locale;
    }

    /** Grouped whole number: 12,345 (en) / 12.345 (de). */
    public static String full(long value, Locale locale) {
        return NumberFormat.getIntegerInstance(locale).format(value);
    }

    /** Whole number below 10,000, then 12.4k, 1.2M, 3G. */
    public static String compact(long value, Locale locale) {
        if (value < 10_000) return full(value, locale);
        double scaled;
        String suffix;
        if (value < 1_000_000) { scaled = value / 1_000.0; suffix = "k"; }
        else if (value < 1_000_000_000L) { scaled = value / 1_000_000.0; suffix = "M"; }
        else { scaled = value / 1_000_000_000.0; suffix = "G"; }
        String text = String.format(locale, "%.1f", scaled);
        char decimal = java.text.DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
        if (text.endsWith(decimal + "0")) text = text.substring(0, text.length() - 2);
        return text + suffix;
    }

    /** Health points stored in hundredths, shown as hearts (two health points each) with one decimal. */
    public static String hearts(long hundredths, Locale locale) {
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setMaximumFractionDigits(1);
        format.setMinimumFractionDigits(0);
        return format.format(hundredths / 200.0) + " \u2665";
    }

    /** Centimeters as meters below 1 km, kilometers (one decimal) from there. */
    public static String distance(long cm, Locale locale) {
        long meters = cm / 100;
        if (meters < 1_000) return full(meters, locale) + " m";
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setMaximumFractionDigits(1);
        format.setMinimumFractionDigits(0);
        return format.format(meters / 1000.0) + " km";
    }

    /** Game ticks (20 per second) as the two largest units: 42 s, 12 min, 5 h 12 min, 3 d 4 h. */
    public static String duration(long ticks) {
        long seconds = Math.max(0L, ticks) / 20;
        if (seconds < 60) return seconds + " s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " min";
        long hours = minutes / 60;
        if (hours < 48) return hours + " h " + (minutes % 60) + " min";
        return (hours / 24) + " d " + (hours % 24) + " h";
    }

    /** Value in its unit; {@code compactNumbers} shortens plain counts (tooltip), the screen shows them in full. */
    public static String value(StatKeys.Unit unit, long value, Locale locale, boolean compactNumbers) {
        return switch (unit) {
            case COUNT -> compactNumbers ? compact(value, locale) : full(value, locale);
            case DAMAGE -> hearts(value, locale);
            case TICKS -> duration(value);
            case CM -> distance(value, locale);
            case BLOCKS -> full(value, locale) + " m";
        };
    }

    /** ISO date of an epoch day, empty for 0. */
    public static String date(long epochDay) {
        return epochDay <= 0 ? "" : LocalDate.ofEpochDay(epochDay).toString();
    }
}
