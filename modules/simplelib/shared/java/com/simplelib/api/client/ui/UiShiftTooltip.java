package com.simplelib.api.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * "Hold Shift for more" in tooltips (owner 2026-10-21): one building block for every Simple mod. Without Shift a long
 * tooltip shows its first lines and then the hint line, with Shift everything. The hint is always the same grey
 * ({@link #HINT}); a tooltip that says what Shift reveals passes its own "Hold Shift for ..." key to {@link #hint(String)}.
 *
 * <p>Pure apart from {@link #held()} (the server-side tests call {@link #collapse(List, int, boolean)} directly).
 */
public final class UiShiftTooltip {
    /** "Hold Shift for more" / "Umschalt halten für mehr". */
    public static final String KEY = "tooltip.simplelib.hold_shift";
    /** Vanilla grey #AAAAAA: 8.8:1 on the tooltip background #100010 (rule A7; DARK_GRAY would be 2.6:1). */
    public static final ChatFormatting HINT = ChatFormatting.GRAY;
    /** The Vanilla tooltip background, the reference of the hint's contrast. */
    public static final int TOOLTIP_BACKGROUND = 0xFF100010;

    private UiShiftTooltip() {}

    /** Whether Shift is held right now (client). */
    public static boolean held() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.hasShiftDown();
    }

    /** The hint line "Hold Shift for more". */
    public static MutableComponent hint() {
        return hint(KEY);
    }

    /** A hint line with its own text ("Hold Shift for contents"), in the shared hint grey. */
    public static MutableComponent hint(String key, Object... args) {
        return Component.translatable(key, args).withStyle(HINT);
    }

    /** {@link #collapse(List, int, boolean)} with the current Shift state. */
    public static List<Component> collapse(List<Component> lines, int brief) {
        return collapse(lines, brief, held());
    }

    /**
     * Without {@code shift}: the first {@code brief} lines and the hint; with {@code shift}, or when nothing would be hidden,
     * {@code lines} unchanged.
     */
    public static List<Component> collapse(List<Component> lines, int brief, boolean shift) {
        if (shift || lines.size() <= brief) return lines;
        List<Component> shown = new ArrayList<>(lines.subList(0, Math.max(0, brief)));
        shown.add(hint());
        return shown;
    }
}
