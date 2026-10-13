package com.simplebuilding.mixin;

import com.simplebuilding.networking.ExtraHeadPayload;
import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.util.ExtraHead;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A client that starts seeing a player gets the head in that player's extra head slot (owner N49). */
@Mixin(Entity.class)
public abstract class EntityExtraHeadSeenMixin {
    @Inject(method = "startSeenByPlayer", at = @At("TAIL"))
    private void simplebuilding$sendExtraHead(ServerPlayer viewer, CallbackInfo ci) {
        if ((Object) this instanceof Player player && !ExtraHead.get(player).isEmpty()
                && PlatformServices.canSendToPlayer(viewer, ExtraHeadPayload.ID)) {
            PlatformServices.sendToPlayer(viewer, new ExtraHeadPayload(player.getId(), ExtraHead.get(player).copy()));
        }
    }
}
