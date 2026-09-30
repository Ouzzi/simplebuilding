package com.simplequalityoflife.mixin.client;
@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.multiplayer.ClientLevel.class)
public abstract class ClientWeatherMixin {
 @org.spongepowered.asm.mixin.injection.Inject(method="tickWeatherEffects",at=@org.spongepowered.asm.mixin.injection.At("HEAD"),cancellable=true)
 private void qol$rain(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){int d=com.simplequalityoflife.Simplequalityoflife.getLocalConfig().qOL.clientRainParticleDensity; if(((net.minecraft.client.multiplayer.ClientLevel)(Object)this).getRandom().nextInt(100)>=Math.clamp(d,0,100))ci.cancel();}
}
