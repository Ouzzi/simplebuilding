package com.simplequestionary;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Questionary Table (N37: reminding of the enchanting table): an obsidian plinth, a wooden cabinet with gold pillars
 * and lapis gem windows, a gilded slab with four lapis gems and an open book on top (model only, no block entity).
 * Glyphs drift from the gems into the book like the enchanting table's from its bookshelves. Use opens the menu like a
 * crafting table; the item put in comes back when the menu closes.
 */
public class QuestionaryTableBlock extends HorizontalDirectionalBlock {
    public static final Component TITLE = Component.translatable("container.simplequestionary.questionary_table");
    private static final VoxelShape BASE = Shapes.or(Block.box(0, 0, 0, 16, 12, 16),
            Block.box(1, 12, 1, 3, 15, 3), Block.box(13, 12, 1, 15, 15, 3), Block.box(1, 12, 13, 3, 15, 15), Block.box(13, 12, 13, 15, 15, 15));
    private static final VoxelShape NORTH_SOUTH = Shapes.or(BASE, Block.box(2.5, 12, 3.5, 13.5, 14.5, 12.5));
    private static final VoxelShape EAST_WEST = Shapes.or(BASE, Block.box(3.5, 12, 2.5, 12.5, 14.5, 13.5));

    public QuestionaryTableBlock(Properties properties) {
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
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NORTH_SOUTH : EAST_WEST;
    }

    /** Enchanting-table glyphs from the four lapis gems (and now and then from further off) into the open book. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(2) != 0) return;
        double bookX = pos.getX() + 0.5, bookY = pos.getY() + 0.85, bookZ = pos.getZ() + 0.5;
        double dx, dy, dz;
        if (random.nextInt(4) == 0) {
            dx = random.nextDouble() * 2 - 1;
            dy = 0.4 + random.nextDouble() * 0.6;
            dz = random.nextDouble() * 2 - 1;
        } else {
            dx = (random.nextBoolean() ? 0.125 : 0.875) - 0.5;
            dz = (random.nextBoolean() ? 0.125 : 0.875) - 0.5;
            dy = 0.15 + random.nextDouble() * 0.35;
        }
        // The particle starts at book + offset and flies into the book (EnchantmentTableParticle).
        level.addParticle(ParticleTypes.ENCHANT, bookX, bookY, bookZ, dx, dy, dz);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!QuestionaryConfig.enabled) return InteractionResult.PASS;
        if (!level.isClientSide()) player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, player) -> new QuestionaryMenu(id, inventory, ContainerLevelAccess.create(level, pos)), TITLE);
    }
}
