package com.simplebuilding.modules.simplechallenges.preset;

/**
 * The Lucky Block mode (S5): a mined block drops another block. {@code RANDOM} rolls anew every time (common blocks more likely),
 * {@code FIXED} (Random Drops) swaps every block for one fixed partner per world seed. {@code alpha} steers how fast the odds fall
 * with the rarity value ({@code 1 / (value + 1)^alpha}), {@code pity} is the number of first mined blocks that give wood and stone.
 */
public record LuckyMode(Kind kind, double alpha, int pity) {
    public enum Kind {
        RANDOM("lucky"), FIXED("randomdrops");

        public final String type;

        Kind(String type) {
            this.type = type;
        }

        public static Kind parse(String type) {
            for (Kind k : values()) if (k.type.equals(type)) return k;
            return null;
        }
    }

    public static final double DEFAULT_ALPHA = 1.3, MIN_ALPHA = 0.1, MAX_ALPHA = 4.0;
    public static final int DEFAULT_PITY = 6, MAX_PITY = 100;
}
