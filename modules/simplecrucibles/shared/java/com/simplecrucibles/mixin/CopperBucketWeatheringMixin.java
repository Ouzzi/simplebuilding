package com.simplecrucibles.mixin;

import com.simplecrucibles.fluid.CrucibleBucketItem;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nachtrag 60: a copper bucket shown in an item frame weathers like a carried one (server side). */
@Mixin(BlockAttachedEntity.class)
public abstract class CopperBucketWeatheringMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void simplecrucibles$weatherFramedBucket(CallbackInfo ci) {
        if (!((Object) this instanceof ItemFrame frame) || frame.level().isClientSide() || !CrucibleBucketItem.isCopper(frame.getItem())) return;
        ItemStack weathered = CrucibleBucketItem.weatherPlaced(frame.getItem(), frame.getRandom());
        if (weathered != null) frame.setItem(weathered);
    }
}
