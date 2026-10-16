package com.simplecrucibles.mixin;

import com.simplecrucibles.fluid.CrucibleBucketItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nachtrag 60: a copper bucket lying in the world weathers like a carried one. */
@Mixin(ItemEntity.class)
public abstract class CopperBucketItemEntityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void simplecrucibles$weatherDroppedBucket(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level().isClientSide() || !CrucibleBucketItem.isCopper(self.getItem())) return;
        ItemStack weathered = CrucibleBucketItem.weatherPlaced(self.getItem(), self.getRandom());
        if (weathered != null) self.setItem(weathered);
    }
}
