package com.simplebuilding.blocks.custom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

/**
 * Where a hammock may hang (docs/ai/PLAN-HAENGEMATTE-2026-10-02.md). A hammock is one block wide and two long and takes
 * two layers: the lower layer is the sagging cloth ({@link HammockBlock}, foot and head like a bed), the upper layer the
 * two rope ends ({@link HammockRopeBlock} {@code end}). It hangs between two anchors on rope height with 2 or 3 free
 * blocks between them; with 3, a rope span ({@code span}) fills the gap at the head end. {@code facing} of the cloth
 * points from the foot to the head, like a bed's; a rope's {@code facing} points towards its anchor.
 *
 * <p>Loader- and version-neutral: only the block-state properties of the two blocks are used here.
 */
public final class HammockLayout {
    public static final int MIN_GAP = 2;
    public static final int MAX_GAP = 3;

    private HammockLayout() {
    }

    /** One possible hammock: upper foot block, direction to the head and the number of free blocks between anchors. */
    public record Spot(BlockPos upperFoot, Direction facing, int gap) {
        public BlockPos upperHead() {
            return upperFoot.relative(facing);
        }

        public BlockPos lowerFoot() {
            return upperFoot.below();
        }

        public BlockPos lowerHead() {
            return upperHead().below();
        }

        /** The rope span between the head and its anchor, only for a gap of 3. */
        public @Nullable BlockPos span() {
            return gap == MAX_GAP ? upperFoot.relative(facing, 2) : null;
        }

        public BlockPos footAnchor() {
            return upperFoot.relative(facing.getOpposite());
        }

        public BlockPos headAnchor() {
            return upperFoot.relative(facing, gap);
        }
    }

    /**
     * Anchors: any block with a collision shape that is not replaceable and not part of a hammock - full blocks,
     * fences, walls, rods, glass, leaves. Air, plants, water and snow layers do not hold a rope.
     */
    public static boolean isAnchor(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.canBeReplaced() || state.getBlock() instanceof HammockBlock
                || state.getBlock() instanceof HammockRopeBlock) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    /**
     * Finds the hammock a click places. A click on the side of a block uses that block (or the one above it) as the
     * foot anchor and the hammock spans away from it; otherwise the player's facing (or its opposite) is the direction
     * and the click position is the lower foot block. The first fitting spot wins, gap 2 before gap 3.
     *
     * @param placeAt  where a normal block would go ({@code BlockPlaceContext#getClickedPos})
     * @param face     the clicked face
     * @param looking  the player's horizontal facing
     */
    public static Optional<Spot> find(Level level, BlockPos placeAt, Direction face, Direction looking) {
        List<Spot> tries = new ArrayList<>();
        if (face.getAxis().isHorizontal()) {
            tries.add(new Spot(placeAt, face, 0));
            tries.add(new Spot(placeAt.above(), face, 0));
        }
        tries.add(new Spot(placeAt.above(), looking, 0));
        tries.add(new Spot(placeAt.above(), looking.getOpposite(), 0));
        for (Spot base : tries) {
            for (int gap = MIN_GAP; gap <= MAX_GAP; gap++) {
                Spot spot = new Spot(base.upperFoot(), base.facing(), gap);
                if (fits(level, spot)) {
                    return Optional.of(spot);
                }
            }
        }
        return Optional.empty();
    }

    /** Both anchors in place, every block of the hammock free, nothing between the anchors but the hammock. */
    public static boolean fits(Level level, Spot spot) {
        if (!isAnchor(level, spot.footAnchor()) || !isAnchor(level, spot.headAnchor())) {
            return false;
        }
        for (BlockPos pos : List.of(spot.upperFoot(), spot.upperHead(), spot.lowerFoot(), spot.lowerHead())) {
            if (!free(level, pos)) {
                return false;
            }
        }
        BlockPos span = spot.span();
        return span == null || free(level, span);
    }

    private static boolean free(Level level, BlockPos pos) {
        return level.getBlockState(pos).canBeReplaced() && level.getWorldBorder().isWithinBounds(pos)
                && !level.isOutsideBuildHeight(pos);
    }

    /** Whether no entity stands where the cloth of the hammock would collide. */
    public static boolean unobstructed(Level level, Map<BlockPos, BlockState> states) {
        for (Map.Entry<BlockPos, BlockState> entry : states.entrySet()) {
            if (!level.isUnobstructed(entry.getValue(), entry.getKey(), CollisionContext.empty())) {
                return false;
            }
        }
        return true;
    }

