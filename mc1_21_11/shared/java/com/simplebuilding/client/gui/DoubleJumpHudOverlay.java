package com.simplebuilding.client.gui;

import com.simplebuilding.client.DoubleJumpController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Small HUD cooldown bar for the air-jump (double jump). Shown centered just above
 * the hotbar while the ability is recharging; it fills up as the cooldown counts down
 * and disappears once the air-jump is ready again.
 */
public final class DoubleJumpHudOverlay {
    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 5;
    private static final int BORDER_COLOR = 0xC0000000;
    private static final int TRACK_COLOR = 0xFF2B2B2B;
    private static final int FILL_CHARGING = 0xFFFFC83C; // amber while recharging
    private static final int FILL_READY = 0xFF4CFF4C;    // green at full charge

    private DoubleJumpHudOverlay() {
    }

    public static void render(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
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

        int screenWidth = context.guiWidth();
        int screenHeight = context.guiHeight();
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - 55; // above the hotbar / xp bar

        // border + empty track
        context.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, BORDER_COLOR);
        context.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, TRACK_COLOR);

        // recharge fill
        int fillWidth = Math.round(BAR_WIDTH * charged);
        if (fillWidth > 0) {
            context.fill(x, y, x + fillWidth, y + BAR_HEIGHT, charged >= 1.0f ? FILL_READY : FILL_CHARGING);
        }

        Font font = client.font;
        context.drawCenteredString(font, Component.literal("Air Jump"), screenWidth / 2, y - 10, 0xFFFFFFFF);
    }
}
