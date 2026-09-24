package com.simplebuilding.blocks.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.blocks.entity.custom.BackpackBlockEntity;
import com.simplebuilding.items.custom.BackpackTier;
import com.simplebuilding.platform.BackpackMenus;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Ein abgestellter Rucksack: Schleichen + Rechtsklick mit dem Rucksack auf einen Block stellt ihn
 * mit der Vorderseite zum Spieler ab, Rechtsklick oeffnet ihn, Abbauen gibt das Rucksack-Item samt
 * Inhalt zurueck (Loot-Tabelle mit {@code copy_components}, ohne Werkzeug- oder
 * Explosionsbedingung - er faellt immer).
 *
 * <p>Form: der Sack (10 x 13 x 6 Pixel) mit einer Vordertasche (8 x 8 x 2). Die Form ist fuer
 * Blickrichtung Norden angegeben und wird fuer die anderen drei Richtungen gedreht.
 */
public class BackpackBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /**
     * Gefaerbt ({@code minecraft:dyed_color} am abgestellten Item): waehlt das Zwei-Ebenen-Modell
     * {@code block/template_backpack_dyed}; die Farbe selbst liegt in der Block-Entity und kommt
     * clientseitig ueber {@code BackpackBlockTint} ins Modell.
     */
    public static final BooleanProperty DYED = BooleanProperty.create("dyed");

    private static final Codec<BackpackTier> TIER_CODEC = Codec.INT.xmap(BackpackTier::byId, BackpackTier::ordinal);
    public static final MapCodec<BackpackBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TIER_CODEC.fieldOf("tier").forGetter(BackpackBlock::getTier),
            propertiesCodec()
    ).apply(i, BackpackBlock::new));

    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Shapes.or(
            Block.box(3.0, 0.0, 5.0, 13.0, 13.0, 11.0),
            Block.box(4.0, 0.0, 3.0, 12.0, 8.0, 5.0)));

    private final BackpackTier tier;

    public BackpackBlock(BackpackTier tier, BlockBehaviour.Properties properties) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DYED, false));
    }

    public BackpackTier getTier() {
        return this.tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BackpackBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(DYED, context.getItemInHand().has(DataComponents.DYED_COLOR));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DYED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack) {
            BackpackMenus.openPlaced(serverPlayer, backpack);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Im Kreativmodus droppt Vanilla beim Abbauen nichts. Ein voller Rucksack faellt trotzdem als
     * Item mit Inhalt heraus - dasselbe, was eine Shulkerkiste tut; ein leerer verschwindet.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops()
                && level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack && !backpack.isEmpty()) {
            ItemStack stack = new ItemStack(state.getBlock());
            stack.applyComponents(backpack.collectComponents());
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
