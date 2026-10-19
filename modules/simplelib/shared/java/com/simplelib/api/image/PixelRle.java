package com.simplelib.api.image;

/**
 * Run-length text of one row of palette indices, short enough to type and to fit build codes: {@code 3a2b.c} is
 * three times entry 0, twice entry 1, one empty pixel, once entry 2. Entries 0..51 are the letters {@link #SYMBOLS}
 * ({@code a..z}, then {@code A..Z}); {@link #EMPTY} is a free (transparent) pixel, index {@code -1}; a decimal
 * count of at least 2 may stand before a symbol. {@code decode(encode(row))} is {@code row}.
 */
public final class PixelRle {
    /** The symbol of entry {@code i} is {@code SYMBOLS.charAt(i)}. */
    public static final String SYMBOLS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    /** Most palette entries a row can name. */
    public static final int MAX_ENTRIES = SYMBOLS.length();
    /** A free pixel (index -1). */
    public static final char EMPTY = '.';
    /** Index of a free pixel. */
    public static final int NONE = -1;

    private PixelRle() {}

    /** The row {@code indices[from, to)}; every index is {@link #NONE} or 0..{@link #MAX_ENTRIES}-1. */
    public static String encode(int[] indices, int from, int to) {
        StringBuilder out = new StringBuilder();
        int i = from;
        while (i < to) {
            int value = indices[i];
            int run = 1;
            while (i + run < to && indices[i + run] == value) {
                run++;
            }
            if (run > 1) {
                out.append(run);
            }
            out.append(symbol(value));
            i += run;
        }
        return out.toString();
    }

    public static String encode(int[] indices) {
        return encode(indices, 0, indices.length);
    }

    /** The character for index {@code index} ({@link #NONE} gives {@link #EMPTY}). */
    public static char symbol(int index) {
        if (index == NONE) {
            return EMPTY;
        }
        if (index < 0 || index >= MAX_ENTRIES) {
            throw new IllegalArgumentException("palette index " + index + " outside 0.." + (MAX_ENTRIES - 1));
        }
        return SYMBOLS.charAt(index);
    }

    /** The index of {@code c}: 0..51, {@link #NONE} for {@link #EMPTY}, or -2 when it is no symbol. */
    public static int index(char c) {
        if (c == EMPTY) {
            return NONE;
        }
        int i = SYMBOLS.indexOf(c);
        return i >= 0 ? i : -2;
    }

    /**
     * The indices of {@code text}, or {@code null} when it is malformed (unknown character, count without symbol,
     * count below 2 or with a leading zero) or longer than {@code maxLength} pixels.
     */
    public static int[] decode(String text, int maxLength) {
        int length = 0;
        int[] out = new int[Math.min(maxLength, Math.max(16, text.length()))];
        int i = 0;
        while (i < text.length()) {
            int run = 1;
            int start = i;
            while (i < text.length() && text.charAt(i) >= '0' && text.charAt(i) <= '9') {
                i++;
            }
            if (i > start) {
                if (text.charAt(start) == '0' || i - start > 6) {
                    return null;
                }
                run = Integer.parseInt(text, start, i, 10);
                if (run < 2) {
                    return null;
                }
            }
            if (i >= text.length()) {
                return null;
            }
            int value = index(text.charAt(i++));
            if (value == -2 || length + run > maxLength) {
                return null;
            }
            if (length + run > out.length) {
                out = java.util.Arrays.copyOf(out, Math.min(maxLength, Math.max(out.length * 2, length + run)));
            }
            java.util.Arrays.fill(out, length, length + run, value);
            length += run;
        }
        return java.util.Arrays.copyOf(out, length);
    }
}
