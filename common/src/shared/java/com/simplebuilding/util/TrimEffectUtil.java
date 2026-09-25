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
 */
public class TrimEffectUtil {

    // --- MUSTER-GEWICHT JE BESATZMATERIAL (getTrimCount) ---
    public static final float PATTERN_WEIGHT_DEFAULT = 1.0f;
    public static final float PATTERN_WEIGHT_NETHERITE = 1.75f;
    /** War 3,5 - ein Enderit-Satz zaehlte als 14 Teile und trug jeden Musterbonus an seinen Deckel. */
    public static final float PATTERN_WEIGHT_ENDERITE = 2.0f;

    // --- MUSTER-RATEN (je Teil, Resonanz 1,0) ---
    public static final float SENTRY_PROJECTILE = 0.05f;
    public static final float VEX_MAGIC = 0.06f;
    public static final float WILD_THORNS = 0.10f;
    public static final float DUNE_BLAST = 0.08f;
    public static final float COAST_DROWN = 0.10f;
    public static final float COAST_AIR_SAVE = 0.20f;
    public static final float WARD_ALL = 0.03f;
    public static final float SILENCE_SONIC = 0.20f;
    public static final float SILENCE_STEALTH = 0.15f;
    public static final float SNOUT_FIRE = 0.05f;
    public static final float RIB_WITHER = 0.10f;
    public static final int RIB_WITHER_TICKS = 40;
    public static final float EYE_DRAGON_BREATH = 0.10f;
    public static final float SPIRE_FALL = 0.08f;
    public static final float FLOW_WIND_CHARGE = 0.10f;
    public static final float BOLT_LIGHTNING = 0.25f;
    public static final float BOLT_SPEED = 0.05f;
    public static final float TIDE_SWIM = 0.10f;
    public static final float WAYFINDER_SPRINT_HUNGER = 0.10f;
    public static final float RAISER_XP = 0.10f;
    public static final float HOST_LUCK = 1.0f;

    // --- MATERIAL-RATEN (je Teil, Resonanz 1,0) ---
    public static final float DIAMOND_PHYSICAL = 0.03f;
    public static final float GOLD_MAGIC = 0.06f;
    public static final float LAPIS_MAGIC = 0.04f;
    public static final float LAPIS_XP = 0.05f;
    public static final float IRON_PROJECTILE = 0.05f;
    public static final float EMERALD_ILLAGER = 0.08f;
    public static final float EMERALD_LUCK = 0.5f;
    public static final float NETHERITE_WITHER_PIERCING = 0.05f;
    public static final float QUARTZ_FIRE = 0.05f;
    public static final float QUARTZ_XP = 0.05f;
    public static final float ENDERITE_ALL = 0.05f;
    public static final float ASTRALIT_PHYSICAL = 0.02f;
    public static final float NIHILITH_PHYSICAL = 0.02f;
    public static final float REDSTONE_SPEED = 0.03f;
    public static final float AMETHYST_HEAL_CHANCE = 0.25f;

    // --- DECKEL (gelten nach Resonanz und Teilezahl) ---
    /** Hoechstens 80 % weniger Schaden je Treffer - so weit reicht auch Vanillas Schutz-Deckel. War 90 %. */
    public static final float DAMAGE_FLOOR = 0.2f;
    /** Die Boni gegen JEDEN Schaden (Ward, Diamant, Enderit, Astralit, Nihilith) zusammen hoechstens 25 %. */
    public static final float GENERIC_REDUCTION_CAP = 0.25f;
    /** Hoechstens halbe Sichtbarkeit, so viel wie ein getragener Mob-Kopf. War 100 % (unsichtbar). */
    public static final float MAX_STEALTH_REDUCTION = 0.5f;
    /** Wie Atmung III. War unbegrenzt (ab 100 % nie mehr Luft verlieren). */
    public static final float MAX_AIR_SAVE_CHANCE = 0.75f;
    /** War 100 % - Sprinten ganz ohne Hunger. */
    public static final float MAX_SPRINT_HUNGER_REDUCTION = 0.5f;
    /** Wie Gluecksbringer III beim Angeln. War unbegrenzt. */
    public static final float MAX_LUCK_BONUS = 3.0f;
    public static final float MAX_XP_BONUS = 0.5f;
    /** Wie Schnelligkeit I. War unbegrenzt. */
    public static final float MAX_LAND_SPEED_BONUS = 0.2f;
    public static final float MAX_SWIM_SPEED_BONUS = 0.5f;
    /** 5 s. War unbegrenzt - ein voller Satz loeschte jede Wither-Wirkung unter 16 s sofort. */
    public static final int MAX_WITHER_REDUCTION_TICKS = 100;
    public static final float MAX_HEAL_CHANCE = 1.0f;

    /** Resonanz fuer Traeger, die keine Spieler sind (Mobs, Ruestungsstaender). */
    public static final float NON_PLAYER_MULTIPLIER = 0.2f;

    // --- HELPER: Multiplikator Logik ---

