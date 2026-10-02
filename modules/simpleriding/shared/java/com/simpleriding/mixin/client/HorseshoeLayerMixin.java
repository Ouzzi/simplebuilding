package com.simpleriding.mixin.client;

import com.simpleriding.client.HorseshoeLayer;
import net.minecraft.client.renderer.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Horse, skeleton horse and zombie horse use the full-size equine mesh. */
@Mixin({HorseRenderer.class, UndeadHorseRenderer.class})
public abstract class HorseshoeLayerMixin {
 @SuppressWarnings({"unchecked","rawtypes"})
 @Inject(method="<init>",at=@At("TAIL"))
 private void simpleriding$layer(CallbackInfo ci){
  ((LivingRendererAccess)(Object)this).simpleriding$addLayer(new HorseshoeLayer((RenderLayerParent)(Object)this,1F));
 }
}
