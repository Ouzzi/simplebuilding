package com.simplebuilding.util;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.component.DataComponentTypes;

public class TrimEffectUtil {

    public static int getTrimCount(LivingEntity entity, String patternPath) {
        int count = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack stack = entity.getEquippedStack(slot);
                var optionalTrim = stack.get(DataComponentTypes.TRIM);
                if (optionalTrim != null) {
                    String id = optionalTrim.pattern().value().assetId().getPath();
                    if (id.contains(patternPath)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static float modifyDamage(LivingEntity entity, float amount, DamageSource source) {
        float multiplier = 1.0f;

        if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            int count = getTrimCount(entity, "sentry");
            if (count > 0) multiplier -= (count * 0.015f);
        }

        if (source.getName().equals("magic") || source.getName().equals("indirectMagic") || (source.getSource() instanceof net.minecraft.entity.mob.VexEntity)) {
            int count = getTrimCount(entity, "vex");
            if (count > 0) multiplier -= (count * 0.02f);
        }

        if (source.getName().equals("cactus") || source.getName().equals("sweetBerryBush")) {
            int count = getTrimCount(entity, "wild");
            if (count > 0) multiplier -= (count * 0.025f);
        }

        if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
            int count = getTrimCount(entity, "dune");
            if (count > 0) multiplier -= (count * 0.015f);
        }

        if (source.isIn(DamageTypeTags.IS_FALL)) {
            int count = getTrimCount(entity, "spire");
            if (count > 0) multiplier -= (count * 0.02f);
        }

        if (source.isIn(DamageTypeTags.IS_FALL) && (source.getName().contains("fall") || source.getName().contains("ender"))) {
            int count = getTrimCount(entity, "eye");
            if (count > 0) multiplier -= (count * 0.015f);
        }

        return Math.max(0, amount * multiplier);
    }
}