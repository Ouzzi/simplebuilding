package com.simplebuilding.client.gui.tooltip;

import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.networking.ReinforcedBundleSelectionPayload;
import com.simplebuilding.platform.ClientNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

public class ReinforcedBundleTooltipSubmenuHandler implements ItemSlotMouseAction {
    private final Minecraft client;

    public ReinforcedBundleTooltipSubmenuHandler(Minecraft client) {
        this.client = client;
    }

    @Override
    public boolean matches(Slot slot) {
        // Das stellt sicher, dass unser Handler nur aktiv wird, wenn die Maus über deinem Bundle ist.
        return slot.hasItem() && slot.getItem().getItem() instanceof com.simplebuilding.items.custom.ReinforcedBundleItem;
    }

    @Override
    public boolean onMouseScrolled(double horizontal, double vertical, int slotId, ItemStack stack) {
        var data = stack.getTooltipImage();
        if (data.isPresent() && data.get() instanceof ReinforcedBundleTooltipData reinforcedData) {
            BundleContents contents = reinforcedData.contents();
            int size = contents.size();

            if (size == 0) return false;

            int currentIndex = contents.getSelectedItemIndex();
            if (currentIndex == BundleContents.NO_SELECTED_ITEM_INDEX) currentIndex = 0;

            // Richtung umkehren für natürliches Scrollen
            int scrollDelta = (int) -vertical;
            int newIndex = Mth.clamp(currentIndex + scrollDelta, 0, size - 1);

            if (newIndex != currentIndex) {
                // 1. Paket an Server senden (für Logik)
                if (this.client.getConnection() != null) {
                    ClientNetworking.send(new ReinforcedBundleSelectionPayload(slotId, newIndex));
                }

                // 2. WICHTIG: Client-Item sofort updaten (für visuelles Feedback im Tooltip)
                ReinforcedBundleItem.setBundleSelectedItem(stack, newIndex);

                return true;
            }
        }
        return false;
    }

    public void onSlotClicked(Slot slot, ContainerInput actionType) {
        // Reset bei Klick, falls gewünscht, oder leer lassen
    }

    @Override
    public void onStopHovering(Slot slot) {
        if (this.client.getConnection() != null) {
            ClientNetworking.send(new ReinforcedBundleSelectionPayload(slot.index, -1));
        }
    }
}