package com.simplebuilding.woodwork;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.version.BlockCodecs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/**
 * Carved wood (docs/ai/PLAN-HOLZWERK-2026-10-09.md, N34/N40): a standing stripped log carved all the way round with a
 * chisel ({@link PotteryCarving}). One click carves all four sides at once: {@link Carving#WOOD} (the wood's own
 * motif, chisel without sherd) or {@link Carving#SHERD} (the pattern of a pottery sherd from the off hand, the same
 * {@link #MOTIF} on every side). Sneaking with the chisel turns the whole block back into the stripped log. Drops
 * itself with both properties (loot {@code copy_state}). Before N40 every side had its own property
 * (north/east/south/west none/wood/sherd); {@link #migrate} rewrites such saved states.
 */
public class CarvedLogBlock extends Block {
    public enum Carving implements StringRepresentable {
        WOOD("wood"), SHERD("sherd");

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
    public static final EnumProperty<Carving> CARVING = EnumProperty.create("carving", Carving.class);
    /** The side properties of the pre-N40 state (one carving per side), only read by {@link #migrate}. */
    private static final String[] OLD_SIDES = {"north", "east", "south", "west"};

    private final WoodKind wood;

    public CarvedLogBlock(Properties properties) {
        this(WoodKind.OAK, properties);
    }

    public CarvedLogBlock(WoodKind wood, Properties properties) {
        super(properties);
        this.wood = wood;
        registerDefaultState(this.stateDefinition.any().setValue(CARVING, Carving.WOOD).setValue(MOTIF, SherdMotif.ANGLER));
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
        builder.add(CARVING, MOTIF);
    }

    public static Carving carving(BlockState state) {
        return state.getValue(CARVING);
    }

    /** Carved all round: with {@code motif} the sherd pattern on every side, without (null) the wood's own motif. */
    public BlockState carvedWith(@Nullable SherdMotif motif) {
        return defaultBlockState().setValue(CARVING, motif == null ? Carving.WOOD : Carving.SHERD)
                .setValue(MOTIF, motif == null ? SherdMotif.ANGLER : motif);
    }

    /**
     * Rewrites the block-state properties of a pre-N40 carved log in place (saved chunk palettes and item
     * {@code block_state} components look the same): any sherd side makes the block a sherd carving, otherwise it is
     * carved with the wood's own motif; the four side keys go. Returns whether anything changed. Only a compound with
     * {@code motif} and an old side key is touched - no other block has a {@code motif} property.
     */
    public static boolean migrate(CompoundTag properties) {
        if (!properties.contains(MOTIF.getName()) || properties.contains(CARVING.getName())) {
            return false;
        }
        boolean old = false;
        boolean sherd = false;
        for (String side : OLD_SIDES) {
            String value = properties.getStringOr(side, "");
            if (value.equals("none") || value.equals("wood") || value.equals("sherd")) {
                old = true;
                sherd |= value.equals("sherd");
            }
        }
        if (!old) {
            return false;
        }
        for (String side : OLD_SIDES) {
            properties.remove(side);
        }
        properties.putString(CARVING.getName(), (sherd ? Carving.SHERD : Carving.WOOD).getSerializedName());
        return true;
    }
}
