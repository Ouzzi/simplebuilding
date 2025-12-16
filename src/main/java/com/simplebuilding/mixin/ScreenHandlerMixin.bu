package com.simplebuilding.mixin;

import com.simplebuilding.blocks.entity.custom.ModChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {

    @Shadow public abstract void setCursorStack(ItemStack stack);
    @Shadow public abstract ItemStack getCursorStack();

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void handleCustomStackSizeClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (slotIndex < 0) return;

        ScreenHandler self = (ScreenHandler)(Object)this;
        if (slotIndex >= self.slots.size()) return;

        Slot slot = self.slots.get(slotIndex);
        Inventory inventory = slot.inventory;

        if (inventory instanceof ModChestBlockEntity) {
            ItemStack slotStack = slot.getStack();
            ItemStack cursorStack = this.getCursorStack();

            // Linksklick auf >64 Stack
            if (actionType == SlotActionType.PICKUP && button == 0 && cursorStack.isEmpty() && !slotStack.isEmpty()) {
                if (slotStack.getCount() > 64) {
                    ItemStack toHand = slotStack.copy();
                    toHand.setCount(64); // Max 64 in die Hand
                    this.setCursorStack(toHand);

                    slotStack.decrement(64);
                    slot.markDirty();
                    ci.cancel();
                }
            }
            // Rechtsklick (Halbieren)
            else if (actionType == SlotActionType.PICKUP && button == 1 && cursorStack.isEmpty() && !slotStack.isEmpty()) {
                if (slotStack.getCount() / 2 > 64) {
                    ItemStack toHand = slotStack.copy();
                    toHand.setCount(64);
                    this.setCursorStack(toHand);
                    slotStack.decrement(64);
                    slot.markDirty();
                    ci.cancel();
                }
            }
        }
    }
}