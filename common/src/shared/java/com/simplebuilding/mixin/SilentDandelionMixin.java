package com.simplebuilding.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplebuilding.util.SilentDandelions;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Uses vanilla's ambient/hurt/death/step sound guards without changing DATA_SILENT. */
@Mixin(Entity.class)
public abstract class SilentDandelionMixin {
    @Unique private boolean simplebuilding$readingSavedSilence;

    @Inject(method = "isSilent", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$areaSilence(CallbackInfoReturnable<Boolean> cir) {
        if (!simplebuilding$readingSavedSilence && !cir.getReturnValueZ()
                && SilentDandelions.affects((Entity) (Object) this)) cir.setReturnValue(true);
    }

    // Exclude only our aura from vanilla's two save queries (also used for dimension transfers).
    // Calling the original operation preserves explicit flags and other mods' isSilent overrides.
    @WrapOperation(method = "saveWithoutId", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isSilent()Z"))
    private boolean simplebuilding$saveOnlyOriginalSilence(Entity entity, Operation<Boolean> original) {
        boolean previous = simplebuilding$readingSavedSilence;
        simplebuilding$readingSavedSilence = true;
        try {
            return original.call(entity);
        } finally {
            simplebuilding$readingSavedSilence = previous;
        }
    }
}
