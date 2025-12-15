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
        // Wir fragen das Inventar nach dem Limit.
        // Unsere Mod-Kiste gibt hier 128/256 zurück.
        // Das Spieler-Inventar gibt 64 zurück (Standard).

        int inventoryLimit = this.inventory.getMaxCountPerStack();

        // Da wir ItemStack global auf 1024 gepatcht haben (via CodecsMixin/ItemStackMixin),
        // würde stack.getMaxCount() jetzt 1024 zurückgeben.
        // Das Slot-Limit ist Math.min(InventoryLimit, StackLimit).

        // Wenn das Inventar-Limit > 64 ist (unsere Kiste), nehmen wir das.
        // Wenn es 64 ist (Spieler), nehmen wir 64.

        // Wir überschreiben die Logik, um sicherzugehen, dass unser 1024-Hack nicht durchschlägt,
        // wo er nicht soll, aber erlaubt wird, wo er soll.

        cir.setReturnValue(inventoryLimit);
    }
}