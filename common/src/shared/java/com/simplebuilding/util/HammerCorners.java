package com.simplebuilding.util;

import com.simplebuilding.blocks.custom.MaterialOctetBlock;
import com.simplebuilding.blocks.custom.OctetCellBlock;
import com.simplebuilding.items.custom.SledgehammerItem;
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
 * ({@link MaterialOctets#cellFor}), and without octets of that material the hammer does not carve there.
 */
public final class HammerCorners {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty CARVED =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("simplebuilding_carved");
    private HammerCorners() {}
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

    /** The eight octets a state's collision shape fills, bit = {@link OctetCellBlock#index}. */
    public static int octets(BlockState state) {
        var boxes = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs();
        int mask = 0;
        for (int i = 0; i < 8; i++) {
            double x = OctetCellBlock.x(i) * 0.5 + 0.25;
            double y = OctetCellBlock.y(i) * 0.5 + 0.25;
            double z = OctetCellBlock.z(i) * 0.5 + 0.25;
            for (var box : boxes) if (box.contains(x, y, z)) { mask |= 1 << i; break; }
        }
        return mask;
    }

    /** The octet just behind the hit point (block-relative) on the hit face. */
    public static int octet(Direction side, Vec3 hit) {
        double e = 1.0E-3;
        return OctetCellBlock.index(hit.x - side.getStepX() * e >= 0.5 ? 1 : 0,
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
        } else if (full && !(block instanceof OctetCellBlock)) {
            base = block;
            stairs = SledgehammerItem.reshapeTarget(block, false, true).orElse(null);
            slab = stairs == null ? null : SledgehammerItem.reshapeTarget(stairs, false, false).orElse(null);
        } else {
            return null;
        }
        return new Family(stairs instanceof StairBlock ? stairs : null, slab instanceof SlabBlock ? slab : null,
                MaterialOctets.cellFor(base));
    }

    public static BlockState subtract(BlockState state, boolean full, Direction side, Vec3 hit) {
        Block block = state.getBlock();
        Family family = family(block, full);
        if (family == null) return null;
        boolean shaped = block instanceof StairBlock || block instanceof SlabBlock;
        int before = shaped ? octets(state) : OctetCellBlock.FULL;
        int bit = 1 << octet(side, hit);
        if ((before & bit) == 0) return null;
        int after = before & ~bit;
        if (after == 0) return null;
        boolean wet = state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED);
        if (family.slab() != null && (after == OctetCellBlock.BOTTOM_HALF || after == OctetCellBlock.TOP_HALF)) {
            return family.slab().defaultBlockState()
                    .setValue(SlabBlock.TYPE, after == OctetCellBlock.BOTTOM_HALF ? SlabType.BOTTOM : SlabType.TOP)
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
            return OctetCellBlock.withMask(family.cell().defaultBlockState().setValue(OctetCellBlock.WATERLOGGED, wet), after);
        }
        return null;
    }

    /** The item of the octet a carve removes from this block (wood, melon), or null for materials without octets. */
    public static @Nullable Item removedPiece(BlockState state) {
        Block block = state.getBlock();
        Family family = family(block, !(block instanceof StairBlock) && !(block instanceof SlabBlock));
        return family == null || !(family.cell() instanceof MaterialOctetBlock cell) ? null : cell.piece();
    }
}
