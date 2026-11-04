package com.simplelib.mixin;

import com.simplelib.api.aging.Aging;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** DW04B-1: a dispenser's honeycomb also waxes aging-chain blocks (Vanilla's behaviour stays for everything else). */
@Mixin(DispenserBlock.class)
abstract class AgingDispenserMixin {
    @Inject(method = "getDispenseMethod", at = @At("RETURN"), cancellable = true)
    private void simplelib$waxChains(Level level, ItemStack stack, CallbackInfoReturnable<DispenseItemBehavior> cir) {
        if (stack.is(Items.HONEYCOMB)) cir.setReturnValue(Aging.dispenseHoneycomb(cir.getReturnValue()));
    }
}
