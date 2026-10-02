package com.simplebuilding.mixin;

import com.simplebuilding.blocks.custom.IronRodBlock;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Eisenstab: findet Vanilla keinen Kupfer-Blitzableiter, zieht der naechste Eisenstab in 32 Bloecken den Blitz an. */
@Mixin(ServerLevel.class)
public abstract class IronRodLightningMixin {
    @Inject(method = "findLightningRod", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$ironRod(BlockPos center, CallbackInfoReturnable<Optional<BlockPos>> cir) {
        if (cir.getReturnValue().isEmpty()) {
            Optional<BlockPos> iron = IronRodBlock.find((ServerLevel) (Object) this, center);
            if (iron.isPresent()) {
                cir.setReturnValue(iron);
            }
        }
    }
}
