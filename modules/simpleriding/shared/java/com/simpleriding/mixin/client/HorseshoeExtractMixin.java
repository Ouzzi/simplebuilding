package com.simpleriding.mixin.client;

import com.simpleriding.Horseshoes;
import com.simpleriding.client.HorseshoeState;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copies the synced hoof code into the render state. */
@Mixin(AbstractHorseRenderer.class)
public abstract class HorseshoeExtractMixin {
 @Inject(method="extractRenderState(Lnet/minecraft/world/entity/animal/equine/AbstractHorse;Lnet/minecraft/client/renderer/entity/state/EquineRenderState;F)V",at=@At("TAIL"))
 private void simpleriding$shoes(AbstractHorse horse,EquineRenderState state,float partialTicks,CallbackInfo ci){
  ((HorseshoeState)state).simpleriding$setShoes(Horseshoes.canWear(horse)?Horseshoes.shoes(Horseshoes.code(horse)):0);
 }
}
