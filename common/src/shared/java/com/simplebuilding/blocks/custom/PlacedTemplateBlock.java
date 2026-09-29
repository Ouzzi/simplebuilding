package com.simplebuilding.blocks.custom;

import com.simplebuilding.version.BlockCodecs;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity;
import com.simplebuilding.util.PlacedPlate;
import com.simplebuilding.util.PlacedTemplates;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Eine abgelegte Schmiedevorlage (Besitzer 2026-09-28, siehe {@link PlacedTemplates}): flach auf dem
 * Boden, an der Wand oder unter der Decke, wie ein Knopf ueber {@link AttachFace} und die
 * Blickrichtung ausgerichtet. Die Vorlage selbst (samt Komponenten) liegt in der
 * {@link PlacedTemplateBlockEntity}; gezeichnet wird sie clientseitig als ausgestanzte Platte von
 * gut einem Pixel Staerke, das Blockmodell traegt nur die Partikeltextur.
 *
 * <p>Wasser kann ihr nichts: sie ist wasserfuellbar wie ein Schild, fliessendes Wasser laeuft darum
 * herum. Abbauen, Explosionen, Kolben und ein weggenommener Halt geben genau den gespeicherten
 * Stapel zurueck ({@link #getDrops}); eine Loot-Tabelle gibt es deshalb nicht.
 *
 * <p>Mit Vorschlaghammer und Aufwertungs-Material in den Haenden bricht sie nie; jeder Linksklick
 * ist dann ein Schlag der Besatz-Aufwertung ({@link #attack}, im Kreativmodus ueber
 * {@code SledgehammerItem#canDestroyBlock}).
 */
public class PlacedTemplateBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock, SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final MapCodec<PlacedTemplateBlock> CODEC = BlockCodecs.simple(PlacedTemplateBlock::new);

    public PlacedTemplateBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.FLOOR).setValue(WATERLOGGED, false));
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    /** Richtung der Schauseite: weg von dem Block, an dem die Vorlage liegt. */
    public static Direction normal(BlockState state) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> state.getValue(FACING);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return state.setValue(WATERLOGGED, water);
    }

    /**
     * Genau die gezeichneten Pixel der Platte ({@link PlacedPlate}): trifft nur, wer auf die Vorlage
     * zeigt, nicht auf die freien Ecken daneben. Ohne Block-Entity (Blockzustand ohne Welt) die ganze
     * Plattenflaeche.
     */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        ItemStack stored = PlacedTemplates.templateAt(level, pos);
        return PlacedPlate.shape(stored.isEmpty() ? null : stored.getItem(), state.getValue(FACE), state.getValue(FACING));
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    // --- Vorlage in der Block-Entity ---------------------------------------------------------

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlacedTemplateBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.PLACED_TEMPLATE_BE) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<PlacedTemplateBlockEntity>) PlacedTemplateBlockEntity::serverTick;
    }

    /**
     * Rechtsklick auf einen abgelegten Oktanten: blendet dessen Auswahl fuer diesen Spieler ein oder
     * aus ({@link PlacedTemplates#toggleOctantOutline}). Vorlagen, Blaupausen und der Attractor
     * reagieren nicht auf Rechtsklick.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isSpectator() || !(level.getBlockEntity(pos) instanceof PlacedTemplateBlockEntity be)
                || !PlacedTemplates.isPlacedOctant(level, pos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            PlacedTemplates.toggleOctantOutline(level, pos, be, player);
        }
        return InteractionResult.SUCCESS;
    }

    /** Genau der gespeicherte Stapel, egal wodurch der Block verschwindet. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof PlacedTemplateBlockEntity placed && !placed.getTemplate().isEmpty()) {
            return List.of(placed.getTemplate().copy());
        }
        return List.of();
    }

    /** Mittlere Maustaste: die abgelegte Vorlage selbst (samt Namen, siehe {@link PlacedTemplateBlockEntity#getName()}). */
    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return PlacedTemplates.templateAt(level, pos).copy();
    }

    // --- Vorschlaghammer ------------------------------------------------------------------------

    /** Mit Hammer und Material in den Haenden bricht die Vorlage nicht; jeder Klick ist ein Schlag. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (PlacedTemplates.isHammerStance(player)) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    /** Linksklick im Ueberlebensmodus (serverseitig aus {@code ServerPlayerGameMode}). */
    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        PlacedTemplates.hit(level, pos, player);
        super.attack(state, level, pos, player);
    }
}
