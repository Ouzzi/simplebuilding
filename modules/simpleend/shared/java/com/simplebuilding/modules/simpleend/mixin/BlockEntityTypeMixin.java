package com.simplebuilding.modules.simpleend.mixin;

import com.simplebuilding.modules.simpleend.EndWood;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The End-wood signs, hanging signs and shelves use the Vanilla block entities (same on every loader). */
@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeMixin {
    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void simpleend$endSigns(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if (self != BlockEntityTypes.SIGN && self != BlockEntityTypes.HANGING_SIGN && self != BlockEntityTypes.SHELF) return;
        for (EndWood w : EndWood.ALL) {
            if (w.sign == null) continue;
            if (self == BlockEntityTypes.SIGN && (state.is(w.sign) || state.is(w.wallSign))) cir.setReturnValue(true);
            if (self == BlockEntityTypes.HANGING_SIGN && (state.is(w.hangingSign) || state.is(w.wallHangingSign))) cir.setReturnValue(true);
            if (self == BlockEntityTypes.SHELF && w.shelf != null && state.is(w.shelf)) cir.setReturnValue(true);
        }
    }
}
