package com.simplevisuals.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;
@Mixin(net.minecraft.client.renderer.item.ItemModelResolver.class)
public abstract class ItemModelMixin {
 @ModifyVariable(method="appendItemLayers",at=@At("HEAD"),argsOnly=true)
 private net.minecraft.world.item.ItemStack visuals$model(net.minecraft.world.item.ItemStack stack){return com.simplevisuals.client.RenamedModels.renderStack(stack);}
}
