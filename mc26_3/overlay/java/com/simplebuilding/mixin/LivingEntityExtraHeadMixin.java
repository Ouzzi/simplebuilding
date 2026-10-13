package com.simplebuilding.mixin;

import com.simplebuilding.util.ExtraHead;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MobVisibility;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A mob head in the extra head slot (owner N49, {@link ExtraHead}) lowers that mob's detection range like Vanilla's
 * helmet slot does ({@code MOB_VISIBILITY}, a zombie head halves it for zombies). Not twice: when the helmet slot
 * already carries the same effect for this mob, the extra head adds nothing. 26.3 only (26.2 has no MOB_VISIBILITY;
 * its twin is empty until the port run).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityExtraHeadMixin {
    @Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$extraHeadVisibility(ServerLevel level, @Nullable Entity targeting, CallbackInfoReturnable<Double> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack head = ExtraHead.get(self);
        if (targeting == null || head.isEmpty()) {
            return;
        }
        double factor = factor(head, targeting);
        if (factor < 1.0 && factor(self.getItemBySlot(EquipmentSlot.HEAD), targeting) >= 1.0) {
            cir.setReturnValue(cir.getReturnValueD() * factor);
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static double factor(ItemStack stack, Entity targeting) {
        MobVisibility visibility = stack.get(DataComponents.MOB_VISIBILITY);
        return visibility != null && visibility.targetingEntityTypes().contains(targeting.typeHolder()) ? visibility.visibility() : 1.0;
    }
}
