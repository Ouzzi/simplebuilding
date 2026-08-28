package com.simplebuilding.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum HopperFilterMode {
    // MC 1.21.11: ChatFormatting.getColor() existiert noch und liefert exakt die RGB-Werte,
    // die 26.2 als benannte TextColor-Konstanten fuehrt (0xFF5555 / 0x55FF55 / 0xFFFF55).
    NONE(Component.literal("Disabled").withStyle(ChatFormatting.RED), ChatFormatting.RED.getColor()),
    WHITELIST(Component.literal("Exact Match").withStyle(ChatFormatting.GREEN), ChatFormatting.GREEN.getColor()),
    TYPE(Component.literal("Type Match").withStyle(ChatFormatting.YELLOW), ChatFormatting.YELLOW.getColor());

    private final Component text;
    private final int color;

    HopperFilterMode(Component text, Integer color) {
        this.text = text;
        this.color = color != null ? color : 0xFFFFFF;
    }

    public Component getText() {
        return text;
    }

    public int getColor() {
        return color;
    }

    public HopperFilterMode next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}