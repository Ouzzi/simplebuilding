package com.simplehammers.engine;

import com.simplehammers.item.SledgehammerItem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

/**
 * The anvil rules for sledgehammers (moved from SimpleBuilding's anvil mixin, REV24-F-18; {@code HammerAnvilMixin}
 * applies them). Hammer + hammer adds both durabilities plus 12 % and combines the enchantments like vanilla for one
 * level; hammer + its repair material mends one eleventh per piece, one level per piece (+1 for a new name, at most
 * 39). Results never raise the repair cost.
 */
public final class HammerAnvil {
    /** The anvil result: the stack, its level cost and how many items the right slot gives up. */
    public record Result(ItemStack stack, int cost, int materials) {
    }

    private HammerAnvil() {
    }

    /** The result for {@code left} + {@code right} with the typed {@code itemName}, or {@code null} for vanilla's own rules. */
    public static @Nullable Result result(ItemStack left, ItemStack right, @Nullable String itemName) {
        if (left.isEmpty() || !SledgehammerItem.isHammer(left)) return null;
        if (SledgehammerItem.isHammer(right)) return combine(left, right, itemName);
        if (left.isDamaged() && left.isValidRepairItem(right)) return repair(left, right, itemName);
        return null;
    }

    private static Result combine(ItemStack left, ItemStack right, @Nullable String itemName) {
        int leftDurability = left.getMaxDamage() - left.getDamageValue();
        int rightDurability = right.getMaxDamage() - right.getDamageValue();
        int bonus = (int) (left.getMaxDamage() * 0.12f);
        int newDamage = Math.max(0, left.getMaxDamage() - (leftDurability + rightDurability + bonus));

        ItemStack result = left.copy();
        result.setDamageValue(newDamage);
        result.set(DataComponents.REPAIR_COST, 0);

        ItemEnchantments leftEnchants = EnchantmentHelper.getEnchantmentsForCrafting(left);
        ItemEnchantments rightEnchants = EnchantmentHelper.getEnchantmentsForCrafting(right);
        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(leftEnchants);
        for (var entry : rightEnchants.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            int rightLevel = entry.getIntValue();
            int leftLevel = leftEnchants.getLevel(enchantment);
            // Wie Vanilla: unvertraegliche Paarungen (Behutsamkeit/Glueck, Schutz, ...) werden verworfen
            boolean compatible = true;
            for (Holder<Enchantment> existing : builder.keySet()) {
                if (!existing.equals(enchantment) && !Enchantment.areCompatible(existing, enchantment)) {
                    compatible = false;
                    break;
                }
            }
            if (!compatible) continue;
            int newLevel = leftLevel == rightLevel ? rightLevel + 1 : Math.max(leftLevel, rightLevel);
            builder.set(enchantment, Math.min(newLevel, enchantment.value().getMaxLevel()));
        }
        EnchantmentHelper.setEnchantments(result, builder.toImmutable());

        rename(left, result, itemName);
        return new Result(result, 1, 1);
    }

    private static Result repair(ItemStack left, ItemStack right, @Nullable String itemName) {
        ItemStack result = left.copy();
        // Teilen durch 11 statt 4
        int repairPerItem = Math.max(1, result.getMaxDamage() / 11);
        int damage = result.getDamageValue();
        int materialsUsed = 0;
        while (damage > 0 && materialsUsed < right.getCount()) {
            damage -= repairPerItem;
            materialsUsed++;
        }
        result.setDamageValue(Math.max(0, damage));

        int cost = materialsUsed;
        if (rename(left, result, itemName)) cost += 1;
        result.set(DataComponents.REPAIR_COST, 0);
        return new Result(result, Math.min(Math.max(cost, 1), 39), materialsUsed);
    }

    /** Vanilla's rename step; true if the name changed. */
    private static boolean rename(ItemStack original, ItemStack result, @Nullable String itemName) {
        if (itemName != null && !StringUtil.isBlank(itemName)) {
            if (!itemName.equals(original.getHoverName().getString())) {
                result.set(DataComponents.CUSTOM_NAME, Component.literal(itemName));
                return true;
            }
        } else if (original.has(DataComponents.CUSTOM_NAME)) {
            result.remove(DataComponents.CUSTOM_NAME);
            return true;
        }
        return false;
    }
}
