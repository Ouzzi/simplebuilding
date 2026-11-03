package com.simplebuilding.util;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Die Raten der Ruestungsbesaetze (Muster und Material), "je Teil bei Resonanz 1,0", fuer die ANZEIGE des Kerns
 * ({@link TrimBonusCatalog}: Tooltip, Nachschlage-Bildschirm). Gewirkt werden die Besatz-Faehigkeiten auf 26.3 von Simple Mobs
 * ({@code simplemobs:trim/TrimAbilities}, ZO9 A / MOB27-3, docs/ai/PLAN-B-TRIMMOVE01-2026-11-03.md) mit denselben Zahlen; auf der
 * 26.2-Linie (bis zum Port-Run) noch vom Kern ({@code TrimEffectUtil extends TrimRates} in common/src/mc26_2). Beide Seiten pinnen
 * dieselbe Tabelle aus docs/TRIM-BALANCE.md in ihren Tests, damit angezeigte und wirkende Werte nicht auseinanderlaufen.
 */
public class TrimRates {

    protected TrimRates() {
    }

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
    /** War 0,20 - der 75-%-Deckel war schon bei Resonanz 0,94 erreicht. */
    public static final float COAST_AIR_SAVE = 0.10f;
    public static final float WARD_ALL = 0.03f;
    public static final float SILENCE_SONIC = 0.20f;
    /** War 0,15 - der 50-%-Deckel war schon bei Resonanz 0,83 erreicht. */
    public static final float SILENCE_STEALTH = 0.08f;
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
    /** War 1,0 - der +3-Deckel war schon bei Resonanz 0,75 erreicht. */
    public static final float HOST_LUCK = 0.5f;
    /** Blockreichweite (Vanilla-Attribut block_interaction_range) in Bloecken. */
    public static final float SHAPER_REACH = 0.25f;

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
    /** Billiges Gegenstueck zum Bolt-Muster. */
    public static final float COPPER_LIGHTNING = 0.10f;
    /** Rueckstossresistenz (Vanilla-Attribut knockback_resistance, 1,0 = keiner). */
    public static final float RESIN_KNOCKBACK_RESISTANCE = 0.025f;

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
    /** Wassertritt-Effizienz (water_movement_efficiency): 0,5 liegt zwischen Wassertritt I und II. */
    public static final float MAX_SWIM_SPEED_BONUS = 0.5f;
    /** So viel wie vier normale Shaper-Teile bei voller Resonanz. */
    public static final float MAX_REACH_BONUS = 2.0f;
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

    /**
     * Exakter Vergleich (REV24-F-10): Pfad gleich {@code name} und Namensraum Vanilla oder ein Simple-Mod
     * (simplebuilding, simpletrims ...). Fremde Muster/Materialien mit aehnlichem Namen (oddeye, smoky_quartz) zaehlen nicht.
     */
    public static boolean isOwn(Identifier id, String name) {
        String ns = id.getNamespace();
        return id.getPath().equals(name) && (ns.equals("minecraft") || ns.startsWith("simple"));
    }

    /** Gewicht eines Besatzteils fuer sein Muster, abhaengig vom Besatzmaterial. */
    public static float patternWeight(String materialName) {
        if (materialName.equals("enderite")) return PATTERN_WEIGHT_ENDERITE;
        if (materialName.equals("netherite")) return PATTERN_WEIGHT_NETHERITE;
        return PATTERN_WEIGHT_DEFAULT;
    }
}
