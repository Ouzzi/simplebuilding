package com.simplebuilding.tweaks.client;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.component.TweaksComponents;
import com.simplebuilding.tweaks.item.TweaksItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Boost-Leiste und Timer der Spawn-Elytra anstelle der XP-Leiste (Simple Tweaks: InGameHudMixin
 * und InGameHudLevelMixin). Wird aus dem XP-Leisten-Mixin gezeichnet.
 */
public final class SpawnElytraHud {
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("hud/experience_bar_background");
    private static final Identifier BLUE_PROGRESS = Identifier.withDefaultNamespace("hud/jump_bar_progress");

    private SpawnElytraHud() {
    }

    public static boolean wearsSpawnElytra(Player player) {
        return player != null && player.getItemBySlot(EquipmentSlot.CHEST).is(TweaksItems.SPAWN_ELYTRA);
    }

    /** Anteil der Leiste (0..182 Pixel), der bei diesem Boost-Stand blau ist. */
    public static int progressWidth(float boostLevel, int maxBoosts) {
        int max = Math.max(1, maxBoosts);
        int current = Math.round(boostLevel * max);
        return current <= 0 ? 0 : current * 182 / max;
    }

    /** "mm:ss" der Restflugzeit, "Active" ohne Timer. */
    public static String timerText(Integer ticksLeft) {
        if (ticksLeft == null) {
            return "Active";
        }
        int seconds = ticksLeft / 20;
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    /** true = gezeichnet, die Vanilla-XP-Leiste entfaellt. */
    public static boolean render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (!wearsSpawnElytra(player)) {
            return false;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        int x = (graphics.guiWidth() - 182) / 2;
        int y = graphics.guiHeight() - 29;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 182, 5);

        Float boost = chest.get(TweaksComponents.BOOST_LEVEL);
        int width = progressWidth(boost == null ? 1.0f : boost, SimpleTweaks.config().spawn.maxBoosts);
        if (width > 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BLUE_PROGRESS, 182, 5, 0, 0, x, y, width, 5);
        }

        Integer ticksLeft = chest.get(TweaksComponents.FLIGHT_TIME);
        String text = timerText(ticksLeft);
        int color;
        if (ticksLeft == null) {
            color = 0xFF55FFFF;
        } else if (ticksLeft / 20 < 30) {
            boolean blink = client.level != null && client.level.getGameTime() % 10 < 5;
            color = blink ? 0xFFFF5555 : 0xFFFFFF55;
        } else {
            color = 0xFF55FF55;
        }
        int textX = (graphics.guiWidth() - client.font.width(text)) / 2;
        int textY = y - 6;
        graphics.drawString(client.font, text, textX + 1, textY, 0xFF000000, false);
        graphics.drawString(client.font, text, textX - 1, textY, 0xFF000000, false);
        graphics.drawString(client.font, text, textX, textY + 1, 0xFF000000, false);
        graphics.drawString(client.font, text, textX, textY - 1, 0xFF000000, false);
        graphics.drawString(client.font, text, textX, textY, color, false);
        return true;
    }
}
