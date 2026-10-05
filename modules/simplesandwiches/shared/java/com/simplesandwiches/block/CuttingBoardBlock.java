package com.simplesandwiches.block;

import com.simplesandwiches.block.CuttingBoardBlockEntity.Stage;
import com.simplesandwiches.config.SandwichConfig;
import com.simplesandwiches.item.KnifeItem;
import com.simplesandwiches.registry.ModItems;
import com.simplesandwiches.sandwich.SandwichFormula;
import com.simplesandwiches.sandwich.SandwichItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.RandomSource;

/**
 * Cutting board (one block per wood type, one shared block entity). State machine (plan section 5):
 * bread -> knife opens -> optional butter (knife + butter slice in the other hand, only first) ->
 * up to {@code maxIngredients} ingredients (knife takes the top one back) -> empty hand closes ->
 * empty hand takes the sandwich; knife reopens a closed sandwich. Feedback is sound and particles
 * only.
 */
public class CuttingBoardBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE_NS = Block.box(1, 0, 2, 15, 2, 14);
    private static final VoxelShape SHAPE_EW = Block.box(2, 0, 1, 14, 2, 15);

    /** Every board action; {@link #plan} decides without side effects (also used for hand hints). */
    public enum Action { NONE, PUT_BREAD, PUT_SANDWICH, OPEN_LOAF, TAKE_BREAD, BUTTER, ADD_INGREDIENT, FULL, POP_INGREDIENT,
        CLOSE, TAKE_SANDWICH, REOPEN }

    public CuttingBoardBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CuttingBoardBlockEntity(pos, state);
    }

    // --- State machine -----------------------------------------------------------------------------

    public static Action plan(CuttingBoardBlockEntity board, ItemStack main, ItemStack off) {
        Stage stage = board.stage();
        boolean knife = main.getItem() instanceof KnifeItem;
        boolean butterPair = (knife && off.is(ModItems.BUTTER_SLICE)) || (main.is(ModItems.BUTTER_SLICE) && off.getItem() instanceof KnifeItem);
        switch (stage) {
            case EMPTY:
                if (main.is(Items.BREAD)) return Action.PUT_BREAD;
                if (main.getItem() instanceof SandwichItem && main.has(com.simplesandwiches.registry.ModComponents.SANDWICH_CONTENTS)) return Action.PUT_SANDWICH;
                return Action.NONE;
            case LOAF:
                if (knife) return Action.OPEN_LOAF;
                return main.isEmpty() ? Action.TAKE_BREAD : Action.NONE;
            case OPEN:
                if (butterPair) {
                    return !board.buttered() && board.ingredients().isEmpty() ? Action.BUTTER : Action.NONE;
                }
                if (knife) return board.ingredients().isEmpty() ? Action.NONE : Action.POP_INGREDIENT;
                if (SandwichFormula.isIngredient(main)) {
                    return board.ingredients().size() < SandwichConfig.maxIngredients ? Action.ADD_INGREDIENT : Action.FULL;
                }
                return main.isEmpty() ? Action.CLOSE : Action.NONE;
            case CLOSED:
                if (knife) return Action.REOPEN;
                return main.isEmpty() ? Action.TAKE_SANDWICH : Action.NONE;
            default:
                return Action.NONE;
        }
    }

    /**
     * Items the board takes precedence for over their own use (eating): bread, sandwiches, knives,
     * butter and sandwich ingredients. Sneaking still bypasses the board (Vanilla rule).
     */
    public static boolean claims(ItemStack stack) {
        return stack.is(Items.BREAD) || stack.getItem() instanceof SandwichItem || stack.getItem() instanceof KnifeItem
                || stack.is(ModItems.BUTTER_SLICE) || SandwichFormula.isIngredient(stack);
    }

    /**
     * Runs one board click for {@code player}. Mutates only on the server. The client may see an
     * outdated board (or none yet); for board items it always consumes the click and leaves the
     * decision to the server, so it never falls through to {@code useItem} and starts eating
     * (owner bug 2026-10-05: bread was eaten instead of laid on the board).
     */
    public static InteractionResult interact(Level level, BlockPos pos, Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (!(level.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board)) {
            return level.isClientSide() && claims(main) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        Action action = plan(board, main, off);
        if (level.isClientSide()) return action != Action.NONE || claims(main) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        if (action == Action.NONE) return claims(main) ? InteractionResult.CONSUME : InteractionResult.PASS;
        ServerLevel server = (ServerLevel) level;
        switch (action) {
            case PUT_BREAD -> {
                main.consume(1, player);
                board.setStage(Stage.LOAF);
                sound(level, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, 1.0F);
            }
            case PUT_SANDWICH -> {
                board.loadSandwich(SandwichItem.contents(main));
                main.consume(1, player);
                sound(level, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, 1.0F);
            }
            case OPEN_LOAF -> {
                damageKnife(player, InteractionHand.MAIN_HAND);
                board.setStage(Stage.OPEN);
                sound(level, pos, SoundEvents.SHEEP_SHEAR, 1.4F);
                crumbs(server, pos, Items.BREAD.getDefaultInstance());
            }
            case TAKE_BREAD -> {
                board.setStage(Stage.EMPTY);
                give(player, new ItemStack(Items.BREAD));
            }
            case BUTTER -> {
                boolean knifeInMain = main.getItem() instanceof KnifeItem;
                (knifeInMain ? off : main).consume(1, player);
                damageKnife(player, knifeInMain ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
                board.setButtered(true);
                sound(level, pos, SoundEvents.HONEYCOMB_WAX_ON, 1.2F);
            }
            case ADD_INGREDIENT -> {
                board.addIngredient(main);
                main.consume(1, player);
                sound(level, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, 1.2F);
            }
            case FULL -> sound(level, pos, SoundEvents.BUNDLE_INSERT_FAIL, 1.0F);
            case POP_INGREDIENT -> {
                ItemStack top = board.popIngredient();
                damageKnife(player, InteractionHand.MAIN_HAND);
                give(player, top);
                sound(level, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, 1.2F);
            }
            case CLOSE -> {
                board.setStage(Stage.CLOSED);
                sound(level, pos, SoundEvents.BOOK_PUT, 1.0F);
            }
            case TAKE_SANDWICH -> {
                ItemStack sandwich = board.sandwich();
                board.setStage(Stage.EMPTY);
                give(player, sandwich);
                sound(level, pos, SoundEvents.ITEM_PICKUP, 1.0F);
            }
            case REOPEN -> {
                damageKnife(player, InteractionHand.MAIN_HAND);
                board.setStage(Stage.OPEN);
                sound(level, pos, SoundEvents.SHEEP_SHEAR, 1.4F);
            }
            default -> {}
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        InteractionResult result = interact(level, pos, player);
        return result == InteractionResult.PASS ? InteractionResult.TRY_WITH_EMPTY_HAND : result;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        return interact(level, pos, player);
    }

    private static void damageKnife(Player player, InteractionHand hand) {
        player.getItemInHand(hand).hurtAndBreak(1, player, hand);
    }

    private static void give(Player player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (player.getMainHandItem().isEmpty()) player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        else player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
    }

    private static void sound(Level level, BlockPos pos, SoundEvent sound, float pitch) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.8F, pitch);
    }

    static void crumbs(ServerLevel level, BlockPos pos, ItemStack stack) {
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.getItem()),
                pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 8, 0.15, 0.05, 0.15, 0.05);
    }
}
