package com.simplebuilding.tweaks.mixin;

import com.simplebuilding.tweaks.spawn.SpawnElytra;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Container-Slots (Truhe, Trichter, Endertruhe, Werkbank, Tierinventar ...) nehmen keine
 * Spawn-Elytra an (Audit 2026-09-26 #3); nur das eigene Spielerinventar, aus dem das Aufraeumen in
 * {@link SpawnElytra#removeStrayElytras} sie im selben Tick wieder entfernt, falls sie nicht im
 * Brust-Slot liegt. Klicken, Umschalt-Klick und Ziehen fragen alle {@code mayPlace}.
 */
@Mixin(Slot.class)
public abstract class SpawnElytraSlotMixin {
    @Shadow
    @Final
    public Container container;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$keepSpawnElytraOut(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (SpawnElytra.isSpawnElytra(stack) && !SpawnElytra.mayHold(container)) {
            cir.setReturnValue(false);
        }
    }
}
