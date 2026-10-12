package com.simplebuilding.modules.simplefarming.mixin;
@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.level.block.FarmlandBlock.class)
public abstract class FarmlandTrampleMixin {
 @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="fallOn",require=0,at=@org.spongepowered.asm.mixin.injection.At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getBbHeight()F"))
 private float qol$protect(float height,@com.llamalad7.mixinextras.sugar.Local(argsOnly=true) net.minecraft.world.entity.Entity e){return com.simplebuilding.modules.simplefarming.Protection.farmland(e)?0:height;}
}
