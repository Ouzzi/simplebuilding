package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.MobsConfig;
import com.simplebuilding.modules.simplemobs.StrongExplosives;
import net.minecraft.world.entity.item.PrimedTnt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Strong TNT (BS25-21): a primed TNT whose block state is the Strong TNT explodes with {@link MobsConfig#strongTntPower}
 * instead of 4. The power is set when the fuse runs out, so nothing extra has to be saved: the block state is.
 */
@Mixin(PrimedTnt.class)
abstract class PrimedTntStrongMixin {
    @Shadow private float explosionPower;

    @Inject(method = "explode", at = @At("HEAD"))
    private void simplemobs$strongPower(CallbackInfo ci) {
        if (((PrimedTnt) (Object) this).getBlockState().is(StrongExplosives.TNT_BLOCK)) {
            this.explosionPower = MobsConfig.strongTntPower;
        }
    }
}
