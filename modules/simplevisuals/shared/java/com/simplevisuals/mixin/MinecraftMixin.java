package com.simplevisuals.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class MinecraftMixin {
 @Inject(method="tick",at=@At("TAIL")) private void visuals$tick(CallbackInfo ci){var mc=(net.minecraft.client.Minecraft)(Object)this;com.simplevisuals.client.Immersion.tick(mc);com.simplevisuals.client.VisualsHud.tick(mc);}
 @Inject(method="onResourceLoadFinished",at=@At("TAIL")) private void visuals$resources(net.minecraft.client.GameLoadCookie cookie,CallbackInfo ci){com.simplevisuals.client.RenamedModels.reload((net.minecraft.client.Minecraft)(Object)this);}
}
