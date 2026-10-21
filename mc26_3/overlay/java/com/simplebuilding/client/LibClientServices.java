package com.simplebuilding.client;

import com.simplebuilding.client.gui.TrimStatsPanel;
import com.simplebuilding.util.ExtraHead;
import com.simplebuilding.util.TrimStatsLayout;
import com.simplelib.api.client.InventoryExtrasClient;
import com.simplelib.api.client.ui.UiShiftTooltip;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Client side of {@code util/LibServices} on 26.3 (split stage 8c-2, docs/ai/PLAN-B-S8C2-STORAGE-2026-10-21.md): Simple
 * Storage's backpack screen rebuilds the player inventory and draws what SimpleBuilding adds to it through SimpleLib
 * {@link InventoryExtrasClient} - the frame of the extra head slot (owner N49), the resonance button and the empty slot's
 * hint, as in the inventory. The slot itself comes through SimpleLib {@code InventoryExtras} ({@code LibServices.init}).
 * Tooltips fold behind SimpleLib's "Hold Shift for more" ({@link UiShiftTooltip}, owner 2026-10-21).
 */
public final class LibClientServices {
    private LibClientServices() {
    }

    /** From each loader's client entry. */
    public static void init() {
        InventoryExtrasClient.register(new InventoryExtrasClient.Panel() {
            private final TrimStatsPanel trimStats = new TrimStatsPanel();

            @Override
            public void background(GuiGraphicsExtractor graphics, int x, int y) {
                TrimStatsPanel.slotFrame(graphics, x + TrimStatsLayout.HEAD_SLOT_X - 1, y + TrimStatsLayout.HEAD_SLOT_Y - 1);
            }

            @Override
            public void foreground(GuiGraphicsExtractor graphics, Font font, Minecraft minecraft, int x, int y, int mouseX, int mouseY,
                                   Slot hovered, ItemStack carried) {
                this.trimStats.render(graphics, font, minecraft, x + TrimStatsLayout.INVENTORY_X, y + TrimStatsLayout.INVENTORY_Y, mouseX, mouseY);
                if (hovered instanceof ExtraHead.HeadSlot && !hovered.hasItem() && carried.isEmpty()) {
                    graphics.setTooltipForNextFrame(font, Component.translatable("gui.simplebuilding.extra_head_slot"), mouseX, mouseY);
                }
            }
        });
    }

    /** Shift held, as every Simple mod reads it ({@link UiShiftTooltip#held}). */
    public static boolean shiftHeld() {
        return UiShiftTooltip.held();
    }

    /** The first {@code brief} lines and "Hold Shift for more" without {@code shift}, else all ({@link UiShiftTooltip#collapse}). */
    public static List<Component> moreOnShift(List<Component> lines, int brief, boolean shift) {
        return UiShiftTooltip.collapse(lines, brief, shift);
    }
}
