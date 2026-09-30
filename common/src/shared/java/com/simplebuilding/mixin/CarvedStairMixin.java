package com.simplebuilding.mixin;

import com.simplebuilding.util.HammerCorners;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Carved stairs keep their removed quarters across neighbor changes and world reloads. */
@Mixin(StairBlock.class)
public abstract class CarvedStairMixin extends Block {
    protected CarvedStairMixin(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) { super(properties); }
    @Inject(method = "<init>", at = @At("RETURN"))
    private void simplebuilding$normalDefault(BlockState base, net.minecraft.world.level.block.state.BlockBehaviour.Properties properties, CallbackInfo ci) {
        if (McVersion.TRANSFORM_HINTS_AND_CORNERS) registerDefaultState(defaultBlockState().setValue(HammerCorners.CARVED, false));
    }
    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void simplebuilding$carvedProperty(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        if (McVersion.TRANSFORM_HINTS_AND_CORNERS) builder.add(HammerCorners.CARVED);
    }
    @Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$keepCarvedShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighbor, BlockState neighborState, RandomSource random,
            CallbackInfoReturnable<BlockState> cir) {
        if (state.hasProperty(HammerCorners.CARVED) && state.getValue(HammerCorners.CARVED)) {
            // Let Vanilla schedule water ticks, then preserve only our deliberate geometry.
            cir.setReturnValue(cir.getReturnValue().setValue(StairBlock.SHAPE, state.getValue(StairBlock.SHAPE)));
        }
    }
}
