package com.simplelib.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No experience repair (Mending and every enchantment with {@code repair_with_xp}) on items in the tag
 * {@code simplelib:xp_repair_incompatible}: a charged gadget (Simple Gadgets' Transposer) takes Unbreaking but no Mending - its
 * durability is a charge refilled with a material, not with experience. Mending and Unbreaking share
 * {@code #minecraft:enchantable/durability}, so the two questions of the enchantment itself are answered: {@code canEnchant}
 * (anvil, {@code enchant_randomly}, {@code /enchant}) and {@code isSupportedItem} (enchanting table). Same rule as SimpleBuilding's
 * {@code EnchantmentMixin} for its own tag; both may apply.
 */
@Mixin(Enchantment.class)
public abstract class XpRepairRefusalMixin {
    @Unique
    private static final TagKey<Item> SIMPLELIB_XP_REPAIR_INCOMPATIBLE = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath("simplelib", "xp_repair_incompatible"));

    @Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
    private void simplelib$noXpRepairOnCanEnchant(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (simplelib$refuses(stack)) cir.setReturnValue(false);
    }

    @Inject(method = "isSupportedItem", at = @At("HEAD"), cancellable = true)
    private void simplelib$noXpRepairOnSupportedItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (simplelib$refuses(stack)) cir.setReturnValue(false);
    }

    @Unique
    private boolean simplelib$refuses(ItemStack stack) {
        return stack.is(SIMPLELIB_XP_REPAIR_INCOMPATIBLE)
                && ((Enchantment) (Object) this).effects().has(EnchantmentEffectComponents.REPAIR_WITH_XP);
    }
}
