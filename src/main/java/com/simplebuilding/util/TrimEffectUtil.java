package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity; // Wichtig

public class TrimEffectUtil {

    // --- HELPER: Multiplikator Logik ---

    /**
     * Holt den dynamischen Multiplikator basierend auf XP und Survival-Stats.
     * Wenn der Spieler kein ServerPlayer ist (z.B. Client-Side Berechnung für Tooltips),
     * geben wir 1.0 zurück oder einen Schätzwert.
     */
    public static float getGlobalMultiplier(LivingEntity entity) {
        if (entity instanceof ServerPlayerEntity serverPlayer) {
            return (float) TrimMultiplierLogic.getMultiplier(serverPlayer);
        }
        // Fallback für Client/Mobs: 20% Effektivität (damit man im Tooltip sieht, dass was passiert)
        // Oder 1.0f, wenn du willst, dass Mobs die volle Power haben.
        return 0.2f;
    }

    /**
     * Zählt, wie viele Teile den Trim haben.
     * Netherite als Material gibt 1.5 Punkte, andere 1.0.
     */
    public static float getTrimCount(LivingEntity entity, String patternPath) {
        if (entity instanceof TrimBenefitUser user && !user.simplebuilding$areTrimBenefitsEnabled()) {
            return 0f;
        }

        float score = 0f;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack stack = entity.getEquippedStack(slot);
                var optionalTrim = stack.get(DataComponentTypes.TRIM);

                if (optionalTrim != null) {
                    String patternId = optionalTrim.pattern().value().assetId().getPath();
                    if (patternId.contains(patternPath)) {
                        String materialName = optionalTrim.material().getKey()
                                .map(key -> key.getValue().getPath()).orElse("");
                        // Netherite Material Upgrade Logic innerhalb des Trims
                        score += materialName.contains("netherite") ? 1.5f : 1.0f;
                    }
                }
            }
        }
        return score;
    }

    /**
     * Zählt, wie viele Teile ein bestimmtes Material haben (unabhängig vom Pattern).
     */
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
                    String materialId = optionalTrim.material().getKey().map(key -> key.getValue().getPath()).orElse("");
                    if (materialId.contains(materialPath)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    // --- HAUPTLOGIK: Schaden Modifizieren ---

    public static float modifyDamage(LivingEntity entity, float amount, DamageSource source) {
        if (amount <= 0) return 0;

        float multiplier = 1.0f;

        // Den globalen Fortschritts-Multiplikator holen (z.B. 0.1 bis 2.0)
        float progressMult = getGlobalMultiplier(entity);

        // --- A. TRIM PATTERNS (Themen-Boni) ---

        // 1. Sentry (Pillager Outpost) -> Projektilschutz
        if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            // Basis: 2.5% pro Teil. Mit ProgressMult (max): sehr stark.
            multiplier -= calculateReduction(entity, "sentry", 0.025f, progressMult);
        }

        // 2. Vex (Mansion) -> Magie Schutz
        if (source.isOf(DamageTypes.MAGIC) || source.isOf(DamageTypes.INDIRECT_MAGIC) || (source.getSource() instanceof net.minecraft.entity.mob.VexEntity)) {
            multiplier -= calculateReduction(entity, "vex", 0.03f, progressMult);
        }

        // 3. Wild (Jungle) -> Fallen/Umwelt (Kaktus, Beeren, Fallschaden leicht)
        if (source.getName().equals("cactus") || source.getName().equals("sweetBerryBush") || source.getName().equals("stalagmite")) {
            multiplier -= calculateReduction(entity, "wild", 0.05f, progressMult);
        }

        // 4. Dune (Desert) -> Explosionsschutz
        if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
            multiplier -= calculateReduction(entity, "dune", 0.03f, progressMult);
        }

        // 5. Coast (Shipwreck) -> Ertrinken / Wasser-Schaden
        if (source.isOf(DamageTypes.DROWN)) {
            multiplier -= calculateReduction(entity, "coast", 0.05f, progressMult);
        }

        // 6. Ward (Ancient City) -> Generic Tanking (Starke allgemeine Reduktion, aber selten)
        // Reduziert ALLEN Schaden leicht
        multiplier -= calculateReduction(entity, "ward", 0.015f, progressMult);

        // 7. Silence (Ancient City) -> Sonic Boom (Warden)
        if (source.getName().equals("sonic_boom")) {
            multiplier -= calculateReduction(entity, "silence", 0.10f, progressMult);
        }

        // 8. Snout (Bastion) -> Knockback Res (Indirekt) oder Lava/Fire Schaden
        // Snout ist eher für Offensive/KB gedacht, aber hier defensiv: Brandschaden
        if (source.isIn(DamageTypeTags.IS_FIRE)) {
            multiplier -= calculateReduction(entity, "snout", 0.02f, progressMult);
        }

        // 9. Rib (Fortress) -> Wither Effekt Schaden
        if (source.isOf(DamageTypes.WITHER)) {
            multiplier -= calculateReduction(entity, "rib", 0.05f, progressMult);
        }

        // 10. Eye (Stronghold) -> Dragon Breath / Void (Void geht meist durch, aber wir versuchen es)
        if (source.isOf(DamageTypes.DRAGON_BREATH)) {
            multiplier -= calculateReduction(entity, "eye", 0.05f, progressMult);
        }

        // 11. Spire (End City) -> Fallschaden / Levitation Crash
        if (source.isIn(DamageTypeTags.IS_FALL)) {
            multiplier -= calculateReduction(entity, "spire", 0.04f, progressMult);
        }

        // 12. Flow (Trial Chambers) -> Wind Charge
        if (source.getSource() != null && source.getSource().getType().toString().contains("wind_charge")) {
            multiplier -= calculateReduction(entity, "flow", 0.05f, progressMult);
        }

        // 13. Bolt (Trial Chambers) -> Blitzschaden
        if (source.isOf(DamageTypes.LIGHTNING_BOLT)) {
            multiplier -= calculateReduction(entity, "bolt", 0.10f, progressMult);
        }

        // --- B. TRIM MATERIALS (Material-Boni) ---
        // Diese sind meistens flache Boni, die aber auch mit dem Progress skalieren sollen.

        // 1. Diamant: Harte Schale (Allgemein Physisch)
        if (!source.isIn(DamageTypeTags.BYPASSES_ARMOR)) {
            int diamondParts = getMaterialCount(entity, "diamond");
            // Basis 1.5% pro Teil * Multiplikator
            if (diamondParts > 0) multiplier -= (diamondParts * 0.015f * progressMult);
        }

        // 2. Gold / Lapis: Magie Dämpfung
        if (source.isOf(DamageTypes.MAGIC) || source.isOf(DamageTypes.INDIRECT_MAGIC)) {
            int goldParts = getMaterialCount(entity, "gold");
            int lapisParts = getMaterialCount(entity, "lapis");
            // Gold ist stärker gegen Magie (3% Basis), Lapis (2% Basis)
            if (goldParts > 0) multiplier -= (goldParts * 0.03f * progressMult);
            if (lapisParts > 0) multiplier -= (lapisParts * 0.02f * progressMult);
        }

        // 3. Eisen: Projektile / Wucht
        if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            int ironParts = getMaterialCount(entity, "iron");
            if (ironParts > 0) multiplier -= (ironParts * 0.02f * progressMult);
        }

        // 4. Emerald: Illager Schutz
        if (source.getAttacker() instanceof IllagerEntity) {
            int emeraldParts = getMaterialCount(entity, "emerald");
            if (emeraldParts > 0) multiplier -= (emeraldParts * 0.04f * progressMult);
        }

        // 5. Netherite: Boss Schutz (Wither, Warden, Dragon)
        if (source.isIn(DamageTypeTags.BYPASSES_ENCHANTMENTS) || source.getAttacker() instanceof net.minecraft.entity.boss.WitherEntity) {
            int netheriteParts = getMaterialCount(entity, "netherite");
            if (netheriteParts > 0) multiplier -= (netheriteParts * 0.02f * progressMult);
        }

        // 6. Quartz: Feuer
        if(source.isIn(DamageTypeTags.IS_FIRE)) {
            int quartzParts = getMaterialCount(entity, "quartz");
            if(quartzParts > 0) multiplier -= (quartzParts * 0.025f * progressMult);
        }

        // Cap: Niemals mehr als 80% Schadensreduktion durch Trims allein (Balance)
        if (multiplier < 0.2f) multiplier = 0.2f;

        return amount * multiplier;
    }

    /**
     * Berechnet die Reduktion für einen Trim-Typ unter Einbeziehung des globalen Multiplikators.
     * @param baseReductionProzent z.B. 0.025f für 2.5%
     */
    private static float calculateReduction(LivingEntity entity, String pattern, float baseReductionProzent, float progressMult) {
        float count = getTrimCount(entity, pattern); // Enthält bereits 1.5x für Netherite
        if (count <= 0) return 0f;

        // Formel: Teile * BasisWert * FortschrittsMultiplikator
        return count * baseReductionProzent * progressMult;
    }

    // --- UTIL GETTER FÜR ANDERE MIXINS ---

    // Wird in LivingEntityMixin für Movement Speed genutzt
    public static float getSwimSpeedMultiplier(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float tideCount = getTrimCount(entity, "tide"); // Ocean Monument Trim

        if (tideCount <= 0) return 1.0f;

        // Basis: +5% Speed pro Teil * Fortschritt
        // Bei Max Level (2.0) und 4 Teilen (4 * 5% = 20%) * 2.0 = +40% Speed
        return 1.0f + (tideCount * 0.05f * progressMult);
    }

    // NEU: Movement Speed an Land (Bolt - Trial Chamber / Redstone Material)
    public static float getLandSpeedMultiplier(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float boltCount = getTrimCount(entity, "bolt");
        int redstoneCount = getMaterialCount(entity, "redstone");

        float bonus = 0f;
        if (boltCount > 0) bonus += (boltCount * 0.03f); // 3% pro Bolt Teil
        if (redstoneCount > 0) bonus += (redstoneCount * 0.015f); // 1.5% pro Redstone Teil

        return 1.0f + (bonus * progressMult);
    }

    // Wird in PlayerEntityMixin für Hunger genutzt
    public static float getExhaustionReduction(PlayerEntity player) {
        float progressMult = getGlobalMultiplier(player);
        float wayfinderCount = getTrimCount(player, "wayfinder"); // Trail Ruins

        if (wayfinderCount <= 0) return 0f;

        // Basis: 5% weniger Hunger pro Teil * Fortschritt
        float reduction = wayfinderCount * 0.05f * progressMult;
        return Math.min(reduction, 0.9f); // Cap bei 90%
    }

    // Wird in PlayerEntityMixin für XP genutzt
    public static float getXPMultiplier(PlayerEntity player) {
        float progressMult = getGlobalMultiplier(player);
        float raiserCount = getTrimCount(player, "raiser"); // Trail Ruins
        int lapisCount = getMaterialCount(player, "lapis");
        int quartzCount = getMaterialCount(player, "quartz");

        float baseBonus = 0f;
        baseBonus += (raiserCount * 0.05f); // 5% pro Raiser Trim
        baseBonus += (lapisCount * 0.02f);  // 2% pro Lapis
        baseBonus += (quartzCount * 0.02f); // 2% pro Quartz

        return 1.0f + (baseBonus * progressMult);
    }

    // Wird in PlayerEntityMixin für Luck genutzt
    public static float getLuckBonus(PlayerEntity player) {
        float progressMult = getGlobalMultiplier(player);
        float hostCount = getTrimCount(player, "host"); // Trail Ruins (Selten)
        int emeraldCount = getMaterialCount(player, "emerald");

        float bonus = 0f;
        // Host Trim ist selten -> hohes Luck
        if (hostCount > 0) bonus += (hostCount * 0.5f);
        if (emeraldCount > 0) bonus += (emeraldCount * 0.25f);

        return bonus * progressMult;
    }

    // Wird in LivingEntityMixin für Stealth Range genutzt
    public static float getStealthMultiplier(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float silenceCount = getTrimCount(entity, "silence"); // Ancient City

        if (silenceCount <= 0) return 1.0f;

        // Reduziert die Sichtweite von Mobs auf den Spieler
        // 10% pro Teil * Mult.
        float reduction = silenceCount * 0.10f * progressMult;
        return Math.max(0.1f, 1.0f - reduction); // Min 10% Sichtweite bleibt
    }

    public static float getAirSaveChance(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float coastCount = getTrimCount(entity, "coast");

        if (coastCount <= 0) return 0f;

        // Basis: 10% Chance pro Teil * Multiplikator
        return coastCount * 0.10f * progressMult;
    }

    public static int getWitherReductionAmount(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float ribCount = getTrimCount(entity, "rib");

        if (ribCount <= 0) return 0;

        // Basis: 20 Ticks (1 Sekunde) pro Teil * Multiplikator
        return (int) (ribCount * 20 * progressMult);
    }

    public static float getAmethystHealChance(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        int amethystCount = getMaterialCount(entity, "amethyst");

        if (amethystCount <= 0) return 0f;

        // Basis: 15% Chance pro Intervall * Multiplikator
        return amethystCount * 0.15f * progressMult;
    }


}