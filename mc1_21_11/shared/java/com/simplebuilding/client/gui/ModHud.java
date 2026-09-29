package com.simplebuilding.client.gui;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.ConfigSaving;
import com.simplebuilding.config.SimplebuildingConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * The mod's HUD as a whole (Immersion 2026-09-28): one switch for every overlay (velocity gauge,
 * rangefinder, air jump bar, spawn elytra), toggled by the "Toggle Mod HUD" key and kept in the
 * client config ({@code showModHud}), plus where the two gadget boxes sit and how large they are
 * ({@code hudPositionX/Y} in percent of the screen, {@code hudScale} in percent).
 *
 * <p>The key gives no on-screen text (owner rule): the HUD appearing or vanishing is the answer, a
 * soft click (higher when it comes back) confirms the press.
 */
public final class ModHud {
    private ModHud() {
    }

    /** Whether the mod's HUD is shown (config {@code showModHud}; default on). */
    public static boolean visible() {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        return config == null || config.showModHud;
    }

    /** The HUD key: flips {@code showModHud}, saves the config and clicks. */
    public static void toggle(Minecraft client) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        if (config == null) {
            return;
        }
        config.showModHud = !config.showModHud;
        ConfigSaving.save();
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), config.showModHud ? 1.2f : 0.8f, 0.4f));
    }

    /** HUD scale as a factor (0.5 to 2.0). */
    public static float scale() {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        int percent = config == null ? 100 : config.hudScale;
        return Math.max(50, Math.min(200, percent)) / 100.0f;
    }

    /**
     * Moves and scales the pose so that a box of {@code width} x {@code height} drawn at (0, 0) lands at
     * the configured place and size; {@code offsetY} (unscaled GUI pixels) shifts it, for two boxes
     * shown at once. Every call must be matched by {@link #end}.
     */
    public static void begin(GuiGraphics context, int width, int height, int offsetY) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        int px = config == null ? 0 : config.hudPositionX;
        int py = config == null ? 50 : config.hudPositionY;
        float scale = scale();
        int[] origin = HudLayout.origin(context.guiWidth(), context.guiHeight(), width, height, scale, px, py);
        context.pose().pushMatrix();
        context.pose().translate(origin[0], origin[1] + offsetY * scale);
        context.pose().scale(scale, scale);
    }

    public static void end(GuiGraphics context) {
        context.pose().popMatrix();
    }
}
