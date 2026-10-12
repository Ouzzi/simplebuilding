package com.simplebuilding.modules.simpleend.mixin;

import com.simplebuilding.modules.simpleend.client.EndTints;
import net.minecraft.client.color.block.BlockColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Grey-brown biome tint for End leaves, grass and bushes (same on every loader). */
@Mixin(BlockColors.class)
public abstract class BlockColorsMixin {
    @Inject(method = "createDefault", at = @At("RETURN"))
    private static void simpleend$tints(CallbackInfoReturnable<BlockColors> cir) {
        EndTints.register(cir.getReturnValue());
    }
}
