package com.simpleores.mixin;

import com.simpleores.gear.EnderiteGear;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * N47-M3 (b-circ46): Enderite armor slows the damage of the void - one piece hurts every 20 ticks, two every 40, three every 60, four every 100
 * (otherwise every 10). Moved with the armor from SimpleBuilding's {@code LivingEntityMixin}, which skips it on 26.3.
 */
@Mixin(LivingEntity.class)
public abstract class EnderiteVoidDamageMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void simpleores$voidDamageInterval(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) && (Object) this instanceof Player player) {
            int pieces = 0;
            for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
                if (EnderiteGear.isEnderiteArmor(player.getItemBySlot(slot))) pieces++;
            }
            if (pieces > 0) {
                int interval = pieces == 1 ? 20 : pieces == 2 ? 40 : pieces == 3 ? 60 : 100;
                if (player.tickCount % interval != 0) cir.setReturnValue(false);
            }
        }
    }
}
