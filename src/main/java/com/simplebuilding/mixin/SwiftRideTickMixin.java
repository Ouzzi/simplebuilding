package com.simplebuilding.mixin;

import com.simplebuilding.util.SwiftRideHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class SwiftRideTickMixin {

    // KEIN @Shadow mehr nötig! Wir nutzen den Helper.

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Performance-Check: Nur weitermachen, wenn ein Spieler das Entity steuert.
        if (entity.hasControllingPassenger() && entity.getControllingPassenger() instanceof PlayerEntity) {

            // Ruft die Logik im Helper auf (dort ist der NBT-Hack via Reflection)
            SwiftRideHelper.applyBoost(entity);

        } else {
            // Wenn kein Spieler mehr reitet, Boost entfernen
            SwiftRideHelper.removeBoost(entity);
        }
    }
}