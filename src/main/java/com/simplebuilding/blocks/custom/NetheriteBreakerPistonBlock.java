package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class NetheriteBreakerPistonBlock extends PistonBlock {
    public static final MapCodec<NetheriteBreakerPistonBlock> CODEC = createCodec(NetheriteBreakerPistonBlock::new);

    public NetheriteBreakerPistonBlock(Settings settings) {
        super(false, settings);
    }

    @Override
    public MapCodec<PistonBlock> getCodec() {
        return (MapCodec<PistonBlock>) (Object) CODEC;
    }

    @Override
    public boolean onSyncedBlockEvent(BlockState state, World world, BlockPos pos, int type, int data) {
        // type 0 = Piston Event (Ausfahren/Einfahren)
        if (type == 0) {
            // Wir prüfen nur beim Ausfahren
            if (!state.get(EXTENDED)) {
                Direction facing = state.get(FACING);
                BlockPos targetPos = pos.offset(facing);
                BlockState targetState = world.getBlockState(targetPos);
                if (!targetState.isAir() && targetState.getHardness(world, targetPos) >= 0) {
                    int power = world.getReceivedRedstonePower(pos);
                    float breakThreshold = (power / 15.0f) * 50.0f;
                    float blockHardness = targetState.getHardness(world, targetPos);

                    // Nur brechen, wenn das Signal stark genug ist!
                    if (blockHardness <= breakThreshold) {

                        if (targetState.getPistonBehavior() != PistonBehavior.BLOCK) {
                            world.breakBlock(targetPos, true);
                            if (!world.isClient()) {
                                world.playSound(null, pos, SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.BLOCKS, 0.5f, 0.8f);
                            }
                        }
                    }
                }
            }
        }

        return super.onSyncedBlockEvent(state, world, pos, type, data);
    }
}