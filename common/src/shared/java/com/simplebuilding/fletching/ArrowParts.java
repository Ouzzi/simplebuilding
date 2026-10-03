package com.simplebuilding.fletching;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.items.ModItems;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Die drei Teile eines Pfeils vom Befiederungstisch (Besitzer 2026-10-01, Balance freigegeben): Spitze, Schaft,
 * Befiederung. Jede Zahl hier ist fest (keine Config), damit nichts ausnutzbar ist; docs/ai/PLAN-B14-FLETCHING.md.
 */
public final class ArrowParts {
    private ArrowParts() {
    }

    /** Spitze: Material, Schadensbonus und Bonus gegen eine Zielgruppe. */
    public enum Tip implements StringRepresentable {
        FLINT("flint", () -> Items.FLINT, 0.0, Target.NONE, 0.0),
        COPPER("copper", () -> Items.COPPER_NUGGET, 0.0, Target.DROWNED, 2.0),
        IRON("iron", () -> Items.IRON_NUGGET, 0.5, Target.ZOMBIES, 2.0),
        GOLD("gold", () -> Items.GOLD_NUGGET, 0.0, Target.UNDEAD, 2.0),
        DIAMOND("diamond", () -> ModItems.DIAMOND_PEBBLE, 1.0, Target.NONE, 0.0),
        NETHERITE("netherite", () -> ModItems.NETHERITE_NUGGET, 1.5, Target.NONE, 0.0),
        ENDERITE("enderite", () -> ModItems.ENDERITE_NUGGET, 1.5, Target.NONE, 0.0),
        AMETHYST("amethyst", () -> Items.AMETHYST_SHARD, 0.0, Target.NONE, 0.0),
        PRISMARINE("prismarine", () -> Items.PRISMARINE_SHARD, 0.0, Target.NONE, 0.0);

        private final String id;
        private final Supplier<Item> input;
        public final double damageBonus;
        private final Target target;
        public final double targetBonus;

        Tip(String id, Supplier<Item> input, double damageBonus, Target target, double targetBonus) {
            this.id = id;
            this.input = input;
            this.damageBonus = damageBonus;
            this.target = target;
            this.targetBonus = targetBonus;
        }

        public Item input() {
            return input.get();
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
     * nur mit {@code McVersion.GADGET_REWORK}) durchbohrt ein Ziel mehr. Netherit- und Enderitstab sind seit 2026-10-03
     * Blitzableiter-Bloecke und keine Schaefte mehr; alte Pfeile mit diesen Schaeften liest {@link #byName} als Stock.
     */
    public enum Shaft implements StringRepresentable {
        STICK("stick", () -> Items.STICK, 1.0, 0.0, 0),
        END_ROD("end_rod", () -> Items.END_ROD, 0.5, 0.0, 0),
        BLAZE_ROD("blaze_rod", () -> Items.BLAZE_ROD, 1.0, 0.0, 0),
        BREEZE_ROD("breeze_rod", () -> Items.BREEZE_ROD, 1.0, 0.0, 0),
        DIAMOND_ROD("diamond_rod", () -> ModItems.DIAMOND_ROD, 1.0, 0.0, 1);

        /**
         * Liest den Schaft tolerant: ein unbekannter Name - etwa {@code netherite_rod}/{@code enderite_rod} aus Pfeilen,
         * die vor 2026-10-03 hergestellt wurden - wird zum Stock, damit der Pfeil-Stapel erhalten bleibt.
         */
        public static final Codec<Shaft> CODEC = Codec.STRING.xmap(Shaft::byName, Shaft::getSerializedName);

        private final String id;
        private final Supplier<Item> input;
        public final double gravity;
        public final double damageBonus;
        public final int extraPierce;

        Shaft(String id, Supplier<Item> input, double gravity, double damageBonus, int extraPierce) {
            this.id = id;
            this.input = input;
            this.gravity = gravity;
            this.damageBonus = damageBonus;
            this.extraPierce = extraPierce;
        }

        /** Der Schaft mit diesem Namen; unbekannte Namen ergeben den Stock. */
        public static Shaft byName(String id) {
            for (Shaft shaft : values()) if (shaft.id.equals(id)) return shaft;
            return STICK;
        }

        public Item input() {
            return input.get();
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /** Befiederung: Material und Schwerkraft-Faktor. */
    public enum Fletching implements StringRepresentable {
        FEATHER("feather", () -> Items.FEATHER, 1.0),
        PHANTOM_MEMBRANE("phantom_membrane", () -> Items.PHANTOM_MEMBRANE, 0.7);

        private final String id;
        private final Supplier<Item> input;
        public final double gravity;

        Fletching(String id, Supplier<Item> input, double gravity) {
            this.id = id;
            this.input = input;
            this.gravity = gravity;
        }

        public Item input() {
            return input.get();
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    private enum Target {
        NONE, DROWNED, ZOMBIES, UNDEAD;

        /** Bei einer Trainingspuppe zaehlt die Art ihres Kopfes ({@code DummyTargets}). */
        boolean matches(Entity entity) {
            net.minecraft.world.entity.EntityType<?> type = com.simplebuilding.dummy.DummyTargets.effectiveType(entity);
            return switch (this) {
                case NONE -> false;
                case DROWNED -> type == net.minecraft.world.entity.EntityTypes.DROWNED;
                case ZOMBIES -> type.builtInRegistryHolder().is(EntityTypeTags.ZOMBIES);
                case UNDEAD -> type.builtInRegistryHolder().is(EntityTypeTags.UNDEAD);
            };
        }
    }

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

        /** Schwerkraft-Faktor aus Schaft und Befiederung. */
        public double gravityFactor() {
            return shaft.gravity * fletching.gravity;
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

    /** Die Teile eines Pfeil-Stapels; Vanilla-Teile, wenn keine gesetzt sind. */
    public static Parts of(ItemStack stack) {
        return stack.getOrDefault(com.simplebuilding.component.ModDataComponentTypes.ARROW_PARTS, Parts.VANILLA);
    }

    /** Ein Stapel {@code count} Pfeile mit diesen Teilen, inklusive Modell-Auswahl. */
    public static ItemStack stack(Parts parts, int count) {
        ItemStack stack = new ItemStack(ModItems.CRAFTED_ARROW, count);
        stack.set(com.simplebuilding.component.ModDataComponentTypes.ARROW_PARTS, parts);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                new net.minecraft.world.item.component.CustomModelData(java.util.List.of(), java.util.List.of(), parts.modelStrings(), java.util.List.of()));
        return stack;
    }

    /** Alle Kombinationen, sortiert Spitze, Schaft, Befiederung (Kreativ-Tab und Suchtab). */
    public static java.util.List<Parts> allCombinations() {
        java.util.List<Parts> out = new java.util.ArrayList<>();
        for (Tip tip : Tip.values()) {
            for (Shaft shaft : Shaft.values()) {
                if (shaft.input() == null) {
                    continue;
                }
                for (Fletching fletching : Fletching.values()) {
                    out.add(new Parts(tip, shaft, fletching));
                }
            }
        }
        return out;
    }

    public static Tip tipFor(ItemStack stack) {
        for (Tip tip : Tip.values()) if (stack.is(tip.input())) return tip;
        return null;
    }

    public static Shaft shaftFor(ItemStack stack) {
        for (Shaft shaft : Shaft.values()) if (shaft.input() != null && stack.is(shaft.input())) return shaft;
        return null;
    }

    public static Fletching fletchingFor(ItemStack stack) {
        for (Fletching fletching : Fletching.values()) if (stack.is(fletching.input())) return fletching;
        return null;
    }
}
