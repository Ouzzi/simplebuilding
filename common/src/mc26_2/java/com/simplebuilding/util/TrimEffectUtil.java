package com.simplebuilding.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.phys.Vec3;

/**
 * Die Boni der Ruestungsbesaetze (Muster und Material). Alle Raten stehen als Konstanten hier und
 * gelten "je Teil bei Resonanz 1,0"; Tooltip und Nachschlage-Bildschirm lesen dieselben Konstanten
 * ueber {@link TrimBonusCatalog}, damit angezeigte und wirkende Werte nicht auseinanderlaufen.
 * Balance-Begruendung und Tabellen: docs/TRIM-BALANCE.md.
 * 26.2-Linie (bis zum Port-Run): auf 26.3 wirken die Besatz-Faehigkeiten in Simple Mobs (ZO9 A / MOB27-3,
 * docs/ai/PLAN-B-TRIMMOVE01-2026-11-03.md); die Raten stehen seitdem in {@link TrimRates}.
 */
public class TrimEffectUtil extends TrimRates {

    /**
     * Zählt, wie viele Teile den Trim haben, gewichtet nach Besatzmaterial
     * (Netherit 1,75, Enderit 2,0, sonst 1,0).
     */
    public static float getTrimCount(LivingEntity entity, String patternPath) {
        if (entity instanceof TrimBenefitUser user && !user.simplebuilding$areTrimBenefitsEnabled()) {
            return 0f;
        }

        float score = 0f;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack stack = entity.getItemBySlot(slot);
                var optionalTrim = stack.get(DataComponents.TRIM);

                if (optionalTrim != null) {
                    if (isOwn(optionalTrim.pattern().value().assetId(), patternPath)) {
                        String materialName = optionalTrim.material().unwrapKey()
                                .map(key -> isOwn(key.identifier(), key.identifier().getPath()) ? key.identifier().getPath() : "").orElse("");
                        score += patternWeight(materialName);
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
                ItemStack stack = entity.getItemBySlot(slot);
                var optionalTrim = stack.get(DataComponents.TRIM);
                if (optionalTrim != null) {
                    if (optionalTrim.material().unwrapKey().map(key -> isOwn(key.identifier(), materialPath)).orElse(false)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    // --- TICK LOGIC (Server-Side Effects) ---

    private static final Identifier ENDERSCAPE_STASIS_PATTERN = Identifier.fromNamespaceAndPath("enderscape", "stasis");

    public static void tick(Player player) {
        // Nur Server-Logik für Status-Effekte
        if (!player.level().isClientSide()) {
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
                     player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 40, amplifier, true, false, true));
                }
            }
        }
    }

    // --- MOVEMENT LOGIC (Nihilith) ---
    // Wird vom PlayerEntityMixin aufgerufen (Client & Server für flüssige Bewegung)
    public static void handleNihilithGravity(Player player) {
        int nihilithPieces = getMaterialCount(player, "nihilith");

        if (nihilithPieces > 0 && player.isShiftKeyDown() && !player.onGround() && !player.getAbilities().flying) {
            Vec3 velocity = player.getDeltaMovement();
            double downwardForce = 0.08 * nihilithPieces;

            if (velocity.y > -2.0) {
                // addVelocity setzt intern 'velocityModified = true', daher brauchen wir es hier nicht manuell.
                player.push(0, -downwardForce, 0);
            }
        }
    }

    /** N24 "faster Elytra": gliding in Astralit trims adds a gentle push along the look direction per piece (flat, no progress scaling), up to {@link #ASTRALIT_GLIDE_MAX_SPEED}. */
    public static final double ASTRALIT_GLIDE_PUSH = 0.012;
    public static final double ASTRALIT_GLIDE_MAX_SPEED = 1.8;

    public static void handleAstralitGlide(Player player) {
        if (!player.isFallFlying()) return;
        int pieces = getMaterialCount(player, "astralit");
        if (pieces <= 0) return;
        Vec3 velocity = player.getDeltaMovement();
        if (velocity.length() >= ASTRALIT_GLIDE_MAX_SPEED) return;
        player.push(player.getLookAngle().scale(ASTRALIT_GLIDE_PUSH * pieces));
    }

    private static int countTrimById(Player player, Identifier patternId) {
        int count = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack stack = player.getItemBySlot(slot);
                if (stack.isEmpty()) continue;
                ArmorTrim trim = stack.get(DataComponents.TRIM);
                if (trim != null && trim.pattern().is(patternId)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Resistenz-Stufe des Enderscape-Stasis-Musters: ab Punktzahl 2 Resistenz I, ab 8 Resistenz II,
     * hoechstens II (vorher III ab 15, erreichbar mit hochgesetzter Basis); -1 = keine.
     */
    public static int stasisAmplifier(double multiplier, int pieces) {
        double powerScore = multiplier * pieces;
        if (powerScore >= 8.0) return 1;
        if (powerScore >= 2.0) return 0;
        return -1;
    }

    private static void applyStasisEffect(Player player, double multiplier, int pieces) {
        int amplifier = stasisAmplifier(multiplier, pieces);

        if (amplifier >= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, amplifier, true, false, true));
        }
    }

    // --- HAUPTLOGIK: Schaden Modifizieren ---

    public static float modifyDamage(LivingEntity entity, float amount, DamageSource source) {
        if (amount <= 0) return 0;

        float multiplier = 1.0f;
        // Boni gegen JEDEN Schaden sammeln sich hier und werden gemeinsam gedeckelt.
        float generic = 0f;
        float progressMult = getGlobalMultiplier(entity);

        // A. TRIM PATTERNS
        if (source.is(DamageTypeTags.IS_PROJECTILE)) multiplier -= calculateReduction(entity, "sentry", SENTRY_PROJECTILE * s("projectile_protection"), progressMult);
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || (source.getDirectEntity() instanceof net.minecraft.world.entity.monster.Vex)) multiplier -= calculateReduction(entity, "vex", VEX_MAGIC * s("magic_protection"), progressMult);
        if (source.getMsgId().equals("cactus") || source.getMsgId().equals("sweetBerryBush") || source.getMsgId().equals("stalagmite")) multiplier -= calculateReduction(entity, "wild", WILD_THORNS * s("thorn_protection"), progressMult);
        if (source.is(DamageTypeTags.IS_EXPLOSION)) multiplier -= calculateReduction(entity, "dune", DUNE_BLAST * s("blast_protection"), progressMult);
        if (source.is(DamageTypes.DROWN)) multiplier -= calculateReduction(entity, "coast", COAST_DROWN * s("drowning_protection"), progressMult);
        generic += calculateReduction(entity, "ward", WARD_ALL * s("all_protection"), progressMult);
        if (source.getMsgId().equals("sonic_boom")) multiplier -= calculateReduction(entity, "silence", SILENCE_SONIC * s("sonic_protection"), progressMult);
        if (source.is(DamageTypeTags.IS_FIRE)) multiplier -= calculateReduction(entity, "snout", SNOUT_FIRE * s("fire_protection"), progressMult);
        if (source.is(DamageTypes.WITHER)) multiplier -= calculateReduction(entity, "rib", RIB_WITHER * s("wither_protection"), progressMult);
        if (source.is(DamageTypes.DRAGON_BREATH)) multiplier -= calculateReduction(entity, "eye", EYE_DRAGON_BREATH * s("dragon_breath_protection"), progressMult);
        if (source.is(DamageTypeTags.IS_FALL)) multiplier -= calculateReduction(entity, "spire", SPIRE_FALL * s("fall_protection"), progressMult);
        if (source.getDirectEntity() != null && source.getDirectEntity().getType().toString().contains("wind_charge")) multiplier -= calculateReduction(entity, "flow", FLOW_WIND_CHARGE * s("wind_charge_protection"), progressMult);
        if (source.is(DamageTypes.LIGHTNING_BOLT)) {
            multiplier -= calculateReduction(entity, "bolt", BOLT_LIGHTNING * s("lightning_protection"), progressMult);
            int copperParts = getMaterialCount(entity, "copper");
            if (copperParts > 0) multiplier -= (copperParts * COPPER_LIGHTNING * s("lightning_protection") * progressMult);
        }

        // B. TRIM MATERIALS
        // --- Vanilla Materials ---
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            int diamondParts = getMaterialCount(entity, "diamond");
            if (diamondParts > 0) generic += (diamondParts * DIAMOND_PHYSICAL * s("physical_protection") * progressMult);
        }
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) {
            int goldParts = getMaterialCount(entity, "gold");
            int lapisParts = getMaterialCount(entity, "lapis");
            if (goldParts > 0) multiplier -= (goldParts * GOLD_MAGIC * s("magic_protection") * progressMult);
            if (lapisParts > 0) multiplier -= (lapisParts * LAPIS_MAGIC * s("magic_protection") * progressMult);
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            int ironParts = getMaterialCount(entity, "iron");
            if (ironParts > 0) multiplier -= (ironParts * IRON_PROJECTILE * s("projectile_protection") * progressMult);
        }
        if (source.getEntity() instanceof AbstractIllager) {
            int emeraldParts = getMaterialCount(entity, "emerald");
            if (emeraldParts > 0) multiplier -= (emeraldParts * EMERALD_ILLAGER * s("illager_protection") * progressMult);
        }
        if (source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS) || source.getEntity() instanceof net.minecraft.world.entity.boss.wither.WitherBoss) {
            int netheriteParts = getMaterialCount(entity, "netherite");
            if (netheriteParts > 0) multiplier -= (netheriteParts * NETHERITE_WITHER_PIERCING * s("wither_piercing_protection") * progressMult);
        }
        if(source.is(DamageTypeTags.IS_FIRE)) {
            int quartzParts = getMaterialCount(entity, "quartz");
            if(quartzParts > 0) multiplier -= (quartzParts * QUARTZ_FIRE * s("fire_protection") * progressMult);
        }

        // --- NEUE MATERIALIEN ---

        // Enderite: All-Round-Schutz (wie das Ward-Muster, aber als Material) gegen jeden Schaden,
        // ausser dem, der die Unverwundbarkeit umgeht (/kill, die Leere).
        int enderiteParts = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) ? 0 : getMaterialCount(entity, "enderite");
        if (enderiteParts > 0) {
            generic += (enderiteParts * ENDERITE_ALL * s("all_protection") * progressMult);
        }

        // Astralit & Nihilith haben Movement Effekte, aber wir geben ihnen
        // auch eine kleine physische Resistenz (wie Diamant aber schwächer), damit sie nicht nutzlos im Kampf sind.
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            int astralParts = getMaterialCount(entity, "astralit");
            int nihilParts = getMaterialCount(entity, "nihilith");
            if (astralParts > 0) generic += (astralParts * ASTRALIT_PHYSICAL * s("physical_protection") * progressMult);
            if (nihilParts > 0) generic += (nihilParts * NIHILITH_PHYSICAL * s("physical_protection") * progressMult);
        }

        multiplier -= Math.min(generic, GENERIC_REDUCTION_CAP);

        if (multiplier < DAMAGE_FLOOR) multiplier = DAMAGE_FLOOR;

        return amount * multiplier;
    }

    /**
     * Staerke einer Wirkung aus {@code server.trimStrengths} (Schluessel wie im {@link TrimBonusCatalog});
     * auf dem Client die des Servers, damit Tooltip und vorhergesagte Bewegung stimmen.
     */
    private static float s(String bonusKey) {
        return com.simplebuilding.config.ServerTuning.trimStrength(bonusKey);
    }

    private static float calculateReduction(LivingEntity entity, String pattern, float baseReductionProzent, float progressMult) {
        float count = getTrimCount(entity, pattern);
        if (count <= 0) return 0f;
        return count * baseReductionProzent * progressMult;
    }

    // --- UTIL GETTER (alle mit Deckel, siehe oben) ---

    public static float getSwimSpeedMultiplier(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float tideCount = getTrimCount(entity, "tide");
        if (tideCount <= 0) return 1.0f;
        return 1.0f + Math.min(tideCount * TIDE_SWIM * s("swimming_speed") * progressMult, MAX_SWIM_SPEED_BONUS);
    }
    public static float getLandSpeedMultiplier(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float boltCount = getTrimCount(entity, "bolt");
        int redstoneCount = getMaterialCount(entity, "redstone");
        float bonus = 0f;
        if (boltCount > 0) bonus += (boltCount * BOLT_SPEED * s("walking_speed"));
        if (redstoneCount > 0) bonus += (redstoneCount * REDSTONE_SPEED * s("walking_speed"));
        return 1.0f + Math.min(bonus * progressMult, MAX_LAND_SPEED_BONUS);
    }
    public static float getExhaustionReduction(Player player) {
        float progressMult = getGlobalMultiplier(player);
        float wayfinderCount = getTrimCount(player, "wayfinder");
        if (wayfinderCount <= 0) return 0f;
        float reduction = wayfinderCount * WAYFINDER_SPRINT_HUNGER * s("sprint_hunger") * progressMult;
        return Math.min(reduction, MAX_SPRINT_HUNGER_REDUCTION);
    }
    public static float getXPMultiplier(Player player) {
        float progressMult = getGlobalMultiplier(player);
        float raiserCount = getTrimCount(player, "raiser");
        int lapisCount = getMaterialCount(player, "lapis");
        int quartzCount = getMaterialCount(player, "quartz");
        float baseBonus = 0f;
        baseBonus += (raiserCount * RAISER_XP * s("experience"));
        baseBonus += (lapisCount * LAPIS_XP * s("experience"));
        baseBonus += (quartzCount * QUARTZ_XP * s("experience"));
        return 1.0f + Math.min(baseBonus * progressMult, MAX_XP_BONUS);
    }
    public static float getLuckBonus(LivingEntity player) {
        float progressMult = getGlobalMultiplier(player);
        float hostCount = getTrimCount(player, "host");
        int emeraldCount = getMaterialCount(player, "emerald");
        float bonus = 0f;
        if (hostCount > 0) bonus += (hostCount * HOST_LUCK * s("luck"));
        if (emeraldCount > 0) bonus += (emeraldCount * EMERALD_LUCK * s("luck"));
        return Math.min(bonus * progressMult, MAX_LUCK_BONUS);
    }
    public static float getStealthMultiplier(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float silenceCount = getTrimCount(entity, "silence");
        if (silenceCount <= 0) return 1.0f;
        float reduction = silenceCount * SILENCE_STEALTH * s("stealth") * progressMult;
        return 1.0f - Math.min(reduction, MAX_STEALTH_REDUCTION);
    }
    public static float getAirSaveChance(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float coastCount = getTrimCount(entity, "coast");
        if (coastCount <= 0) return 0f;
        return Math.min(coastCount * COAST_AIR_SAVE * s("breath_saving") * progressMult, MAX_AIR_SAVE_CHANCE);
    }
    public static int getWitherReductionAmount(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float ribCount = getTrimCount(entity, "rib");
        if (ribCount <= 0) return 0;
        return Math.min((int) (ribCount * RIB_WITHER_TICKS * s("wither_shortening") * progressMult), MAX_WITHER_REDUCTION_TICKS);
    }
    public static float getAmethystHealChance(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        int amethystCount = getMaterialCount(entity, "amethyst");
        if (amethystCount <= 0) return 0f;
        return Math.min(amethystCount * AMETHYST_HEAL_CHANCE * s("healing_chance") * progressMult, MAX_HEAL_CHANCE);
    }

    /** Rueckstossresistenz aus Harz-Besatz (Material, je Teil). */
    public static float getKnockbackResistanceBonus(LivingEntity entity) {
        int resinCount = getMaterialCount(entity, "resin");
        if (resinCount <= 0) return 0f;
        return resinCount * RESIN_KNOCKBACK_RESISTANCE * s("knockback_resistance") * getGlobalMultiplier(entity);
    }

    /** Zusaetzliche Blockreichweite aus dem Shaper-Muster, in Bloecken. */
    public static float getReachBonus(LivingEntity entity) {
        float shaperCount = getTrimCount(entity, "shaper");
        if (shaperCount <= 0) return 0f;
        return Math.min(shaperCount * SHAPER_REACH * s("block_reach") * getGlobalMultiplier(entity), MAX_REACH_BONUS);
    }
}
