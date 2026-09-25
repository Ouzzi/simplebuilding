package com.simplebuilding.tweaks.neoforge.mixin;

import com.simplebuilding.tweaks.xp.StackLimits;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Raketen-Stapelgroesse auf NeoForge: NeoForge deklariert ItemStack#getMaxStackSize selbst (fragt
 * das Item), deshalb hier ein Inject statt des Fabric/Forge-Mixins, das die Methode erst anlegt.
 */
@Mixin(ItemStack.class)
public abstract class TweaksNeoForgeItemStackMixin {
    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$limitStack(CallbackInfoReturnable<Integer> cir) {
        int limited = StackLimits.limit((ItemStack) (Object) this, cir.getReturnValueI());
        if (limited != cir.getReturnValueI()) {
            cir.setReturnValue(limited);
        }
    }
}
