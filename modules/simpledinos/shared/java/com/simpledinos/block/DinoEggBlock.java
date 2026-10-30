package com.simpledinos.block;

import com.simpledinos.DinoKind;
import com.simpledinos.SimpleDinos;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A dinosaur egg (concept 3.3), one block per kind. Right click with an empty hand sits the player on it (see
 * {@link DinoEggBlockEntity}); {@link #PHASE} 0..11 shows the brooding in twelve steps, 12..14 the three crack steps of the
 * brooded egg that hatches later. No random tick and no ticker: scheduled ticks run only while somebody sits or the egg waits.
 */
public class DinoEggBlock extends BaseEntityBlock {
    public static final int BROOD_PHASES = 12;
    public static final int MAX_PHASE = 14;
    public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, MAX_PHASE);
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 12.0, 13.0);

    private final DinoKind kind;

    public DinoEggBlock(DinoKind kind, BlockBehaviour.Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(this.stateDefinition.any().setValue(PHASE, 0));
    }

    public DinoKind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PHASE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DinoEggBlockEntity(pos, state);
    }

    /** Only an empty hand sits on the egg; a block or item in hand places or uses itself as usual. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof DinoEggBlockEntity be)) return InteractionResult.PASS;
        if (be.brooded()) {
            player.sendOverlayMessage(Component.translatable("simpledinos.egg.brooded"));
            return InteractionResult.CONSUME;
        }
        if (player.isPassenger()) return InteractionResult.CONSUME;
        if (!be.sit(server, player)) {
            player.sendOverlayMessage(Component.translatable("simpledinos.egg.occupied"));
            return InteractionResult.CONSUME;
        }
        SimpleDinos.LOG.debug("{} sits on a {} egg at {}", player.getName().getString(), kind.id, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof DinoEggBlockEntity be) be.tickScheduled(level);
    }
}
