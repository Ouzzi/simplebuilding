package com.simplebuilding.blocks.custom;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A hammock (26.3, docs/ai/PLAN-HAENGEMATTE-2026-10-02.md). A bed in vanilla's sense ({@link AbstractBedBlock}), so the
 * sleeping pose, its orientation, "leave bed" and the wake-up check are vanilla's; what differs:
 * <ul>
 *   <li>the cloth cells hang under the rope cells ({@link HammockLayout}, straight or diagonal, 2 to 4 cells between
 *       the anchors); the whole hammock falls as soon as any part or an anchor is gone, and only the cloth head drops
 *       the item (loot table); the cloth head also checks itself every 10 ticks (a diagonal anchor is no face
 *       neighbour);</li>
 *   <li>usable day and night ({@link HammockTime#restAllowed}) but no sleep: never sets the respawn point, never skips
 *       the night, and lying down keeps the phantom statistic ({@code time_since_rest}) instead of resetting it;</li>
 *   <li>refusals are a sound only for the player, no text.</li>
 * </ul>
 * The 26.2 twin in {@code common/src/mc26_2} is never registered ({@code McVersion.HAMMOCK} is false there).
 */
public class HammockBlock extends AbstractBedBlock {
    /** The player lies this high above the lower head block (the cloth sags to 4/16). */
    public static final double SLEEP_HEIGHT = 0.25;
    private static final BedRule USABLE = new BedRule(BedRule.Rule.ALWAYS, BedRule.Rule.NEVER, false, false, Optional.empty());
    private static final BedRule NOT_NOW = new BedRule(BedRule.Rule.NEVER, BedRule.Rule.NEVER, false, false, Optional.empty());
    private static final Map<Direction, VoxelShape> CLOTH = Util.make(() -> Shapes.rotateHorizontal(Block.box(1.0, 2.0, 0.0, 15.0, 10.0, 16.0)));
    private static final Map<Direction, VoxelShape> CLOTH_COLLISION = Util.make(() -> Shapes.rotateHorizontal(Block.box(1.0, 2.0, 0.0, 15.0, 5.0, 16.0)));
    private static final VoxelShape DIAGONAL_CLOTH = Block.box(2.0, 2.0, 2.0, 14.0, 10.0, 14.0);
    private static final VoxelShape DIAGONAL_COLLISION = Block.box(2.0, 2.0, 2.0, 14.0, 5.0, 14.0);
    /** The cloth head checks its hammock (anchors included) this often. */
    public static final int CHECK_TICKS = 10;

    private final DyeColor color;

    public HammockBlock(DyeColor color, BlockBehaviour.Properties properties) {
        super(properties);
        this.color = color;
        // The default state is the loot owner, so a loot roll on the default state hands over the hammock.
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.HEAD)
                .setValue(OCCUPIED, false).setValue(HammockLayout.DIAGONAL, false).setValue(HammockLayout.GAP, HammockLayout.MIN_GAP)
                .setValue(HammockLayout.INDEX, HammockLayout.headCell(HammockLayout.MIN_GAP)));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HammockLayout.DIAGONAL, HammockLayout.GAP, HammockLayout.INDEX);
    }

    public DyeColor getColor() {
        return this.color;
    }

    // --- shape -------------------------------------------------------------------------------------------------

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HammockLayout.DIAGONAL) ? DIAGONAL_CLOTH : CLOTH.get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HammockLayout.DIAGONAL) ? DIAGONAL_COLLISION : CLOTH_COLLISION.get(state.getValue(FACING));
    }

    /**
     * Forge 26.3: {@code IForgeBlock#isBed} only knows {@code BedBlock}; without this the lying direction (and the
     * sleeping-pose orientation) is UP. No {@code @Override}: Fabric/NeoForge have no such method with this signature.
     */
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, @Nullable Entity entity) {
        return true;
    }

    @Override
    public OptionalDouble getSleepHeight(BlockState state, Level level, BlockPos pos) {
        return state.is(this) && state.getValue(PART) == BedPart.HEAD
                ? OptionalDouble.of(SLEEP_HEIGHT) : OptionalDouble.empty();
    }

    // --- hanging -----------------------------------------------------------------------------------------------

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return HammockLayout.intact(level, pos, state) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && state.getValue(PART) == BedPart.HEAD) {
            level.scheduleTick(pos, this, CHECK_TICKS);
        }
    }

    /** Scheduled: after a part went (every cell), and every {@link #CHECK_TICKS} ticks for the cloth head. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        HammockLayout.check(level, pos, state);
        if (state.getValue(PART) == BedPart.HEAD && level.getBlockState(pos).is(this)) {
            level.scheduleTick(pos, this, CHECK_TICKS);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        HammockLayout.scheduleChecks(level, pos, state);
    }

    /** The hammock item places every block itself ({@code HammockItem}); a plain block placement never happens. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
    }

    /** Creative mode: take the loot owner away first and quietly, so the falling rest drops nothing (as with beds). */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        HammockLayout.quietlyRemoveLootOwner(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    // --- resting -----------------------------------------------------------------------------------------------

    @Override
    protected EnvironmentAttribute<BedRule> getBedEnvironmentAttribute() {
        return EnvironmentAttributes.BED_RULE; // unused: getBedRule is overridden
    }

    /** "May rest, never sets spawn" in a dimension with a day clock; otherwise "may not" - vanilla wakes with that. */
    @Override
    public BedRule getBedRule(Level level, BlockPos pos) {
        return HammockTime.restAllowed(level) ? USABLE : NOT_NOW;
    }

    @Override
    protected InteractionResult destroyOnUse(BlockState state, Level level, BlockPos pos, Player player) {
        return InteractionResult.PASS;
    }

    @Override
    protected void destroyOnLeave(Level level, BlockPos pos) {
    }

    @Override
    public void onStopSleeping(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            HammockTime.markWake(server);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        if (player instanceof ServerPlayer serverPlayer && !rest(serverPlayer, HammockLayout.clothHead(level, pos))) {
            HammockLayout.refuse(serverPlayer, null);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * Lies the player down in the hammock whose cloth head is at {@code head}: day or night, if it is free, no monster
     * is near (as for beds, not in creative) and the player is not lying already. The phantom statistic stays.
     */
    public static boolean rest(ServerPlayer player, @Nullable BlockPos head) {
        if (head == null) {
            return false;
        }
        Level level = player.level();
        BlockState state = level.getBlockState(head);
        if (!(state.getBlock() instanceof HammockBlock) || state.getValue(PART) != BedPart.HEAD || state.getValue(OCCUPIED)
                || player.isSleeping() || !player.isAlive() || !HammockTime.restAllowed(level)) {
            return false;
        }
        if (!player.isCreative()) {
            Vec3 center = Vec3.atBottomCenterOf(head);
            List<Monster> monsters = level.getEntitiesOfClass(Monster.class, new AABB(center.x - 8.0, center.y - 5.0, center.z - 8.0,
                    center.x + 8.0, center.y + 5.0, center.z + 8.0), monster -> monster.isPreventingPlayerRest(player.level(), player));
            if (!monsters.isEmpty()) {
                return false;
            }
        }
        Stat<?> sinceRest = Stats.CUSTOM.get(Stats.TIME_SINCE_REST);
        int before = player.getStats().getValue(sinceRest);
        if (!player.startSleeping(head)) {
            return false;
        }
        // ServerPlayer#startSleeping resets it; a hammock is no night's rest (no phantom protection).
        player.awardStat(sinceRest, before);
        player.level().updateSleepingPlayerList();
        return true;
    }

}
