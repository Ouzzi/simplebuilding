package com.simplemobheads.mixin;

import com.simplemobheads.MobHeadType;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The heads use Vanilla's {@code SkullBlockEntity} (note block sound, renderer, wearing), whose type {@code minecraft:skull}
 * only knows Vanilla's heads as valid blocks; without this {@code BlockEntity#validateBlockState} refuses the block. A mixin
 * instead of loader APIs so every loader goes the same way (as SimpleBuilding and Simple Fun did for their heads).
 */
@Mixin(BlockEntityType.class)
public abstract class SkullBlockEntityTypeMixin {
    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void simplemobheads$acceptMobHeads(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == BlockEntityTypes.SKULL && state.getBlock() instanceof AbstractSkullBlock skull && skull.getType() instanceof MobHeadType) {
            cir.setReturnValue(true);
        }
    }
}
