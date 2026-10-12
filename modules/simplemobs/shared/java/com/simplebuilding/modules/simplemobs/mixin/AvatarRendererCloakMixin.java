package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.client.CloakLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cloak of deception (owner 12.10.): every player renderer gets the cloak on the back (loader-neutral). */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererCloakMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "<init>", at = @At("TAIL"))
    private void simplemobs$cloakLayer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        AvatarRenderer self = (AvatarRenderer) (Object) this;
        ((LivingEntityRendererAccessor) self).simplemobs$addLayer(new CloakLayer(self, context.getModelSet(), context.getEquipmentAssets()));
    }
}
