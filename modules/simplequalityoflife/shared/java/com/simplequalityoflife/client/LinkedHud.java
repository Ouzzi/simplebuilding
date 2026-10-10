package com.simplequalityoflife.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * The marked container as a small HUD slot (top left, container style like the linked panel): the block's item in
 * a sunk slot. HUD only, no screen. The server says when it appears and disappears; a new player object (respawn,
 * rejoin) clears it, because the mark does not survive either.
 */
public final class LinkedHud {
    private static final UiPalette PALETTE = UiPalette.derived(0xFFCE9148);
    private static ItemStack icon = ItemStack.EMPTY;
    private static Object owner;

    private LinkedHud() {
    }

    static void set(ItemStack stack) {
        icon = stack;
        owner = Minecraft.getInstance().player;
    }

    public static boolean active() {
        Minecraft minecraft = Minecraft.getInstance();
        if (owner != minecraft.player) icon = ItemStack.EMPTY;
        return !icon.isEmpty();
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active() || minecraft.gui.screen() != null) return;
        UiBoxes.box(graphics, 4, 4, 28, 30, PALETTE);
        UiBoxes.slot(graphics, 9, 9, PALETTE);
        graphics.item(icon, 10, 10);
    }
}
