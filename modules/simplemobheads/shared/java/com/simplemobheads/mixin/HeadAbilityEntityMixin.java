package com.simplemobheads.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplemobheads.ability.HeadAbilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Drowned head (magma bubble columns do not drag down), warden head (every second footstep sends no vibration) and the
 * right click on a player wearing a cow, sheep or mooshroom head ({@link HeadAbilities#interact}).
 */
@Mixin(Entity.class)
public abstract class HeadAbilityEntityMixin {
    @Inject(method = "onInsideBubbleColumn", at = @At("HEAD"), cancellable = true)
    private void simplemobheads$drownedHeadInside(boolean dragDown, CallbackInfo ci) {
        if (HeadAbilities.resistsDownwardBubbles((Entity) (Object) this, dragDown)) {
            ci.cancel();
        }
    }

    @Inject(method = "onAboveBubbleColumn", at = @At("HEAD"), cancellable = true)
    private void simplemobheads$drownedHeadAbove(boolean dragDown, BlockPos pos, CallbackInfo ci) {
        if (HeadAbilities.resistsDownwardBubbles((Entity) (Object) this, dragDown)) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "vibrationAndSoundEffectsFromBlock", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V"))
    private void simplemobheads$wardenHeadMuffles(Level level, Holder<GameEvent> event, Vec3 pos, GameEvent.Context context, Operation<Void> original) {
        if (!HeadAbilities.muffledStep((Entity) (Object) this)) {
            original.call(level, event, pos, context);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void simplemobheads$headInteract(Player user, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        if ((Object) this instanceof Player target) {
            InteractionResult result = HeadAbilities.interact(target, user, hand);
            if (result != null) {
                cir.setReturnValue(result);
            }
        }
    }
}
