package com.simpleshops.client;

import com.simpleshops.SimpleShops;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The maintenance sign (owner E1ANS F5, 12.11.): while the owner works on his shop, a customer's open screen shows this pixel-art
 * sign over the trade rows instead of the goods (hanging wooden sign, a little bee with a hard hat, warning band; texture from
 * {@code tools/textures/shop_maintenance_2026_11_12.py}). The two lines are drawn in the game font from the language files, dark
 * ink on the light boards: at least 6.5:1 against every board pixel under them (A7, measured in the a-e1ans preview).
 */
public final class MaintenanceSign {
    public static final Identifier TEXTURE = SimpleShops.id("textures/gui/maintenance_sign.png");
    public static final int WIDTH = 144, HEIGHT = 60;
    /** Tops of the two text lines inside the sign (the generator keeps these board rows free of dark grain). */
    public static final int TITLE_Y = 26, SUB_Y = 40;
    public static final int INK = 0xFF3A2A16;

    /** Draws the sign with its top-left corner at {@code x, y} (screen coordinates). */
    public static void draw(GuiGraphicsExtractor graphics, Font font, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, WIDTH, HEIGHT, WIDTH, HEIGHT);
        line(graphics, font, Component.translatable("simpleshops.screen.maintenance"), x, y + TITLE_Y);
        line(graphics, font, Component.translatable("simpleshops.screen.maintenance.owner"), x, y + SUB_Y);
    }

    private static void line(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y) {
        graphics.text(font, text, x + (WIDTH - font.width(text)) / 2 + 1, y, INK, false);
    }

    private MaintenanceSign() {}
}
