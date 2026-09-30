package com.simplebuilding.util;

import com.simplebuilding.items.custom.SledgehammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.Vec3;

/** Subtracts exactly the aimed quarter of the non-solid half. No diagonal stair is invented. */
public final class HammerCorners {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty CARVED =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("simplebuilding_carved");
    private HammerCorners() {}
    public static int ticks(int normalTicks) { return Math.max(1, (normalTicks * 2 + 2) / 3); }
    public static int corner(Vec3 hit) { return (hit.x >= 0.5 ? 1 : 0) | (hit.z >= 0.5 ? 2 : 0); }
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
    public static BlockState subtract(BlockState state, boolean full, Direction side, Vec3 hit) {
        boolean stairs = state.getBlock() instanceof StairBlock;
        Half half = stairs ? state.getValue(StairBlock.HALF)
                : (side == Direction.DOWN || (side != Direction.UP && hit.y < 0.5) ? Half.TOP : Half.BOTTOM);
        // A stair's solid base is retained; aiming at that base cannot remove a corner above it.
        if (stairs && (half == Half.BOTTOM ? hit.y < 0.5 : hit.y > 0.5)) return null;
        int before = stairs ? mask(state) : 15;
        int bit = 1 << corner(hit);
        if ((before & bit) == 0) return null;
        int after = before & ~bit;
        if (stairs && after == 0) {
            return SledgehammerItem.reshapeTarget(state.getBlock(), false, false)
                    .filter(b -> b instanceof SlabBlock)
                    .map(b -> b.defaultBlockState().setValue(SlabBlock.TYPE, half == Half.BOTTOM ? SlabType.BOTTOM : SlabType.TOP)
                            .setValue(SlabBlock.WATERLOGGED, state.getValue(StairBlock.WATERLOGGED))).orElse(null);
        }
        BlockState target = stairs ? state : SledgehammerItem.reshapeTarget(state.getBlock(), false, full)
                .filter(b -> b instanceof StairBlock).map(b -> b.defaultBlockState()).orElse(null);
        if (target == null) return null;
        target = target.setValue(StairBlock.HALF, half);
        for (Direction facing : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            for (StairsShape shape : StairsShape.values()) {
                BlockState candidate = target.setValue(StairBlock.FACING, facing).setValue(StairBlock.SHAPE, shape);
                if (mask(candidate) == after) return candidate.hasProperty(CARVED) ? candidate.setValue(CARVED, true) : candidate;
            }
        }
        return null; // Two diagonally opposite quarters cannot be represented by vanilla stairs.
    }
}
