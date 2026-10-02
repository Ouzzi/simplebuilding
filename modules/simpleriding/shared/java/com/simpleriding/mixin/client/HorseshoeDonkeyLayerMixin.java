package com.simpleriding.mixin.client;

import com.simpleriding.client.HorseshoeLayer;
import net.minecraft.client.model.animal.equine.DonkeyModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Donkeys and mules use the Vanilla mesh scaled like their own model layers. */
@Mixin(DonkeyRenderer.class)
public abstract class HorseshoeDonkeyLayerMixin {
 @SuppressWarnings({"unchecked","rawtypes"})
 @Inject(method="<init>",at=@At("TAIL"))
 private void simpleriding$layer(EntityRendererProvider.Context context,EquipmentClientInfo.LayerType saddleLayer,ModelLayerLocation saddleModel,DonkeyRenderer.Type adult,DonkeyRenderer.Type baby,CallbackInfo ci){
  float scale=adult==DonkeyRenderer.Type.MULE?DonkeyModel.MULE_SCALE:DonkeyModel.DONKEY_SCALE;
  ((LivingRendererAccess)(Object)this).simpleriding$addLayer(new HorseshoeLayer((RenderLayerParent)(Object)this,scale));
 }
}
