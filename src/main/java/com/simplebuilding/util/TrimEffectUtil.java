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

                        // Netherite Bonus (1.5x)
                        String materialName = optionalTrim.material().getKey()
                                .map(key -> key.getValue().getPath())
                                .orElse("");

                        if (materialName.equals("netherite")) {
                            score += 1.5f;
                        } else {
                            score += 1.0f;
                        }
                    }
                }
            }
        }
        return score;
    }

    // NEU: Methode um Material-Teile zu zählen (unabhängig vom Pattern)
    public static int getMaterialCount(LivingEntity entity, String materialPath) {
        if (entity instanceof TrimBenefitUser user && !user.simplebuilding$areTrimBenefitsEnabled()) {
            return 0;
        }

        int count = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack stack = entity.getEquippedStack(slot);
                var optionalTrim = stack.get(DataComponentTypes.TRIM);

                if (optionalTrim != null) {
                    // Wir prüfen hier nur das Material
                    String materialId = optionalTrim.material().getKey().map(key -> key.getValue().getPath()).orElse("");
                    if (materialId.contains(materialPath)) {
                        count++;
                    }
                }
            }
        }
        return count;
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

        // --- NEU: WARD (Warden / Sonic Boom Schutz) ---
        // Funktioniert gegen Sonic Boom und Warden-Angriffe
        if (source.getName().equals("sonic_boom") || (source.getAttacker() instanceof net.minecraft.entity.mob.WardenEntity)) {
             float count = getTrimCount(entity, "ward");
             if (count > 0) {
                 // 5% Schutz pro Teil -> mit Netherite 7.5%
                 multiplier -= (count * 0.05f);
                 activeTrim = "Ward";
             }
        }

        // 1. DIAMANT: Harte Schale
        // Diamant Trims geben flache Schadensreduktion gegen ALLES (außer Void/Starve).
        // 2.5% pro Teil -> 10% bei vollem Set.
        if (!source.isIn(DamageTypeTags.BYPASSES_ARMOR)) {
            int diamondCount = getMaterialCount(entity, "diamond");
            if (diamondCount > 0) {
                multiplier -= (diamondCount * 0.025f);
                // Logging optional
            }
        }

        // 2. GOLD: Magische Dämpfung
        // Gold ist weich, aber magisch leitfähig. Reduziert Magieschaden deutlich.
        // 5% pro Teil -> 20% bei vollem Set.
        if (source.getName().equals("magic") || source.getName().equals("indirectMagic") || source.getName().equals("dragonBreath")) {
            int goldCount = getMaterialCount(entity, "gold");
            if (goldCount > 0) {
                multiplier -= (goldCount * 0.05f);
            }
        }

        // 3. IRON (optional): Wucht-Resistenz
        // Gut gegen Explosionen oder Mob-Angriffe (nicht Magie).
        // Hier als Beispiel, falls gewünscht.
        if (!source.isIn(DamageTypeTags.IS_EXPLOSION) && !source.isIn(DamageTypeTags.BYPASSES_ARMOR)) {
            int ironCount = getMaterialCount(entity, "iron");
            if (ironCount > 0) {
                multiplier -= (ironCount * 0.03f);
            }
        }

        // Sicherheitsbegrenzung
        if (multiplier < 0.2f) multiplier = 0.2f;

        if (multiplier < 1.0f) {
            // Optionales Logging
             // float reduction = (1.0f - multiplier) * 100;
             // Simplebuilding.LOGGER.info("Trim Bonus Active: {}! Reduced by {}%", activeTrim, reduction);
        }

        return Math.max(0, amount * multiplier);
    }
}