package com.simplebuilding.modules.simpletools.mixin;

import com.simplebuilding.modules.simpletools.soulbound.Soulbound;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Respawn after a death: the Soulbound stacks move from the dead player into the same slots of the new one. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerRestoreMixin {
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void simpletools$restoreSoulbound(ServerPlayer old, boolean restoreAll, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        boolean vanillaKeeps = self.level().getGameRules().get(GameRules.KEEP_INVENTORY) || old.isSpectator();
        Soulbound.restore(self, old, restoreAll, vanillaKeeps);
    }
}
