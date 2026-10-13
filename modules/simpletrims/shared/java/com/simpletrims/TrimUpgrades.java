package com.simpletrims;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * The three trim upgrades at the smithing table, in one place: Glowing (glow ink, one level), Emitting (glowstone
 * dust, radiance up to 5) and Pulsating (echo shard, once). Read by the recipe type {@code simpletrims:trim_upgrade},
 * so Vanilla's smithing table, SimpleBuilding's auto smither and recipe displays all get the same result.
 */
public final class TrimUpgrades {
    private TrimUpgrades() {}

    /** Takes the upgrade: in {@code #minecraft:trimmable_armor} or equippable. */
    public static boolean isValidArmor(ItemStack armor) {
        return !armor.isEmpty() && (armor.is(ItemTags.TRIMMABLE_ARMOR) || armor.get(DataComponents.EQUIPPABLE) != null);
    }

    /**
     * The upgraded armor; {@link ItemStack#EMPTY} if the combination is ours but at its cap; {@code null} if it is not
     * ours (other template or material, no armor) or the module is switched off.
     */
    public static @Nullable ItemStack result(ItemStack template, ItemStack armor, ItemStack material) {
        if (!SimpleTrims.on() || !isValidArmor(armor)) return null;
        if (template.is(TrimsRegistry.GLOWING_TRIM_TEMPLATE) && material.is(Items.GLOW_INK_SAC)) {
            int level = TrimGlow.getGlowLevel(armor);
            if (level >= TrimGlow.MAX_GLOW_LEVEL) return ItemStack.EMPTY;
            ItemStack out = armor.copyWithCount(1);
            TrimGlow.setGlowLevel(out, level + 1);
            return out;
        }
        if (template.is(TrimsRegistry.EMITTING_TRIM_TEMPLATE) && material.is(Items.GLOWSTONE_DUST)) {
            if (TrimGlow.getEmissionLevel(armor) >= TrimGlow.MAX_EMISSION_LEVEL) return ItemStack.EMPTY;
            ItemStack out = armor.copyWithCount(1);
            TrimGlow.incrementEmissionLevel(out);
            return out;
        }
        if (template.is(TrimsRegistry.PULSATING_TRIM_TEMPLATE) && material.is(Items.ECHO_SHARD)) {
            if (TrimGlow.isPulsating(armor)) return ItemStack.EMPTY;
            ItemStack out = armor.copyWithCount(1);
            TrimGlow.setPulsating(out, true);
            return out;
        }
        return null;
    }
}
