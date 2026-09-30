package com.simplefun.mixin.client;

import com.simplefun.client.AnimalSkullModels;
import com.simplefun.heads.AnimalHead;
import java.util.Map;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(SkullBlockRenderer.class)
public class AnimalSkullRendererMixin {
  @Shadow @Final private static Map<SkullBlock.Type, Identifier> SKIN_BY_TYPE;

  @Inject(method = "<clinit>", at = @At("TAIL"))
  private static void fun$skins(CallbackInfo c) {
    for (var t : AnimalHead.values()) SKIN_BY_TYPE.put(t, AnimalSkullModels.texture(t));
  }

  @Inject(method = "createModel", at = @At("HEAD"), cancellable = true)
  private static void fun$model(
      EntityModelSet set, SkullBlock.Type type, CallbackInfoReturnable<SkullModelBase> c) {
    if (type instanceof AnimalHead t) c.setReturnValue(AnimalSkullModels.model(t));
  }
}
