package com.simplebuilding.util;

import static com.simplebuilding.util.TrimEffectUtil.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

/**
 * Welche Boni ein Besatzmuster bzw. -material gibt, mit den Raten aus {@link TrimEffectUtil}.
 * Die Zuordnung spiegelt die {@code contains}-Pruefungen dort; Tooltip und Nachschlage-Bildschirm
 * lesen nur von hier, damit angezeigte und wirkende Werte nicht wieder auseinanderlaufen
 * (vorher standen im Tooltip eigene, teils halbierte oder erfundene Zahlen).
 */
public final class TrimBonusCatalog {

    public enum Kind {
        /** Anteil, angezeigt als "+x% Name" (Vanilla-Attributformat). */
        PERCENT,
        /** Glueckspunkte, angezeigt als "+x Name". */
        POINTS,
        /** Ticks, angezeigt in Sekunden. */
        TICKS,
        /** Faktor auf das Muster dieses Teils ("x1,75"), haengt nicht an der Resonanz. */
        FACTOR,
        /** Rueckstossresistenz: wie Vanilla-Tooltips mal 10 angezeigt (Netherit-Ruestung "+1"). */
        KNOCKBACK,
        /** Wirkung ohne Zahl (Sprungkraft, Sturzflug). */
        TEXT
    }

    public record Bonus(String key, float perPiece, Kind kind) {
        public Component describe(float weight, float resonance) {
            Component name = Component.translatable("trim_bonus.simplebuilding." + key);
            return switch (kind) {
                case PERCENT -> Component.translatable("attribute.modifier.plus.1",
                        format(perPiece * weight * resonance * 100f), name);
                case POINTS -> Component.translatable("attribute.modifier.plus.0",
                        format(perPiece * weight * resonance), name);
                case TICKS -> Component.translatable("tooltip.simplebuilding.trim_bonus.seconds",
                        format(perPiece * weight * resonance / 20f), name);
                case FACTOR -> Component.translatable("tooltip.simplebuilding.trim_bonus.factor",
                        format(perPiece), name);
                case KNOCKBACK -> Component.translatable("attribute.modifier.plus.0",
                        format(perPiece * weight * resonance * 10f), name);
                case TEXT -> name;
            };
        }
    }

    private TrimBonusCatalog() {}

    public static List<Bonus> forPattern(String patternPath) {
        List<Bonus> list = new ArrayList<>();
        if (patternPath.contains("sentry")) list.add(new Bonus("projectile_protection", SENTRY_PROJECTILE, Kind.PERCENT));
        if (patternPath.contains("vex")) list.add(new Bonus("magic_protection", VEX_MAGIC, Kind.PERCENT));
        if (patternPath.contains("wild")) list.add(new Bonus("thorn_protection", WILD_THORNS, Kind.PERCENT));
        if (patternPath.contains("dune")) list.add(new Bonus("blast_protection", DUNE_BLAST, Kind.PERCENT));
        if (patternPath.contains("coast")) {
            list.add(new Bonus("drowning_protection", COAST_DROWN, Kind.PERCENT));
            list.add(new Bonus("breath_saving", COAST_AIR_SAVE, Kind.PERCENT));
        }
        if (patternPath.contains("ward")) list.add(new Bonus("all_protection", WARD_ALL, Kind.PERCENT));
        if (patternPath.contains("silence")) {
            list.add(new Bonus("sonic_protection", SILENCE_SONIC, Kind.PERCENT));
            list.add(new Bonus("stealth", SILENCE_STEALTH, Kind.PERCENT));
        }
        if (patternPath.contains("snout")) list.add(new Bonus("fire_protection", SNOUT_FIRE, Kind.PERCENT));
        if (patternPath.contains("rib")) {
            list.add(new Bonus("wither_protection", RIB_WITHER, Kind.PERCENT));
            list.add(new Bonus("wither_shortening", RIB_WITHER_TICKS, Kind.TICKS));
        }
        if (patternPath.contains("eye")) list.add(new Bonus("dragon_breath_protection", EYE_DRAGON_BREATH, Kind.PERCENT));
        if (patternPath.contains("spire")) list.add(new Bonus("fall_protection", SPIRE_FALL, Kind.PERCENT));
        if (patternPath.contains("flow")) list.add(new Bonus("wind_charge_protection", FLOW_WIND_CHARGE, Kind.PERCENT));
        if (patternPath.contains("bolt")) {
            list.add(new Bonus("lightning_protection", BOLT_LIGHTNING, Kind.PERCENT));
            list.add(new Bonus("walking_speed", BOLT_SPEED, Kind.PERCENT));
        }
        if (patternPath.contains("tide")) list.add(new Bonus("swimming_speed", TIDE_SWIM, Kind.PERCENT));
        if (patternPath.contains("wayfinder")) list.add(new Bonus("sprint_hunger", WAYFINDER_SPRINT_HUNGER, Kind.PERCENT));
        if (patternPath.contains("raiser")) list.add(new Bonus("experience", RAISER_XP, Kind.PERCENT));
        if (patternPath.contains("host")) list.add(new Bonus("luck", HOST_LUCK, Kind.POINTS));
        if (patternPath.contains("shaper")) list.add(new Bonus("block_reach", SHAPER_REACH, Kind.POINTS));
        return list;
    }

