package com.simplemachines.mixin;

import com.simplemachines.piston.DoublePistonBlock;
import com.simplemachines.piston.DoublePistonHeadBlock;
import com.simplemachines.piston.DoublePistons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * DW04C-6: while a Double Piston goes from stage 1 back to 0 its base is, like Vanilla's piston, the moving block (source piston).
 * Vanilla gives such a block the collision of the extended base plus its sliding head only for its own pistons and else the
 * Vanilla piston head alone; here the Double Piston gets its extended base and its own head (entities on the base keep standing).
 */
@Mixin(PistonMovingBlockEntity.class)
public abstract class DoublePistonMovingMixin {
    @Shadow private BlockState movedState;
    @Shadow private Direction direction;
    @Shadow private boolean extending;
    @Shadow private boolean isSourcePiston;
    @Shadow private float progress;

    private boolean simplemachines$doubleSource() {
        return isSourcePiston && !extending && movedState.getBlock() instanceof DoublePistonBlock;
    }

    private BlockState simplemachines$head() {
        return ((DoublePistonBlock) movedState.getBlock()).head().defaultBlockState()
                .setValue(DirectionalBlock.FACING, movedState.getValue(DirectionalBlock.FACING))
                .setValue(DoublePistonHeadBlock.SHORT, progress > 0.25F);
    }

    @Inject(method = "getCollisionRelatedBlockState", at = @At("HEAD"), cancellable = true)
    private void simplemachines$doubleHead(CallbackInfoReturnable<BlockState> cir) {
        if (simplemachines$doubleSource()) cir.setReturnValue(simplemachines$head());
    }

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void simplemachines$doubleShape(BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        if (!simplemachines$doubleSource()) return;
        float back = 1.0F - progress;
        VoxelShape head = simplemachines$head().getCollisionShape(level, pos)
                .move(direction.getStepX() * back, direction.getStepY() * back, direction.getStepZ() * back);
        cir.setReturnValue(Shapes.or(movedState.setValue(DoublePistons.EXT, 1).getCollisionShape(level, pos), head));
    }
}
