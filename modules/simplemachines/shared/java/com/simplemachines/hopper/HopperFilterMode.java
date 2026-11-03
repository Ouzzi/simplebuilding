package com.simplemachines.hopper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

/** The filter key of a mod hopper (SimpleBuilding's {@code HopperFilterMode}, stage 3 copy): off - exact - same kind. */
public enum HopperFilterMode {
    NONE(Component.translatable("simplemachines.hopper_filter.none").withStyle(ChatFormatting.RED), TextColor.RED.getValue()),
    WHITELIST(Component.translatable("simplemachines.hopper_filter.whitelist").withStyle(ChatFormatting.GREEN), TextColor.GREEN.getValue()),
    TYPE(Component.translatable("simplemachines.hopper_filter.type").withStyle(ChatFormatting.YELLOW), TextColor.YELLOW.getValue());

    private final Component text;
    private final int color;

    HopperFilterMode(Component text, int color) {
        this.text = text;
        this.color = color;
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

    /** The mode of a saved or synced ordinal; anything unknown is {@link #NONE}. */
    public static HopperFilterMode of(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : NONE;
    }
}
