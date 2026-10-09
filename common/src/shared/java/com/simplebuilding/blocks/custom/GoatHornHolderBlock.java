package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.GoatHornHolderBlockEntity;
import com.simplebuilding.version.BlockCodecs;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A goat horn put down as a holder (owner 2026-10-08, queue N24 "Ziegenhorn platzierbar; Fackeln oder stabartige Items
 * hineinstecken"; docs/ai/PLAN-PLATZIEREN-N24-2026-10-09.md).
 *
 * <p>Sneaking + right-click with a goat horn on the top of a block stands it there, its mouth up; on a wall it hangs
 * from a little peg, the mouth tilted up away from the wall ({@link #tryPlace}, from {@code ItemUseOnMixin}). Not under a
 * ceiling. The horn itself (with its instrument) is kept in the {@link GoatHornHolderBlockEntity} and dropped again.
 *
 * <p>Right-click with a torch (every {@link BaseTorchBlock} item: torch, soul, copper, redstone torch) or a rod that can
 * stand ({@link StandingRodBlock.Rod}: stick, bone, blaze, breeze, diamond rod) puts one into the horn; it stands in
 * the mouth as its 3D model ({@code GoatHornHolderRenderer}). Torches light the holder like the torch block (state
 * {@link #LIGHT}) and flicker; the blaze rod glows a little like a standing one. An empty hand takes it out again.
 */
public class GoatHornHolderBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
    /** Light of the held item (0 when empty). */
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    public static final MapCodec<GoatHornHolderBlock> CODEC = BlockCodecs.simple(GoatHornHolderBlock::new);
    /** Height of the mouth's top above the block's bottom, in pixels: standing and on a wall. */
    public static final float FLOOR_MOUTH = 9.0F;
    public static final float WALL_MOUTH = 13.0F;
    /** How deep the held item sits in the mouth, in pixels. */
    public static final float DEPTH = 3.0F;

    private static final Map<Direction, VoxelShape> FLOOR_SHAPES = shapes(5, 0, 5, 11, 9, 14);
    private static final Map<Direction, VoxelShape> WALL_SHAPES = shapes(5, 4, 5, 11, 13, 16);

    public GoatHornHolderBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.FLOOR).setValue(LIGHT, 0));
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public static int light(BlockState state) {
        return state.getValue(LIGHT);
    }

    /** The shape for facing north (pixels), turned for the other directions like the block state model. */
    private static Map<Direction, VoxelShape> shapes(double x0, double y0, double z0, double x1, double y1, double z1) {
        Map<Direction, VoxelShape> out = new EnumMap<>(Direction.class);
        out.put(Direction.NORTH, Block.box(x0, y0, z0, x1, y1, z1));
        out.put(Direction.EAST, Block.box(16 - z1, y0, x0, 16 - z0, y1, x1));
        out.put(Direction.SOUTH, Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0));
        out.put(Direction.WEST, Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0));
        return out;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, LIGHT);
    }

    /** Floor and walls only - a horn does not hang under a ceiling. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null || state.getValue(FACE) == AttachFace.CEILING ? null : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(FACE) == AttachFace.WALL ? WALL_SHAPES : FLOOR_SHAPES).get(state.getValue(FACING));
    }

    // =====================================================================================
    // Putting the horn down
    // =====================================================================================

    /** A goat horn (any instrument). */
    public static boolean isHorn(ItemStack stack) {
        return stack.is(Items.GOAT_HORN);
    }

    /**
     * Sneaking + right-click with a goat horn on a top or a wall: puts it down as a holder. Null (the horn is blown as
     * usual) without sneaking, under a ceiling, where it cannot hang or may not be built.
     */
    public static @Nullable InteractionResult tryPlace(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (ModBlocks.GOAT_HORN_HOLDER == null || player == null || !player.isSecondaryUseActive() || !isHorn(stack)
                || context.getClickedFace() == Direction.DOWN || !player.mayBuild()) {
            return null;
        }
        BlockPlaceContext place = new BlockPlaceContext(context);
        if (!place.canPlace()) {
            return null;
        }
        Level level = context.getLevel();
        BlockPos pos = place.getClickedPos();
        BlockState state = ModBlocks.GOAT_HORN_HOLDER.getStateForPlacement(place);
        if (state == null || !state.canSurvive(level, pos)) {
            return null;
        }
        if (!level.isClientSide()) {
            if (!level.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE)) {
                return null;
            }
            if (level.getBlockEntity(pos) instanceof GoatHornHolderBlockEntity be) {
                be.setHorn(stack.copyWithCount(1));
            }
            SoundType sound = state.getSoundType();
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    // =====================================================================================
    // What fits into the horn
    // =====================================================================================

    /** A torch item (its block is a {@link BaseTorchBlock}) or a rod that can stand. */
    public static boolean fits(ItemStack stack) {
        return !stack.isEmpty() && (Block.byItem(stack.getItem()) instanceof BaseTorchBlock || StandingRodBlock.Rod.of(stack) != null);
    }

    /** Light of a held item: the torch block's own light, a standing rod's light, else 0. */
    public static int lightOf(ItemStack held) {
        if (held.isEmpty()) {
            return 0;
        }
        Block block = Block.byItem(held.getItem());
        if (block instanceof BaseTorchBlock) {
            return block.defaultBlockState().getLightEmission();
        }
        StandingRodBlock.Rod rod = StandingRodBlock.Rod.of(held);
        return rod == null ? 0 : rod.light();
    }

    /** Right-click with something that fits into the empty horn puts one in; everything else goes on (empty hand). */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!fits(stack) || !(level.getBlockEntity(pos) instanceof GoatHornHolderBlockEntity be) || !be.held().isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            be.setHeld(stack.copyWithCount(1));
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.1F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    /** Empty hand: takes the held item out (into the hand, or dropped when the inventory is full). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSpectator() || !(level.getBlockEntity(pos) instanceof GoatHornHolderBlockEntity be) || be.held().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack out = be.held().copy();
            be.setHeld(ItemStack.EMPTY);
            if (!player.getInventory().add(out)) {
                com.simplebuilding.version.McVersion.drop(player, out, false, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 1.1F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return InteractionResult.SUCCESS;
    }

    // =====================================================================================
    // Block entity, drops, particles
    // =====================================================================================

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GoatHornHolderBlockEntity(pos, state);
    }

    /** The horn and what it holds, whatever removes the block. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof GoatHornHolderBlockEntity be) {
            if (!be.horn().isEmpty()) {
                drops.add(be.horn().copy());
            }
            if (!be.held().isEmpty()) {
                drops.add(be.held().copy());
            }
        }
        return drops;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return level.getBlockEntity(pos) instanceof GoatHornHolderBlockEntity be && !be.horn().isEmpty() ? be.horn().copy()
                : new ItemStack(Items.GOAT_HORN);
    }

    /** Where the tip of a held item is, relative to the block (for the flame). */
    public static double tipY(BlockState state, float itemHeight) {
        float mouth = state.getValue(FACE) == AttachFace.WALL ? WALL_MOUTH : FLOOR_MOUTH;
        return (mouth - DEPTH + itemHeight) / 16.0;
    }

    /** A held torch flickers like the torch block: smoke and its flame at the tip. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof GoatHornHolderBlockEntity be)) {
            return;
        }
        Block torch = Block.byItem(be.held().getItem());
        if (!(torch instanceof BaseTorchBlock)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + tipY(state, 10.0F) + 0.05;
        double z = pos.getZ() + 0.5;
        ParticleOptions flame = torch == Blocks.SOUL_TORCH ? ParticleTypes.SOUL_FIRE_FLAME
                : torch == Blocks.COPPER_TORCH ? ParticleTypes.COPPER_FIRE_FLAME
                : torch == Blocks.REDSTONE_TORCH ? DustParticleOptions.REDSTONE : ParticleTypes.FLAME;
        if (torch != Blocks.REDSTONE_TORCH) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
        }
        level.addParticle(flame, x, y, z, 0.0, 0.0, 0.0);
    }
}
