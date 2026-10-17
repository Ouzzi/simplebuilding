package com.simplebuilding.modules.simpletweaks.play.mixin;

import com.simplebuilding.modules.simpletweaks.play.afk.Afk;
import com.simplebuilding.modules.simpletweaks.play.death.DeathHint;
import com.simplebuilding.modules.simpletweaks.play.keep.KeepInventory;
import com.simplebuilding.modules.simpletweaks.play.pvp.Pvp;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Death hint (T5), partial keep inventory on respawn (T27), personal PvP (T9), grey AFK name (T24). */
@Mixin(ServerPlayer.class)
public abstract class PlayServerPlayerMixin {
    @Inject(method = "die", at = @At("RETURN"))
    private void simpletweaks$playDied(DamageSource source, CallbackInfo ci) { DeathHint.died((ServerPlayer) (Object) this); }

    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void simpletweaks$playRestore(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
        if (!restoreAll) KeepInventory.restore((ServerPlayer) (Object) this, oldPlayer);
    }

    @Inject(method = "canHarmPlayer", at = @At("RETURN"), cancellable = true)
    private void simpletweaks$playPvp(Player target, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && !Pvp.allows((ServerPlayer) (Object) this, target)) cir.setReturnValue(false);
    }

    @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
    private void simpletweaks$playAfkName(CallbackInfoReturnable<Component> cir) {
        var self = (ServerPlayer) (Object) this;
        if (Afk.afk(self)) cir.setReturnValue(Afk.tabName(self, cir.getReturnValue()));
    }
}
