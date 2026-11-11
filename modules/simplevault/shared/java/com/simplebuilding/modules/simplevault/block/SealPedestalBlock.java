package com.simplebuilding.modules.simplevault.block;

import com.simplebuilding.modules.simplevault.VaultPuzzles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Seal Pedestal (UNDER21): one per riddle chamber. {@link #READY} once its riddle is solved (glows in the part's colour); then
 * right-click hands out the seal part ({@link VaultPuzzles#takeFragment}) - again whenever the altar still lacks it, so a lost
 * part never locks anyone in. Locked it tells what the room asks (A14). Unbreakable.
 */
public class SealPedestalBlock extends Block {
    public static final EnumProperty<SealPart> PART = EnumProperty.create("part", SealPart.class);
    public static final BooleanProperty READY = BooleanProperty.create("ready");

    public SealPedestalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, SealPart.LIGHT).setValue(READY, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, READY);
    }

    public static int light(BlockState state) {
        return state.getValue(READY) ? 10 : 2;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) VaultPuzzles.takeFragment(server, pos, state, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(READY) && random.nextInt(3) == 0) {
            level.addParticle(new DustParticleOptions(state.getValue(PART).color, 1.0F), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.05,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.03, 0.0);
        }
    }
}
