package com.simplebuilding.client.gui;

import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * 26.3: the resonance button belongs to Simple Resonance (MOB27-3 R2, docs/ai/PLAN-B-TRIMSTATS27-2026-11-11.md). It draws the button
 * itself - in the Vanilla inventory with its own mixin, in rebuilt inventories through SimpleLib's {@code InventoryExtrasClient} - so
 * {@link #render} draws nothing here. What stays core is the frame of the extra head slot ({@link #slotFrame}). The tooltip methods
 * hand the module's lines through ({@code ResonanceBridge}, reflection; empty without Simple Resonance) for the client tests.
 * The 26.2 twin in common/src/mc26_2 still draws the old button.
 */
public final class TrimStatsPanel {
    public static final int HEIGHT = com.simplebuilding.util.TrimStatsLayout.HEIGHT;

    /** The compact value, e.g. "0.22x". */
    public static String compact(double resonance) {
        return String.format(java.util.Locale.ROOT, "%.2fx", resonance);
    }

    /** Nothing on 26.3: Simple Resonance draws the button at the same place. */
    public void render(GuiGraphicsExtractor context, Font font, Minecraft minecraft, int x, int y, int mouseX, int mouseY) {
    }

    /** An empty Vanilla slot frame (18x18 around the 16x16 item at {@code x + 1, y + 1}) on a plain background. */
    public static void slotFrame(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        g.fill(x, y, x + 17, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + 17, 0xFF373737);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
    }

    /** What Simple Resonance's hovered button shows ({@code shift}: every line). */
    public static List<Component> shown(Player player, boolean shift) {
        return lines("shown", new Class<?>[] {Object.class, boolean.class}, player, shift);
    }

    /** Every line of Simple Resonance's tooltip. */
    public static List<Component> tooltip(Player player) {
        return lines("tooltip", new Class<?>[] {Object.class}, player);
    }

    @SuppressWarnings("unchecked")
    private static List<Component> lines(String name, Class<?>[] types, Object... args) {
        try {
            Method method = Class.forName(com.simplebuilding.util.TrimMultiplierLogic.BRIDGE).getMethod(name, types);
            return (List<Component>) method.invoke(null, args);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return List.of();
        }
    }
}
