package com.simplebuilding.effect;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * Eigene Effekte und Traenke (2026-10-02): der Listige Shulker ({@link CraftyShulkerEffect}) und seine Traenke.
 * Nur mit {@link McVersion#CRAFTY_SHULKER} (26.3); sonst bleiben die Holder {@code null}. Fabric registriert beim
 * Start, NeoForge/Forge im RegisterEvent der jeweiligen Registry (erst MOB_EFFECT, dann POTION). Gebraut wird
 * datengetrieben (26.3-Datagen {@code ModBrewingProvider}): Seltsamer Trank + Shulkerkopf, Redstone verlaengert,
 * Schwarzpulver/Drachenatem wie bei Vanilla.
 */
public final class ModEffects {
    public static final Identifier CRAFTY_SHULKER_ID = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "crafty_shulker");
    public static final Identifier LONG_CRAFTY_SHULKER_ID = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "long_crafty_shulker");
    /** Shulker-Lila (Trankfarbe). */
    public static final int CRAFTY_SHULKER_COLOR = 0xC08ED6;
    /** 3:00 wie die meisten Vanilla-Traenke, verlaengert 8:00. */
    public static final int DURATION = 3600;
    public static final int LONG_DURATION = 9600;
    /** Bloecke, auf oder in denen der Teleport nie landet (Feuer, Magma, Kaktus, Pulverschnee ...). */
    public static final TagKey<Block> UNSAFE_LANDING = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "crafty_shulker_unsafe"));

    public static @Nullable Holder<MobEffect> CRAFTY_SHULKER;
    /** Seelenbrand (Crucible P5, McVersion.CRUCIBLE): nach Kontakt mit Seelen-Lava. */
    public static final Identifier SOUL_BURN_ID = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "soul_burn");
    public static @Nullable Holder<MobEffect> SOUL_BURN;
    public static @Nullable Holder<Potion> CRAFTY_SHULKER_POTION;
    public static @Nullable Holder<Potion> LONG_CRAFTY_SHULKER_POTION;

    // Queue N20/N24 (McVersion.BREWING_EFFECTS): four perception effects and their potions, plus potions of the
    // vanilla effects darkness and nausea. Ingredients and durations: docs/ai/PLAN-BRAUEN-WERKBANK-2026-10-09.md.
    public static final int SHIVERING_COLOR = 0x9FD3EA;
    public static final int MIRAGE_COLOR = 0xE2B45A;
    public static final int REVERSE_MIRAGE_COLOR = 0x7E64C8;
    public static final int FADED_COLOR = 0x9A9A9A;
    /** Harmful potions like Weakness: 1:30, extended 4:00; Shivering II 0:45; Nausea 0:45 / 2:00. */
    public static final int HARMFUL_DURATION = 1800;
    public static final int LONG_HARMFUL_DURATION = 4800;
    public static final int STRONG_DURATION = 900;
    public static final int NAUSEA_DURATION = 900;
    public static final int LONG_NAUSEA_DURATION = 2400;
    public static @Nullable Holder<MobEffect> SHIVERING;
    public static @Nullable Holder<MobEffect> MIRAGE;
    public static @Nullable Holder<MobEffect> REVERSE_MIRAGE;
    public static @Nullable Holder<MobEffect> FADED;
    public static @Nullable Holder<Potion> DARKNESS_POTION;
    public static @Nullable Holder<Potion> LONG_DARKNESS_POTION;
    public static @Nullable Holder<Potion> NAUSEA_POTION;
    public static @Nullable Holder<Potion> LONG_NAUSEA_POTION;
    public static @Nullable Holder<Potion> SHIVERING_POTION;
    public static @Nullable Holder<Potion> LONG_SHIVERING_POTION;
    public static @Nullable Holder<Potion> STRONG_SHIVERING_POTION;
    public static @Nullable Holder<Potion> MIRAGE_POTION;
    public static @Nullable Holder<Potion> LONG_MIRAGE_POTION;
    public static @Nullable Holder<Potion> REVERSE_MIRAGE_POTION;
    public static @Nullable Holder<Potion> LONG_REVERSE_MIRAGE_POTION;
    public static @Nullable Holder<Potion> FADED_POTION;
    public static @Nullable Holder<Potion> LONG_FADED_POTION;

    private ModEffects() {
    }

    public static void registerEffects() {
        if (McVersion.CRUCIBLE && SOUL_BURN == null) {
            SOUL_BURN = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, SOUL_BURN_ID, new SoulBurnEffect(SoulBurnEffect.COLOR));
        }
        if (McVersion.BREWING_EFFECTS && SHIVERING == null) {
            SHIVERING = effect("shivering", SHIVERING_COLOR);
            MIRAGE = effect("mirage", MIRAGE_COLOR);
            REVERSE_MIRAGE = effect("reverse_mirage", REVERSE_MIRAGE_COLOR);
            FADED = effect("faded", FADED_COLOR);
        }
        if (!McVersion.CRAFTY_SHULKER || CRAFTY_SHULKER != null) {
            return;
        }
        CRAFTY_SHULKER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, CRAFTY_SHULKER_ID,
                new CraftyShulkerEffect(CRAFTY_SHULKER_COLOR));
    }

    private static Holder<MobEffect> effect(String name, int color) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name),
                new PerceptionEffect(color));
    }

    /** Like vanilla: every variant of a potion has the same name (translation key), only id and effect differ. */
    private static Holder<Potion> potion(String id, String name, Holder<MobEffect> effect, int duration, int amplifier) {
        return Registry.registerForHolder(BuiltInRegistries.POTION, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, id),
                new Potion(name, new MobEffectInstance(effect, duration, amplifier)));
    }

    public static void registerPotions() {
        if (SHIVERING != null && SHIVERING_POTION == null) {
            DARKNESS_POTION = potion("darkness", "darkness", MobEffects.DARKNESS, HARMFUL_DURATION, 0);
            LONG_DARKNESS_POTION = potion("long_darkness", "darkness", MobEffects.DARKNESS, LONG_HARMFUL_DURATION, 0);
            NAUSEA_POTION = potion("nausea", "nausea", MobEffects.NAUSEA, NAUSEA_DURATION, 0);
            LONG_NAUSEA_POTION = potion("long_nausea", "nausea", MobEffects.NAUSEA, LONG_NAUSEA_DURATION, 0);
            SHIVERING_POTION = potion("shivering", "shivering", SHIVERING, HARMFUL_DURATION, 0);
            LONG_SHIVERING_POTION = potion("long_shivering", "shivering", SHIVERING, LONG_HARMFUL_DURATION, 0);
            STRONG_SHIVERING_POTION = potion("strong_shivering", "shivering", SHIVERING, STRONG_DURATION, 1);
            MIRAGE_POTION = potion("mirage", "mirage", MIRAGE, HARMFUL_DURATION, 0);
            LONG_MIRAGE_POTION = potion("long_mirage", "mirage", MIRAGE, LONG_HARMFUL_DURATION, 0);
            REVERSE_MIRAGE_POTION = potion("reverse_mirage", "reverse_mirage", REVERSE_MIRAGE, HARMFUL_DURATION, 0);
            LONG_REVERSE_MIRAGE_POTION = potion("long_reverse_mirage", "reverse_mirage", REVERSE_MIRAGE, LONG_HARMFUL_DURATION, 0);
            FADED_POTION = potion("faded", "faded", FADED, HARMFUL_DURATION, 0);
            LONG_FADED_POTION = potion("long_faded", "faded", FADED, LONG_HARMFUL_DURATION, 0);
        }
        if (CRAFTY_SHULKER == null || CRAFTY_SHULKER_POTION != null) {
            return;
        }
        // Wie Vanilla: beide Stufen heissen "crafty_shulker" (Name = Uebersetzungsschluessel), nur die Dauer unterscheidet sich.
        CRAFTY_SHULKER_POTION = Registry.registerForHolder(BuiltInRegistries.POTION, CRAFTY_SHULKER_ID,
                new Potion("crafty_shulker", new MobEffectInstance(CRAFTY_SHULKER, DURATION)));
        LONG_CRAFTY_SHULKER_POTION = Registry.registerForHolder(BuiltInRegistries.POTION, LONG_CRAFTY_SHULKER_ID,
                new Potion("crafty_shulker", new MobEffectInstance(CRAFTY_SHULKER, LONG_DURATION)));
    }
}
