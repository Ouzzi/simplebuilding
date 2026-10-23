package com.simplebuilding.blocks.custom;

import com.simplebuilding.blocks.entity.custom.PlacedPaintBoxBlockEntity;
import com.simplebuilding.items.custom.ColorBrushItem;
import com.simplebuilding.items.custom.PaintBoxItem;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Ein abgestellter Farbkasten (W24-2, Besitzer 23.10.; Form im Plan docs/ai/PLAN-A-TEX24B-2026-10-23.md): Schleichen +
 * Rechtsklick mit einem Farbkasten auf eine Oberseite stellt ihn als flachen Koffer ab (Deckel = Icon B4 von oben,
 * Schloss zum Spieler). Der Kasten samt Inhalt liegt in der {@link PlacedPaintBoxBlockEntity}. Rechtsklick mit leerer
 * Hand nimmt ihn wieder auf, Rechtsklick mit Farbstoff legt ihn hinein; Abbauen, Kolben, Explosionen und ein
 * weggenommener Boden geben ihn mit Inhalt zurueck (kreativ ein nicht leerer auch). Nur auf tragfaehigem Boden, nicht an
 * Wand oder Decke: ein Farbkasten ist ein Tischgegenstand.
 */
public class PlacedPaintBoxBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Tier> TIER = EnumProperty.create("tier", Tier.class);

    /** Der Koffer des Modells (block/template_placed_paint_box, Schloss nach Norden; tools/textures/w24b_2026_10_23.py). */
    private static final VoxelShape NORTH_SHAPE = Block.box(1.0, 0.0, 3.0, 15.0, 3.0, 14.0);
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(NORTH_SHAPE);

    /** Die vier Farbkasten-Stufen, je mit eigenem Modell. */
    public enum Tier implements StringRepresentable {
        BASIC("basic"), REINFORCED("reinforced"), NETHERITE("netherite"), ENDERITE("enderite");

        private final String name;

        Tier(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        /** Die Stufe eines Farbkastens, oder null fuer alles andere. */
        public static @Nullable Tier of(ItemStack stack) {
            Item item = stack.getItem();
            if (item == com.simplebuilding.items.ModItems.PAINT_BOX) return BASIC;
            if (item == com.simplebuilding.items.ModItems.REINFORCED_PAINT_BOX) return REINFORCED;
            if (item == com.simplebuilding.items.ModItems.NETHERITE_PAINT_BOX) return NETHERITE;
            if (item == com.simplebuilding.items.ModItems.ENDERITE_PAINT_BOX) return ENDERITE;
            return null;
        }
    }

    public PlacedPaintBoxBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TIER, Tier.BASIC));
    }

    public static final com.mojang.serialization.MapCodec<PlacedPaintBoxBlock> CODEC =
            com.simplebuilding.version.BlockCodecs.simple(PlacedPaintBoxBlock::new);

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TIER);
    }

    // --- Abstellen (aus PaintBoxItem#useOn) -----------------------------------------------------------

    /**
     * Schleichen + Rechtsklick eines Farbkastens auf eine Oberseite; null, wenn nichts abgestellt wird (dann laeuft
     * das gewohnte Verhalten weiter).
     */
    public static @Nullable InteractionResult tryPlace(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Block block = com.simplebuilding.blocks.ModBlocks.PLACED_PAINT_BOX;
        Tier tier = Tier.of(stack);
        if (block == null || PlacedPaintBoxBlockEntity.TYPE == null || player == null || !player.isSecondaryUseActive()
                || context.getClickedFace() != Direction.UP || tier == null || !player.mayBuild()) {
            return null;
        }
        BlockPlaceContext place = new BlockPlaceContext(context);
        Level level = context.getLevel();
        BlockPos pos = place.getClickedPos();
        BlockState state = block.defaultBlockState().setValue(TIER, tier).setValue(FACING, place.getHorizontalDirection().getOpposite());
        if (!place.canPlace() || !state.canSurvive(level, pos)) {
            return null;
        }
        if (!level.isClientSide()) {
            if (!level.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE)) {
                return null;
            }
            if (level.getBlockEntity(pos) instanceof PlacedPaintBoxBlockEntity be) {
                be.setBox(stack.copyWithCount(1));
            }
            SoundType sound = state.getSoundType();
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

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

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return PlacedPaintBoxBlockEntity.TYPE == null ? null : new PlacedPaintBoxBlockEntity(pos, state);
    }

    // --- Benutzen ---------------------------------------------------------------------------------------

    /** Rechtsklick mit Farbstoff legt ihn hinein (so viel passt); alles andere wie mit leerer Hand bzw. normal. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (ColorBrushItem.dyeColor(stack) == null || !(level.getBlockEntity(pos) instanceof PlacedPaintBoxBlockEntity be)) {
            return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack box = be.getBox().copy();
            int moved = PaintBoxItem.insert(box, stack);
            if (moved > 0) {
                be.setBox(box);
                level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8F, 0.8F + level.getRandom().nextFloat() * 0.4F);
            } else {
                level.playSound(null, pos, SoundEvents.BUNDLE_INSERT_FAIL, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Rechtsklick mit leerer Hand: den Kasten samt Inhalt aufnehmen (in die Hand, sonst ins Inventar, sonst davor). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PlacedPaintBoxBlockEntity be) || be.getBox().isEmpty() || !player.mayBuild()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack box = be.getBox().copy();
            be.setBox(ItemStack.EMPTY);
            level.removeBlock(pos, false);
            if (player.getMainHandItem().isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, box);
            } else if (!player.getInventory().add(box) && !box.isEmpty()) {
                com.simplebuilding.version.McVersion.drop(player, box, false, false);
            }
            level.playSound(null, pos, SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.BLOCKS, 0.8F, 0.8F + level.getRandom().nextFloat() * 0.4F);
            level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
        }
        return InteractionResult.SUCCESS;
    }

    // --- Zurueckgeben ------------------------------------------------------------------------------------

    /** Genau der gespeicherte Farbkasten samt Inhalt, egal wodurch der Block verschwindet. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof PlacedPaintBoxBlockEntity placed && !placed.getBox().isEmpty()) {
            return List.of(placed.getBox().copy());
        }
        return List.of();
    }

    /** Kreativ droppt Vanilla nichts; ein Kasten mit Inhalt faellt trotzdem heraus (wie das abgestellte Buendel). */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops() && level.getBlockEntity(pos) instanceof PlacedPaintBoxBlockEntity be
                && !PaintBoxItem.contents(be.getBox()).isEmpty()) {
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, be.getBox().copy());
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return level.getBlockEntity(pos) instanceof PlacedPaintBoxBlockEntity be ? be.getBox().copy() : ItemStack.EMPTY;
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
