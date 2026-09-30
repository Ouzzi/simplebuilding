package com.simplequalityoflife.mixin;

import com.simplequalityoflife.Simplequalityoflife;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PowderSnowBlock.class)
public class PowderSnowWalkMixin {

    @Inject(method = "canEntityWalkOnPowderSnow", at = @At("RETURN"), cancellable = true)
    private static void simplequalityoflife$allowFrostWalker(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!Simplequalityoflife.configFor(entity.level()).frostWalkerWalkOnPowderSnow) return;
        if (cir.getReturnValue()) return;

        if (entity instanceof LivingEntity living) {
            Optional<Registry<Enchantment>> registry = living.level().registryAccess().lookup(Registries.ENCHANTMENT);
            if (registry.isEmpty()) return;
            Optional<Holder.Reference<Enchantment>> frostWalker = registry.get().get(Enchantments.FROST_WALKER);
            if (frostWalker.isEmpty()) return;
            int level = EnchantmentHelper.getEnchantmentLevel(frostWalker.get(), living);
            if (level > 0) {
                cir.setReturnValue(true);
            }
        }
    }
}
