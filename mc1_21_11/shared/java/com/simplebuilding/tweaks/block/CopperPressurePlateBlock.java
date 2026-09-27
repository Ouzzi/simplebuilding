package com.simplebuilding.tweaks.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.tweaks.block.entity.CopperPressurePlateBlockEntity;
import com.simplebuilding.tweaks.block.entity.OwnedBlockEntity;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Kupfer-Druckplatte (Simple Tweaks): loest erst aus, wenn ein Spieler 1/2/3/4 s darauf steht - je
 * oxidierter, desto laenger - und laesst nach dem Verlassen genauso lange verzoegert wieder los.
 * Oxidiert wie Kupferbloecke; die Axt kratzt eine Stufe ab. Honigwabe wachst sie wie Vanilla-Kupfer
 * (gewachste Platten oxidieren nicht), die Axt kratzt das Wachs wieder ab. Oxidations- und Wachsfolge
 * stehen hier selbst (Simple Tweaks: Fabrics OxidizableBlocksRegistry), damit sie auf jedem Loader
 * gleich laufen; der Besitzer bleibt beim Oxidieren, Wachsen und Abkratzen erhalten. Gedrueckt sinkt
 * sie wie eine Vanilla-Druckplatte ein ({@link #POWERED}: halbe Hoehe, Modell {@code _down}).
 */
public class CopperPressurePlateBlock extends PadBlock implements WeatheringCopper {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final MapCodec<CopperPressurePlateBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(CopperPressurePlateBlock::getAge),
            Codec.BOOL.fieldOf("waxed").forGetter(CopperPressurePlateBlock::isWaxed),
            propertiesCodec()
    ).apply(i, CopperPressurePlateBlock::new));

    /** Wie Vanilla-Druckplatten: 1 Pixel hoch, gedrueckt ein halbes. */
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 1, 15);
    private static final VoxelShape SHAPE_PRESSED = Block.box(1, 0, 1, 15, 0.5, 15);

    private final WeatheringCopper.WeatherState weatherState;
    private final boolean waxed;

    public CopperPressurePlateBlock(WeatheringCopper.WeatherState weatherState, boolean waxed, BlockBehaviour.Properties properties) {
        super(properties, SHAPE, PadOwnership.OWNER_PLATE, PadOwnership.STRANGER_PLATE);
        this.weatherState = weatherState;
        this.waxed = waxed;
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    /** Ungewachste Stufen in Oxidationsreihenfolge. */
    public static List<Block> stages() {
        return List.of(TweaksBlocks.COPPER_PRESSURE_PLATE, TweaksBlocks.EXPOSED_COPPER_PRESSURE_PLATE,
                TweaksBlocks.WEATHERED_COPPER_PRESSURE_PLATE, TweaksBlocks.OXIDIZED_COPPER_PRESSURE_PLATE);
    }

    /** Gewachste Stufen, in derselben Reihenfolge wie {@link #stages()}. */
    public static List<Block> waxedStages() {
        return List.of(TweaksBlocks.WAXED_COPPER_PRESSURE_PLATE, TweaksBlocks.WAXED_EXPOSED_COPPER_PRESSURE_PLATE,
                TweaksBlocks.WAXED_WEATHERED_COPPER_PRESSURE_PLATE, TweaksBlocks.WAXED_OXIDIZED_COPPER_PRESSURE_PLATE);
    }

    /** Stehzeit bis zum Ausloesen und ebenso die Wartezeit bis zum Loslassen: 20/40/60/80 Ticks. */
    public static int requiredTicks(WeatheringCopper.WeatherState state) {
        return 20 * (state.ordinal() + 1);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public WeatheringCopper.WeatherState getAge() {
        return weatherState;
    }

    public boolean isWaxed() {
        return waxed;
    }

    @Override
    public Optional<BlockState> getNext(BlockState state) {
        int index = weatherState.ordinal();
        return !waxed && index + 1 < 4 ? Optional.of(stages().get(index + 1).withPropertiesOf(state)) : Optional.empty();
    }

    /** Eine Oxidationsstufe zurueck (Axt); gewachste Platten verlieren zuerst ihr Wachs. */
    public Optional<BlockState> getPreviousState(BlockState state) {
        int index = weatherState.ordinal();
        return !waxed && index > 0 ? Optional.of(stages().get(index - 1).withPropertiesOf(state)) : Optional.empty();
    }

    /** Dieselbe Stufe gewachst; leer, wenn schon gewachst. */
    public Optional<BlockState> getWaxedState(BlockState state) {
        return waxed ? Optional.empty() : Optional.of(waxedStages().get(weatherState.ordinal()).withPropertiesOf(state));
    }

    /** Dieselbe Stufe ohne Wachs; leer, wenn nicht gewachst. */
    public Optional<BlockState> getUnwaxedState(BlockState state) {
        return waxed ? Optional.of(stages().get(weatherState.ordinal()).withPropertiesOf(state)) : Optional.empty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(POWERED) ? SHAPE_PRESSED : SHAPE;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) && direction == Direction.UP ? 15 : 0;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !waxed && weatherState != WeatheringCopper.WeatherState.OXIDIZED;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (waxed) {
            return;
        }
        UUID owner = level.getBlockEntity(pos) instanceof OwnedBlockEntity owned ? owned.getOwner() : null;
        changeOverTime(state, level, pos, random);
        if (owner != null && level.getBlockState(pos) != state && level.getBlockEntity(pos) instanceof OwnedBlockEntity owned) {
            owned.setOwner(owner);
        }
    }

    /**
     * Honigwabe wachst (Vanilla-Partikel und -Geraeusch), die Axt kratzt Wachs ab oder - ungewachst -
     * eine Oxidationsstufe, jeweils wie bei Kupferbloecken.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Items.HONEYCOMB)) {
            Optional<BlockState> waxedState = getWaxedState(state);
            if (waxedState.isEmpty()) {
                return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
            }
            if (!level.isClientSide()) {
                if (player instanceof ServerPlayer serverPlayer) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
                }
                replaceKeepingOwner(level, pos, waxedState.get(), player);
                level.levelEvent(null, LevelEvent.PARTICLES_AND_SOUND_WAX_ON, pos, 0);
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (!stack.is(ItemTags.AXES)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        Optional<BlockState> unwaxed = getUnwaxedState(state);
        Optional<BlockState> target = unwaxed.isPresent() ? unwaxed : getPreviousState(state);
        if (target.isEmpty()) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (!level.isClientSide()) {
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
            }
            replaceKeepingOwner(level, pos, target.get(), player);
            if (unwaxed.isPresent()) {
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0f, 1.0f);
                level.levelEvent(null, LevelEvent.PARTICLES_WAX_OFF, pos, 0);
            } else {
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0f, 1.0f);
                level.levelEvent(null, LevelEvent.PARTICLES_SCRAPE, pos, 0);
            }
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        }
        return InteractionResult.SUCCESS;
    }

    /** Setzt die neue Stufe und gibt ihr den Besitzer der alten mit. */
    private static void replaceKeepingOwner(Level level, BlockPos pos, BlockState next, Player player) {
        UUID owner = level.getBlockEntity(pos) instanceof OwnedBlockEntity owned ? owned.getOwner() : null;
        level.setBlock(pos, next, Block.UPDATE_ALL_IMMEDIATE);
        if (owner != null && level.getBlockEntity(pos) instanceof OwnedBlockEntity owned) {
            owned.setOwner(owner);
        }
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, next));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperPressurePlateBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, TweaksBlockEntities.COPPER_PRESSURE_PLATE, CopperPressurePlateBlockEntity::serverTick);
    }
}
