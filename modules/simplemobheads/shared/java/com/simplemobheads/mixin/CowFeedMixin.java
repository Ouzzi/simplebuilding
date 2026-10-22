package com.simplemobheads.mixin;

import com.simplemobheads.easter.Giants;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The giant cow (EH21 CO-2): a cow eats a hay bale from the player's hand - each one a rare try at growing into a giant. */
@Mixin(AbstractCow.class)
public abstract class CowFeedMixin {
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void simplemobheads$hayForAGiant(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if ((Object) this instanceof Cow cow) {
            InteractionResult result = Giants.feedHay(cow, player, hand, null);
            if (result != null) {
                cir.setReturnValue(result);
            }
        }
    }
}
