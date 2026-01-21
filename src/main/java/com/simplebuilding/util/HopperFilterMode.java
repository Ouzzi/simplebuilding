package com.simplebuilding.util;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public enum HopperFilterMode {
    NONE(Text.literal("None").formatted(Formatting.RED)),
    WHITELIST(Text.literal("Whitelist").formatted(Formatting.GREEN)), // Filtert genau dieses Item
    TYPE(Text.literal("Type Match").formatted(Formatting.YELLOW)); // Filtert Item-Typ (ignoriert NBT)

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