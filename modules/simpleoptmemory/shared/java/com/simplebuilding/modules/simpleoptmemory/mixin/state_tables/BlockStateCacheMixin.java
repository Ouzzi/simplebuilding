package com.simplebuilding.modules.simpleoptmemory.mixin.state_tables;

import com.simplebuilding.modules.simpleoptmemory.MemoryOpt;
import com.simplebuilding.modules.simpleoptmemory.StateTables;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** O4: the per-state face-support table (18 booleans, read-only) is shared between states with the same pattern. */
@Mixin(targets = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase$Cache")
abstract class BlockStateCacheMixin {
    @Shadow @Final @Mutable private boolean[] faceSturdy;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void simpleoptmemory$share(BlockState state, CallbackInfo ci) {
        if (MemoryOpt.live(MemoryOpt.STATE_TABLES)) faceSturdy = StateTables.share(faceSturdy);
    }
}
