package com.simplegadgets.mixin;

import com.simplegadgets.attractor.AttractorFilter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Marks a player's death drops ({@link AttractorFilter}, G3; was SimpleBuilding's LivingEntityMixin): another player's Attractor
 * leaves them alone. createItemStackToDrop, not drop: 26.3 calls it from Inventory#dropAll directly.
 */
@Mixin(LivingEntity.class)
public abstract class DeathDropMarkMixin {
    @Inject(method = "createItemStackToDrop", at = @At("RETURN"))
    private void simplegadgets$markDeathDrop(CallbackInfoReturnable<ItemEntity> cir) {
        AttractorFilter.markIfDeathDrop((LivingEntity) (Object) this, cir.getReturnValue());
    }
}