    /**
     * Die Resonanz des Traegers. Spieler (Server UND Client - der Client rechnet mit den
     * synchronisierten Statistiken, sonst zeigten Tooltips und die vom Client vorhergesagte
     * Laufgeschwindigkeit einen anderen Wert als der Server) nutzen TrimMultiplierLogic;
     * alle anderen Traeger bekommen einen festen Wert.
     */
    public static float getGlobalMultiplier(LivingEntity entity) {
        if (entity instanceof Player player) {
            return (float) TrimMultiplierLogic.getMultiplier(player);
        }
        return NON_PLAYER_MULTIPLIER;
    }

    /** Gewicht eines Besatzteils fuer sein Muster, abhaengig vom Besatzmaterial. */
    public static float patternWeight(String materialName) {
        if (materialName.contains("enderite")) return PATTERN_WEIGHT_ENDERITE;
        if (materialName.contains("netherite")) return PATTERN_WEIGHT_NETHERITE;
        return PATTERN_WEIGHT_DEFAULT;
    }

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
                    String patternId = optionalTrim.pattern().value().assetId().getPath();
                    if (patternId.contains(patternPath)) {
                        String materialName = optionalTrim.material().unwrapKey()
                                .map(key -> key.identifier().getPath()).orElse("");
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
                    String materialId = optionalTrim.material().unwrapKey().map(key -> key.identifier().getPath()).orElse("");
                    if (materialId.contains(materialPath)) {
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

    private static void applyStasisEffect(Player player, double multiplier, int pieces) {
        double powerScore = multiplier * pieces;
        int amplifier = -1;
        if (powerScore >= 15.0) amplifier = 2;
        else if (powerScore >= 8.0) amplifier = 1;
        else if (powerScore >= 2.0) amplifier = 0;

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
        if (source.is(DamageTypeTags.IS_PROJECTILE)) multiplier -= calculateReduction(entity, "sentry", SENTRY_PROJECTILE, progressMult);
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || (source.getDirectEntity() instanceof net.minecraft.world.entity.monster.Vex)) multiplier -= calculateReduction(entity, "vex", VEX_MAGIC, progressMult);
        if (source.getMsgId().equals("cactus") || source.getMsgId().equals("sweetBerryBush") || source.getMsgId().equals("stalagmite")) multiplier -= calculateReduction(entity, "wild", WILD_THORNS, progressMult);
        if (source.is(DamageTypeTags.IS_EXPLOSION)) multiplier -= calculateReduction(entity, "dune", DUNE_BLAST, progressMult);
        if (source.is(DamageTypes.DROWN)) multiplier -= calculateReduction(entity, "coast", COAST_DROWN, progressMult);
        generic += calculateReduction(entity, "ward", WARD_ALL, progressMult);
        if (source.getMsgId().equals("sonic_boom")) multiplier -= calculateReduction(entity, "silence", SILENCE_SONIC, progressMult);
        if (source.is(DamageTypeTags.IS_FIRE)) multiplier -= calculateReduction(entity, "snout", SNOUT_FIRE, progressMult);
        if (source.is(DamageTypes.WITHER)) multiplier -= calculateReduction(entity, "rib", RIB_WITHER, progressMult);
        if (source.is(DamageTypes.DRAGON_BREATH)) multiplier -= calculateReduction(entity, "eye", EYE_DRAGON_BREATH, progressMult);
        if (source.is(DamageTypeTags.IS_FALL)) multiplier -= calculateReduction(entity, "spire", SPIRE_FALL, progressMult);
        if (source.getDirectEntity() != null && source.getDirectEntity().getType().toString().contains("wind_charge")) multiplier -= calculateReduction(entity, "flow", FLOW_WIND_CHARGE, progressMult);
        if (source.is(DamageTypes.LIGHTNING_BOLT)) multiplier -= calculateReduction(entity, "bolt", BOLT_LIGHTNING, progressMult);

        // B. TRIM MATERIALS
        // --- Vanilla Materials ---
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            int diamondParts = getMaterialCount(entity, "diamond");
            if (diamondParts > 0) generic += (diamondParts * DIAMOND_PHYSICAL * progressMult);
        }
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) {
            int goldParts = getMaterialCount(entity, "gold");
            int lapisParts = getMaterialCount(entity, "lapis");
            if (goldParts > 0) multiplier -= (goldParts * GOLD_MAGIC * progressMult);
            if (lapisParts > 0) multiplier -= (lapisParts * LAPIS_MAGIC * progressMult);
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            int ironParts = getMaterialCount(entity, "iron");
            if (ironParts > 0) multiplier -= (ironParts * IRON_PROJECTILE * progressMult);
        }
        if (source.getEntity() instanceof AbstractIllager) {
            int emeraldParts = getMaterialCount(entity, "emerald");
            if (emeraldParts > 0) multiplier -= (emeraldParts * EMERALD_ILLAGER * progressMult);
        }
        if (source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS) || source.getEntity() instanceof net.minecraft.world.entity.boss.wither.WitherBoss) {
            int netheriteParts = getMaterialCount(entity, "netherite");
            if (netheriteParts > 0) multiplier -= (netheriteParts * NETHERITE_WITHER_PIERCING * progressMult);
        }
        if(source.is(DamageTypeTags.IS_FIRE)) {
            int quartzParts = getMaterialCount(entity, "quartz");
            if(quartzParts > 0) multiplier -= (quartzParts * QUARTZ_FIRE * progressMult);
        }

        // --- NEUE MATERIALIEN ---

        // Enderite: All-Round-Schutz (wie das Ward-Muster, aber als Material) gegen JEDEN Schaden.
        int enderiteParts = getMaterialCount(entity, "enderite");
        if (enderiteParts > 0) {
            generic += (enderiteParts * ENDERITE_ALL * progressMult);
        }

        // Astralit & Nihilith haben Movement Effekte, aber wir geben ihnen
        // auch eine kleine physische Resistenz (wie Diamant aber schwächer), damit sie nicht nutzlos im Kampf sind.
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            int astralParts = getMaterialCount(entity, "astralit");
            int nihilParts = getMaterialCount(entity, "nihilith");
            if (astralParts > 0) generic += (astralParts * ASTRALIT_PHYSICAL * progressMult);
            if (nihilParts > 0) generic += (nihilParts * NIHILITH_PHYSICAL * progressMult);
        }

        multiplier -= Math.min(generic, GENERIC_REDUCTION_CAP);

        if (multiplier < DAMAGE_FLOOR) multiplier = DAMAGE_FLOOR;

        return amount * multiplier;
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
        return 1.0f + Math.min(tideCount * TIDE_SWIM * progressMult, MAX_SWIM_SPEED_BONUS);
    }
    public static float getLandSpeedMultiplier(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float boltCount = getTrimCount(entity, "bolt");
        int redstoneCount = getMaterialCount(entity, "redstone");
        float bonus = 0f;
        if (boltCount > 0) bonus += (boltCount * BOLT_SPEED);
        if (redstoneCount > 0) bonus += (redstoneCount * REDSTONE_SPEED);
        return 1.0f + Math.min(bonus * progressMult, MAX_LAND_SPEED_BONUS);
    }
    public static float getExhaustionReduction(Player player) {
        float progressMult = getGlobalMultiplier(player);
        float wayfinderCount = getTrimCount(player, "wayfinder");
        if (wayfinderCount <= 0) return 0f;
        float reduction = wayfinderCount * WAYFINDER_SPRINT_HUNGER * progressMult;
        return Math.min(reduction, MAX_SPRINT_HUNGER_REDUCTION);
    }
    public static float getXPMultiplier(Player player) {
        float progressMult = getGlobalMultiplier(player);
        float raiserCount = getTrimCount(player, "raiser");
        int lapisCount = getMaterialCount(player, "lapis");
        int quartzCount = getMaterialCount(player, "quartz");
        float baseBonus = 0f;
        baseBonus += (raiserCount * RAISER_XP);
        baseBonus += (lapisCount * LAPIS_XP);
        baseBonus += (quartzCount * QUARTZ_XP);
        return 1.0f + Math.min(baseBonus * progressMult, MAX_XP_BONUS);
    }
    public static float getLuckBonus(Player player) {
        float progressMult = getGlobalMultiplier(player);
        float hostCount = getTrimCount(player, "host");
        int emeraldCount = getMaterialCount(player, "emerald");
        float bonus = 0f;
        if (hostCount > 0) bonus += (hostCount * HOST_LUCK);
        if (emeraldCount > 0) bonus += (emeraldCount * EMERALD_LUCK);
        return Math.min(bonus * progressMult, MAX_LUCK_BONUS);
    }
    public static float getStealthMultiplier(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float silenceCount = getTrimCount(entity, "silence");
        if (silenceCount <= 0) return 1.0f;
        float reduction = silenceCount * SILENCE_STEALTH * progressMult;
        return 1.0f - Math.min(reduction, MAX_STEALTH_REDUCTION);
    }
    public static float getAirSaveChance(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float coastCount = getTrimCount(entity, "coast");
        if (coastCount <= 0) return 0f;
        return Math.min(coastCount * COAST_AIR_SAVE * progressMult, MAX_AIR_SAVE_CHANCE);
    }
    public static int getWitherReductionAmount(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        float ribCount = getTrimCount(entity, "rib");
        if (ribCount <= 0) return 0;
        return Math.min((int) (ribCount * RIB_WITHER_TICKS * progressMult), MAX_WITHER_REDUCTION_TICKS);
    }
    public static float getAmethystHealChance(LivingEntity entity) {
        float progressMult = getGlobalMultiplier(entity);
        int amethystCount = getMaterialCount(entity, "amethyst");
        if (amethystCount <= 0) return 0f;
        return Math.min(amethystCount * AMETHYST_HEAL_CHANCE * progressMult, MAX_HEAL_CHANCE);
    }
}
