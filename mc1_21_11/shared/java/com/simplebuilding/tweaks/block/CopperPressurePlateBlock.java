package com.simplebuilding.tweaks.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.tweaks.block.entity.CopperPressurePlateBlockEntity;
import com.simplebuilding.tweaks.block.entity.OwnedBlockEntity;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Kupfer-Druckplatte (Simple Tweaks): loest erst aus, wenn ein Spieler 1/2/3/4 s darauf steht - je
 * oxidierter, desto laenger. Oxidiert wie Kupferbloecke; die Axt kratzt eine Stufe ab. Die
 * Oxidationsfolge steht hier selbst (Simple Tweaks: Fabrics OxidizableBlocksRegistry), damit sie
 * auf jedem Loader gleich laeuft; der Besitzer bleibt beim Oxidieren erhalten.
 */
public class CopperPressurePlateBlock extends PadBlock implements WeatheringCopper {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final MapCodec<CopperPressurePlateBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(CopperPressurePlateBlock::getAge),
            propertiesCodec()
    ).apply(i, CopperPressurePlateBlock::new));

    private final WeatheringCopper.WeatherState weatherState;

    public CopperPressurePlateBlock(WeatheringCopper.WeatherState weatherState, BlockBehaviour.Properties properties) {
        super(properties, Block.box(1, 0, 1, 15, 1, 15), PadOwnership.OWNER_PLATE, PadOwnership.STRANGER_PLATE);
        this.weatherState = weatherState;
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    /** Stufen in Oxidationsreihenfolge. */
    public static List<Block> stages() {
        return List.of(TweaksBlocks.COPPER_PRESSURE_PLATE, TweaksBlocks.EXPOSED_COPPER_PRESSURE_PLATE,
                TweaksBlocks.WEATHERED_COPPER_PRESSURE_PLATE, TweaksBlocks.OXIDIZED_COPPER_PRESSURE_PLATE);
    }

    /** Stehzeit bis zum Ausloesen: 20/40/60/80 Ticks. */
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

    @Override
    public Optional<BlockState> getNext(BlockState state) {
        int index = weatherState.ordinal();
        return index + 1 < 4 ? Optional.of(stages().get(index + 1).withPropertiesOf(state)) : Optional.empty();
    }

    public Optional<BlockState> getPreviousState(BlockState state) {
        int index = weatherState.ordinal();
        return index > 0 ? Optional.of(stages().get(index - 1).withPropertiesOf(state)) : Optional.empty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
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
        return weatherState != WeatheringCopper.WeatherState.OXIDIZED;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        UUID owner = level.getBlockEntity(pos) instanceof OwnedBlockEntity owned ? owned.getOwner() : null;
        changeOverTime(state, level, pos, random);
        if (owner != null && level.getBlockState(pos) != state && level.getBlockEntity(pos) instanceof OwnedBlockEntity owned) {
            owned.setOwner(owner);
        }
    }

    /** Axt kratzt eine Oxidationsstufe ab, wie bei Kupferbloecken. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(ItemTags.AXES)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        Optional<BlockState> previous = getPreviousState(state);
        if (previous.isEmpty()) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (!level.isClientSide()) {
            UUID owner = level.getBlockEntity(pos) instanceof OwnedBlockEntity owned ? owned.getOwner() : null;
            level.setBlock(pos, previous.get(), Block.UPDATE_ALL_IMMEDIATE);
            if (owner != null && level.getBlockEntity(pos) instanceof OwnedBlockEntity owned) {
                owned.setOwner(owner);
            }
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0f, 1.0f);
            level.levelEvent(null, LevelEvent.PARTICLES_SCRAPE, pos, 0);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        }
        return InteractionResult.SUCCESS;
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
