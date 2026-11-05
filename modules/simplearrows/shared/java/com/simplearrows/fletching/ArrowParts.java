package com.simplearrows.fletching;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;

/**
 * Die drei Teile eines Pfeils vom Befiederungstisch (Besitzer 2026-10-01, Balance freigegeben): Spitze, Schaft,
 * Befiederung. Jede Zahl hier ist fest (keine Config), damit nichts ausnutzbar ist; docs/ai/PLAN-B14-FLETCHING.md.
 *
 * <p>Seit b-arrows03 (04.11., aus dem SB-Kern nach Simple Arrows) sind die Teile datengetrieben: welches Item ein Teil ist,
 * sagt sein Item-Tag {@code simplearrows:fletching/<art>/<teil>} (Vanilla-Items fest, Items anderer Mods {@code required:false}).
 * Ein Teil mit leerem Tag (Mod fehlt) gibt es in dieser Installation nicht ({@link #available}).
 */
public final class ArrowParts {
    private ArrowParts() {
    }

    /** Spitze: Material, Schadensbonus und Bonus gegen eine Zielgruppe. */
    public enum Tip implements StringRepresentable {
        FLINT("flint", 0.0, Target.NONE, 0.0),
        COPPER("copper", 0.0, Target.DROWNED, 2.0),
        IRON("iron", 0.5, Target.ZOMBIES, 2.0),
        GOLD("gold", 0.0, Target.UNDEAD, 2.0),
        DIAMOND("diamond", 1.0, Target.NONE, 0.0),
        NETHERITE("netherite", 1.5, Target.NONE, 0.0),
        ENDERITE("enderite", 1.5, Target.NONE, 0.0),
        AMETHYST("amethyst", 0.0, Target.NONE, 0.0),
        PRISMARINE("prismarine", 0.0, Target.NONE, 0.0),
        /** Lead nugget (Simple Ores): a heavy head, +0.5 damage like iron but no target bonus, and the arrow falls faster. Appended last: the wire codec uses the ordinal. */
        LEAD("lead", 0.5, Target.NONE, 0.0, LEAD_GRAVITY);

        private final String id;
        private final TagKey<Item> tag;
        public final double damageBonus;
        private final Target target;
        public final double targetBonus;

        /** Gravity factor of this head (1 = plain). */
        public final double gravity;

        Tip(String id, double damageBonus, Target target, double targetBonus) {
            this(id, damageBonus, target, targetBonus, 1.0);
        }

        Tip(String id, double damageBonus, Target target, double targetBonus, double gravity) {
            this.gravity = gravity;
            this.id = id;
            this.tag = partTag("tip", id);
            this.damageBonus = damageBonus;
            this.target = target;
            this.targetBonus = targetBonus;
        }

        public TagKey<Item> tag() {
            return tag;
        }

        /** Das erste Item des Tags (Anzeige), {@code null} wenn es in dieser Installation keins gibt. */
        public Item input() {
            return first(tag);
        }

