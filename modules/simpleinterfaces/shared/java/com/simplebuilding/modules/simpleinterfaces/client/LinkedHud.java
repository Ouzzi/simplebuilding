package com.simplebuilding.modules.simpleinterfaces.client;

import com.simplebuilding.modules.simpleinterfaces.linked.LinkedContainers;
import com.simplebuilding.modules.simpleinterfaces.network.LinkedCancelPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The marked container as a slim HUD banner on the right edge, a little above the middle (where the Octant readout
 * would sit when it is moved to the right, but clear of it): the block's item, its kind, and its own name when it has
 * one. Same look as the mod's gadget panels (flat dark box, 1 px frame in the octant bronze band tone, 78 % opacity,
 * cut corners). HUD only, no screen. The server says when it appears and disappears; a new player object (respawn,
 * rejoin) clears it, because the mark does not survive either. Escape ends the mark ({@link #cancel}).
 */
public final class LinkedHud {
    /** The octant bronze, the base of the gadget panels' box and frame (copied: this module does not read core classes). */
    private static final int BRONZE = 0x6F5F4B;
    private static final int ALPHA = 0xC8;
    private static final int FILL = ALPHA << 24 | scale(BRONZE, 0.20);
    private static final int FRAME = ALPHA << 24 | scale(BRONZE, 0.64);
    private static final int SECONDARY = 0xFFB8B8B8;
    private static final int MARGIN = 10, PADDING = 4, ICON = 16, MAX_TEXT = 110, HEIGHT = ICON + PADDING * 2;
    /** Gap between the banner's bottom edge and the vertical middle of the screen (the Octant box reaches up ~30 px). */
    private static final int ABOVE_MIDDLE = 40;
    private static ItemStack icon = ItemStack.EMPTY;
    private static Component title = Component.empty();
    private static Object owner;

    private LinkedHud() {
    }

    static void set(ItemStack stack, Component name) {
        icon = stack;
        title = name;
        owner = Minecraft.getInstance().player;
    }

    public static boolean active() {
        Minecraft minecraft = Minecraft.getInstance();
        if (owner != minecraft.player) icon = ItemStack.EMPTY;
        return !icon.isEmpty();
    }

    /**
     * Escape in the world while a container is marked: ends the mark (the server decides, the banner goes at once)
     * instead of opening the pause menu. False when nothing is marked (Escape then behaves as always).
     */
    public static boolean cancel() {
        if (!active()) return false;
        icon = ItemStack.EMPTY;
        LinkedContainers.cancelToServer.accept(new LinkedCancelPayload());
        return true;
    }

    /** The kind line (the block's name) and the name line (the container's own name when it differs, else empty). */
    static Component[] lines(ItemStack stack, Component name) {
        Component kind = stack.getHoverName();
        boolean named = !name.getString().isEmpty() && !name.getString().equals(kind.getString());
        return new Component[] {kind, named ? name : Component.empty()};
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active() || minecraft.gui.screen() != null) return;
        Font font = minecraft.font;
        Component[] lines = lines(icon, title);
        boolean two = !lines[1].getString().isEmpty();
        int text = Math.min(MAX_TEXT, Math.max(font.width(lines[0]), two ? font.width(lines[1]) : 0));
        int width = PADDING + ICON + PADDING + text + PADDING;
        int x = graphics.guiWidth() - MARGIN - width;
        int y = graphics.guiHeight() / 2 - ABOVE_MIDDLE - HEIGHT;
        graphics.fill(x + 1, y, x + width - 1, y + 1, FRAME);
        graphics.fill(x + 1, y + HEIGHT - 1, x + width - 1, y + HEIGHT, FRAME);
        graphics.fill(x, y + 1, x + 1, y + HEIGHT - 1, FRAME);
        graphics.fill(x + width - 1, y + 1, x + width, y + HEIGHT - 1, FRAME);
        graphics.fill(x + 1, y + 1, x + width - 1, y + HEIGHT - 1, FILL);
        graphics.item(icon, x + PADDING, y + PADDING);
        int tx = x + PADDING + ICON + PADDING;
        int ty = y + (two ? PADDING + 1 : (HEIGHT - font.lineHeight) / 2);
        graphics.text(font, font.substrByWidth(lines[0], text).getVisualOrderText(), tx, ty, 0xFFFFFFFF, true);
        if (two) graphics.text(font, font.substrByWidth(lines[1], text).getVisualOrderText(), tx, ty + font.lineHeight + 1, SECONDARY, true);
    }

    private static int scale(int color, double f) {
        int r = (int) Math.min(255, ((color >> 16) & 255) * f), g = (int) Math.min(255, ((color >> 8) & 255) * f),
                b = (int) Math.min(255, (color & 255) * f);
        return r << 16 | g << 8 | b;
    }
}
