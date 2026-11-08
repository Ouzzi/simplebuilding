package com.simplebuilding.modules.simplelayers.block;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Neon Light (concept §2.1): a ceiling light, on / flickering / broken. Light 15 while on or flickering, 0 when broken; the
 * flicker is only the animated texture and a rare buzz, the light value never changes (no light updates, performance).
 * Right-click with an empty hand cycles the mode (not where the Backrooms protect their blocks).
 */
public class NeonLightBlock extends BackroomBlock {
    public enum Mode implements StringRepresentable {
        ON("on"), FLICKER("flicker"), BROKEN("broken");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    public NeonLightBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MODE, Mode.ON));
    }

    public static int light(BlockState state) {
        return state.getValue(MODE) == Mode.BROKEN ? 0 : 15;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (protectedFor(player)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            Mode next = Mode.values()[(state.getValue(MODE).ordinal() + 1) % Mode.values().length];
            level.setBlock(pos, state.setValue(MODE, next), 3);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(MODE) == Mode.FLICKER && random.nextInt(60) == 0) {
            level.playLocalSound(pos, LayersRegistry.NEON_BUZZ, SoundSource.BLOCKS, 0.35F, 0.9F + random.nextFloat() * 0.2F, false);
        }
    }
}
