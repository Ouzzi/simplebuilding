package com.simplestorage.client;

import com.simplestorage.bundle.BundleTooltipData;
import com.simplestorage.bundle.DyedStorage;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;

/**
 * The client tooltip of a Simple Storage bundle or quiver, one factory for every loader (split stage 8c-1, copy of
 * SimpleBuilding's {@code ReinforcedBundleTooltips}): Vanilla's {@link ClientBundleTooltip} with the fill bar scaled to the
 * capacity and, for a dyed bundle, slots lightly tinted in its colour.
 */
public final class BundleTooltips {
    /** Capacity text colour of an undyed bundle (SimpleBuilding's {@code guiDrawHelper} default). */
    private static final int DEFAULT_TEXT = 0xFFFFAA00;

    private BundleTooltips() {
    }

    public static ClientBundleTooltip create(BundleTooltipData data) {
        ClientBundleTooltip component = new ClientBundleTooltip(data.contents());
        BundleTooltipTint tint = (BundleTooltipTint) component;
        tint.simplestorage$setCapacityScale((float) data.maxCapacity() / 64.0f);
        tint.simplestorage$setProgressColor(0xFF000000 | textColour(data.dyeColor()));
        tint.simplestorage$setSlotTint(DyedStorage.spriteTint(data.dyeColor()));
        return component;
    }

    /** The capacity text colour: the light tone of the dye a firework would show (SimpleBuilding's colour themes), else gold. */
    static int textColour(int dye) {
        net.minecraft.world.item.DyeColor color = dye == DyedStorage.UNDYED ? null : net.minecraft.world.item.DyeColor.byFireworkColor(dye);
        if (color == null) return DEFAULT_TEXT;
        return switch (color) {
            case WHITE -> 0xFFFFFF;
            case ORANGE -> 0xFFAD33;
            case MAGENTA -> 0xFF55FF;
            case LIGHT_BLUE -> 0x66FFFF;
            case YELLOW -> 0xFFFF66;
            case LIME -> 0x88FF33;
            case PINK -> 0xFF99CC;
            case GRAY -> 0xAAAAAA;
            case LIGHT_GRAY -> 0xDDDDDD;
            case CYAN -> 0x00FFFF;
            case PURPLE -> 0xCC66FF;
            case BLUE -> 0x6699FF;
            case BROWN -> 0xCC9966;
            case GREEN -> 0x66FF66;
            case RED -> 0xFF6666;
            case BLACK -> 0xAAAAAA;
        };
    }
}
