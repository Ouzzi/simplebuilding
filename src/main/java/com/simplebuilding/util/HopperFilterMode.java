package com.simplebuilding.util;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public enum HopperFilterMode {
    NONE(Text.literal("Disabled").formatted(Formatting.RED), Formatting.RED.getColorValue()),
    WHITELIST(Text.literal("Exact Match").formatted(Formatting.GREEN), Formatting.GREEN.getColorValue()),
    TYPE(Text.literal("Type Match").formatted(Formatting.YELLOW), Formatting.YELLOW.getColorValue());

    private final Text text;
    private final int color;

    HopperFilterMode(Text text, Integer color) {
        this.text = text;
        this.color = color != null ? color : 0xFFFFFF;
    }

    public Text getText() {
        return text;
    }

    public int getColor() {
        return color;
    }

    public HopperFilterMode next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}