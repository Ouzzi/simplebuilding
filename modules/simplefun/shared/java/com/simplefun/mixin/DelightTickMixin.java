package com.simplefun.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;import net.minecraft.server.level.ServerPlayer;
@Mixin(ServerPlayer.class)public class DelightTickMixin {@Inject(method="tick",at=@At("TAIL"))private void fun$tick(CallbackInfo c){com.simplefun.FunDelights.tick((ServerPlayer)(Object)this);}}
