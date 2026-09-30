package com.simplebuilding.mixin;

import com.simplebuilding.blocks.custom.AstralVaultBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityType.class)
public class AstralBlockEntityTypeMixin {
    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$vault(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == BlockEntityTypes.ENDER_CHEST && state.getBlock() instanceof AstralVaultBlock)
            cir.setReturnValue(true);
    }
}
