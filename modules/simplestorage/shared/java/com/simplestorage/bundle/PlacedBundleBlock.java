package com.simplestorage.bundle;


import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Ein abgestelltes Buendel (Besitzer 2026-09-28, siehe {@link PlacedBundles}): Schleichen +
 * Rechtsklick mit einem Buendel (seit B23-M11 auch einem Koecher) auf einen Block stellt es als 3D-Buendel ab:
 * auf die Oberseite gestellt faellt es wie Sand, sobald nichts darunter ist; an eine Seitenflaeche gehaengt haengt es
 * wie ein Item-Rahmen an der Wand ({@link PlacedStorage#WALL}); an die Decke nie (B23-M12). Das Buendel selbst (samt Inhalt und allen Komponenten) liegt in
 * der {@link PlacedBundleBlockEntity}; das Blockmodell waehlt die Stufe ({@link #TIER}) und, bei
 * gefaerbten Buendeln, das Zwei-Ebenen-Modell ({@link #DYED}), dessen Leder-Ebene die Farbe der
 * Block-Entity annimmt.
 *
 * <p>Wer schleichend darauf schaut, sieht das oberste Item ueber dem Buendel schweben (zur eigenen
 * Kamera gedreht); Schleichen + Mausrad waehlt es, ein Rechtsklick nimmt genau dieses Item heraus,
 * Schleichen + Rechtsklick mit einem Item legt es hinein ({@link PlacedBundles}). Abbauen,
 * Explosionen, Kolben und ein weggenommener Boden geben das Buendel mit seinem ganzen Inhalt
 * zurueck ({@link #getDrops}); im Kreativmodus faellt ein nicht leeres Buendel trotzdem heraus.
 *
 * <p>Form: das Modell {@code block/template_placed_bundle} (Boden, Bauch, Schulter, Hals, Zipfel, Knoten),
 * fuer Blickrichtung Norden angegeben und fuer die anderen drei Richtungen gedreht.
 */
public class PlacedBundleBlock extends BaseEntityBlock implements PlacedStorage.Falling {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Tier> TIER = EnumProperty.create("tier", Tier.class);
    /** Gefaerbt (Komponente {@code dyed_color} oder ein farbiges Vanilla-Buendel): Leder-Ebene getoent. */
    public static final BooleanProperty DYED = BooleanProperty.create("dyed");

    /**
     * Die Quader des Modells (Pixel, Vorderseite nach Norden); das Modell muss dazu passen. Von oben abgerundet:
     * Boden, Schulter und Zipfel mit einer Stufe an jeder Ecke, der Bauch mit zwei (tools/textures/placed_bundle_textures.py).
     */
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(4.0, 0.0, 5.0, 12.0, 1.0, 11.0),
            Block.box(5.0, 0.0, 4.0, 11.0, 1.0, 12.0),
            Block.box(3.0, 1.0, 5.0, 13.0, 7.0, 11.0),
            Block.box(4.0, 1.0, 4.0, 12.0, 7.0, 12.0),
            Block.box(5.0, 1.0, 3.0, 11.0, 7.0, 13.0),
            Block.box(4.0, 7.0, 5.0, 12.0, 8.0, 11.0),
            Block.box(5.0, 7.0, 4.0, 11.0, 8.0, 12.0),
            Block.box(6.0, 8.0, 6.0, 10.0, 9.0, 10.0),
            Block.box(5.0, 9.0, 6.0, 11.0, 11.0, 10.0),
            Block.box(6.0, 9.0, 5.0, 10.0, 11.0, 11.0),
            Block.box(7.0, 8.0, 4.0, 9.0, 10.0, 6.0));
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(NORTH_SHAPE);
    /** An der Wand: dieselben Quader, Rueckseite an der Wand (+{@link #WALL_SHIFT} nach Sueden) und ebenso weit angehoben. */
    public static final int WALL_SHIFT = 3;
    private static final Map<Direction, VoxelShape> WALL_SHAPES = Shapes.rotateHorizontal(NORTH_SHAPE.move(0.0, WALL_SHIFT / 16.0, WALL_SHIFT / 16.0));
    /** Koecher (stehendes Rohr mit Pfeilen, block/template_placed_quiver); an der Wand wie das Buendel verschoben. */
    private static final VoxelShape QUIVER_NORTH = Shapes.or(Block.box(5.0, 0.0, 5.0, 11.0, 11.0, 11.0), Block.box(5.5, 11.0, 5.5, 10.5, 15.0, 10.5));
    private static final Map<Direction, VoxelShape> QUIVER_SHAPES = Shapes.rotateHorizontal(QUIVER_NORTH);
    private static final Map<Direction, VoxelShape> QUIVER_WALL_SHAPES = Shapes.rotateHorizontal(QUIVER_NORTH.move(0.0, WALL_SHIFT / 16.0, WALL_SHIFT / 16.0));

    /** Die Stufen mit eigenem Modell; das Vanilla-Buendel ({@link #BUNDLE}) auch in allen 16 Farben; die vier Koecher (B23-M11). */
    public enum Tier implements StringRepresentable {
        BUNDLE("bundle"),
        REINFORCED("reinforced"),
        NETHERITE("netherite"),
        ENDERITE("enderite"),
        QUIVER("quiver"),
        REINFORCED_QUIVER("reinforced_quiver"),
        NETHERITE_QUIVER("netherite_quiver"),
        ENDERITE_QUIVER("enderite_quiver");

        private final String name;

        Tier(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public boolean isQuiver() {
            return this.name.endsWith("quiver");
        }
    }

    public PlacedBundleBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(TIER, Tier.BUNDLE).setValue(DYED, false).setValue(PlacedStorage.WALL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TIER, DYED, PlacedStorage.WALL);
    }

    /**
     * Stufe und Farbe nach dem Buendel in der Hand (Koecher vorerst ungefaerbt: ein Koecher-Modell je Stufe); Boden oder
     * Wand nach der geklickten Seite ({@link PlacedStorage#attach}), nie an die Decke.
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        Tier tier = PlacedBundles.tierOf(stack);
        BlockState state = this.defaultBlockState()
                .setValue(TIER, tier == null ? Tier.BUNDLE : tier)
                .setValue(DYED, (tier == null || !tier.isQuiver()) && PlacedBundles.dyeColor(stack) != DyedStorage.UNDYED);
        return PlacedStorage.attach(state, context, FACING);
    }

    /** Boden: immer (faellt sonst wie Sand); Wand: fester Block dahinter wie beim Item-Rahmen. */
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

    /** Am Boden ohne Halt: faellt samt Inhalt wie Sand ({@link PlacedStorage#fall}). */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (PlacedStorage.shouldFall(state, level, pos)) {
            PlacedStorage.fall(level, pos, state);
        }
    }

    @Override
    public ItemStack storedStack(BlockEntity blockEntity) {
        return blockEntity instanceof PlacedBundleBlockEntity be ? be.getBundle().copy() : ItemStack.EMPTY;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean wall = state.getValue(PlacedStorage.WALL);
        Map<Direction, VoxelShape> shapes = state.getValue(TIER).isQuiver() ? (wall ? QUIVER_WALL_SHAPES : QUIVER_SHAPES) : (wall ? WALL_SHAPES : SHAPES);
        return shapes.get(state.getValue(FACING));
    }

    // --- Buendel in der Block-Entity ------------------------------------------------------------

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlacedBundleBlockEntity(pos, state);
    }

    /** Rechtsklick (mit leerer Hand, oder ohne Schleichen mit beliebiger): das gezeigte Item herausnehmen. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof PlacedBundleBlockEntity be) || be.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            PlacedBundles.takeShown(level, pos, be, player);
        }
        return InteractionResult.SUCCESS;
    }

    /** Genau das gespeicherte Buendel samt Inhalt, egal wodurch der Block verschwindet. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof PlacedBundleBlockEntity placed && !placed.getBundle().isEmpty()) {
            return List.of(placed.getBundle().copy());
        }
        return List.of();
    }

    /**
     * Im Kreativmodus droppt Vanilla beim Abbauen nichts; ein Buendel mit Inhalt faellt trotzdem heraus
     * (wie Rucksack und Shulkerkiste), ein leeres verschwindet.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops()
                && level.getBlockEntity(pos) instanceof PlacedBundleBlockEntity be && !be.isEmpty()) {
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, be.getBundle().copy());
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Mittlere Maustaste: das abgestellte Buendel selbst. */
    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return level.getBlockEntity(pos) instanceof PlacedBundleBlockEntity be ? be.getBundle().copy() : ItemStack.EMPTY;
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
