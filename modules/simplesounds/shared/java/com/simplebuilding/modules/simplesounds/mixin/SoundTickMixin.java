package com.simplebuilding.modules.simplesounds.mixin;
import com.simplebuilding.modules.simplesounds.SoundClient;
@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.Minecraft.class)
public abstract class SoundTickMixin {
 @org.spongepowered.asm.mixin.injection.Inject(method="tick",at=@org.spongepowered.asm.mixin.injection.At("TAIL"))
 private void sounds$tick(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){SoundClient.tick((net.minecraft.client.Minecraft)(Object)this);}
}
