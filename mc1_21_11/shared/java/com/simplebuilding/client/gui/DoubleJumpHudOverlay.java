package com.simplebuilding.client.gui;

import com.simplebuilding.client.DoubleJumpController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * HUD cooldown bar for the air jump (double jump), drawn like vanilla's horse jump bar: the
 * {@code hud/jump_bar_*} sprites in the contextual bar slot (182x5, where the experience bar sits),
 * filling up while the ability recharges and gone once it is ready. Vanilla swaps that slot the
 * same way while riding a horse, so nothing collides with the heart and armour rows any more
 * (the old 80x5 fill bar with an "Air Jump" label sat 55 px above the bottom, right on top of
 * extra heart rows). The experience level number is drawn again on top, as vanilla does.
 */
public final class DoubleJumpHudOverlay {
    public static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/jump_bar_background");
    public static final Identifier PROGRESS_SPRITE = Identifier.withDefaultNamespace("hud/jump_bar_progress");
    /** Vanilla's contextual bar: 182x5, 24 px above the bottom edge plus its own height. */
    public static final int BAR_WIDTH = 182;
    public static final int BAR_HEIGHT = 5;
    public static final int BAR_BOTTOM_OFFSET = 24 + BAR_HEIGHT;

    private DoubleJumpHudOverlay() {
    }

    public static void render(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        if (client.options.hideGui) {
            // Fabric's element registry hangs the mod's overlays inside vanilla's own layers,
            // which F1 switches off as a whole; NeoForge's layer event does not, and there the
            // air jump bar, the speedometer and the rangefinder stayed on a hidden HUD. The
            // question has to be asked here, once, so both loaders give the same answer.
            return;
        }
        if (!DoubleJumpController.isOnCooldown()) {
            return; // only visible while the air-jump is recharging
        }
        int max = DoubleJumpController.getCooldownMax();
        if (max <= 0) {
            return;
        }
        int remaining = DoubleJumpController.getCooldownRemaining();
        float charged = Math.max(0.0f, Math.min(1.0f, (float) (max - remaining) / (float) max));

        int x = (context.guiWidth() - BAR_WIDTH) / 2;
        int y = context.guiHeight() - BAR_BOTTOM_OFFSET;
        context.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, x, y, BAR_WIDTH, BAR_HEIGHT);
        int fillWidth = Math.round(BAR_WIDTH * charged);
        if (fillWidth > 0) {
            context.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS_SPRITE, BAR_WIDTH, BAR_HEIGHT, 0, 0,
                    x, y, fillWidth, BAR_HEIGHT);
        }

        if (client.gameMode != null && client.gameMode.hasExperience() && client.player.experienceLevel > 0) {
            ContextualBarRenderer.renderExperienceLevel(context, client.font, client.player.experienceLevel);
        }
    }
}
