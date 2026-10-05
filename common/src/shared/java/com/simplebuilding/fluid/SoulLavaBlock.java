package com.simplebuilding.fluid;

import com.simplebuilding.advancement.ModTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

/**
 * The soul lava block (owner round 2 answers 26/27 + round 3 answer 54):
 * <ul>
 *   <li>not replaceable - neither source nor flowing block - by players, building tools, other fluids or
 *       pistons (its properties have no {@code replaceable()}, push reaction BLOCK); only creative players may
 *       build into it. A source goes away only by scooping it with a bucket ({@link #pickupBlock});</li>
 *   <li>water touching it does not harden the soul lava: the water side turns instead - a water source into a
 *       quartz block, flowing water into blackstone.</li>
 * </ul>
 */
public class SoulLavaBlock extends LiquidBlock {
    private final FlowingFluid soulFluid;

    public SoulLavaBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
        this.soulFluid = fluid;
    }

    /** Only creative players may build into soul lava (owner 26). */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        Player player = context.getPlayer();
        return player != null && player.getAbilities().instabuild;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        convertWater(level, pos);
        level.scheduleTick(pos, state.getFluidState().getType(), soulFluid.getTickDelay(level));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        convertWater(level, pos);
        level.scheduleTick(pos, state.getFluidState().getType(), soulFluid.getTickDelay(level));
    }

    /** Turns every touching water block: source -> quartz block, flowing -> blackstone. True when one turned. */
    public static boolean convertWater(LevelAccessor level, BlockPos pos) {
        boolean any = false;
        for (Direction direction : Direction.values()) {
            BlockPos water = pos.relative(direction);
            BlockState state = level.getBlockState(water);
            if (!state.is(Blocks.WATER)) continue;
            FluidState fluid = state.getFluidState();
            level.setBlock(water, (fluid.isSource() ? Blocks.QUARTZ_BLOCK : Blocks.BLACKSTONE).defaultBlockState(), Block.UPDATE_ALL);
            level.levelEvent(1501, water, 0);
            any = true;
        }
        return any;
    }

    @Override
    public ItemStack pickupBlock(@Nullable LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state) {
        ItemStack bucket = super.pickupBlock(user, level, pos, state);
        if (!bucket.isEmpty() && user instanceof Player player) ModTriggers.feature(player, ModTriggers.SOUL_LAVA_SCOOPED);
        return bucket;
    }
}
