package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.util.PlacedSmallParts;
import com.simplebuilding.version.BlockCodecs;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Bis zu vier Kleinteile auf einem Fleck (Besitzer 2026-10-02, siehe {@link PlacedSmallParts}): Steinkiesel,
 * Feuersteinsplitter, Vanilla-Kleinteile und Eier in beliebiger Mischung. Die Teile (samt Komponenten) liegen in der
 * {@link PlacedSmallPartsBlockEntity}; gezeichnet werden sie vom {@code PlacedSmallPartsRenderer}, das Blockmodell
 * traegt nur die Partikeltextur. Nur auf dem Boden ({@link #FACING} = Blickrichtung beim ersten Ablegen), ohne
 * Kollision, wasserfuellbar wie die abgelegte Vorlage, von Kolben zerstoert.
 *
 * <p>Abbauen ({@link #getDrops}, {@link #spawnAfterBreak}, {@link #playerWillDestroy}): jedes liegende Teil faellt als es
 * selbst heraus; Eier mit Behutsamkeit ebenso, sonst zerbrechen sie und schluepfen wie geworfene Eier. Explosionen,
 * Kolben und ein weggenommener Boden bauen ohne Werkzeug ab. Keine Loot-Tabelle.
 *
 * <p>Kerzen und Seegurken (2026-10-03): {@link #LIT}, {@link #CANDLES} und {@link #PICKLES} tragen, was das Licht braucht
 * ({@link #light}); die Zaehler gleicht die Block-Entity nach jeder Teil-Aenderung ab. Feuerzeug, Feuerkugel und
 * brennende Geschosse zuenden an, die leere Hand und Wasser loeschen - wie beim Vanilla-Kerzenblock.
 */
public class PlacedSmallPartsBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** Die Kerzen des Flecks brennen (alle zusammen, wie bei einem Vanilla-Kerzenblock). */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Zahl der Kerzen unter den Teilen (aus der Block-Entity abgeglichen, nur fuers Licht). */
    public static final IntegerProperty CANDLES = IntegerProperty.create("candles", 0, PlacedSmallParts.MAX_PARTS);
    /** Zahl der Seegurken unter den Teilen (aus der Block-Entity abgeglichen, nur fuers Licht). */
    public static final IntegerProperty PICKLES = IntegerProperty.create("pickles", 0, PlacedSmallParts.MAX_PARTS);
    public static final MapCodec<PlacedSmallPartsBlock> CODEC = BlockCodecs.simple(PlacedSmallPartsBlock::new);

    public PlacedSmallPartsBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // Alte Welten: Haeufchen ohne lit/candles/pickles laden mit diesen Standardwerten - richtig, sie hatten keine.
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false)
                .setValue(LIT, false).setValue(CANDLES, 0).setValue(PICKLES, 0));
    }

    /**
     * Licht wie die Vanilla-Bloecke, nur aus dem Zustand: unter Wasser leuchten Seegurken (3 + 3 je Gurke), trocken
     * brennende Kerzen (3 je Kerze). Kerzen brennen nie unter Wasser, Gurken leuchten nie trocken.
     */
    public static int light(BlockState state) {
        if (state.getValue(WATERLOGGED)) {
            int pickles = state.getValue(PICKLES);
            return pickles > 0 ? Math.min(15, 3 + 3 * pickles) : 0;
        }
        return state.getValue(LIT) ? Math.min(15, 3 * state.getValue(CANDLES)) : 0;
    }

    /** Der Zustand mit den Zaehlern dieser Teile; ohne Kerzen brennt nichts. */
    public static BlockState withCounts(BlockState state, List<ItemStack> parts) {
        int candles = Math.min(PlacedSmallParts.MAX_PARTS, PlacedSmallParts.candles(parts));
        int pickles = Math.min(PlacedSmallParts.MAX_PARTS, PlacedSmallParts.pickles(parts));
        return state.setValue(CANDLES, candles).setValue(PICKLES, pickles).setValue(LIT, state.getValue(LIT) && candles > 0);
    }

    /** Laesst sich anzuenden: Kerzen da, aus, trocken (wie {@code CandleBlock#canLight}). */
    public static boolean canLight(BlockState state) {
        return state.getValue(CANDLES) > 0 && !state.getValue(LIT) && !state.getValue(WATERLOGGED);
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, LIT, CANDLES, PICKLES);
    }

    /** Oberkante der Teile in die Blickrichtung; wassergefuellt in einer Wasserquelle. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(WATERLOGGED, water);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    /** Pixelgenau die gezeichneten Teile (in der Block-Entity zwischengespeichert). */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof PlacedSmallPartsBlockEntity be) {
            return be.shape();
        }
        return PlacedSmallParts.shape(List.of(), state.getValue(FACING));
    }

    /** Braucht einen Boden, der seine Mitte traegt (wie das gelegte Ei). */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
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
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /** Wasser hinein loescht brennende Kerzen (wie {@code CandleBlock#placeLiquid}). */
    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        if (state.getValue(WATERLOGGED) || fluid.getType() != Fluids.WATER) {
            return false;
        }
        BlockState wet = state.setValue(WATERLOGGED, true);
        if (state.getValue(LIT)) {
            wet = wet.setValue(LIT, false);
            level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        level.setBlock(pos, wet, Block.UPDATE_ALL);
        level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
        return true;
    }

    /** Ein brennendes Geschoss zuendet die Kerzen an (wie {@code AbstractCandleBlock#onProjectileHit}). */
    @Override
    protected void onProjectileHit(net.minecraft.world.level.Level level, BlockState state, net.minecraft.world.phys.BlockHitResult hit,
                                   net.minecraft.world.entity.projectile.Projectile projectile) {
        if (!level.isClientSide() && projectile.isOnFire() && canLight(state)) {
            level.setBlock(hit.getBlockPos(), state.setValue(LIT, true), Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    /** Loescht die Kerzen: Rauch an jedem Docht (Client), Klang und Spielereignis (Server). */
    public static void extinguish(@Nullable net.minecraft.world.entity.player.Player player, BlockState state, net.minecraft.world.level.Level level, BlockPos pos) {
        if (level.isClientSide()) {
            com.simplebuilding.util.PlacedPartParticles.wickSmoke(level, pos, state);
            return;
        }
        level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL_IMMEDIATE);
        level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
    }

    /** Flamme und Rauch der brennenden Kerzen, dezente Glanz-Partikel leuchtender Teile ({@code PlacedPartParticles}). */
    @Override
    public void animateTick(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, RandomSource random) {
        com.simplebuilding.util.PlacedPartParticles.animate(state, level, pos, random);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlacedSmallPartsBlockEntity(pos, state);
    }

    /** Jedes liegende Teil; Eier nur mit Behutsamkeit (das Werkzeug kommt aus den Loot-Parametern). */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(be instanceof PlacedSmallPartsBlockEntity pile)) {
            return List.of();
        }
        return PlacedSmallParts.drops(pile.parts(), PlacedSmallParts.silkTouch(params.getLevel(), params.getOptionalParameter(LootContextParams.TOOL) instanceof ItemStack tool ? tool : null));
    }

    /**
     * Abbauen ohne Spieler (Explosion, Kolben, weggenommener Boden): die Block-Entity steht dann noch in der Welt,
     * also zerbrechen hier die Eier. Beim Abbauen durch einen Spieler ist sie schon entfernt - dann
     * {@link #playerWillDestroy}.
     */
    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (level.getBlockEntity(pos) instanceof PlacedSmallPartsBlockEntity pile && !PlacedSmallParts.silkTouch(level, tool)) {
            PlacedSmallParts.breakEggs(level, pos, pile.parts(), level.getRandom());
        }
    }

    /**
     * Abbauen durch einen Spieler im Ueberlebensmodus: kurz bevor der Block verschwindet (die Block-Entity steht noch),
     * zerbrechen ohne Behutsamkeit in der Haupthand die Eier. Die Drops kommen danach aus {@link #getDrops}; dann ist die
     * Block-Entity schon aus der Welt, {@link #spawnAfterBreak} findet keine mehr - kein Ei zerbricht zweimal. Im
     * Kreativmodus zerbricht nichts (wie beim alten gelegten Ei).
     */
    @Override
    public BlockState playerWillDestroy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        if (level instanceof ServerLevel server && !player.isCreative() && level.getBlockEntity(pos) instanceof PlacedSmallPartsBlockEntity pile
                && !PlacedSmallParts.silkTouch(server, player.getMainHandItem())) {
            PlacedSmallParts.breakEggs(server, pos, pile.parts(), server.getRandom());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Rechtsklick mit einem Klumpen auf eine liegende Shulkerschale wertet sie um eine Stufe auf
     * ({@link com.simplebuilding.util.ShulkerShells}); alles andere geht weiter an das Item.
     */
    @Override
    protected net.minecraft.world.InteractionResult useItemOn(ItemStack stack, BlockState state, net.minecraft.world.level.Level level,
                                                              BlockPos pos, net.minecraft.world.entity.player.Player player,
                                                              net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (com.simplebuilding.util.ShulkerShells.upgrade(level, pos, player, stack)) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        // Vorschlaghammer wendet eine abgelegte Schallplatte (A-Seite <-> B-Seite, 2026-10-03).
        if (com.simplebuilding.util.DiscFlips.flip(level, pos, player, stack, hand)) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        // Leere Hand loescht brennende Kerzen (wie CandleBlock#useItemOn).
        if (stack.isEmpty() && state.getValue(LIT) && player.getAbilities().mayBuild) {
            extinguish(player, state, level, pos);
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        // Eissplitter loescht brennende Kerzen und schmilzt dabei (ein Splitter, 2026-10-05).
        if (isChip(stack, com.simplebuilding.items.ModItems.ICE_CHIP) && state.getValue(LIT) && player.getAbilities().mayBuild) {
            extinguish(player, state, level, pos);
            if (!level.isClientSide()) {
                stack.consume(1, player);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        // Feuerzeug, Feuerkugel oder Feuerkugelsplitter zuenden die Kerzen an (Vanilla-Klang, Haltbarkeit bzw. ein Stueck).
        boolean flint = stack.is(net.minecraft.world.item.Items.FLINT_AND_STEEL);
        if ((flint || stack.is(net.minecraft.world.item.Items.FIRE_CHARGE) || isChip(stack, com.simplebuilding.items.ModItems.FIRE_CHIP))
                && canLight(state)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL_IMMEDIATE);
                RandomSource random = level.getRandom();
                if (flint) {
                    level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.4F + 0.8F);
                    stack.hurtAndBreak(1, player, hand);
                } else {
                    level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
                    stack.consume(1, player);
                }
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** Splitter-Pruefung, die auch ohne die Kleinteile (26.2: Item {@code null}) sicher ist. */
    private static boolean isChip(ItemStack stack, @Nullable net.minecraft.world.item.Item chip) {
        return chip != null && stack.is(chip);
    }

    /** Mittlere Maustaste: das zuletzt dazugelegte Teil. */
    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        if (level.getBlockEntity(pos) instanceof PlacedSmallPartsBlockEntity pile && !pile.parts().isEmpty()) {
            return pile.parts().getLast().copyWithCount(1);
        }
        return ItemStack.EMPTY;
    }
}
