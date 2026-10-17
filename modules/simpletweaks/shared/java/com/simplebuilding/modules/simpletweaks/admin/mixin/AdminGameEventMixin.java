package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanish: no game events (sculk sensors, shriekers, wardens) caused by a vanished player. */
@Mixin(ServerLevel.class)
public abstract class AdminGameEventMixin {
    @Inject(method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V", at = @At("HEAD"), cancellable = true)
    private void simpletweaks$vibration(Holder<GameEvent> event, Vec3 pos, GameEvent.Context context, CallbackInfo ci) {
        if (context.sourceEntity() != null && Vanish.vanished(context.sourceEntity())) ci.cancel();
    }
}
