package com.simplelib.api.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Client half of {@link com.simplelib.api.InventoryExtras} (split stage 8c-2): what a mod draws into a rebuilt player
 * inventory - SimpleBuilding's frame of the extra head slot, its resonance button and the empty slot's hint. Coordinates are
 * on screen, {@code x}/{@code y} the top left of the 176 px wide player inventory panel.
 */
public final class InventoryExtrasClient {
    private static final List<Panel> PANELS = new CopyOnWriteArrayList<>();

    private InventoryExtrasClient() {}

    public interface Panel {
        /** Background pass, under the slots. */
        default void background(GuiGraphicsExtractor graphics, int x, int y) {}

        /** After the slots and tooltips: buttons and hover texts. */
        default void foreground(GuiGraphicsExtractor graphics, Font font, Minecraft minecraft, int x, int y, int mouseX, int mouseY,
                                @Nullable Slot hovered, ItemStack carried) {}
    }

    public static void register(Panel panel) {
        PANELS.add(panel);
    }

    public static void unregister(Panel panel) {
        PANELS.remove(panel);
    }

    public static void background(GuiGraphicsExtractor graphics, int x, int y) {
        for (Panel panel : PANELS) panel.background(graphics, x, y);
    }

    public static void foreground(GuiGraphicsExtractor graphics, Font font, Minecraft minecraft, int x, int y, int mouseX, int mouseY,
                                  @Nullable Slot hovered, ItemStack carried) {
        for (Panel panel : PANELS) panel.foreground(graphics, font, minecraft, x, y, mouseX, mouseY, hovered, carried);
    }
}
