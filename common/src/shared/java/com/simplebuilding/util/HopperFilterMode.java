package com.simplebuilding.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

public enum HopperFilterMode {
    // MC 26.2: ChatFormatting.getColor() wurde entfernt. Die RGB-Werte der Legacy-Farben liegen
    // jetzt als benannte TextColor-Konstanten vor; TextColor.RED/GREEN/YELLOW tragen exakt
    // dieselben Werte (0xFF5555 / 0x55FF55 / 0xFFFF55) wie zuvor ChatFormatting.*.getColor().
    NONE(Component.translatable("simplebuilding.hopper_filter.none").withStyle(ChatFormatting.RED), TextColor.RED.getValue()),
    WHITELIST(Component.translatable("simplebuilding.hopper_filter.whitelist").withStyle(ChatFormatting.GREEN), TextColor.GREEN.getValue()),
    TYPE(Component.translatable("simplebuilding.hopper_filter.type").withStyle(ChatFormatting.YELLOW), TextColor.YELLOW.getValue());

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