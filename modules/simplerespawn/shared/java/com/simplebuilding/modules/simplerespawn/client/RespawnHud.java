package com.simplebuilding.modules.simplerespawn.client;

import com.simplebuilding.modules.simplerespawn.SimpleRespawn;
import com.simplebuilding.modules.simplerespawn.network.RespawnStatePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * HUD of Simple Respawn (mixin at the end of {@code Hud#extractRenderState}): while spectating a red veil, the time
 * left, the revive bar and how to open the menu again; for a helper the revive bar under the crosshair; looking at a
 * downed body a short hint.
 */
public final class RespawnHud {
    private RespawnHud() {}

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) return;
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        if (RespawnClient.downed()) {
            if (mc.gui.screen() != null) return;
            graphics.fill(0, 0, w, h, 0x40600000);
            int seconds = (RespawnClient.ticksLeft + 19) / 20;
            graphics.centeredText(mc.font, Component.translatable("hud.simplerespawn.downed", Component.literal(Integer.toString(seconds))
                    .withStyle(ChatFormatting.YELLOW)), w / 2, 12, 0xFFFF5555);
            graphics.centeredText(mc.font, Component.translatable("hud.simplerespawn.menu", mc.options.keyInventory.getTranslatedKeyMessage()),
                    w / 2, 24, 0xFFC0C0C0);
            if (RespawnClient.progress > 0f) {
                graphics.centeredText(mc.font, Component.translatable("screen.simplerespawn.helped", RespawnClient.other), w / 2, h / 2 + 12, 0xFF80FF80);
                bar(graphics, w / 2 - 50, h / 2 + 24, 100, RespawnClient.progress);
            }
            return;
        }
        if (RespawnClient.role == RespawnStatePayload.REVIVING) {
            graphics.centeredText(mc.font, Component.translatable("hud.simplerespawn.reviving", RespawnClient.other), w / 2, h / 2 + 12, 0xFF80FF80);
            bar(graphics, w / 2 - 50, h / 2 + 24, 100, RespawnClient.progress);
        } else if (mc.crosshairPickEntity instanceof Player body && SimpleRespawn.CLIENT_DOWNED.contains(body.getUUID())) {
            graphics.centeredText(mc.font, Component.translatable("hud.simplerespawn.hint", mc.options.keyUse.getTranslatedKeyMessage()),
                    w / 2, h / 2 + 12, 0xFFFFFFFF);
        }
    }

    /** A plain progress bar: dark frame, green fill. */
    static void bar(GuiGraphicsExtractor graphics, int x, int y, int width, float progress) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + 6, 0xFF000000);
        graphics.fill(x, y, x + width, y + 5, 0xFF3A3A3A);
        graphics.fill(x, y, x + Math.round(width * Math.max(0f, Math.min(1f, progress))), y + 5, 0xFF55DD55);
    }
}
