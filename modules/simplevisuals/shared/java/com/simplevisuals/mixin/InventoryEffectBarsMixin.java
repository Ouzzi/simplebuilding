package com.simplevisuals.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;import net.minecraft.resources.Identifier;import com.mojang.renderpearl.api.pipeline.RenderPipeline;
@Mixin(net.minecraft.client.gui.screens.inventory.EffectsInInventory.class)
public abstract class InventoryEffectBarsMixin {
 @WrapOperation(method="extractEffects",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
 private void visuals$bar(GuiGraphicsExtractor g,RenderPipeline pipeline,Identifier sprite,int x,int y,int w,int h,Operation<Void> original,@Local net.minecraft.world.effect.MobEffectInstance effect){original.call(g,pipeline,sprite,x,y,w,h);if(w==18)com.simplevisuals.client.VisualsHud.effectBar(g,effect,x,y+h,w);}
}
