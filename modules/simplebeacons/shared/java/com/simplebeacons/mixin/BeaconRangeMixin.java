package com.simplebeacons.mixin;

import com.simplebeacons.beacon.RangeFactor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SP22-10: the Vanilla beacon's range times the pyramid's material factor ({@link RangeFactor#vanillaPercent}, switch
 * {@code vanillaBeaconMaterialRange}). HEAD remembers the percent of this call (server thread only), the box inflate
 * uses it; an Iron pyramid or the switch off keeps Vanilla's range exactly.
 */
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconRangeMixin {
    @Unique
    private static int simplebeacons$percent = RangeFactor.NONE;

    @Inject(method = "applyEffects", at = @At("HEAD"))
    private static void simplebeacons$factor(Level level, BlockPos pos, int levels, Holder<MobEffect> primary, Holder<MobEffect> secondary,
                                             CallbackInfo ci) {
        simplebeacons$percent = level.isClientSide() ? RangeFactor.NONE : RangeFactor.vanillaPercent(level, pos, levels);
    }

    @ModifyArg(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;inflate(D)Lnet/minecraft/world/phys/AABB;"))
    private static double simplebeacons$range(double range) {
        return RangeFactor.apply(range, simplebeacons$percent);
    }
}
