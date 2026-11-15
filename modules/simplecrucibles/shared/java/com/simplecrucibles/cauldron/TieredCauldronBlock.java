package com.simplecrucibles.cauldron;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * N57-3 (owner 14.11.): the Netherite cauldron holds two buckets, the Enderite cauldron four - a reinforced cauldron (same
 * contents, Vanilla interaction tables, partner buckets, heat source) with {@link #capacity} buckets of room. The content is
 * counted in units: water and powder snow in thirds of a bucket ({@code stored * 3 + level}, like Vanilla's levels), lava and
 * the extreme fluid in whole buckets ({@code stored + 1}); {@link #STORED} is the number of full buckets under the top one.
 *
 * <ul>
 *   <li>A bucket (Vanilla or partner) pours one bucket in while there is room (a not quite full top fills up like Vanilla); an
 *       empty bucket takes one bucket out. A full Enderite bucket pours both its buckets, an empty one takes two, when there is
 *       room / enough inside.</li>
 *   <li>Bottles, washing, rain and dripstone run Vanilla's tables against the top bucket; while there is room the top shows as
 *       not full to them, so a bottle or rain keeps filling up to the last unit. Only a water bottle works on an empty one
 *       (other mods' empty-cauldron interactions would turn it into their own block and lose the tier).</li>
 *   <li>The fill height shows the whole content ({@link #shownLevel}); the comparator reads 1-3 the same way.</li>
 * </ul>
 * Vanilla impact (A12): none, own blocks only; filling and using up take as long per unit as Vanilla, a cauldron only lasts
 * 2x / 4x as long.
 */
public class TieredCauldronBlock extends ReinforcedCauldronBlock {
    /** Full buckets under the top bucket (0 .. capacity - 1; the Netherite cauldron only reaches 1). */
    public static final IntegerProperty STORED = IntegerProperty.create("stored", 0, 3);
    private static final VoxelShape[] FILLED = new VoxelShape[4];

    static {
        for (int level = 1; level <= 3; level++) {
            FILLED[level] = Shapes.or(AbstractCauldronBlock.SHAPE, Block.column(12.0, 4.0, 6.0 + level * 3.0));
        }
    }

    private final int capacity;

    public TieredCauldronBlock(Properties properties, int capacity) {
        super(properties);
        this.capacity = Math.max(1, Math.min(4, capacity));
        registerDefaultState(defaultBlockState().setValue(STORED, 0));
    }

    /** Buckets this cauldron holds. */
    public int capacity() {
        return capacity;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STORED);
    }

    // ------------------------------------------------------------------ units

    /** Units per bucket: 3 for water and powder snow (levels), 1 for lava and the extreme fluid. */
    public static int unitsPerBucket(Content content) {
        return content.layered() ? 3 : 1;
    }

    /** The content in units (0 empty). */
    public static int units(BlockState state) {
        Content content = state.getValue(CONTENT);
        if (content == Content.EMPTY) return 0;
        int stored = state.hasProperty(STORED) ? state.getValue(STORED) : 0;
        return content.layered() ? stored * 3 + levelOf(state) : stored + 1;
    }

    public int maxUnits(Content content) {
        return capacity * unitsPerBucket(content);
    }

    /** Whole buckets inside (a not quite full top does not count). */
    public static int buckets(BlockState state) {
        Content content = state.getValue(CONTENT);
        return content == Content.EMPTY ? 0 : units(state) / unitsPerBucket(content);
    }

    /** The state with {@code units} of {@code content} (0 or less: empty), capped at the capacity. */
    public BlockState withUnits(Content content, int units) {
        if (content == Content.EMPTY || units <= 0) return with(Content.EMPTY, 3).setValue(STORED, 0);
        units = Math.min(units, maxUnits(content));
        if (!content.layered()) return with(content, 3).setValue(STORED, units - 1);
        int stored = (units - 1) / 3;
        return with(content, units - stored * 3).setValue(STORED, stored);
    }

    /** Fill height 1-3 for the whole content (the top bucket alone would look empty with three buckets inside). */
    public int shownLevel(BlockState state) {
        Content content = state.getValue(CONTENT);
        if (content == Content.EMPTY) return 0;
        return Math.max(1, Math.min(3, (int) Math.ceil(3.0 * units(state) / maxUnits(content))));
    }

    @Override
    public boolean isFull(BlockState state) {
        Content content = state.getValue(CONTENT);
        return content != Content.EMPTY && units(state) >= maxUnits(content);
    }

    @Override
    protected double getContentHeight(BlockState state) {
        Content content = state.getValue(CONTENT);
        if (content == Content.EMPTY) return 0.0;
        return content.layered() ? (6.0 + 3.0 * shownLevel(state)) / 16.0 : 0.9375;
    }

    /** The top bucket as a Vanilla cauldron; while there is room a full top shows as level 2, so Vanilla lets it fill. */
    private @Nullable BlockState vanillaTop(BlockState state) {
        BlockState vanilla = toVanilla(state);
        if (vanilla == null || !vanilla.hasProperty(LayeredCauldronBlock.LEVEL)) return vanilla;
        int level = levelOf(state);
        if (level == 3 && !isFull(state)) level = 2;
        return vanilla.setValue(LayeredCauldronBlock.LEVEL, level);
    }

    private static int vanillaLevel(BlockState vanilla) {
        if (vanilla.is(Blocks.CAULDRON)) return 0;
        return vanilla.hasProperty(LayeredCauldronBlock.LEVEL) ? vanilla.getValue(LayeredCauldronBlock.LEVEL) : 3;
    }

    // ------------------------------------------------------------------ buckets

    /** Vanilla's buckets (water, lava, powder snow; the empty bucket takes them). */
    private static final ReinforcedCauldrons.Bucket VANILLA = new ReinforcedCauldrons.Bucket() {
        @Override
        public @Nullable Content pours(ItemStack held) {
            if (held.is(Items.WATER_BUCKET)) return Content.WATER;
            if (held.is(Items.LAVA_BUCKET)) return Content.LAVA;
            if (held.is(Items.POWDER_SNOW_BUCKET)) return Content.POWDER_SNOW;
            return null;
        }

        @Override
        public ItemStack afterPour(ItemStack held) {
            return new ItemStack(Items.BUCKET);
        }

        @Override
        public @Nullable ItemStack take(ItemStack held, Content content) {
            if (!held.is(Items.BUCKET)) return null;
            return switch (content) {
                case WATER -> new ItemStack(Items.WATER_BUCKET);
                case LAVA -> new ItemStack(Items.LAVA_BUCKET);
                case POWDER_SNOW -> new ItemStack(Items.POWDER_SNOW_BUCKET);
                default -> null;
            };
        }
    };

    private static List<ReinforcedCauldrons.Bucket> bucketKinds() {
        List<ReinforcedCauldrons.Bucket> kinds = new ArrayList<>(ReinforcedCauldrons.BUCKETS);
        kinds.add(VANILLA);
        return kinds;
    }

    /**
     * Pours {@code held} in (both buckets of a full Enderite bucket when there is room) or takes buckets out with it; the new
     * state and what stays in the hand, or null when the bucket does not fit this cauldron.
     */
    public @Nullable BucketResult bucket(BlockState state, ItemStack held) {
        Content content = state.getValue(CONTENT);
        int units = units(state);
        for (ReinforcedCauldrons.Bucket kind : bucketKinds()) {
            Content poured = kind.pours(held);
            if (poured != null) {
                if (content != Content.EMPTY && (poured != content || units >= maxUnits(content))) continue;
                int per = unitsPerBucket(poured);
                ItemStack hand = held;
                int count = 0;
                // a not quite full top fills up like Vanilla; every further bucket only goes in whole
                do {
                    units = Math.min(maxUnits(poured), units + per);
                    hand = kind.afterPour(hand);
                    count++;
                } while (poured.equals(kind.pours(hand)) && units + per <= maxUnits(poured));
                return new BucketResult(withUnits(poured, units), hand, poured, count, true);
            }
            if (content == Content.EMPTY) continue;
            int per = unitsPerBucket(content);
            if (units < per) continue;
            ItemStack filled = kind.take(held, content);
            if (filled == null) continue;
            units -= per;
            int count = 1;
            ItemStack more;
            while (units >= per && (more = kind.take(filled, content)) != null) {
                filled = more;
                units -= per;
                count++;
            }
            return new BucketResult(withUnits(content, units), filled, content, count, false);
        }
        return null;
    }

    public record BucketResult(BlockState state, ItemStack hand, Content content, int buckets, boolean poured) {}

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        BucketResult result = bucket(state, stack);
        if (result != null) {
            if (!level.isClientSide()) {
                ItemStack back = player.hasInfiniteMaterials() && result.poured() ? stack : result.hand();
                player.setItemInHand(hand, result.poured() ? back : ItemUtils.createFilledResult(stack, player, back));
                player.awardStat(result.poured() ? Stats.FILL_CAULDRON : Stats.USE_CAULDRON);
                level.setBlockAndUpdate(pos, result.state());
                boolean hot = result.content().hot();
                level.playSound(null, pos, result.poured()
                        ? (hot ? SoundEvents.BUCKET_EMPTY_LAVA : result.content() == Content.POWDER_SNOW ? SoundEvents.BUCKET_EMPTY_POWDER_SNOW : SoundEvents.BUCKET_EMPTY)
                        : (hot ? SoundEvents.BUCKET_FILL_LAVA : result.content() == Content.POWDER_SNOW ? SoundEvents.BUCKET_FILL_POWDER_SNOW : SoundEvents.BUCKET_FILL),
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, result.poured() ? GameEvent.FLUID_PLACE : GameEvent.FLUID_PICKUP, pos);
            }
            return InteractionResult.SUCCESS;
        }
        // a filled bucket that does not fit (full, other content) stays in the hand - Vanilla's table would pour it away
        for (ReinforcedCauldrons.Bucket kind : bucketKinds()) {
            if (kind.pours(stack) != null) return InteractionResult.FAIL;
        }
        Content content = state.getValue(CONTENT);
        if (content == Content.EMPTY && !stack.is(Items.POTION)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        BlockState vanilla = vanillaTop(state);
        if (vanilla == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        var table = switch (content) {
            case WATER -> CauldronInteractions.WATER;
            case LAVA -> CauldronInteractions.LAVA;
            case POWDER_SNOW -> CauldronInteractions.POWDER_SNOW;
            default -> CauldronInteractions.EMPTY;
        };
        InteractionResult out = table.get(stack).interact(vanilla, level, pos, player, hand, stack);
        if (!level.isClientSide()) settle(level, pos, state, vanillaLevel(vanilla));
        return out;
    }

    /**
     * After a Vanilla behaviour wrote its result at {@code pos} against the top bucket ({@code before} = the top's Vanilla level
     * it saw): the change in levels goes onto the whole content and the tiered cauldron comes back.
     */
    private void settle(Level level, BlockPos pos, BlockState old, int before) {
        BlockState now = level.getBlockState(pos);
        if (now.getBlock() == this) return;
        BlockState back = fromVanilla(now);
        if (back == null) return; // a partner block (not reached: only a water bottle runs on an empty one)
        Content was = old.getValue(CONTENT), is = back.getValue(CONTENT);
        int after = is == Content.EMPTY ? 0 : vanillaLevel(now);
        if (was == Content.EMPTY || is == was || is == Content.EMPTY) {
            Content content = was == Content.EMPTY ? is : was;
            // layered contents change by levels (= units); lava goes 0 <-> 3 for one bucket (= one unit)
            int delta = content.layered() ? after - before : (after - before) / 3;
            int units = (was == Content.EMPTY ? 0 : units(old)) + delta;
            level.setBlockAndUpdate(pos, withUnits(content, units));
        } else {
            level.setBlockAndUpdate(pos, back);
        }
    }

    /** Rain and snow fill it like the Vanilla cauldron, up to the last unit. */
    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        BlockState vanilla = vanillaTop(state);
        if (vanilla == null) return;
        vanilla.getBlock().handlePrecipitation(vanilla, level, pos, precipitation);
        settle(level, pos, state, vanillaLevel(vanilla));
    }

    /** Dripstone: an empty cauldron takes water or lava, water fills up one unit at a time (Vanilla rules). */
    @Override
    protected void receiveStalactiteDrip(BlockState state, Level level, BlockPos pos, Fluid fluid) {
        Content content = state.getValue(CONTENT);
        BlockState next = null;
        if (content == Content.EMPTY) {
            next = fluid == Fluids.WATER ? withUnits(Content.WATER, 1) : fluid == Fluids.LAVA ? withUnits(Content.LAVA, 1) : null;
        } else if (content == Content.WATER && fluid == Fluids.WATER && !isFull(state)) {
            next = withUnits(Content.WATER, units(state) + 1);
        } else if (content == Content.LAVA && fluid == Fluids.LAVA && !isFull(state)) {
            next = withUnits(Content.LAVA, units(state) + 1);
        }
        if (next == null) return;
        level.setBlockAndUpdate(pos, next);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(next));
        level.levelEvent(fluid == Fluids.LAVA ? 1046 : 1047, pos, 0);
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return state.getValue(CONTENT) == Content.EMPTY ? super.getEntityInsideCollisionShape(state, level, pos, entity) : FILLED[shownLevel(state)];
    }

    /** Water and powder snow put out a burning entity and lose one unit (Vanilla); lava and the extreme fluid as the reinforced one. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
        Content content = state.getValue(CONTENT);
        if (!content.layered()) {
            super.entityInside(state, level, pos, entity, effects, precise);
            return;
        }
        if (level instanceof ServerLevel server) {
            BlockPos at = pos.immutable();
            effects.runBefore(InsideBlockEffectType.EXTINGUISH, e -> {
                if (e.isOnFire() && e.mayInteract(server, at)) {
                    BlockState now = level.getBlockState(at);
                    if (now.getBlock() == this && now.getValue(CONTENT).layered()) {
                        BlockState after = withUnits(Content.WATER, units(now) - 1);
                        level.setBlockAndUpdate(at, after);
                        level.gameEvent(GameEvent.BLOCK_CHANGE, at, GameEvent.Context.of(after));
                    }
                }
            });
        }
        effects.apply(InsideBlockEffectType.EXTINGUISH);
        if (content == Content.POWDER_SNOW) effects.apply(InsideBlockEffectType.FREEZE);
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return shownLevel(state);
    }
}
