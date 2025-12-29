package com.simplebuilding.client.gui.tooltip;

import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
        // Prüft, ob das Item im Slot unser spezielles Bundle ist
        return slot.hasStack() && slot.getStack().getTooltipData().isPresent()
                && slot.getStack().getTooltipData().get() instanceof ReinforcedBundleTooltipData;
    }

    @Override
    public boolean onScroll(double horizontal, double vertical, int slotId, ItemStack stack) {
        var data = stack.getTooltipData();
        if (data.isPresent() && data.get() instanceof ReinforcedBundleTooltipData reinforcedData) {
            BundleContentsComponent contents = reinforcedData.contents();
            int size = contents.size();

            if (size == 0) return false;

            int currentIndex = contents.getSelectedStackIndex();
            if (currentIndex == -1) {
                currentIndex = 0;
            }

            // Scroll-Richtung umkehren für intuitiveres Gefühl (optional)
            int scrollDelta = (int) -vertical;
            int newIndex = currentIndex + scrollDelta;

            // Index sicherstellen (zwischen 0 und size-1)
            newIndex = MathHelper.clamp(newIndex, 0, size - 1);

            if (newIndex != currentIndex) {
                // Sende Paket an Server
                if (this.client.getNetworkHandler() != null) {
                    this.client.getNetworkHandler().sendPacket(new BundleItemSelectedC2SPacket(slotId, newIndex));
                }
                // Gibt true zurück, damit das Event konsumiert wird (kein normales Inventar-Scrollen)
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMouseClick(Slot slot, SlotActionType actionType) {
        // Reset Auswahl bei Klick
        reset(slot);
    }

    @Override
    public void reset(Slot slot) {
        if (this.client.getNetworkHandler() != null) {
            this.client.getNetworkHandler().sendPacket(new BundleItemSelectedC2SPacket(slot.id, -1));
        }
    }
}