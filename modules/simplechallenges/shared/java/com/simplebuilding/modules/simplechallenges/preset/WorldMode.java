package com.simplebuilding.modules.simplechallenges.preset;

import java.util.List;

/**
 * The world mode of a Skyblock-family preset (S4): the world starts in the void and the mode builds what stands in it.
 * {@code ISLAND} a start island ({@code style}), {@code ONEBLOCK} one block that grows back with ever new blocks,
 * {@code SKYGRID} a grid of single blocks every {@code spacing} blocks. It combines with an unlock mode of the same preset.
 */
public record WorldMode(Kind kind, String style, int spacing) {
    public enum Kind {
        ISLAND("skyblock"), ONEBLOCK("oneblock"), SKYGRID("skygrid");

        public final String type;

        Kind(String type) {
            this.type = type;
        }

        public static Kind parse(String type) {
            for (Kind k : values()) if (k.type.equals(type)) return k;
            return null;
        }
    }

    public static final List<String> ISLAND_STYLES = List.of("classic", "mini", "desert");
    public static final int MIN_SPACING = 2, MAX_SPACING = 8;
}
