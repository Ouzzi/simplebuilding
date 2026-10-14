package com.simplebuilding.mixin;

import com.simplebuilding.util.ExtraHead;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A head in the extra head slot (owner N55, {@link ExtraHead}) that is freeze immune wearable ({@code
 * #minecraft:freeze_immune_wearables}: the stray head, like Vanilla's leather armor) protects against powder snow like
 * the same head in the helmet slot; Vanilla's check only looks at the armor slots.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityExtraHeadFreezeMixin {
    @Inject(method = "canFreeze", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$extraHeadFreezeImmunity(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && ExtraHead.get((LivingEntity) (Object) this).is(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
            cir.setReturnValue(false);
        }
    }
}
