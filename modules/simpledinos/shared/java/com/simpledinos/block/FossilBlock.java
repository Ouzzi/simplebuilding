package com.simpledinos.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The fossil block (concept 3.2): a very rare find that is brushed free with the Vanilla brush. Vanilla's BrushItem only talks to
 * {@code BrushableBlockEntity}, so this block watches its brushers itself: a right click with a brush starts a block tick every
 * game tick, which asks the nearby players whether one of them is brushing exactly this block. The progress and its rollback live
 * in {@link FossilBlockEntity}; the visible {@link #STAGE} follows the progress in ten steps.
 */
public class FossilBlock extends BaseEntityBlock {
    public static final int STAGES = 10;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, STAGES - 1);

    public FossilBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FossilBlockEntity(pos, state);
    }

    /** A brush in hand: keep watching this block, then let the brush item do its own (animation, use) work. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (stack.is(Items.BRUSH)) {
            if (level instanceof ServerLevel server) server.scheduleTick(pos, this, 1);
            return InteractionResult.PASS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof FossilBlockEntity be)) return;
        boolean brushing = brushing(level, pos);
        be.advance(level, level.getGameTime(), brushing);
        if (!level.getBlockState(pos).is(this)) return; // finished: the block is gone
        if (brushing) level.scheduleTick(pos, this, 1);
        else if (be.progress() > 0) level.scheduleTick(pos, this, 5);
    }

    /** True when a player in reach holds a brush in use and looks at {@code pos} (the same ray BrushItem uses). */
    public static boolean brushing(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        List<ServerPlayer> players = level.players();
        for (ServerPlayer p : players) {
            if (!p.isUsingItem() || !p.getUseItem().is(Items.BRUSH) || p.distanceToSqr(center) > 100.0) continue;
            HitResult hit = ProjectileUtil.getHitResultOnViewVector(p, EntitySelector.CAN_BE_PICKED, p.blockInteractionRange());
            if (hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK && block.getBlockPos().equals(pos)) return true;
        }
        return false;
    }
}
