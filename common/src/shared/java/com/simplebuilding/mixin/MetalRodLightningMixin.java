package com.simplebuilding.mixin;

import com.simplebuilding.blocks.custom.MetalRodBlock;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Metallstaebe: findet Vanilla keinen Kupfer-Blitzableiter, zieht der naechste Eisen- oder Goldstab den Blitz an,
 * dessen eigene Reichweite (32 bzw. 64 Bloecke) reicht ({@link MetalRodBlock#find}).
 */
@Mixin(ServerLevel.class)
public abstract class MetalRodLightningMixin {
    @Inject(method = "findLightningRod", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$metalRod(BlockPos center, CallbackInfoReturnable<Optional<BlockPos>> cir) {
        if (cir.getReturnValue().isEmpty()) {
            Optional<BlockPos> metal = MetalRodBlock.find((ServerLevel) (Object) this, center);
            if (metal.isPresent()) {
                cir.setReturnValue(metal);
            }
        }
    }
}
