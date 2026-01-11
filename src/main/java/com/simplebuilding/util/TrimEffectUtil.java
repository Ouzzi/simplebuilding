package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import com.simplebuilding.util.TrimBenefitUser;

public class TrimEffectUtil {

    public static float getTrimCount(LivingEntity entity, String patternPath) {
        // 1. Config-Check
        if (entity instanceof TrimBenefitUser user && !user.simplebuilding$areTrimBenefitsEnabled()) {
            return 0f;
        }

        float score = 0f;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack stack = entity.getEquippedStack(slot);

                var optionalTrim = stack.get(DataComponentTypes.TRIM);

                if (optionalTrim != null) {
                    // Pattern Check
                    String patternId = optionalTrim.pattern().value().assetId().getPath();
                    if (patternId.contains(patternPath)) {

                        // FIX: Wir nutzen den Registry-Key statt .assetName()
                        // Das funktioniert immer, egal wie die Methode in der Klasse heißt.
                        String materialName = optionalTrim.material().getKey()
                                .map(key -> key.getValue().getPath()) // Gibt z.B. "netherite" oder "diamond" zurück
                                .orElse("");

                        if (materialName.equals("netherite")) {
                            score += 1.5f; // Bonus
                        } else {
                            score += 1.0f; // Standard
                        }
                    }
                }
            }
        }
        return score;
    }

    public static float modifyDamage(LivingEntity entity, float amount, DamageSource source) {
        float multiplier = 1.0f;
        String activeTrim = "";

        // Sentry: Projektile
        if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            float count = getTrimCount(entity, "sentry");
            if (count > 0) {
                multiplier -= (count * 0.015f);
                activeTrim = "Sentry";
            }
        }

        // Vex: Magie
        if (source.getName().equals("magic") || source.getName().equals("indirectMagic") || (source.getSource() instanceof net.minecraft.entity.mob.VexEntity)) {
            float count = getTrimCount(entity, "vex");
            if (count > 0) {
                multiplier -= (count * 0.02f);
                activeTrim = "Vex";
            }
        }

        // Wild: Kakteen/Beeren
        if (source.getName().equals("cactus") || source.getName().equals("sweetBerryBush")) {
            float count = getTrimCount(entity, "wild");
            if (count > 0) {
                multiplier -= (count * 0.025f);
                activeTrim = "Wild";
            }
        }

        // Dune: Explosion
        if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
            float count = getTrimCount(entity, "dune");
            if (count > 0) {
                multiplier -= (count * 0.015f);
                activeTrim = "Dune";
            }
        }

        // Spire: Fallschaden
        if (source.isIn(DamageTypeTags.IS_FALL)) {
            float count = getTrimCount(entity, "spire");
            if (count > 0) {
                multiplier -= (count * 0.02f);
                activeTrim = "Spire";
            }
        }

        // Eye: Enderperle / Fall
        if (source.isIn(DamageTypeTags.IS_FALL) && (source.getName().contains("fall") || source.getName().contains("ender"))) {
            float count = getTrimCount(entity, "eye");
            if (count > 0) {
                multiplier -= (count * 0.015f);
                activeTrim = "Eye";
            }
        }

        // Sicherheitsbegrenzung: Max 80% Schaden reduzieren, nie negativ
        if (multiplier < 0.2f) multiplier = 0.2f;

        return Math.max(0, amount * multiplier);
    }
}