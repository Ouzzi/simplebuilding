package com.simpleblocks.mixin;

import com.simpleblocks.snow.SnowCaps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** SNOW32-FALL: falling snow also puts one snow cap on fences, walls, stairs and slabs ({@link SnowCaps#tick}); Vanilla's tick runs unchanged. */
@Mixin(ServerLevel.class)
public abstract class SnowCapsMixin {
    @Inject(method = "tickPrecipitation", at = @At("HEAD"))
    private void simpleblocks$snowCaps(BlockPos pos, CallbackInfo ci) {
        SnowCaps.tick((ServerLevel) (Object) this, pos);
    }
}
