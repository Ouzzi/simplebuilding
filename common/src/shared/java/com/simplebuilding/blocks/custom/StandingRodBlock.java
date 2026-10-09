package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.util.PlacedSmallParts;
import com.simplebuilding.util.PlacedTemplates;
import com.simplebuilding.version.BlockCodecs;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A stick, bone, blaze rod, breeze rod or diamond rod standing upright (owner 2026-10-04,
 * docs/ai/PLAN-SENKRECHTE-RODS-2026-10-04.md). One block for all of them: {@link #ROD} picks the model (a thin column
 * made from the item texture's pixels, without the lightning rod's head), the light, the sound and the drop; there is
 * no block item - the rod item itself is placed and dropped.
 *
 * <p>Sneaking + right-click on the top of a block with one of the rods stands it up ({@link #tryPlace}, from
 * {@code ItemUseOnMixin}, ahead of the lying small parts - on a pile it is still added lying). It needs a floor that
 * holds its middle or another standing rod below (stacked rods make posts), its thin shape collides, so it holds a
 * hammock's rope like any anchor ({@link HammockLayout#isAnchor}). Waterloggable, popped by pistons.
 */
public class StandingRodBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<Rod> ROD = EnumProperty.create("rod", Rod.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /**
     * Another standing rod stands on this one (N16, 2026-10-09): the column goes on to the top of the block
     * ({@code standing_<rod>_up}), so stacked rods join without a gap.
     */
    public static final BooleanProperty UP = BlockStateProperties.UP;
    /** The column of a rod with another one above it: full block height. */
    public static final VoxelShape CONNECTED_SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);
    public static final MapCodec<StandingRodBlock> CODEC = BlockCodecs.simple(StandingRodBlock::new);

    /** The rods that stand: state name, item, column height in pixels (= rows of the item texture), light, sound. */
    public enum Rod implements StringRepresentable {
        STICK("stick", () -> Items.STICK, 13, 0, SoundType.WOOD),
        BONE("bone", () -> Items.BONE, 14, 0, SoundType.BONE_BLOCK),
        /** Glows a little, like the blaze it came from. */
        BLAZE_ROD("blaze_rod", () -> Items.BLAZE_ROD, 14, 5, SoundType.METAL),
        BREEZE_ROD("breeze_rod", () -> Items.BREEZE_ROD, 14, 0, SoundType.METAL),
        DIAMOND_ROD("diamond_rod", () -> com.simplebuilding.items.ModItems.DIAMOND_ROD, 14, 0, SoundType.METAL);

        private final String name;
        private final Supplier<Item> item;
        private final int height;
        private final int light;
        private final SoundType sound;
        private final VoxelShape shape;

        Rod(String name, Supplier<Item> item, int height, int light, SoundType sound) {
            this.name = name;
            this.item = item;
            this.height = height;
            this.light = light;
            this.sound = sound;
            this.shape = Block.box(6.0, 0.0, 6.0, 10.0, height, 10.0);
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        /** The item that stands as this rod (null only if it does not exist on this line). */
        public @Nullable Item item() {
            return item.get();
        }

        public int height() {
            return height;
        }

        public int light() {
            return light;
        }

        /** The rod for this item, or null. */
        public static @Nullable Rod of(ItemStack stack) {
            if (stack.isEmpty()) {
                return null;
            }
            for (Rod rod : values()) {
                Item item = rod.item();
                if (item != null && stack.is(item)) {
                    return rod;
                }
            }
            return null;
        }
    }

    public StandingRodBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ROD, Rod.STICK).setValue(WATERLOGGED, false).setValue(UP, false));
    }

    /** Light of the state: only the blaze rod glows (a little). */
    public static int light(BlockState state) {
        return state.getValue(ROD).light();
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROD, WATERLOGGED, UP);
    }

    // =====================================================================================
    // Standing it up
    // =====================================================================================

    /**
     * Sneaking + right-click on the top of a block with a rod: stands it up on the block above the clicked one. Null
     * (the usual behaviour goes on) without sneaking, on a side or the bottom, on or above a pile of small parts (the
     * rod is added to the pile lying), where the server options switch the item off, or where it cannot stand.
     */
    public static @Nullable InteractionResult tryPlace(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (ModBlocks.STANDING_ROD == null || player == null || !player.isSecondaryUseActive()
                || context.getClickedFace() != Direction.UP || !player.mayBuild()) {
            return null;
        }
        Rod rod = Rod.of(stack);
        if (rod == null || !allowed(stack)) {
            return null;
        }
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        if (PlacedSmallParts.isPileSpot(level, clicked, stack) || PlacedSmallParts.isPileSpot(level, clicked.above(), stack)) {
            return null;
        }
        BlockPlaceContext place = new BlockPlaceContext(context);
        if (!place.canPlace()) {
            return null;
        }
        BlockPos pos = place.getClickedPos();
        BlockState state = ModBlocks.STANDING_ROD.defaultBlockState().setValue(ROD, rod)
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER)
                .setValue(UP, level.getBlockState(pos.above()).getBlock() instanceof StandingRodBlock);
        if (!state.canSurvive(level, pos) || !level.isUnobstructed(state, pos, CollisionContext.of(player))) {
            return null;
        }
        if (!level.isClientSide()) {
            if (!level.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE)) {
                return null;
            }
            SoundType sound = rod.sound;
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    /** The server options of the small parts apply: {@code placeVanillaItems} and {@code placeDisabledItems}. */
    public static boolean allowed(ItemStack stack) {
        var features = com.simplebuilding.config.ServerTuning.get().features;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if ("minecraft".equals(id.getNamespace()) && !features.placeVanillaItems) {
            return false;
        }
        return !PlacedTemplates.itemListed(features.placeDisabledItems, id);
    }

    // =====================================================================================
    // Block behaviour
    // =====================================================================================

    /** The thin column (4 x 4 px around the middle, as tall as the rod, full height under another rod) - outline and collision. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(UP) ? CONNECTED_SHAPE : state.getValue(ROD).shape;
    }

    /** On a floor that holds its middle, or on another standing rod. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).getBlock() instanceof StandingRodBlock || Block.canSupportCenter(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (direction == Direction.UP) {
            state = state.setValue(UP, neighborState.getBlock() instanceof StandingRodBlock);
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    // No @Override on purpose (same signature on both lines, but keep 26.2 compiling if it differs).
    protected SoundType getSoundType(BlockState state) {
        return state.getValue(ROD).sound;
    }

    /** The rod item itself (no loot table). */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Item item = state.getValue(ROD).item();
        return item == null ? List.of() : List.of(new ItemStack(item));
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        Item item = state.getValue(ROD).item();
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
