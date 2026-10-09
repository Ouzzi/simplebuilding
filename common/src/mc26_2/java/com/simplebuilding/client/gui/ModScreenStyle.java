package com.simplebuilding.client.gui;

import com.simplebuilding.fletching.FletchingMenu;
import com.simplebuilding.screen.AutoSmitherMenu;
import com.simplebuilding.screen.BackpackLayout;
import com.simplebuilding.screen.BackpackMenu;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.screen.TieredChestMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 26.2 twin of the 26.3 overlay: there is no SimpleLib on 26.2, so the mod screens keep their old look. Every
 * method answers {@code false} ("not drawn"); the filter key is a plain Vanilla button.
 */
public final class ModScreenStyle {
    public static final boolean ACTIVE = false;

    private ModScreenStyle() {}

    public static boolean tieredChest(GuiGraphicsExtractor g, TieredChestMenu menu, Font font, Component title, int left, int top) {
        return false;
    }

    public static boolean tieredChestLabels(GuiGraphicsExtractor g, Font font, TieredChestMenu menu, Component title, int titleX, int titleY) {
        return false;
    }

    public static Button hopperFilterButton(int x, int y, Button.OnPress onPress, NetheriteHopperScreenHandler menu) {
        return Button.builder(Component.empty(), onPress).bounds(x, y, 18, 18).build();
    }

    public static boolean hopper(GuiGraphicsExtractor g, NetheriteHopperScreenHandler menu, Font font, Component title, int left, int top,
            int imageWidth) {
        return false;
    }

    public static boolean hopperLabels(GuiGraphicsExtractor g, Font font, NetheriteHopperScreenHandler menu, Component title, int x, int y) {
        return false;
    }

    public static boolean autoSmither(GuiGraphicsExtractor g, AutoSmitherMenu menu, Font font, Component title, int left, int top,
            int imageWidth) {
        return false;
    }

    public static boolean autoSmitherLabels(GuiGraphicsExtractor g, Font font, Component title, int x, int y) {
        return false;
    }

    public static Button crafterFilterButton(int x, int y, Button.OnPress onPress, com.simplebuilding.screen.AutonomousCrafterMenu menu) {
        return Button.builder(Component.empty(), onPress).bounds(x, y, 18, 18).build();
    }

    public static boolean autonomousCrafter(GuiGraphicsExtractor g, com.simplebuilding.screen.AutonomousCrafterMenu menu, Font font,
            Component title, int left, int top, int imageWidth) {
        return false;
    }

    public static boolean autonomousCrafterLabels(GuiGraphicsExtractor g, Font font, Component title, int x, int y) {
        return false;
    }

    public static boolean storageCraftingTable(GuiGraphicsExtractor g, com.simplebuilding.screen.StorageCraftingMenu menu, Font font,
            Component title, int left, int top, int imageWidth) {
        return false;
    }

    public static boolean storageCraftingTableLabels(GuiGraphicsExtractor g, Font font, Component title, int x, int y) {
        return false;
    }

    public static boolean fletching(GuiGraphicsExtractor g, FletchingMenu menu, Font font, Component title, int left, int top,
            int imageWidth) {
        return false;
    }

    public static boolean fletchingLabels(GuiGraphicsExtractor g, Font font, Component title, int x, int y) {
        return false;
    }

    /** Slot colour of the styled backpack (client tests); 0 = not styled. */
    public static int backpackSlotColor(BackpackMenu menu) {
        return 0;
    }

    public static boolean backpack(GuiGraphicsExtractor g, BackpackMenu menu, BackpackLayout layout, int left, int top) {
        return false;
    }
}