    public static List<Bonus> forMaterial(String materialPath) {
        List<Bonus> list = new ArrayList<>();
        if (materialPath.contains("diamond")) list.add(new Bonus("physical_protection", DIAMOND_PHYSICAL, Kind.PERCENT));
        if (materialPath.contains("gold")) list.add(new Bonus("magic_protection", GOLD_MAGIC, Kind.PERCENT));
        if (materialPath.contains("lapis")) {
            list.add(new Bonus("magic_protection", LAPIS_MAGIC, Kind.PERCENT));
            list.add(new Bonus("experience", LAPIS_XP, Kind.PERCENT));
        }
        if (materialPath.contains("iron")) list.add(new Bonus("projectile_protection", IRON_PROJECTILE, Kind.PERCENT));
        if (materialPath.contains("emerald")) {
            list.add(new Bonus("illager_protection", EMERALD_ILLAGER, Kind.PERCENT));
            list.add(new Bonus("luck", EMERALD_LUCK, Kind.POINTS));
        }
        if (materialPath.contains("netherite")) {
            list.add(new Bonus("wither_piercing_protection", NETHERITE_WITHER_PIERCING, Kind.PERCENT));
            list.add(new Bonus("pattern_strength", PATTERN_WEIGHT_NETHERITE, Kind.FACTOR));
        }
        if (materialPath.contains("quartz")) {
            list.add(new Bonus("fire_protection", QUARTZ_FIRE, Kind.PERCENT));
            list.add(new Bonus("experience", QUARTZ_XP, Kind.PERCENT));
        }
        if (materialPath.contains("enderite")) {
            list.add(new Bonus("all_protection", ENDERITE_ALL, Kind.PERCENT));
            list.add(new Bonus("pattern_strength", PATTERN_WEIGHT_ENDERITE, Kind.FACTOR));
        }
        if (materialPath.contains("astralit")) {
            list.add(new Bonus("physical_protection", ASTRALIT_PHYSICAL, Kind.PERCENT));
            list.add(new Bonus("jump_boost", 0f, Kind.TEXT));
        }
        if (materialPath.contains("nihilith")) {
            list.add(new Bonus("physical_protection", NIHILITH_PHYSICAL, Kind.PERCENT));
            list.add(new Bonus("dive", 0f, Kind.TEXT));
        }
        if (materialPath.contains("redstone")) list.add(new Bonus("walking_speed", REDSTONE_SPEED, Kind.PERCENT));
        if (materialPath.contains("amethyst")) list.add(new Bonus("healing_chance", AMETHYST_HEAL_CHANCE, Kind.PERCENT));
        if (materialPath.contains("copper")) list.add(new Bonus("lightning_protection", COPPER_LIGHTNING, Kind.PERCENT));
        if (materialPath.contains("resin")) list.add(new Bonus("knockback_resistance", RESIN_KNOCKBACK_RESISTANCE, Kind.KNOCKBACK));
        return list;
    }

    /**
     * Die Tooltip-Zeilen fuer ein besetztes Teil im Stil der Vanilla-Attributliste: Leerzeile,
     * graue Kopfzeile, blaue "+x% Name"-Zeilen. Werte gelten fuer DIESES Teil bei der
     * uebergebenen Resonanz; das Muster ist schon mit dem Materialgewicht verrechnet.
     */
    public static List<Component> tooltipLines(ArmorTrim trim, float resonance) {
        String pattern = trim.pattern().value().assetId().getPath();
        String material = trim.material().unwrapKey().map(key -> key.identifier().getPath()).orElse("");
        float weight = patternWeight(material);

        List<Component> lines = new ArrayList<>();
        for (Bonus bonus : forPattern(pattern)) {
            lines.add(bonus.describe(weight, resonance).copy().withStyle(ChatFormatting.BLUE));
        }
        for (Bonus bonus : forMaterial(material)) {
            lines.add(bonus.describe(1.0f, resonance).copy().withStyle(ChatFormatting.BLUE));
        }
        if (lines.isEmpty()) return lines;

        List<Component> out = new ArrayList<>(lines.size() + 2);
        out.add(Component.empty());
        out.add(Component.translatable("tooltip.simplebuilding.trim_bonus.header", format(resonance))
                .withStyle(ChatFormatting.GRAY));
        out.addAll(lines);
        return out;
    }

    /** Wie Vanillas Attributzahlen: hoechstens zwei Nachkommastellen, ohne Nullen am Ende. */
    public static String format(float value) {
        String s = String.format(Locale.ROOT, "%.2f", value);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }
}
