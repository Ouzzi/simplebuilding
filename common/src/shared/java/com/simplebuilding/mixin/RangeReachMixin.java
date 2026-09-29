package com.simplebuilding.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.util.RangeReach;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Range on the Attractor and the Gauge widens their own radius, not the player's block reach
 * (owner 2026-09-29). The enchantment's {@code minecraft:attributes} effect has no conditions, so
 * vanilla would add {@code block_interaction_range} for every item carrying it; both overloads of
 * {@code EnchantmentHelper#forEachModifier} get a consumer that drops that one modifier for the
 * items {@link RangeReach#keepsReach} names.
 */
@Mixin(EnchantmentHelper.class)
public abstract class RangeReachMixin {

    @ModifyVariable(method = "forEachModifier(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
            at = @At("HEAD"), argsOnly = true)
    private static BiConsumer<Holder<Attribute>, AttributeModifier> simplebuilding$noReachBySlot(
            BiConsumer<Holder<Attribute>, AttributeModifier> consumer, @Local(argsOnly = true) ItemStack stack) {
        return RangeReach.filter(stack, consumer);
    }

    @ModifyVariable(method = "forEachModifier(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V",
            at = @At("HEAD"), argsOnly = true)
    private static BiConsumer<Holder<Attribute>, AttributeModifier> simplebuilding$noReachByGroup(
            BiConsumer<Holder<Attribute>, AttributeModifier> consumer, @Local(argsOnly = true) ItemStack stack) {
        return RangeReach.filter(stack, consumer);
    }
}
