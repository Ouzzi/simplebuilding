package com.simplemobheads.mixin;

import com.simplemobheads.easter.Technoblade;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.pig.Pig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Technoblade (EH21 TO-1): lightning leaves Technoblade a pig (no zombified piglin, no fire). */
@Mixin(Pig.class)
public abstract class PigThunderMixin {
    @Inject(method = "thunderHit", at = @At("HEAD"), cancellable = true)
    private void simplemobheads$crownResistsLightning(ServerLevel level, LightningBolt bolt, CallbackInfo ci) {
        if (Technoblade.resistsLightning((Pig) (Object) this)) {
            ci.cancel();
        }
    }
}
