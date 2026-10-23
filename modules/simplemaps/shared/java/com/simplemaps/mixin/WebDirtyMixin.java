package com.simplemaps.mixin;

import com.simplemaps.web.WebMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Web map stage 3: every block change (placing, mining, explosions, pistons, fluids) marks its tile for the next export. */
@Mixin(ServerLevel.class)
public abstract class WebDirtyMixin {
    @Inject(method = "sendBlockUpdated", at = @At("TAIL"))
    private void simplemaps$webDirty(BlockPos pos, BlockState old, BlockState current, int flags, CallbackInfo ci) {
        if (old != current) WebMap.blockChanged((ServerLevel) (Object) this, pos);
    }
}
