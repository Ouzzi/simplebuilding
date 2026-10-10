package com.simplesandwiches.block;

import com.simplesandwiches.config.SandwichConfig;
import com.simplesandwiches.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Cauldron with milk that ripens by itself (owner decisions F5/F6, 2026-10-04).
 *
 * <ul>
 *   <li>Milk bucket into an empty cauldron -> {@code milk}; four visible stages over
 *       {@code butterTicks} -> {@code butter} (stays until taken with an empty hand -> butter block).</li>
 *   <li>Fermented spider eye into milk -> {@code curdling}; four stages over {@code cheeseTicks} ->
 *       {@code cheese}. The cheese must be taken with an empty hand within
 *       {@code cheeseHarvestWindowTicks}, otherwise it turns into {@code spoiled} milk that can only be
 *       emptied (gives nothing).</li>
 * </ul>
 * Ripening uses scheduled block ticks, which only run in loaded, ticking chunks.
 *
 * <p>{@code reinforced}: made from SimpleLib's reinforced cauldron (which runs the Vanilla cauldron
 * interactions, milk included, and sets this flag). Emptying it returns that cauldron and breaking it
 * drops it; SimpleLib is found by id only, so the module stays independent.
 */
public class MilkCauldronBlock extends AbstractCauldronBlock {
    public static final int STAGES = 4;
    public static final EnumProperty<Content> CONTENT = EnumProperty.create("content", Content.class);
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, STAGES - 1);
    public static final BooleanProperty REINFORCED = BooleanProperty.create("reinforced");
    private static final Identifier REINFORCED_CAULDRON = Identifier.fromNamespaceAndPath("simplelib", "reinforced_cauldron");

    public enum Content implements StringRepresentable {
        MILK, BUTTER, CURDLING, CHEESE, SPOILED;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public boolean ripe() {
            return this == BUTTER || this == CHEESE;
        }
    }

    public MilkCauldronBlock(Properties properties) {
        super(properties, new CauldronInteraction.Dispatcher());
        registerDefaultState(stateDefinition.any().setValue(CONTENT, Content.MILK).setValue(STAGE, 0).setValue(REINFORCED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONTENT, STAGE, REINFORCED);
    }

    @Override
    public boolean isFull(BlockState state) {
        return true;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return 0.9375;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
        return switch (state.getValue(CONTENT)) {
            case MILK, CURDLING -> 1 + state.getValue(STAGE);
            case BUTTER, CHEESE -> 15;
            case SPOILED -> 8;
        };
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(emptied(state).getBlock());
    }

    /** The empty cauldron this one returns to: SimpleLib's reinforced cauldron when it was made from one. */
    public static BlockState emptied(BlockState state) {
        if (state.hasProperty(REINFORCED) && state.getValue(REINFORCED)) {
            var block = BuiltInRegistries.BLOCK.getOptional(REINFORCED_CAULDRON);
            if (block.isPresent()) return block.get().defaultBlockState();
        }
        return Blocks.CAULDRON.defaultBlockState();
    }

    /** The loot table covers the normal one; the reinforced one drops SimpleLib's reinforced cauldron. */
    @Override
    protected java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        if (!state.getValue(REINFORCED) || emptied(state).is(Blocks.CAULDRON)) return super.getDrops(state, params);
        Float radius = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.EXPLOSION_RADIUS);
        if (radius != null && radius > 0 && params.getLevel().getRandom().nextFloat() > 1.0F / radius) return java.util.List.of();
        return java.util.List.of(new ItemStack(emptied(state).getBlock()));
    }

    /** Ticks between two visible stages (and for cheese the harvest window). */
    public static int interval(Content content) {
        return switch (content) {
            case MILK -> Math.max(1, SandwichConfig.butterTicks / STAGES);
            case CURDLING -> Math.max(1, SandwichConfig.cheeseTicks / STAGES);
            case CHEESE -> SandwichConfig.cheeseHarvestWindowTicks;
            default -> 0;
        };
    }

    /** Sets {@code content} at stage 0 and schedules its next ripening step. */
    public static void start(Level level, BlockPos pos, Content content) {
        BlockState here = level.getBlockState(pos);
        boolean reinforced = here.is(ModBlocks.MILK_CAULDRON) ? here.getValue(REINFORCED)
                : BuiltInRegistries.BLOCK.getKey(here.getBlock()).equals(REINFORCED_CAULDRON);
        level.setBlock(pos, ModBlocks.MILK_CAULDRON.defaultBlockState().setValue(CONTENT, content).setValue(STAGE, 0)
                .setValue(REINFORCED, reinforced), Block.UPDATE_ALL);
        int wait = interval(content);
        if (wait > 0) level.scheduleTick(pos, ModBlocks.MILK_CAULDRON, wait);
        level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
    }

    /** The next state after one scheduled step (null = unchanged). */
    public static BlockState next(BlockState state) {
        Content content = state.getValue(CONTENT);
        int stage = state.getValue(STAGE);
        return switch (content) {
            case MILK -> stage < STAGES - 1 ? state.setValue(STAGE, stage + 1) : state.setValue(CONTENT, Content.BUTTER).setValue(STAGE, 0);
            case CURDLING -> stage < STAGES - 1 ? state.setValue(STAGE, stage + 1) : state.setValue(CONTENT, Content.CHEESE).setValue(STAGE, 0);
            case CHEESE -> state.setValue(CONTENT, Content.SPOILED).setValue(STAGE, 0);
            default -> null;
        };
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState next = next(state);
        if (next == null) return;
        level.setBlock(pos, next, Block.UPDATE_ALL);
        Content content = next.getValue(CONTENT);
        if (content.ripe()) level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.4F);
        if (content == Content.SPOILED) level.playSound(null, pos, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.BLOCKS, 0.8F, 0.6F);
        int wait = interval(content);
        if (wait > 0) level.scheduleTick(pos, this, wait);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // Placed by other means (/setblock, structure): keep ripening going.
        if (!level.isClientSide() && !oldState.is(this) && interval(state.getValue(CONTENT)) > 0 && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, interval(state.getValue(CONTENT)));
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        Content content = state.getValue(CONTENT);
        ItemStack filled = content == Content.MILK ? filledContainer(stack) : ItemStack.EMPTY;
        if ((stack.is(Items.BUCKET) && (content == Content.MILK || content == Content.SPOILED)) || !filled.isEmpty()) {
            if (!level.isClientSide()) {
                // Spoiled milk is only poured out: the bucket stays empty.
                if (content == Content.MILK) player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filled));
                level.setBlockAndUpdate(pos, emptied(state));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.FERMENTED_SPIDER_EYE) && content == Content.MILK) {
            if (!level.isClientSide()) {
                stack.consume(1, player);
                start(level, pos, Content.CURDLING);
                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 1.3F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        Content content = state.getValue(CONTENT);
        if (!content.ripe() && content != Content.SPOILED) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, emptied(state));
            if (content == Content.SPOILED) {
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8F, 0.6F);
            } else {
                ItemStack block = new ItemStack(content == Content.BUTTER ? ModBlocks.BUTTER_BLOCK : ModBlocks.CHEESE_BLOCK);
                player.setItemInHand(InteractionHand.MAIN_HAND, block);
                level.playSound(null, pos, SoundEvents.HONEY_BLOCK_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** Item tag of the milk containers (Vanilla's milk bucket; other mods add theirs): pouring leaves the crafting remainder. */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> MILK_BUCKETS = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, Identifier.fromNamespaceAndPath("simplesandwiches", "milk_buckets"));

    /** What stays in the hand after pouring a milk container: its crafting remainder, the plain bucket otherwise. */
    @SuppressWarnings("deprecation")
    private static ItemStack emptiedContainer(ItemStack stack) {
        var remainder = stack.getItem().getCraftingRemainder();
        return remainder == null ? new ItemStack(Items.BUCKET) : remainder.create();
    }

    /** The milk container {@code held} turns into when it takes milk: the tagged item whose remainder is {@code held}'s item. */
    private static ItemStack filledContainer(ItemStack held) {
        if (held.is(Items.BUCKET)) return new ItemStack(Items.MILK_BUCKET);
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(MILK_BUCKETS)) {
            @SuppressWarnings("deprecation")
            var remainder = holder.value().getCraftingRemainder();
            if (remainder != null && remainder.item().value() == held.getItem()) return held.transmuteCopy(holder.value(), 1);
        }
        return ItemStack.EMPTY;
    }

    /** Empty cauldron + milk bucket (registered on {@code CauldronInteractions.EMPTY}). */
    public static InteractionResult fillWithMilk(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        if (!level.isClientSide()) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, emptiedContainer(stack)));
            start(level, pos, Content.MILK);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Content content = state.getValue(CONTENT);
        double x = pos.getX() + 0.25 + random.nextDouble() * 0.5, y = pos.getY() + 0.95, z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
        if (content.ripe() && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y + 0.1, z, 0, 0.02, 0);
        } else if (content == Content.SPOILED && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.MYCELIUM, x, y, z, 0, 0, 0);
            if (random.nextInt(4) == 0) level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
        } else if (content == Content.CURDLING && random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0, 0.01, 0);
        }
    }
}
