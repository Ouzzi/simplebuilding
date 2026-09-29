package com.simplebuilding.blocks.custom;

import com.simplebuilding.version.BlockCodecs;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.blocks.entity.custom.PlacedBundleBlockEntity;
import com.simplebuilding.util.DyedStorage;
import com.simplebuilding.util.PlacedBundles;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * Rechtsklick mit einem Buendel auf die Oberseite eines Blocks stellt es als 3D-Buendel ab - nur auf
 * den Boden, nie an Wand oder Decke. Das Buendel selbst (samt Inhalt und allen Komponenten) liegt in
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
public class PlacedBundleBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Tier> TIER = EnumProperty.create("tier", Tier.class);
    /** Gefaerbt (Komponente {@code dyed_color} oder ein farbiges Vanilla-Buendel): Leder-Ebene getoent. */
    public static final BooleanProperty DYED = BooleanProperty.create("dyed");

    public static final MapCodec<PlacedBundleBlock> CODEC = BlockCodecs.simple(PlacedBundleBlock::new);

    /** Die Quader des Modells (Pixel, Vorderseite nach Norden); das Modell muss dazu passen. */
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(4.0, 0.0, 4.0, 12.0, 1.0, 12.0),
            Block.box(3.0, 1.0, 3.0, 13.0, 7.0, 13.0),
            Block.box(4.0, 7.0, 4.0, 12.0, 8.0, 12.0),
            Block.box(6.0, 8.0, 6.0, 10.0, 9.0, 10.0),
            Block.box(5.0, 9.0, 5.0, 11.0, 11.0, 11.0),
            Block.box(7.0, 8.0, 4.0, 9.0, 10.0, 6.0));
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(NORTH_SHAPE);

    /** Die Stufen mit eigenem Modell; das Vanilla-Buendel ({@link #BUNDLE}) auch in allen 16 Farben. */
    public enum Tier implements StringRepresentable {
        BUNDLE("bundle"),
        REINFORCED("reinforced"),
        NETHERITE("netherite"),
        ENDERITE("enderite");

        private final String name;

        Tier(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public PlacedBundleBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(TIER, Tier.BUNDLE).setValue(DYED, false));
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TIER, DYED);
    }

    /** Vorderseite zum Spieler, Stufe und Farbe nach dem Buendel in der Hand; nur auf einen tragenden Boden. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        Tier tier = PlacedBundles.tierOf(stack);
        BlockState state = this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(TIER, tier == null ? Tier.BUNDLE : tier)
                .setValue(DYED, PlacedBundles.dyeColor(stack) != DyedStorage.UNDYED);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    /** Wie eine Laterne: der Block darunter muss die Mitte tragen. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
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
