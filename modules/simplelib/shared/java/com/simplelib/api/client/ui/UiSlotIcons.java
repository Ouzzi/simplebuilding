package com.simplelib.api.client.ui;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/**
 * Empty-slot icons (armour silhouettes, shield, lapis ...) in the slot colours of a styled screen (owner N37: in some
 * themes, e.g. the brown backpack, Vanilla's grey icons were hardly visible). A mod registers a {@link Source} that
 * names the palette a slot of its screen is drawn in; {@code SlotIconTintMixin} then blits the icon in
 * {@link UiPalette#slotIcon()} instead of Vanilla's grey. Screens no source knows stay Vanilla.
 */
public final class UiSlotIcons {
    /** The palette {@code slot} of {@code screen} is drawn in, or {@code null} when the source does not style it. */
    @FunctionalInterface
    public interface Source {
        @Nullable UiPalette palette(AbstractContainerScreen<?> screen, Slot slot);
    }

    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();

    private UiSlotIcons() {}

    public static void register(Source source) {
        if (!SOURCES.contains(source)) SOURCES.add(source);
    }

    /** The palette of the first source that styles {@code slot}, or {@code null} (Vanilla look). */
    public static @Nullable UiPalette palette(AbstractContainerScreen<?> screen, Slot slot) {
        if (!UiStyleToggle.isEnabled()) return null;
        for (Source source : SOURCES) {
            UiPalette p = source.palette(screen, slot);
            if (p != null) return p;
        }
        return null;
    }

    /**
     * The {@code blitSprite(..., color)} tint that turns Vanilla's one-grey icon {@code icon} into {@code p}'s
     * {@link UiPalette#slotIcon()} (the tint multiplies, so each channel is target x 255 / grey, at most 255).
     */
    public static int tint(UiPalette p, Identifier icon) {
        int grey = grey(icon.getPath()), target = p.slotIcon(), color = 0xFF000000;
        for (int shift = 16; shift >= 0; shift -= 8) color |= Math.min(255, ((target >> shift) & 255) * 255 / grey) << shift;
        return color;
    }

    /**
     * The single grey of a Vanilla 26.3 empty-slot sprite (measured): saddle and mount armor 124, potion and blaze powder
     * 104, banner and dye 55, banner pattern 58, all others (tools, armor, lapis, ingots ...) 85.
     */
    public static int grey(String path) {
        return switch (path) {
            case "container/slot/saddle", "container/slot/horse_armor", "container/slot/llama_armor",
                    "container/slot/nautilus_armor_inventory" -> 124;
            case "container/slot/potion", "container/slot/brewing_fuel" -> 104;
            case "container/slot/banner", "container/slot/dye" -> 55;
            case "container/slot/banner_pattern" -> 58;
            default -> 85;
        };
    }
}
