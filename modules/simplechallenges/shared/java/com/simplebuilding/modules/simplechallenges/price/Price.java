package com.simplebuilding.modules.simplechallenges.price;

import com.simplebuilding.modules.simplechallenges.preset.Currency;

/** What one cell costs: {@code count} of {@code item} (items), credits or experience levels. */
public record Price(Currency currency, String item, int count) {
    public static Price items(String item, int count) {
        return new Price(Currency.ITEMS, item, count);
    }

    public static Price credits(int count) {
        return new Price(Currency.CREDITS, "", count);
    }

    public static Price xp(int levels) {
        return new Price(Currency.XP, "", levels);
    }

    /** Stored form in the world state: {@code items|minecraft:dirt|16}. */
    public String encode() {
        return currency.id() + "|" + item + "|" + count;
    }

    public static Price decode(String s) {
        String[] p = s.split("\\|", -1);
        if (p.length != 3) return null;
        Currency c = Currency.parse(p[0]);
        try {
            return c == null ? null : new Price(c, p[1], Integer.parseInt(p[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
