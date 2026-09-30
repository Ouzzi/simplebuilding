package com.simplevisuals.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
@Mixin(net.minecraft.client.gui.contextualbar.LocatorBar.class)
public abstract class LocatorMixin {
 @WrapOperation(method="lambda$extractRenderState$1",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
 private void visuals$head(GuiGraphicsExtractor g,RenderPipeline pipeline,Identifier sprite,int x,int y,int w,int h,int tint,Operation<Void> original,@Local(argsOnly=true) net.minecraft.world.waypoints.TrackedWaypoint waypoint){
  var mc=net.minecraft.client.Minecraft.getInstance();var uuid=waypoint.id().left();
  var player=uuid.isPresent()&&mc.getConnection()!=null?mc.getConnection().getPlayerInfo(uuid.get()):null;
  if(!com.simplevisuals.Visuals.CONFIG.visuals.enablePlayerLocator||player==null){original.call(g,pipeline,sprite,x,y,w,h,tint);return;}
  var skin=player.getSkin().body().texturePath();
  g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,skin,x,y,8,8,w,h,8,8,64,64);
  g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,skin,x,y,40,8,w,h,8,8,64,64);
 }
}
