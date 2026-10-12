package com.simplebuilding.modules.simplerespawn.mixin;

import com.simplebuilding.modules.simplerespawn.RespawnServer;
import com.simplebuilding.modules.simplerespawn.SimpleRespawn;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Both sides: a downed player lies (swimming pose), cannot attack or interact; a right-click on a downed body revives. */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void simplerespawn$lie(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (SimpleRespawn.isDowned(self)) {
            self.setPose(Pose.SWIMMING);
            ci.cancel();
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void simplerespawn$noAttack(Entity target, CallbackInfo ci) {
        if (SimpleRespawn.isDowned((Player) (Object) this)) ci.cancel();
    }

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void simplerespawn$interact(Entity target, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        Player self = (Player) (Object) this;
        if (SimpleRespawn.isDowned(self)) {
            cir.setReturnValue(InteractionResult.PASS);
        } else if (target instanceof Player body && SimpleRespawn.isDowned(body)) {
            if (self instanceof ServerPlayer helper && body instanceof ServerPlayer downed) RespawnServer.tryRevive(helper, downed);
            cir.setReturnValue(self.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
        }
    }
}