    /** The block states of a hammock at {@code spot}, in placing order (cloth, rope ends, rope span). */
    public static Map<BlockPos, BlockState> states(Block hammock, Block rope, Spot spot) {
        Map<BlockPos, BlockState> out = new LinkedHashMap<>();
        BlockState cloth = hammock.defaultBlockState().setValue(HammockBlock.FACING, spot.facing()).setValue(HammockBlock.OCCUPIED, false);
        out.put(spot.lowerFoot(), cloth.setValue(HammockBlock.PART, BedPart.FOOT));
        out.put(spot.lowerHead(), cloth.setValue(HammockBlock.PART, BedPart.HEAD));
        BlockState end = rope.defaultBlockState().setValue(HammockRopeBlock.KIND, HammockRopeBlock.Kind.END);
        out.put(spot.upperFoot(), end.setValue(HammockRopeBlock.FACING, spot.facing().getOpposite()));
        out.put(spot.upperHead(), end.setValue(HammockRopeBlock.FACING, spot.facing()));
        BlockPos span = spot.span();
        if (span != null) {
            out.put(span, rope.defaultBlockState().setValue(HammockRopeBlock.KIND, HammockRopeBlock.Kind.SPAN)
                    .setValue(HammockRopeBlock.FACING, spot.facing()));
        }
        return out;
    }

    /** The side of a cloth block that faces its anchor: behind the foot, in front of the head. */
    public static Direction outward(BlockState cloth) {
        Direction facing = cloth.getValue(HammockBlock.FACING);
        return cloth.getValue(HammockBlock.PART) == BedPart.FOOT ? facing.getOpposite() : facing;
    }

    /**
     * The cloth head block (the sleeping spot and the only block that drops the item) of the hammock that the block at
     * {@code pos} belongs to, or {@code null} when that hammock is already incomplete.
     */
    public static @Nullable BlockPos clothHead(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof HammockRopeBlock) {
            if (state.getValue(HammockRopeBlock.KIND) == HammockRopeBlock.Kind.SPAN) {
                pos = pos.relative(state.getValue(HammockRopeBlock.FACING).getOpposite());
                state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof HammockRopeBlock)) {
                    return null;
                }
            }
            pos = pos.below();
            state = level.getBlockState(pos);
        }
        if (!(state.getBlock() instanceof HammockBlock)) {
            return null;
        }
        return state.getValue(HammockBlock.PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(HammockBlock.FACING));
    }

    /** A cloth block hangs while its partner along the hammock and the rope end above it (towards its anchor) are there. */
    public static boolean clothHangs(BlockGetter level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(HammockBlock.FACING);
        BedPart part = state.getValue(HammockBlock.PART);
        BlockState partner = level.getBlockState(pos.relative(part == BedPart.FOOT ? facing : facing.getOpposite()));
        if (!partner.is(state.getBlock()) || partner.getValue(HammockBlock.FACING) != facing || partner.getValue(HammockBlock.PART) == part) {
            return false;
        }
        BlockState above = level.getBlockState(pos.above());
        return above.getBlock() instanceof HammockRopeBlock && above.getValue(HammockRopeBlock.KIND) == HammockRopeBlock.Kind.END
                && above.getValue(HammockRopeBlock.FACING) == outward(state);
    }

    /**
     * A rope end hangs over its cloth block and reaches its anchor directly or through a rope span; a span hangs between
     * a rope end behind it and the anchor in front.
     */
    public static boolean ropeHangs(BlockGetter level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(HammockRopeBlock.FACING);
        if (state.getValue(HammockRopeBlock.KIND) == HammockRopeBlock.Kind.SPAN) {
            BlockState behind = level.getBlockState(pos.relative(facing.getOpposite()));
            return isAnchor(level, pos.relative(facing)) && behind.getBlock() instanceof HammockRopeBlock
                    && behind.getValue(HammockRopeBlock.KIND) == HammockRopeBlock.Kind.END && behind.getValue(HammockRopeBlock.FACING) == facing;
        }
        BlockState below = level.getBlockState(pos.below());
        if (!(below.getBlock() instanceof HammockBlock) || outward(below) != facing) {
            return false;
        }
        BlockState front = level.getBlockState(pos.relative(facing));
        if (front.getBlock() instanceof HammockRopeBlock) {
            return front.getValue(HammockRopeBlock.KIND) == HammockRopeBlock.Kind.SPAN && front.getValue(HammockRopeBlock.FACING) == facing;
        }
        return isAnchor(level, pos.relative(facing));
    }

    /**
     * Refusal without text (owner rule): the dispenser's fail sound for this player only and, where a hammock could not
     * be hung, a puff of smoke.
     */
    public static void refuse(net.minecraft.server.level.ServerPlayer player, @Nullable BlockPos at) {
        com.simplebuilding.util.Feedback.playTo(player, net.minecraft.sounds.SoundEvents.DISPENSER_FAIL,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.4F);
        if (at != null) {
            player.level().sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, at.getX() + 0.5, at.getY() + 0.5,
                    at.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.01);
        }
    }

    /**
     * Creative breaking: removes the cloth head (the loot owner) quietly first, so the falling rest drops nothing, as
     * vanilla does for beds. {@code pos} is the block the player breaks.
     */
    public static void quietlyRemoveLootOwner(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (level.isClientSide() || !player.preventsBlockDrops()) {
            return;
        }
        BlockPos head = clothHead(level, pos);
        if (head != null && !head.equals(pos)) {
            BlockState headState = level.getBlockState(head);
            level.setBlock(head, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            level.levelEvent(player, 2001, head, Block.getId(headState));
        }
    }
}
