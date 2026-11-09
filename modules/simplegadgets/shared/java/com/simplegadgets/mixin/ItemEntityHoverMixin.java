package com.simplegadgets.mixin;

import com.simplegadgets.attractor.PlacedAttractor;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Besitzer N34: an item a placed Attractor let hover falls again as soon as none holds it (G3; was SimpleBuilding's ItemEntityMixin). */
@Mixin(ItemEntity.class)
public abstract class ItemEntityHoverMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void simplegadgets$releaseAttractorHover(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.isNoGravity() && !self.level().isClientSide()) {
            PlacedAttractor.releaseIfAbandoned(self);
        }
    }
}
