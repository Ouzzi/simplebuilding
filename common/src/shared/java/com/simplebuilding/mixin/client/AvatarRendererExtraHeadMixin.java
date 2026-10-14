package com.simplebuilding.mixin.client;

import com.simplebuilding.util.ExtraHead;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Owner N55: a head in the extra head slot has precedence over a head in the helmet slot. Both heads work (their effects
 * apply), but only one model is drawn - the extra head's ({@code ExtraHeadLayer}) - so the helmet slot's head is taken out
 * of the render state: Vanilla's {@code CustomHeadLayer} and the "mod head replaces the wearer's head" rule then see
 * nothing in the helmet slot. A helmet (armor) is not touched; it is drawn over the extra head.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererExtraHeadMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void simplebuilding$extraHeadHasPrecedence(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        if (!state.isInvisible && entity instanceof Player player && !ExtraHead.get(player).isEmpty()) {
            state.wornHeadType = null;
            state.wornHeadProfile = null;
            state.headItem.clear();
        }
    }
}
