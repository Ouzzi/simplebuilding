package com.simplebuilding.modules.simpletools.mixin;

import com.simplebuilding.modules.simpletools.vault.RemovalContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A recorded death drop leaves the world for good (not unloading or changing dimension): the ledger decides its state. */
@Mixin(Entity.class)
public abstract class EntityRemovedMixin {
    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void simpletools$itemRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof ItemEntity item && reason.shouldDestroy() && self.getRemovalReason() == null && !self.level().isClientSide()) {
            RemovalContext.removed(item);
        }
    }
}
