package com.simplebuilding.mixin;

import com.simplebuilding.blocks.entity.custom.ModChestBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {

    @Shadow @Final public Inventory inventory;

    @Inject(method = "getMaxItemCount(Lnet/minecraft/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
    private void limitStackSizeByInventory(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (this.inventory instanceof ModChestBlockEntity modChest) {
            cir.setReturnValue(modChest.getMaxCountPerStack());
        }
        else {
            if (stack.getItem().getMaxCount() == 64) {
                cir.setReturnValue(64);
            }
        }
    }
}