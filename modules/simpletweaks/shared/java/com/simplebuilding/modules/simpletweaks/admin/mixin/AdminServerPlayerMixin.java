package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.simplebuilding.modules.simpletweaks.admin.blocklog.BlockRecorder;
import com.simplebuilding.modules.simpletweaks.admin.deaths.DeathBackups;
import com.simplebuilding.modules.simpletweaks.admin.freeze.Freeze;
import com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish;
import java.util.OptionalInt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Container log (open/close), death backups, vanish visibility, freeze (drop key). */
@Mixin(ServerPlayer.class)
public abstract class AdminServerPlayerMixin {
    @Inject(method = "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;", at = @At("RETURN"))
    private void simpletweaks$opened(MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
        if (cir.getReturnValue().isPresent()) BlockRecorder.opened((ServerPlayer) (Object) this);
    }

    @Inject(method = "doCloseContainer", at = @At("HEAD"))
    private void simpletweaks$closing(CallbackInfo ci) { BlockRecorder.closed((ServerPlayer) (Object) this); }

    @Inject(method = "die", at = @At("HEAD"))
    private void simpletweaks$died(DamageSource source, CallbackInfo ci) { DeathBackups.died((ServerPlayer) (Object) this, source); }

    @Inject(method = "broadcastToPlayer", at = @At("RETURN"), cancellable = true)
    private void simpletweaks$visible(ServerPlayer viewer, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && Vanish.hidden((ServerPlayer) (Object) this, viewer)) cir.setReturnValue(false);
    }

    @Inject(method = "drop(Z)V", at = @At("HEAD"), cancellable = true)
    private void simpletweaks$drop(boolean all, CallbackInfo ci) {
        var self = (ServerPlayer) (Object) this;
        if (Freeze.frozen(self)) { self.containerMenu.sendAllDataToRemote(); ci.cancel(); }
    }
}
