package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanish: mobs do not see (target) vanished players. */
@Mixin(LivingEntity.class)
public abstract class AdminLivingEntityMixin {
    @Inject(method = "canBeSeenByAnyone", at = @At("RETURN"), cancellable = true)
    private void simpletweaks$seen(CallbackInfoReturnable<Boolean> cir) { if (cir.getReturnValueZ() && Vanish.vanished((LivingEntity) (Object) this)) cir.setReturnValue(false); }
}
