package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity; // Wichtig
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

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

                        // Material Boni für Trim-Effektivität
                        if (materialName.contains("enderite")) {
                            score += 3.5f; // Enderite: Extrem starker Boost für Pattern-Effekte
                        } else if (materialName.contains("netherite")) {
                            score += 1.75f; // Netherite: Starker Boost
                        } else {
                            score += 1.0f; // Standard
                        }
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

    // --- TICK LOGIC (Server-Side Effects) ---

    private static final Identifier ENDERSCAPE_STASIS_PATTERN = Identifier.of("enderscape", "stasis");

    public static void tick(PlayerEntity player) {
        // Nur Server-Logik für Status-Effekte
        if (!player.getEntityWorld().isClient()) {
            double multiplier = TrimMultiplierLogic.getMultiplier(player);

            // 1. Stasis Trim Check
            int stasisPieces = countTrimById(player, ENDERSCAPE_STASIS_PATTERN);
            if (stasisPieces > 0) {
                applyStasisEffect(player, multiplier, stasisPieces);
            }

            // 2. Astralit (Jump Boost)
            int astralitPieces = getMaterialCount(player, "astralit");
            if (astralitPieces > 0) {
                // Je nach Anzahl und Multiplikator Jump Boost I oder II
                double jumpScore = astralitPieces * multiplier;
                int amplifier = -1;

                if (jumpScore >= 8.0) amplifier = 1; // Jump Boost II
                else if (jumpScore >= 2.0) amplifier = 0; // Jump Boost I

                if (amplifier >= 0) {
                     player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 40, amplifier, true, false, true));
                }
            }
        }
    }

    // --- MOVEMENT LOGIC (Nihilith) ---
    // Wird vom PlayerEntityMixin aufgerufen (Client & Server für flüssige Bewegung)
    public static void handleNihilithGravity(PlayerEntity player) {
        int nihilithPieces = getMaterialCount(player, "nihilith");

        if (nihilithPieces > 0 && player.isSneaking() && !player.isOnGround() && !player.getAbilities().flying) {
            Vec3d velocity = player.getVelocity();
            double downwardForce = 0.08 * nihilithPieces;

            if (velocity.y > -2.0) {
                // addVelocity setzt intern 'velocityModified = true', daher brauchen wir es hier nicht manuell.
                player.addVelocity(0, -downwardForce, 0);
            }
        }
    }

    private static int countTrimById(PlayerEntity player, Identifier patternId) {
        int count = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack stack = player.getEquippedStack(slot);
                if (stack.isEmpty()) continue;
                ArmorTrim trim = stack.get(DataComponentTypes.TRIM);
                if (trim != null && trim.pattern().matchesId(patternId)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void applyStasisEffect(PlayerEntity player, double multiplier, int pieces) {
        double powerScore = multiplier * pieces;
        int amplifier = -1;
        if (powerScore >= 15.0) amplifier = 2;
        else if (powerScore >= 8.0) amplifier = 1;
        else if (powerScore >= 2.0) amplifier = 0;

        if (amplifier >= 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 80, amplifier, true, false, true));
        }
    }

    // --- HAUPTLOGIK: Schaden Modifizieren ---

    public static float modifyDamage(LivingEntity entity, float amount, DamageSource source) {
        if (amount <= 0) return 0;

        float multiplier = 1.0f;
        float progressMult = getGlobalMultiplier(entity);

        // A. TRIM PATTERNS
        // ... (Deine bestehenden Pattern-Logiken bleiben hier gleich) ...
        if (source.isIn(DamageTypeTags.IS_PROJECTILE)) multiplier -= calculateReduction(entity, "sentry", 0.05f, progressMult);
        if (source.isOf(DamageTypes.MAGIC) || source.isOf(DamageTypes.INDIRECT_MAGIC) || (source.getSource() instanceof net.minecraft.entity.mob.VexEntity)) multiplier -= calculateReduction(entity, "vex", 0.06f, progressMult);
        if (source.getName().equals("cactus") || source.getName().equals("sweetBerryBush") || source.getName().equals("stalagmite")) multiplier -= calculateReduction(entity, "wild", 0.10f, progressMult);
        if (source.isIn(DamageTypeTags.IS_EXPLOSION)) multiplier -= calculateReduction(entity, "dune", 0.08f, progressMult);
        if (source.isOf(DamageTypes.DROWN)) multiplier -= calculateReduction(entity, "coast", 0.10f, progressMult);
        multiplier -= calculateReduction(entity, "ward", 0.03f, progressMult);
        if (source.getName().equals("sonic_boom")) multiplier -= calculateReduction(entity, "silence", 0.20f, progressMult);
        if (source.isIn(DamageTypeTags.IS_FIRE)) multiplier -= calculateReduction(entity, "snout", 0.05f, progressMult);
        if (source.isOf(DamageTypes.WITHER)) multiplier -= calculateReduction(entity, "rib", 0.10f, progressMult);
        if (source.isOf(DamageTypes.DRAGON_BREATH)) multiplier -= calculateReduction(entity, "eye", 0.10f, progressMult);
        if (source.isIn(DamageTypeTags.IS_FALL)) multiplier -= calculateReduction(entity, "spire", 0.08f, progressMult);
        if (source.getSource() != null && source.getSource().getType().toString().contains("wind_charge")) multiplier -= calculateReduction(entity, "flow", 0.10f, progressMult);
        if (source.isOf(DamageTypes.LIGHTNING_BOLT)) multiplier -= calculateReduction(entity, "bolt", 0.25f, progressMult);

        // B. TRIM MATERIALS
        // --- Vanilla Materials ---
        if (!source.isIn(DamageTypeTags.BYPASSES_ARMOR)) {
            int diamondParts = getMaterialCount(entity, "diamond");
            if (diamondParts > 0) multiplier -= (diamondParts * 0.03f * progressMult);
        }
        if (source.isOf(DamageTypes.MAGIC) || source.isOf(DamageTypes.INDIRECT_MAGIC)) {
            int goldParts = getMaterialCount(entity, "gold");
            int lapisParts = getMaterialCount(entity, "lapis");
            if (goldParts > 0) multiplier -= (goldParts * 0.06f * progressMult);
            if (lapisParts > 0) multiplier -= (lapisParts * 0.04f * progressMult);
        }
        if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            int ironParts = getMaterialCount(entity, "iron");
            if (ironParts > 0) multiplier -= (ironParts * 0.05f * progressMult);
        }
        if (source.getAttacker() instanceof IllagerEntity) {
            int emeraldParts = getMaterialCount(entity, "emerald");
            if (emeraldParts > 0) multiplier -= (emeraldParts * 0.08f * progressMult);
        }
        if (source.isIn(DamageTypeTags.BYPASSES_ENCHANTMENTS) || source.getAttacker() instanceof net.minecraft.entity.boss.WitherEntity) {
            int netheriteParts = getMaterialCount(entity, "netherite");
            if (netheriteParts > 0) multiplier -= (netheriteParts * 0.05f * progressMult);
        }
        if(source.isIn(DamageTypeTags.IS_FIRE)) {
            int quartzParts = getMaterialCount(entity, "quartz");
            if(quartzParts > 0) multiplier -= (quartzParts * 0.05f * progressMult);
        }

        // --- NEUE MATERIALIEN ---

        // Enderite: All-Round Tank (ähnlich wie Ward Pattern, aber als Material)
        // Reduziert ALLEN Schaden signifikant.
        int enderiteParts = getMaterialCount(entity, "enderite");
        if (enderiteParts > 0) {
            // 5% Reduktion auf ALLES pro Teil (bei Max Progress -> 10% pro Teil)
            multiplier -= (enderiteParts * 0.05f * progressMult);
        }

        // Astralit & Nihilith haben Movement Effekte, aber wir geben ihnen
        // auch eine kleine physische Resistenz (wie Diamant aber schwächer), damit sie nicht nutzlos im Kampf sind.
        if (!source.isIn(DamageTypeTags.BYPASSES_ARMOR)) {
            int astralParts = getMaterialCount(entity, "astralit");
            int nihilParts = getMaterialCount(entity, "nihilith");
            if (astralParts > 0) multiplier -= (astralParts * 0.02f * progressMult);
            if (nihilParts > 0) multiplier -= (nihilParts * 0.02f * progressMult);
        }

        if (multiplier < 0.1f) multiplier = 0.1f;

        return amount * multiplier;
    }

    private static float calculateReduction(LivingEntity entity, String pattern, float baseReductionProzent, float progressMult) {
        float count = getTrimCount(entity, pattern);
        if (count <= 0) return 0f;
        return count * baseReductionProzent * progressMult;
    }

    // --- UTIL GETTER ---

    // (Deine existierenden Getter bleiben unverändert)
    public static float getSwimSpeedMultiplier(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float tideCount = getTrimCount(entity, "tide");
        if (tideCount <= 0) return 1.0f;
        return 1.0f + (tideCount * 0.10f * progressMult);
    }
    public static float getLandSpeedMultiplier(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float boltCount = getTrimCount(entity, "bolt");
        int redstoneCount = getMaterialCount(entity, "redstone");
        float bonus = 0f;
        if (boltCount > 0) bonus += (boltCount * 0.05f);
        if (redstoneCount > 0) bonus += (redstoneCount * 0.03f);
        return 1.0f + (bonus * progressMult);
    }
    public static float getExhaustionReduction(PlayerEntity player) {
        float progressMult = getGlobalMultiplier(player);
        float wayfinderCount = getTrimCount(player, "wayfinder");
        if (wayfinderCount <= 0) return 0f;
        float reduction = wayfinderCount * 0.10f * progressMult;
        return Math.min(reduction, 1.0f);
    }
    public static float getXPMultiplier(PlayerEntity player) {
        float progressMult = getGlobalMultiplier(player);
        float raiserCount = getTrimCount(player, "raiser");
        int lapisCount = getMaterialCount(player, "lapis");
        int quartzCount = getMaterialCount(player, "quartz");
        float baseBonus = 0f;
        baseBonus += (raiserCount * 0.10f);
        baseBonus += (lapisCount * 0.05f);
        baseBonus += (quartzCount * 0.05f);
        return 1.0f + (baseBonus * progressMult);
    }
    public static float getLuckBonus(PlayerEntity player) {
        float progressMult = getGlobalMultiplier(player);
        float hostCount = getTrimCount(player, "host");
        int emeraldCount = getMaterialCount(player, "emerald");
        float bonus = 0f;
        if (hostCount > 0) bonus += (hostCount * 1.0f);
        if (emeraldCount > 0) bonus += (emeraldCount * 0.5f);
        return bonus * progressMult;
    }
    public static float getStealthMultiplier(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float silenceCount = getTrimCount(entity, "silence");
        if (silenceCount <= 0) return 1.0f;
        float reduction = silenceCount * 0.15f * progressMult;
        return Math.max(0.0f, 1.0f - reduction);
    }
    public static float getAirSaveChance(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float coastCount = getTrimCount(entity, "coast");
        if (coastCount <= 0) return 0f;
        return coastCount * 0.20f * progressMult;
    }
    public static int getWitherReductionAmount(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float ribCount = getTrimCount(entity, "rib");
        if (ribCount <= 0) return 0;
        return (int) (ribCount * 40 * progressMult);
    }
    public static float getAmethystHealChance(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        int amethystCount = getMaterialCount(entity, "amethyst");
        if (amethystCount <= 0) return 0f;
        return amethystCount * 0.25f * progressMult;
    }
}