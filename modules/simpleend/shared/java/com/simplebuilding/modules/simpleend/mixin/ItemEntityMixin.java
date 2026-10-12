package com.simplebuilding.modules.simpleend.mixin;

import com.simplebuilding.modules.simpleend.EndRegistry;
import com.simplebuilding.modules.simpleend.SimpleEnd;
import com.simplebuilding.modules.simpleend.SimpleEndConfig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dropped End-wood items (tag simpleend:void_safe): twice the despawn time (age starts at -6000) and they cannot be lost
 * in the void - below the world they float back up to minY + 1 and hover there without gravity.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Shadow private int age;

    @Inject(method = "tick", at = @At("HEAD"))
    private void simpleend$voidSafe(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        Level level = self.level();
        if (level.isClientSide() || !SimpleEnd.on() || !self.getItem().is(EndRegistry.VOID_SAFE)) return;
        if (SimpleEndConfig.longerDespawn && age == 0 && self.tickCount <= 1) age = -6000;
        if (!SimpleEndConfig.voidSafeItems) return;
        double floor = level.getMinY() + 1;
        if (self.getY() < floor) {
            self.setNoGravity(true);
            self.setDeltaMovement(0, Math.min(0.4, floor - self.getY()), 0);
            self.needsSync = true;
        } else if (self.isNoGravity() && self.getY() < floor + 0.5 && self.getDeltaMovement().y > 0) {
            self.setDeltaMovement(Vec3.ZERO);
        }
    }
}
