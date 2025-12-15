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

    @Shadow public abstract int getMaxItemCount();

    @Inject(method = "getMaxItemCount(Lnet/minecraft/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
    private void overrideMaxItemCount(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        // Wenn das Inventar dieses Slots zu unserer Mod-Kiste gehört
        if (this.inventory instanceof ModChestBlockEntity) {
            // Ignoriere stack.getMaxCount() (was 64 wäre) und nimm das Limit des Inventars (128/256)
            cir.setReturnValue(this.getMaxItemCount());
        }
    }
}