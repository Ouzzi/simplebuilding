package com.simplebuilding.client.gui;

import com.simplebuilding.client.DoubleJumpController;
import com.simplebuilding.tweaks.client.SpawnElytraHud;
import com.simplebuilding.util.AirJumpBarRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.client.gui.contextualbar.JumpableVehicleBar;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * The air jump cooldown as a bar in vanilla's contextual bar slot - exactly where and as large as
 * the experience bar (182x5, 29 px above the bottom), in the mod's own sky-blue sprites
 * ({@code simplebuilding:hud/air_jump_bar_*}), filling up while the ability recharges (owner
 * 2026-09-29). It is the only thing on screen for the air jump: no box, no label.
 *
 * <p>It is not a HUD layer of its own. {@code HudContextualBarMixin} asks {@link #takesTheSlot}
 * where vanilla draws its contextual bar (experience, locator or vehicle jump bar) and draws this
 * bar instead, so it follows vanilla's own switching - see {@link AirJumpBarRule} for the order.
 * The experience level number stays vanilla's (it is drawn over the locator bar as well).
 */
public final class DoubleJumpHudOverlay {
    public static final Identifier BACKGROUND_SPRITE = Identifier.fromNamespaceAndPath("simplebuilding", "hud/air_jump_bar_background");
    public static final Identifier PROGRESS_SPRITE = Identifier.fromNamespaceAndPath("simplebuilding", "hud/air_jump_bar_progress");
    /** Vanilla's contextual bar: 182x5, 24 px above the bottom edge plus its own height. */
    public static final int BAR_WIDTH = ContextualBar.WIDTH;
    public static final int BAR_HEIGHT = ContextualBar.HEIGHT;
    public static final int BAR_BOTTOM_OFFSET = ContextualBar.MARGIN_BOTTOM + ContextualBar.HEIGHT;

    private DoubleJumpHudOverlay() {
    }

    /**
     * Whether the air jump bar takes the contextual bar slot now. {@code vanillaBar} is the bar
     * vanilla is about to draw (null when unknown, e.g. a direct call from a test).
     */
    public static boolean takesTheSlot(@Nullable ContextualBar vanillaBar) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) {
            return false;
        }
        return AirJumpBarRule.showsAirJumpBar(
                vanillaBar instanceof JumpableVehicleBar,
                client.gameMode.hasExperience(),
                AirJumpBarRule.experienceChangedRecently(client.player.experienceDisplayStartTick, client.player.tickCount),
                DoubleJumpController.getCooldownRemaining(),
                DoubleJumpController.getCooldownMax(),
                ModHud.visible(),
                SpawnElytraHud.wearsSpawnElytra(client.player));
    }

    /**
     * Draws the bar if it takes the slot now. Used by tests and anyone without vanilla's bar at
     * hand; the mixin calls {@link #takesTheSlot} with vanilla's bar and then {@link #drawBar}.
     */
    public static void render(GuiGraphicsExtractor context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.hud.isHidden()) {
            return; // F1: vanilla skips the whole hotbar group, a direct call has to ask itself
        }
        if (takesTheSlot(null)) {
            drawBar(context);
        }
    }

    /** The bar itself, at the running cooldown's progress. */
    public static void drawBar(GuiGraphicsExtractor context) {
        int x = (context.guiWidth() - BAR_WIDTH) / 2;
        int y = context.guiHeight() - BAR_BOTTOM_OFFSET;
        context.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, x, y, BAR_WIDTH, BAR_HEIGHT);
        int fillWidth = AirJumpBarRule.progressWidth(DoubleJumpController.getCooldownRemaining(), DoubleJumpController.getCooldownMax());
        if (fillWidth > 0) {
            context.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS_SPRITE, BAR_WIDTH, BAR_HEIGHT, 0, 0,
                    x, y, fillWidth, BAR_HEIGHT);
        }
    }
}
