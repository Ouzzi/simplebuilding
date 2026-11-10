package com.simplebuilding.modules.simplelayers.world;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.block.LevelDoorBlock;
import com.simplebuilding.modules.simplelayers.block.NeonLightBlock;
import com.simplebuilding.modules.simplelayers.world.gen.Piece;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Block state of every generator {@link Piece} (BACK18): the one place where the Minecraft-free generators meet the game. Built
 * on first use, after the registries (the module's blocks) exist.
 */
public final class BackroomsBlocks {
    private static volatile Map<Piece, BlockState> states;

    private BackroomsBlocks() {}

    public static BlockState state(Piece piece) {
        Map<Piece, BlockState> m = states;
        if (m == null) states = m = build();
        return m.get(piece);
    }

    private static BlockState concrete(DyeColor color) {
        return Blocks.CONCRETE.pick(color).defaultBlockState();
    }

    private static BlockState neon(NeonLightBlock.Mode mode) {
        return LayersRegistry.NEON_LIGHT.defaultBlockState().setValue(NeonLightBlock.MODE, mode);
    }

    private static BlockState door(Block block, DoubleBlockHalf half) {
        return block.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, half);
    }

    private static BlockState sealed(DoubleBlockHalf half) {
        return door(LayersRegistry.LEVEL_DOOR, half).setValue(LevelDoorBlock.SEALED, Boolean.TRUE);
    }

    private static BlockState stairs(Block block, Direction facing, Half half) {
        return block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing).setValue(BlockStateProperties.HALF, half);
    }

    private static BlockState facing(Block block, Direction facing) {
        return block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
    }

    private static BlockState rod(Direction facing) {
        return Blocks.LIGHTNING_ROD.waxed().unaffected().defaultBlockState().setValue(BlockStateProperties.FACING, facing);
    }

    private static BlockState lever(Direction facing) {
        return Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing).setValue(BlockStateProperties.POWERED, Boolean.FALSE);
    }

    private static BlockState tape(Direction.Axis axis) {
        return LayersRegistry.CAUTION_TAPE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, axis);
    }

    private static Map<Piece, BlockState> build() {
        Map<Piece, BlockState> m = new EnumMap<>(Piece.class);
        m.put(Piece.AIR, Blocks.AIR.defaultBlockState());
        m.put(Piece.BEDROCK, Blocks.BEDROCK.defaultBlockState());
        m.put(Piece.RIFT, LayersRegistry.RIFT.defaultBlockState());
        m.put(Piece.CARPET, LayersRegistry.DAMP_CARPET.defaultBlockState());
        m.put(Piece.WALLPAPER, LayersRegistry.WALLPAPER.defaultBlockState());
        m.put(Piece.STRIPED_WALLPAPER, LayersRegistry.STRIPED_WALLPAPER.defaultBlockState());
        m.put(Piece.DOTTED_WALLPAPER, LayersRegistry.DOTTED_WALLPAPER.defaultBlockState());
        m.put(Piece.CEILING_TILE, LayersRegistry.CEILING_TILE.defaultBlockState());
        m.put(Piece.STAINED_CEILING_TILE, LayersRegistry.STAINED_CEILING_TILE.defaultBlockState());
        m.put(Piece.NEON_ON, neon(NeonLightBlock.Mode.ON));
        m.put(Piece.NEON_FLICKER, neon(NeonLightBlock.Mode.FLICKER));
        m.put(Piece.NEON_BROKEN, neon(NeonLightBlock.Mode.BROKEN));
        m.put(Piece.EXIT_SIGN, LayersRegistry.EXIT_SIGN.defaultBlockState());
        m.put(Piece.EXIT_DOOR_LOWER, door(LayersRegistry.EXIT_DOOR, DoubleBlockHalf.LOWER));
        m.put(Piece.EXIT_DOOR_UPPER, door(LayersRegistry.EXIT_DOOR, DoubleBlockHalf.UPPER));
        m.put(Piece.LEVEL_DOOR_LOWER, door(LayersRegistry.LEVEL_DOOR, DoubleBlockHalf.LOWER));
        m.put(Piece.LEVEL_DOOR_UPPER, door(LayersRegistry.LEVEL_DOOR, DoubleBlockHalf.UPPER));
        m.put(Piece.SEALED_DOOR_LOWER, sealed(DoubleBlockHalf.LOWER));
        m.put(Piece.SEALED_DOOR_UPPER, sealed(DoubleBlockHalf.UPPER));
        m.put(Piece.COBWEB, Blocks.COBWEB.defaultBlockState());
        m.put(Piece.DESK, Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP));
        m.put(Piece.CHAIR_N, facing(Blocks.OAK_STAIRS, Direction.NORTH));
        m.put(Piece.CHAIR_S, facing(Blocks.OAK_STAIRS, Direction.SOUTH));
        m.put(Piece.LOGS_X, Blocks.STRIPPED_OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        m.put(Piece.CRAFTING_TABLE, Blocks.CRAFTING_TABLE.defaultBlockState());
        m.put(Piece.STEP_E, stairs(Blocks.BIRCH_STAIRS, Direction.EAST, Half.BOTTOM));
        m.put(Piece.STEP_S, stairs(Blocks.BIRCH_STAIRS, Direction.SOUTH, Half.BOTTOM));
        m.put(Piece.STAIR_BASE, Blocks.BIRCH_PLANKS.defaultBlockState());
        m.put(Piece.END_FLOOR, Blocks.BIRCH_PLANKS.defaultBlockState());
        m.put(Piece.END_WALL, concrete(DyeColor.WHITE));
        m.put(Piece.END_CEILING, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        m.put(Piece.CONCRETE_FLOOR, Blocks.SMOOTH_STONE.defaultBlockState());
        m.put(Piece.PILLAR, Blocks.POLISHED_ANDESITE.defaultBlockState());
        m.put(Piece.CONCRETE_WALL, concrete(DyeColor.LIGHT_GRAY));
        m.put(Piece.CONCRETE_CEILING, concrete(DyeColor.GRAY));
        m.put(Piece.CRATE_OAK, Blocks.OAK_PLANKS.defaultBlockState());
        m.put(Piece.CRATE_SPRUCE, Blocks.SPRUCE_PLANKS.defaultBlockState());
        m.put(Piece.SHELF, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP));
        m.put(Piece.RACK_POST, Blocks.IRON_BARS.defaultBlockState());
        m.put(Piece.MARKING, concrete(DyeColor.YELLOW));
        m.put(Piece.HAZARD_BLACK, concrete(DyeColor.BLACK));
        m.put(Piece.PIPE_WALL, Blocks.TUFF_BRICKS.defaultBlockState());
        m.put(Piece.PIPE_FLOOR, Blocks.TUFF.defaultBlockState());
        m.put(Piece.PIPE_CEILING, Blocks.POLISHED_TUFF.defaultBlockState());
        m.put(Piece.PIPE_X, rod(Direction.EAST));
        m.put(Piece.PIPE_Z, rod(Direction.SOUTH));
        m.put(Piece.PIPE_Y, rod(Direction.UP));
        m.put(Piece.COPPER_BOX, Blocks.CUT_COPPER.waxed().exposed().defaultBlockState());
        m.put(Piece.BULB, Blocks.COPPER_BULB.waxed().exposed().defaultBlockState().setValue(BlockStateProperties.LIT, Boolean.TRUE));
        m.put(Piece.GRATE, Blocks.COPPER_GRATE.waxed().exposed().defaultBlockState());
        m.put(Piece.LADDER_E, facing(Blocks.LADDER, Direction.EAST));
        m.put(Piece.LADDER_S, facing(Blocks.LADDER, Direction.SOUTH));
        m.put(Piece.LEVER_N, lever(Direction.NORTH));
        m.put(Piece.LEVER_E, lever(Direction.EAST));
        m.put(Piece.LEVER_W, lever(Direction.WEST));
        m.put(Piece.TILE, Blocks.QUARTZ_BRICKS.defaultBlockState());
        m.put(Piece.TILE_FLOOR, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        m.put(Piece.POOL_TILE, Blocks.PRISMARINE_BRICKS.defaultBlockState());
        m.put(Piece.WATER, Blocks.WATER.defaultBlockState());
        m.put(Piece.SEA_LANTERN, Blocks.SEA_LANTERN.defaultBlockState());
        // Arch corners: upside-down stairs, their back towards the wall beside the opening.
        m.put(Piece.ARCH_E, stairs(Blocks.SMOOTH_QUARTZ_STAIRS, Direction.EAST, Half.TOP));
        m.put(Piece.ARCH_W, stairs(Blocks.SMOOTH_QUARTZ_STAIRS, Direction.WEST, Half.TOP));
        m.put(Piece.ARCH_N, stairs(Blocks.SMOOTH_QUARTZ_STAIRS, Direction.NORTH, Half.TOP));
        m.put(Piece.ARCH_S, stairs(Blocks.SMOOTH_QUARTZ_STAIRS, Direction.SOUTH, Half.TOP));
        m.put(Piece.TILE_COLUMN, Blocks.QUARTZ_PILLAR.defaultBlockState());
        m.put(Piece.PAD, concrete(DyeColor.GRAY));
        m.put(Piece.GRAVEL, Blocks.GRAVEL.defaultBlockState());
        m.put(Piece.STONE, Blocks.STONE.defaultBlockState());
        m.put(Piece.STONE_BRICKS, Blocks.STONE_BRICKS.defaultBlockState());
        m.put(Piece.CRACKED_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
        m.put(Piece.POST, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        m.put(Piece.WARNING_LAMP, Blocks.COPPER_BULB.waxed().unaffected().defaultBlockState().setValue(BlockStateProperties.LIT, Boolean.TRUE));
        m.put(Piece.TAPE_X, tape(Direction.Axis.X));
        m.put(Piece.TAPE_Z, tape(Direction.Axis.Z));
        m.put(Piece.SIGN_N, facing(LayersRegistry.CAUTION_SIGN, Direction.NORTH));
        m.put(Piece.SIGN_E, facing(LayersRegistry.CAUTION_SIGN, Direction.EAST));
        m.put(Piece.SIGN_S, facing(LayersRegistry.CAUTION_SIGN, Direction.SOUTH));
        m.put(Piece.SIGN_W, facing(LayersRegistry.CAUTION_SIGN, Direction.WEST));
        m.put(Piece.GLITCHED_BEDROCK, LayersRegistry.GLITCHED_BEDROCK.defaultBlockState());
        m.put(Piece.CHAIN, Blocks.IRON_CHAIN.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
        for (Piece p : Piece.values()) if (!m.containsKey(p)) throw new IllegalStateException("no block state for " + p);
        return m;
    }
}
