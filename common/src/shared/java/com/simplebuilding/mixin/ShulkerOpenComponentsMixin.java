package com.simplebuilding.mixin;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.util.ShulkerLids;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shulker state (owner N17): the open-standing flag of a shulker box goes from the item to the placed box and back
 * (pick block, creative drops). Every container block entity calls up to these base methods.
 */
@Mixin(BlockEntity.class)
public abstract class ShulkerOpenComponentsMixin {

    @Inject(method = "applyImplicitComponents", at = @At("HEAD"))
    private void simplebuilding$openFromItem(DataComponentGetter components, CallbackInfo ci) {
        if (this instanceof ShulkerLids.Kept kept) {
            kept.simplebuilding$setKeptOpen(Boolean.TRUE.equals(components.get(ModDataComponentTypes.SHULKER_OPEN)));
        }
    }

    @Inject(method = "collectImplicitComponents", at = @At("HEAD"))
    private void simplebuilding$openToItem(DataComponentMap.Builder components, CallbackInfo ci) {
        if (this instanceof ShulkerLids.Kept kept && kept.simplebuilding$keptOpen()) {
            components.set(ModDataComponentTypes.SHULKER_OPEN, true);
        }
    }
}
