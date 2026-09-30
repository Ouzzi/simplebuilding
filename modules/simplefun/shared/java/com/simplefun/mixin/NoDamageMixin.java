package com.simplefun.mixin;

import com.simplefun.SimplefunCommon;
import com.simplefun.registry.ModEnchantments;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class NoDamageMixin {

  @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
  private float simplefun$modifyDamage(float amount, ServerLevel level, DamageSource source) {
    if (!SimplefunCommon.getConfig().fun.enableNoDamage) return amount;

    if (source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
        && source.getDirectEntity() instanceof Player player) {
      ItemStack stack = player.getMainHandItem();

      // Feathers deal no damage.
      if (stack.is(Items.FEATHER)) {
        fun$knockback(player, stack, level, source);
        return 0.0f;
      }

      // No-Damage enchantment.
      Optional<Holder.Reference<Enchantment>> noDamage =
          level
              .registryAccess()
              .lookupOrThrow(Registries.ENCHANTMENT)
              .get(ModEnchantments.NO_DAMAGE);
      if (noDamage.isPresent()
          && EnchantmentHelper.getItemEnchantmentLevel(noDamage.get(), stack) > 0) {
        fun$knockback(player, stack, level, source);
        return 0.0f;
      }
    }
    return amount;
  }

  private void fun$knockback(
      Player player, ItemStack stack, ServerLevel level, DamageSource source) {
    LivingEntity victim = (LivingEntity) (Object) this;
    float strength = .4f + EnchantmentHelper.modifyKnockback(level, stack, victim, source, 0) * .5f;
    double angle = player.getYRot() * Math.PI / 180;
    victim.knockback(Math.min(2.4, strength), Math.sin(angle), -Math.cos(angle), source, 0);
  }
}
