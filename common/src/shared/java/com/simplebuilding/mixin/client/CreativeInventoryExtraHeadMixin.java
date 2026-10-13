package com.simplebuilding.mixin.client;

import com.simplebuilding.client.gui.TrimStatsPanel;
import com.simplebuilding.util.ExtraHead;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The extra head slot (owner N49, {@link ExtraHead}) on the creative inventory tab: Vanilla would put slot 46 into the
 * hotbar row (it places every index it does not know as an inventory slot). It goes right of the armour, mirroring the
 * off-hand slot on the left (35, 20), with its own frame.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryExtraHeadMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    protected CreativeInventoryExtraHeadMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Unique
    private static final int HEAD_X = 127, HEAD_Y = 20;

    @Shadow
    private static CreativeModeTab selectedTab;

    @ModifyArg(method = "selectTab", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"),
            index = 2)
    private int simplebuilding$extraHeadX(Slot target, int index, int x, int y) {
        return target instanceof ExtraHead.HeadSlot ? HEAD_X : x;
    }

    @ModifyArg(method = "selectTab", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"),
            index = 3)
    private int simplebuilding$extraHeadY(Slot target, int index, int x, int y) {
        return target instanceof ExtraHead.HeadSlot ? HEAD_Y : y;
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void simplebuilding$extraHeadFrame(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (selectedTab != null && selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
            TrimStatsPanel.slotFrame(graphics, this.leftPos + HEAD_X - 1, this.topPos + HEAD_Y - 1);
        }
    }
}
