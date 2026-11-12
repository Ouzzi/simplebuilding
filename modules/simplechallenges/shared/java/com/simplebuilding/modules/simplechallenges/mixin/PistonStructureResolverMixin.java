package com.simplebuilding.modules.simplechallenges.mixin;

import com.simplebuilding.modules.simplechallenges.core.Enforcement;
import com.simplebuilding.modules.simplechallenges.core.Gate;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Unlock borders: a piston that would take, put or destroy a block in a locked cell does not move (like a block it cannot push).
 * Otherwise the refused {@code Level.setBlock} leaves half of the move behind - a pulled block is copied (the original stays in the locked
 * cell), a pushed one vanishes. Asked on both sides, so the client does not animate a move the server refuses. Inert without a chosen challenge ({@code Gate}).
 */
@Mixin(PistonStructureResolver.class)
public abstract class PistonStructureResolverMixin {
    @Shadow @Final private Level level;
    @Shadow @Final private boolean extending;
    @Shadow @Final private BlockPos startPos;
    @Shadow @Final private Direction pushDirection;
    @Shadow @Final private List<BlockPos> toPush;
    @Shadow @Final private List<BlockPos> toDestroy;

    @Inject(method = "resolve", at = @At("RETURN"), cancellable = true)
    private void simplechallenges$keepLockedCells(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || Gate.grid(level) == null) return;
        boolean lockedMove = extending && Enforcement.locked(level, startPos); // the piston head stands there
        for (BlockPos p : toPush) {
            if (lockedMove) break;
            lockedMove = Enforcement.locked(level, p) || Enforcement.locked(level, p.relative(pushDirection));
        }
        for (BlockPos p : toDestroy) {
            if (lockedMove) break;
            lockedMove = Enforcement.locked(level, p);
        }
        if (lockedMove) cir.setReturnValue(false);
    }
}
