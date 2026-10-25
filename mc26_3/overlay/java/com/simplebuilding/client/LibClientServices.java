package com.simplebuilding.client;

import com.simplebuilding.client.gui.HudPanel;
import com.simplebuilding.client.gui.TrimStatsPanel;
import com.simplebuilding.util.ExtraHead;
import com.simplebuilding.util.TrimStatsLayout;
import com.simplelib.api.client.GadgetHud;
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
        // OW24-2 G1: the gadget readouts stack through SimpleLib's GadgetHud, so Simple Gadgets' Gauge (order 1) sits between
        // the Octant (0) and the Resonance Rod (2) as before; where the boxes go stays SimpleBuilding's client config.
        for (HudPanel.Slot slot : HudPanel.Slot.values()) {
            // The Gauge and the Resonance Rod register their boxes themselves on 26.3 (Simple Gadgets).
            if ((slot == HudPanel.Slot.GAUGE || slot == HudPanel.Slot.ROD) && !com.simplebuilding.version.McVersion.GADGETS_IN_SB) continue;
            GadgetHud.register(hudSlotId(slot), slot.ordinal(), client -> HudPanel.active(slot, client));
        }
        // OW24-2 G2: the Transposer's model property (Simple Gadgets) asks SimpleBuilding's full hand-hint question.
        com.simplelib.api.GadgetActions.registerHandHint(com.simplebuilding.util.TransformTargets::canTransformTarget);
        GadgetHud.useSettings(() -> {
            var config = com.simplebuilding.Simplebuilding.getConfig();
            return config == null ? GadgetHud.DEFAULTS
                    : new GadgetHud.Settings(config.showModHud, config.hudPositionX, config.hudPositionY, config.hudScale);
        });
    }

    private static String hudSlotId(HudPanel.Slot slot) {
        return "simplebuilding:" + slot.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Offset of SimpleBuilding's HUD box {@code slot} among every shown gadget box (SimpleLib {@link GadgetHud}); 26.2: null = own stack. */
    public static Integer gadgetHudOffset(HudPanel.Slot slot, Minecraft client) {
        return GadgetHud.stackOffset(hudSlotId(slot), client);
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
