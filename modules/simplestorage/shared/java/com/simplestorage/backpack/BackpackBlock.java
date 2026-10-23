package com.simplestorage.backpack;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.simplestorage.bundle.PlacedStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
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
 * Ein abgestellter Rucksack (split stage 8c-2, from SimpleBuilding's {@code BackpackBlock}; 26.3 only, no block codec): Schleichen + Rechtsklick mit dem Rucksack auf einen Block stellt ihn
 * mit der Vorderseite zum Spieler ab, Rechtsklick oeffnet ihn, Abbauen gibt das Rucksack-Item samt
 * Inhalt zurueck (Loot-Tabelle mit {@code copy_components}, ohne Werkzeug- oder
 * Explosionsbedingung - er faellt immer).
 *
 * <p>Wo er steht (B23-M12, {@link PlacedStorage}): auf eine Oberseite gestellt faellt er wie Sand samt Inhalt, sobald
 * nichts darunter ist; an eine Seitenflaeche gehaengt haengt er wie ein Item-Rahmen an der Wand (Rueckseite an der Wand,
 * {@link #WALL_SHIFT} angehoben); an die Decke nie.
 *
 * <p>Form: der Sack (10 x 13 x 6 Pixel) mit einer Vordertasche (8 x 8 x 2). Die Form ist fuer
 * Blickrichtung Norden angegeben und wird fuer die anderen drei Richtungen gedreht.
 */
public class BackpackBlock extends BaseEntityBlock implements PlacedStorage.Falling {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /**
     * Gefaerbt ({@code minecraft:dyed_color} am abgestellten Item): waehlt das Zwei-Ebenen-Modell
     * {@code block/template_backpack_dyed}; die Farbe selbst liegt in der Block-Entity und kommt
     * clientseitig ueber {@code BackpackBlockTint} ins Modell.
     */
    public static final BooleanProperty DYED = BooleanProperty.create("dyed");

    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(3.0, 0.0, 5.0, 13.0, 13.0, 11.0),
            Block.box(4.0, 0.0, 3.0, 12.0, 8.0, 5.0));
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(NORTH_SHAPE);
    /** An der Wand: Rueckseite (z 11) an die Wand (z 16), um so viel nach Sueden und {@link #WALL_LIFT} angehoben. */
    public static final int WALL_SHIFT = 5, WALL_LIFT = 1;
    private static final Map<Direction, VoxelShape> WALL_SHAPES = Shapes.rotateHorizontal(NORTH_SHAPE.move(0.0, WALL_LIFT / 16.0, WALL_SHIFT / 16.0));

    private final BackpackTier tier;

    public BackpackBlock(BackpackTier tier, BlockBehaviour.Properties properties) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DYED, false)
                .setValue(PlacedStorage.WALL, false));
    }

    public BackpackTier getTier() {
        return this.tier;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BackpackBlockEntity(pos, state);
    }

    /** Boden oder Wand nach der geklickten Seite, nie an die Decke ({@link PlacedStorage#attach}). */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return PlacedStorage.attach(this.defaultBlockState().setValue(DYED, context.getItemInHand().has(DataComponents.DYED_COLOR)),
                context, FACING);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DYED, PlacedStorage.WALL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(PlacedStorage.WALL) ? WALL_SHAPES : SHAPES).get(state.getValue(FACING));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return PlacedStorage.canSurvive(state, level, pos, FACING);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, PlacedStorage.FALL_DELAY);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(PlacedStorage.WALL)) {
            if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
                return Blocks.AIR.defaultBlockState();
            }
        } else {
            ticks.scheduleTick(pos, this, PlacedStorage.FALL_DELAY);
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    /** Am Boden ohne Halt: faellt samt Inhalt wie Sand. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (PlacedStorage.shouldFall(state, level, pos)) {
            PlacedStorage.fall(level, pos, state);
        }
    }

    /** Der Rucksack samt Inhalt und Farbe aus der Block-Entity (fuer einen beim Fallen zerbrochenen). */
    @Override
    public ItemStack storedStack(BlockEntity blockEntity) {
        ItemStack stack = new ItemStack(this);
        stack.applyComponents(blockEntity.collectComponents());
        return stack;
    }

    /**
     * Abgestellt (Immersion 2026-09-28): zum Wollklang des Blocks das Rascheln von Leder und ein
     * Staubwoelkchen am Boden.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        // SimpleBuildings Erfolg "Lager aufschlagen" ({@code storage/pitching_camp}) fragt seit 8c-2 Vanillas placed_block.
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            server.playSound(null, pos, net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER.value(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 0.9f);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.1,
                    pos.getZ() + 0.5, 4, 0.2, 0.02, 0.2, 0.0);
        }
    }

    /**
     * Komparator am abgestellten Rucksack (Besitzer 2026-09-28): der Fuellstand wie bei einer Truhe,
     * gerechnet mit den Stapelgrenzen des Rucksacks (Tiefe Taschen eingerechnet). Trichter sehen ihn
     * weiterhin nicht (siehe {@link BackpackBlockEntity}).
     */
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack
                ? net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(backpack.container()) : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack) {
            // Aufgeschnallt: Leder-Rascheln, etwas heller als beim Abstellen.
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER.value(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 1.2f);
            BackpackMenuProviders.open(serverPlayer, BackpackMenuProviders.placed(backpack));
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
