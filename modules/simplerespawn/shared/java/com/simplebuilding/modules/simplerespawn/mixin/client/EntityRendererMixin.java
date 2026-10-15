package com.simplebuilding.modules.simplerespawn.mixin.client;

import com.simplebuilding.modules.simplerespawn.client.RespawnClient;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The name tag above a downed player is red and shows the bleed-out time left. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void simplerespawn$downedName(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (entity instanceof Player player) cir.setReturnValue(RespawnClient.nameTag(player, cir.getReturnValue()));
    }
}
