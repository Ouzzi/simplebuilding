package com.simplebuilding.woodwork;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.version.BlockCodecs;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Carved wood (docs/ai/PLAN-HOLZWERK-2026-10-09.md, N34): a standing stripped log whose four sides are carved one by
 * one with a chisel ({@link PotteryCarving}). Every side is {@link Carving#NONE} (plain), {@link Carving#WOOD} (the
 * wood's own motif, chisel without sherd) or {@link Carving#SHERD} (the pattern of a pottery sherd from the off hand);
 * all sherd sides of a block show the same {@link #MOTIF} (a block state cannot hold a motif per side). Drops itself
 * with all five properties (loot {@code copy_state}).
 */
public class CarvedLogBlock extends Block {
    public enum Carving implements StringRepresentable {
        NONE("none"), WOOD("wood"), SHERD("sherd");

        private final String name;

        Carving(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final MapCodec<CarvedLogBlock> CODEC = BlockCodecs.simple(CarvedLogBlock::new);
    public static final EnumProperty<SherdMotif> MOTIF = EnumProperty.create("motif", SherdMotif.class);
    public static final EnumProperty<Carving> NORTH = EnumProperty.create("north", Carving.class);
    public static final EnumProperty<Carving> EAST = EnumProperty.create("east", Carving.class);
    public static final EnumProperty<Carving> SOUTH = EnumProperty.create("south", Carving.class);
    public static final EnumProperty<Carving> WEST = EnumProperty.create("west", Carving.class);

    private final WoodKind wood;

    public CarvedLogBlock(Properties properties) {
        this(WoodKind.OAK, properties);
    }

    public CarvedLogBlock(WoodKind wood, Properties properties) {
        super(properties);
        this.wood = wood;
        registerDefaultState(this.stateDefinition.any().setValue(NORTH, Carving.WOOD).setValue(EAST, Carving.NONE)
                .setValue(SOUTH, Carving.NONE).setValue(WEST, Carving.NONE).setValue(MOTIF, SherdMotif.ANGLER));
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public WoodKind wood() {
        return this.wood;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, MOTIF);
    }

    /** The property of a horizontal side. */
    public static EnumProperty<Carving> side(Direction side) {
        return switch (side) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException("not a horizontal side: " + side);
        };
    }

    public static Carving carving(BlockState state, Direction side) {
        return state.getValue(side(side));
    }

    /** {@code state} with only {@code side} carved (a sherd motif when {@code motif} is not null, else the wood's own). */
    public BlockState only(Direction side, SherdMotif motif) {
        BlockState state = defaultBlockState().setValue(NORTH, Carving.NONE);
        return state.setValue(side(side), motif == null ? Carving.WOOD : Carving.SHERD).setValue(MOTIF, motif == null ? SherdMotif.ANGLER : motif);
    }

    /** Whether any side still carries a carving. */
    public static boolean carved(BlockState state) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (state.getValue(side(side)) != Carving.NONE) {
                return true;
            }
        }
        return false;
    }
}
