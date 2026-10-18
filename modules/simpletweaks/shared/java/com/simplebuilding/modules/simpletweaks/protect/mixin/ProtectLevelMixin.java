package com.simplebuilding.modules.simpletweaks.protect.mixin;

import com.simplebuilding.modules.simpletweaks.protect.AntiXray;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Anti-X-Ray: every block change (mining, placing, explosions, pistons, fluids) re-sends concealed neighbours. */
@Mixin(ServerLevel.class)
public abstract class ProtectLevelMixin {
    @Inject(method = "sendBlockUpdated", at = @At("TAIL"))
    private void simpletweaks$reveal(BlockPos pos, BlockState old, BlockState current, int flags, CallbackInfo ci) {
        if (old != current) AntiXray.revealAround((ServerLevel) (Object) this, pos);
    }
}
