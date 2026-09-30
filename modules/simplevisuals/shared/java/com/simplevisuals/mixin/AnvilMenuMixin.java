package com.simplevisuals.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(net.minecraft.world.inventory.AnvilMenu.class)
public abstract class AnvilMenuMixin {
 @Inject(method="validateName",at=@At("HEAD"),cancellable=true)
 private static void visuals$name(String name,CallbackInfoReturnable<String> ci){ci.setReturnValue(com.simplevisuals.Visuals.formatName(name,com.simplevisuals.Visuals.serverFormatting()));}
}