        /** Zusaetzlicher Schaden gegen dieses Ziel (Grundbonus plus Zielbonus). */
        public double bonusAgainst(Entity entity) {
            return damageBonus + (target.matches(entity) ? targetBonus : 0.0);
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /**
     * Schaft: Material, Schwerkraft-Faktor, Schadensbonus und zusaetzliche Durchbohrung. Der Diamantstab (2026-10-02,
     * SimpleBuildings {@code diamond_rod}, nur mit SimpleBuilding) durchbohrt ein Ziel mehr. Netherit- und Enderitstab sind seit 2026-10-03
     * Blitzableiter-Bloecke und keine Schaefte mehr; alte Pfeile mit diesen Schaeften liest {@link #byName} als Stock.
     */
    public enum Shaft implements StringRepresentable {
        STICK("stick", 1.0, 0.0, 0),
        END_ROD("end_rod", 0.5, 0.0, 0),
        BLAZE_ROD("blaze_rod", 1.0, 0.0, 0),
        BREEZE_ROD("breeze_rod", 1.0, 0.0, 0),
        DIAMOND_ROD("diamond_rod", 1.0, 0.0, 1);

        /**
         * Liest den Schaft tolerant: ein unbekannter Name - etwa {@code netherite_rod}/{@code enderite_rod} aus Pfeilen,
         * die vor 2026-10-03 hergestellt wurden - wird zum Stock, damit der Pfeil-Stapel erhalten bleibt.
         */
        public static final Codec<Shaft> CODEC = Codec.STRING.xmap(Shaft::byName, Shaft::getSerializedName);

        private final String id;
        private final TagKey<Item> tag;
        public final double gravity;
        public final double damageBonus;
        public final int extraPierce;

        Shaft(String id, double gravity, double damageBonus, int extraPierce) {
            this.id = id;
            this.tag = partTag("shaft", id);
            this.gravity = gravity;
            this.damageBonus = damageBonus;
            this.extraPierce = extraPierce;
        }

        /** Der Schaft mit diesem Namen; unbekannte Namen ergeben den Stock. */
        public static Shaft byName(String id) {
            for (Shaft shaft : values()) if (shaft.id.equals(id)) return shaft;
            return STICK;
        }

        public TagKey<Item> tag() {
            return tag;
        }

        public Item input() {
            return first(tag);
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /** Befiederung: Material und Schwerkraft-Faktor. */
    public enum Fletching implements StringRepresentable {
        FEATHER("feather", 1.0),
        PHANTOM_MEMBRANE("phantom_membrane", 0.7);

        private final String id;
        private final TagKey<Item> tag;
        public final double gravity;

        Fletching(String id, double gravity) {
            this.id = id;
            this.tag = partTag("fletching", id);
            this.gravity = gravity;
        }

        public TagKey<Item> tag() {
            return tag;
        }

        public Item input() {
            return first(tag);
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    private enum Target {
        NONE, DROWNED, ZOMBIES, UNDEAD;

        /** Bei einer Trainingspuppe (SimpleBuilding) zaehlt die Art ihres Kopfes ({@link DummyHeads}). */
        boolean matches(Entity entity) {
            net.minecraft.world.entity.EntityType<?> type = DummyHeads.effectiveType(entity);
            return switch (this) {
                case NONE -> false;
                case DROWNED -> type == net.minecraft.world.entity.EntityTypes.DROWNED;
                case ZOMBIES -> type.builtInRegistryHolder().is(EntityTypeTags.ZOMBIES);
                case UNDEAD -> type.builtInRegistryHolder().is(EntityTypeTags.UNDEAD);
            };
        }
    }

    /** Blei-Spitze: Schwerkraft-Faktor (schwerer Kopf: faellt schneller; Phantomhaut mit 0,7 gleicht ihn fast aus). */
    public static final double LEAD_GRAVITY = 1.5;
    /** Ein Pfeil ist fuer die erste Sekunde ohne Schwerkraft (Enderit-Spitze). */
    public static final int ENDERITE_WEIGHTLESS_TICKS = 20;
    /** Lohenrute: so viele Ticks brennt das Ziel zusaetzlich zu Flamme. */
    public static final int BLAZE_EXTRA_FIRE_TICKS = 60;
    /** Amethyst-Spitze: Splitterschaden und Radius. */
    public static final float AMETHYST_SPLASH_DAMAGE = 1.0F;
    public static final double AMETHYST_SPLASH_RADIUS = 1.5;
    /** Prismarin-Spitze: Luftwiderstand statt Wasserwiderstand unter Wasser. */
    public static final float PRISMARINE_WATER_INERTIA = 0.99F;
    /** Netherit-Spitze: zusaetzliche Durchbohrung. */
    public static final int NETHERITE_EXTRA_PIERCE = 1;
    /** Ergebnis einer Herstellung, wie Vanillas Pfeilrezept. */
    public static final int ARROWS_PER_CRAFT = 4;

    /** Die Teile eines Pfeils. */
    public record Parts(Tip tip, Shaft shaft, Fletching fletching) {
        public static final Parts VANILLA = new Parts(Tip.FLINT, Shaft.STICK, Fletching.FEATHER);
        public static final Codec<Parts> CODEC = RecordCodecBuilder.create(i -> i.group(
                StringRepresentable.fromEnum(Tip::values).fieldOf("tip").forGetter(Parts::tip),
                Shaft.CODEC.fieldOf("shaft").forGetter(Parts::shaft),
                StringRepresentable.fromEnum(Fletching::values).fieldOf("fletching").forGetter(Parts::fletching)
        ).apply(i, Parts::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Parts> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.idMapper(i -> Tip.values()[i], Tip::ordinal), Parts::tip,
                ByteBufCodecs.idMapper(i -> Shaft.values()[i], Shaft::ordinal), Parts::shaft,
                ByteBufCodecs.idMapper(i -> Fletching.values()[i], Fletching::ordinal), Parts::fletching,
                Parts::new);

        /** Schwerkraft-Faktor aus Spitze, Schaft und Befiederung. */
        public double gravityFactor() {
            return tip.gravity * shaft.gravity * fletching.gravity;
        }

        /** Zusaetzlicher Schaden gegen dieses Ziel: Spitze (Grund- und Zielbonus) plus Schaft. */
        public double bonusAgainst(Entity entity) {
            return tip.bonusAgainst(entity) + shaft.damageBonus;
        }

        /** Zusaetzliche Durchbohrung aus Spitze (Netherit) und Schaft (Diamant). */
        public int extraPierce() {
            return (tip == Tip.NETHERITE ? NETHERITE_EXTRA_PIERCE : 0) + shaft.extraPierce;
        }

        /** Die Strings fuer die Item-Modell-Auswahl ({@code custom_model_data}): Spitze, Schaft, Befiederung. */
        public java.util.List<String> modelStrings() {
            return java.util.List.of(tip.getSerializedName(), shaft.getSerializedName(), fletching.getSerializedName());
        }
    }

    /** Item-Tag eines Teils, z. B. {@code simplearrows:fletching/tip/iron}. */
    static TagKey<Item> partTag(String kind, String id) {
        return TagKey.create(Registries.ITEM, com.simplearrows.SimpleArrows.id("fletching/" + kind + "/" + id));
    }

    /** Das erste Item eines Tags, {@code null} wenn er leer ist (oder noch nicht gebunden). */
    static Item first(TagKey<Item> tag) {
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            return holder.value();
        }
        return null;
    }

    /** Ob es das Teil in dieser Installation gibt (sein Tag hat ein Item). */
    public static boolean available(StringRepresentable part) {
        TagKey<Item> tag = switch (part) {
            case Tip tip -> tip.tag();
            case Shaft shaft -> shaft.tag();
            case Fletching fletching -> fletching.tag();
            default -> null;
        };
        return tag != null && first(tag) != null;
    }

    /** Die Teile eines Pfeil-Stapels; Vanilla-Teile, wenn keine gesetzt sind. */
    public static Parts of(ItemStack stack) {
        return stack.getOrDefault(Fletchings.ARROW_PARTS, Parts.VANILLA);
    }

    /** Ein Stapel {@code count} Pfeile mit diesen Teilen, inklusive Modell-Auswahl. */
    public static ItemStack stack(Parts parts, int count) {
        ItemStack stack = new ItemStack(Fletchings.CRAFTED_ARROW, count);
        stack.set(Fletchings.ARROW_PARTS, parts);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                new net.minecraft.world.item.component.CustomModelData(java.util.List.of(), java.util.List.of(), parts.modelStrings(), java.util.List.of()));
        return stack;
    }

    /**
     * Alle Kombinationen der Teile, die es in dieser Installation gibt, sortiert Spitze, Schaft, Befiederung (Kreativ-Tab,
     * Suchtab, Testzentrale). Erst nach dem Laden der Daten aufrufen (Tags).
     */
    public static java.util.List<Parts> allCombinations() {
        java.util.List<Parts> out = new java.util.ArrayList<>();
        for (Tip tip : Tip.values()) {
            if (!available(tip)) continue;
            for (Shaft shaft : Shaft.values()) {
                if (!available(shaft)) continue;
                for (Fletching fletching : Fletching.values()) {
                    if (available(fletching)) out.add(new Parts(tip, shaft, fletching));
                }
            }
        }
        return out;
    }

    public static Tip tipFor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (Tip tip : Tip.values()) if (stack.is(tip.tag())) return tip;
        return null;
    }

    public static Shaft shaftFor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (Shaft shaft : Shaft.values()) if (stack.is(shaft.tag())) return shaft;
        return null;
    }

    public static Fletching fletchingFor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (Fletching fletching : Fletching.values()) if (stack.is(fletching.tag())) return fletching;
        return null;
    }
}
