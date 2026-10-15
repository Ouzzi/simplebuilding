package com.simplebuilding.modules.simplemobmanners.mixin;

import com.simplebuilding.modules.simplemobmanners.FrogBucket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Frogs have no mobInteract of their own: hook the Animal one and only act for frogs holding a water bucket. */
@Mixin(Animal.class)
public abstract class FrogBucketMixin {
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void simplemobmanners$catchFrog(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if ((Object) this instanceof Frog frog) {
            FrogBucket.pickup(frog, player, hand).ifPresent(cir::setReturnValue);
        }
    }
}
