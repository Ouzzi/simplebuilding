package com.simplebuilding.tweaks.mixin;

import com.simplebuilding.tweaks.xp.StackLimits;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Raketen-Stapelgroesse (Simple Tweaks: ItemStackMixin, Config rocketStackSize). Auf 1.21.11
 * deklariert ItemStack getMaxStackSize auf jedem Loader selbst, also ein Inject (auf 26.x ist die
 * Methode dort eine Default-Methode von ItemInstance, siehe die 26.2-Fassung).
 */
@Mixin(ItemStack.class)
public abstract class TweaksItemStackMixin {
    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$limitStack(CallbackInfoReturnable<Integer> cir) {
        int limited = StackLimits.limit((ItemStack) (Object) this, cir.getReturnValueI());
        if (limited != cir.getReturnValueI()) {
            cir.setReturnValue(limited);
        }
    }
}
