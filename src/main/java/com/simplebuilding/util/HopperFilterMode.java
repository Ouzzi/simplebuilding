package com.simplebuilding.util;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public enum HopperFilterMode {
    NONE(Text.literal("None").formatted(Formatting.RED)),
    EXACT(Text.literal("Exact Match").formatted(Formatting.GREEN)), // Prüft Item + Komponenten (NBT)
    TYPE(Text.literal("Item Type").formatted(Formatting.YELLOW)); // Prüft nur Item-ID (ignoriert Name/Enchants etc.)

    private final Text text;

    HopperFilterMode(Text text) {
        this.text = text;
    }

    public Text getText() {
        return text;
    }

    public HopperFilterMode next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}