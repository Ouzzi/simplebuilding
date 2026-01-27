package com.simplebuilding.client.gui.tooltip;

import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.networking.ReinforcedBundleSelectionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.TooltipSubmenuHandler;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.BundleItemSelectedC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class ReinforcedBundleTooltipSubmenuHandler implements TooltipSubmenuHandler {
    private final MinecraftClient client;

    public ReinforcedBundleTooltipSubmenuHandler(MinecraftClient client) {
        this.client = client;
    }

    @Override
    public boolean isApplicableTo(Slot slot) {
        // Das stellt sicher, dass unser Handler nur aktiv wird, wenn die Maus über deinem Bundle ist.
        return slot.hasStack() && slot.getStack().getItem() instanceof com.simplebuilding.items.custom.ReinforcedBundleItem;
    }

    @Override
    public boolean onScroll(double horizontal, double vertical, int slotId, ItemStack stack) {
        var data = stack.getTooltipData();
        if (data.isPresent() && data.get() instanceof ReinforcedBundleTooltipData reinforcedData) {
            BundleContentsComponent contents = reinforcedData.contents();
            int size = contents.size();

            if (size == 0) return false;

            int currentIndex = contents.getSelectedStackIndex();
            if (currentIndex == -1) currentIndex = 0;

            // Richtung umkehren für natürliches Scrollen
            int scrollDelta = (int) -vertical;
            int newIndex = MathHelper.clamp(currentIndex + scrollDelta, 0, size - 1);

            if (newIndex != currentIndex) {
                // 1. Paket an Server senden (für Logik)
                if (this.client.getNetworkHandler() != null) {
                    ClientPlayNetworking.send(new ReinforcedBundleSelectionPayload(slotId, newIndex));
                }

                // 2. WICHTIG: Client-Item sofort updaten (für visuelles Feedback im Tooltip)
                ReinforcedBundleItem.setBundleSelectedItem(stack, newIndex);

                return true;
            }
        }
        return false;
    }

    @Override
    public void onMouseClick(Slot slot, SlotActionType actionType) {
        // Reset bei Klick, falls gewünscht, oder leer lassen
    }

    @Override
    public void reset(Slot slot) {
        if (this.client.getNetworkHandler() != null) {
            ClientPlayNetworking.send(new ReinforcedBundleSelectionPayload(slot.id, -1));
        }
    }
}