package com.simplebuilding.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;

/**
 * Rissiges Eis (Besitzer N25): wer darauf steht, laesst es reissen - jede Sekunde mit jemandem darauf eine Stufe mehr
 * (Modelle auf Vanillas {@code frosted_ice_0..3}), nach der letzten Stufe wird es zu Wasser (im Nether verdampft es,
 * wie Eis). Steht niemand mehr darauf, bleibt der erreichte Riss stehen. Sonst verhaelt es sich wie Eis
 * (Licht schmilzt es, abgebaut ohne Behutsamkeit wird es zu Wasser, wenn darunter Wasser oder ein Block steht).
 */
public class CrackedIceBlock extends IceBlock {
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    /** Ticks je Rissstufe: vier Stufen, also nach gut vier Sekunden Draufstehen Wasser. */
    public static final int TICKS_PER_CRACK = 20;

    public CrackedIceBlock(Properties settings) {
        super(settings);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity && !entity.isSpectator()
                && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, TICKS_PER_CRACK);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isStoodOn(level, pos)) {
            return;
        }
        int age = state.getValue(AGE);
        if (age < MAX_AGE) {
            level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.7F, 0.6F + 0.15F * age);
            level.scheduleTick(pos, this, TICKS_PER_CRACK);
        } else {
            level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.8F, 1.2F);
            melt(state, level, pos);
        }
    }

    /** Steht ein Lebewesen (kein Zuschauer) auf der Oberseite? */
    public static boolean isStoodOn(Level level, BlockPos pos) {
        AABB top = new AABB(pos.getX(), pos.getY() + 1.0, pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.25, pos.getZ() + 1.0);
        return !level.getEntitiesOfClass(LivingEntity.class, top, entity -> !entity.isSpectator()).isEmpty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
