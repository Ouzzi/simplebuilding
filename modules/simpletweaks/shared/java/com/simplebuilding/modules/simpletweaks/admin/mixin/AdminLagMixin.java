package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.simplebuilding.modules.simpletweaks.admin.lag.LagFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lag finder (hooks optional on every loader; they only act while /lag samples): block entity ticks, redstone changes. */
@Mixin(Level.class)
public abstract class AdminLagMixin {
    @WrapOperation(method = "tickBlockEntities", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void simpletweaks$time(TickingBlockEntity ticker, Operation<Void> original) {
        if (!LagFinder.sampling) { original.call(ticker); return; }
        long t = System.nanoTime();
        original.call(ticker);
        LagFinder.blockEntity((Level) (Object) this, ticker.getPos(), ticker.getType(), System.nanoTime() - t);
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", require = 0, at = @At("RETURN"))
    private void simpletweaks$changed(BlockPos pos, BlockState state, int flags, int limit, CallbackInfoReturnable<Boolean> cir) {
        if (LagFinder.sampling && cir.getReturnValueZ() && !((Level) (Object) this).isClientSide()) LagFinder.changed((Level) (Object) this, pos, state);
    }
}
