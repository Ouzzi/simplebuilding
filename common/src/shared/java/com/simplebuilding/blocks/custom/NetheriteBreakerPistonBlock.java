package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class NetheriteBreakerPistonBlock extends PistonBaseBlock {
    public static final MapCodec<NetheriteBreakerPistonBlock> CODEC = simpleCodec(NetheriteBreakerPistonBlock::new);

    public NetheriteBreakerPistonBlock(Properties settings) {
        super(false, settings);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<PistonBaseBlock> codec() {
        return (MapCodec<PistonBaseBlock>) (Object) CODEC;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int type, int data) {
        // type 0 = Piston Event (Ausfahren/Einfahren)
        if (type == 0) {
            // Wir prüfen nur beim Ausfahren
            if (!state.getValue(EXTENDED)) {
                Direction facing = state.getValue(FACING);
                BlockPos targetPos = pos.relative(facing);
                BlockState targetState = world.getBlockState(targetPos);
                if (!targetState.isAir() && targetState.getDestroySpeed(world, targetPos) >= 0) {
                    int power = world.getBestNeighborSignal(pos);
                    float breakThreshold = (power / 15.0f) * 50.0f;
                    float blockHardness = targetState.getDestroySpeed(world, targetPos);

                    // Nur brechen, wenn das Signal stark genug ist!
                    if (blockHardness <= breakThreshold) {

                        if (targetState.getPistonPushReaction() != PushReaction.BLOCK) {
                            world.destroyBlock(targetPos, true);
                            if (!world.isClientSide()) {
                                world.playSound(null, pos, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.BLOCKS, 0.5f, 0.8f);
                            }
                        }
                    }
                }
            }
        }

        return super.triggerEvent(state, world, pos, type, data);
    }
}