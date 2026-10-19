package com.simplehammers.engine;

import com.simplehammers.item.SledgehammerItem;
import com.simplelib.api.OctetCells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Sneaking with the sledgehammer removes exactly the aimed octet (0.5^3) of a full block, stairs or slab (Queue
 * Nachtrag 24). The rest becomes a slab or a vanilla stair when it is one; any other shape (e.g. one octet gone above
 * and one below, two diagonal quarters, a slab missing a quarter) falls apart into the material's octet cell
 * (SimpleLib's {@link OctetCells}; Simple Octets registers the cells), and without octets of that material the hammer does not carve there.
 * Moved from SimpleBuilding (split stage 7). The octet layout (index {@code x | y << 1 | z << 2}, properties
 * {@code o<x><y><z>}) is Simple Octets' cell format, read here without importing that mod.
 */
public class HammerCorners {
    /**
     * Carved stairs keep their shape ({@code CarvedStairMixin}). The property keeps SimpleBuilding's name: block states in
     * saved chunks name it, so a world carved before the move loads unchanged.
     */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty CARVED =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("simplebuilding_carved");
    /** All eight octets, the lower and the upper half as masks (Simple Octets' layout). */
    public static final int FULL = 0xFF;
    public static final int BOTTOM_HALF = 0x33;
    public static final int TOP_HALF = 0xCC;
    protected HammerCorners() {}

    static int index(int x, int y, int z) {
        return (x & 1) | (y & 1) << 1 | (z & 1) << 2;
    }

    static int x(int index) {
        return index & 1;
    }

    static int y(int index) {
        return (index >> 1) & 1;
    }

    static int z(int index) {
        return (index >> 2) & 1;
    }

    /** A cell's state holding exactly {@code mask} (properties {@code o<x><y><z>}); full cells hold no water. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static BlockState withMask(BlockState state, int mask) {
        var definition = state.getBlock().getStateDefinition();
        for (int i = 0; i < 8; i++) {
            var property = definition.getProperty("o" + x(i) + y(i) + z(i));
            if (property instanceof net.minecraft.world.level.block.state.properties.BooleanProperty bool) {
                state = state.setValue(bool, (mask & (1 << i)) != 0);
            }
        }
        return mask == FULL && state.hasProperty(BlockStateProperties.WATERLOGGED) ? state.setValue(BlockStateProperties.WATERLOGGED, false) : state;
    }
    public static int ticks(int normalTicks) { return Math.max(1, (normalTicks * 2 + 2) / 3); }
    public static int corner(Vec3 hit) { return (hit.x >= 0.5 ? 1 : 0) | (hit.z >= 0.5 ? 2 : 0); }

    /** The four quarters of a stair's non-solid half (bit = {@link #corner}). */
    public static int mask(BlockState state) {
        double y = state.getValue(StairBlock.HALF) == Half.BOTTOM ? 0.75 : 0.25;
        int mask = 0;
        var boxes = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs();
        for (int i = 0; i < 4; i++) {
            double x = (i & 1) == 0 ? 0.25 : 0.75;
            double z = (i & 2) == 0 ? 0.25 : 0.75;
            for (var box : boxes) if (box.contains(x, y, z)) { mask |= 1 << i; break; }
        }
        return mask;
    }

    /** The eight octets a state's collision shape fills, bit = {@link #index}. */
    public static int octets(BlockState state) {
        var boxes = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs();
        int mask = 0;
        for (int i = 0; i < 8; i++) {
            double x = x(i) * 0.5 + 0.25;
            double y = y(i) * 0.5 + 0.25;
            double z = z(i) * 0.5 + 0.25;
            for (var box : boxes) if (box.contains(x, y, z)) { mask |= 1 << i; break; }
        }
        return mask;
    }

    /** The octet just behind the hit point (block-relative) on the hit face. */
    public static int octet(Direction side, Vec3 hit) {
        double e = 1.0E-3;
        return index(hit.x - side.getStepX() * e >= 0.5 ? 1 : 0,
                hit.y - side.getStepY() * e >= 0.5 ? 1 : 0, hit.z - side.getStepZ() * e >= 0.5 ? 1 : 0);
    }

    /** Stairs, slab and octet cell of a block's material; null for blocks the hammer does not carve. */
    private record Family(@Nullable Block stairs, @Nullable Block slab, @Nullable Block cell) {}

    private static @Nullable Family family(Block block, boolean full) {
        Block stairs;
        Block slab;
        Block base;
        if (block instanceof StairBlock) {
            stairs = block;
            slab = SledgehammerItem.reshapeTarget(block, false, false).orElse(null);
            base = SledgehammerItem.reshapeTarget(block, true, false).orElse(null);
        } else if (block instanceof SlabBlock) {
            slab = block;
            stairs = SledgehammerItem.reshapeTarget(block, true, false).orElse(null);
            base = stairs == null ? null : SledgehammerItem.reshapeTarget(stairs, true, false).orElse(null);
        } else if (full && OctetCells.baseOf(block) == null) {
            base = block;
            stairs = SledgehammerItem.reshapeTarget(block, false, true).orElse(null);
            slab = stairs == null ? null : SledgehammerItem.reshapeTarget(stairs, false, false).orElse(null);
        } else {
            return null;
        }
        return new Family(stairs instanceof StairBlock ? stairs : null, slab instanceof SlabBlock ? slab : null,
                OctetCells.cellFor(base));
    }

    public static BlockState subtract(BlockState state, boolean full, Direction side, Vec3 hit) {
        Block block = state.getBlock();
        Family family = family(block, full);
        if (family == null) return null;
        boolean shaped = block instanceof StairBlock || block instanceof SlabBlock;
        int before = shaped ? octets(state) : FULL;
        int bit = 1 << octet(side, hit);
        if ((before & bit) == 0) return null;
        int after = before & ~bit;
        if (after == 0) return null;
        boolean wet = state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED);
        if (family.slab() != null && (after == BOTTOM_HALF || after == TOP_HALF)) {
            return family.slab().defaultBlockState()
                    .setValue(SlabBlock.TYPE, after == BOTTOM_HALF ? SlabType.BOTTOM : SlabType.TOP)
                    .setValue(SlabBlock.WATERLOGGED, wet);
        }
        if (family.stairs() != null) {
            BlockState target = (block == family.stairs() ? state : family.stairs().defaultBlockState()).setValue(StairBlock.WATERLOGGED, wet);
            for (Half half : Half.values()) {
                for (Direction facing : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                    for (StairsShape shape : StairsShape.values()) {
                        BlockState candidate = target.setValue(StairBlock.HALF, half).setValue(StairBlock.FACING, facing)
                                .setValue(StairBlock.SHAPE, shape);
                        if (octets(candidate) == after) {
                            return candidate.hasProperty(CARVED) ? candidate.setValue(CARVED, true) : candidate;
                        }
                    }
                }
            }
        }
        // Not a slab or a vanilla stair: the material's octets, if it has any.
        if (family.cell() != null) {
            BlockState cell = family.cell().defaultBlockState();
            if (cell.hasProperty(BlockStateProperties.WATERLOGGED)) cell = cell.setValue(BlockStateProperties.WATERLOGGED, wet);
            return withMask(cell, after);
        }
        return null;
    }

    /** The item of the octet a carve removes from this block (wood, melon), or null for materials without octets. */
    public static @Nullable Item removedPiece(BlockState state) {
        Block block = state.getBlock();
        Family family = family(block, !(block instanceof StairBlock) && !(block instanceof SlabBlock));
        return family == null ? null : OctetCells.pieceOf(family.cell());
    }
}
