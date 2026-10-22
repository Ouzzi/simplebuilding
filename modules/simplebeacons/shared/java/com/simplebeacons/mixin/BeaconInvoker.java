package com.simplebeacons.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Tests call Vanilla's private {@code applyEffects} directly to check {@link BeaconRangeMixin}. */
@Mixin(BeaconBlockEntity.class)
public interface BeaconInvoker {
    @Invoker("applyEffects")
    static void simplebeacons$applyEffects(Level level, BlockPos pos, int levels, Holder<MobEffect> primary, Holder<MobEffect> secondary) {
        throw new AssertionError();
    }
}
