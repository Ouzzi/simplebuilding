package com.simplebuilding.effect;

import com.simplebuilding.fluid.SoulLava;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Seelenbrand (owner round 2 answer 28 + round 3 answer 55): after touching soul lava, by default for two minutes,
 * every {@link SoulLava#soulBurnIntervalTicks()} ticks a {@link SoulLava#soulBurnChance()} chance of 1
 * fire damage - on cold ground (water, ice, snow below the feet) at half that interval, so the chill bites
 * twice as often (owner round 11 P1). Fire resistance only protects against the burning and the fire damage
 * while it lasts - the effect itself keeps running, so damage comes back when the protection ends first. The
 * damage is Vanilla's on_fire type (tag is_fire): the Fire Resistance effect blocks it, the Fire
 * Protection enchantment reduces it.
 */
public class SoulBurnEffect extends MobEffect {
    /** Soul fire turquoise. */
    public static final int COLOR = 0x3FD9E0;

    public SoulBurnEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
        if (mob.hasEffect(MobEffects.FIRE_RESISTANCE) || mob.getType().fireImmune()) return true;
        int interval = intervalTicks(level, mob.blockPosition());
        MobEffectInstance self = mob.getEffect(ModEffects.SOUL_BURN);
        if (self == null) return true;
        int left = self.getDuration();
        boolean due = left < 0 ? level.getGameTime() % interval == 0 : left % interval == 0;
        if (!due) return true;
        if (mob.getRandom().nextFloat() < SoulLava.soulBurnChance()) {
            mob.hurtServer(level, mob.damageSources().onFire(), 1.0F);
        }
        return true;
    }

    /**
     * Every tick: the gate needs the entity's remaining duration (and its cold ground), which this
     * hook does not get, so the pacing lives in {@link #applyEffectTick}.
     */
    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return true;
    }

    /** Ticks between damage rolls at {@code pos}: halved on cold ground, never below one. */
    public static int intervalTicks(ServerLevel level, BlockPos pos) {
        int base = SoulLava.soulBurnIntervalTicks();
        return coldGround(level, pos) ? Math.max(1, base / 2) : base;
    }

    /** The feet block, or the block below the feet, holds water, ice or snow. */
    public static boolean coldGround(ServerLevel level, BlockPos pos) {
        return coldAt(level, pos) || coldAt(level, pos.below());
    }

    private static boolean coldAt(ServerLevel level, BlockPos pos) {
        if (level.getFluidState(pos).is(FluidTags.WATER)) return true;
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.ICE) || state.is(BlockTags.SNOW)
                || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW);
    }
}
