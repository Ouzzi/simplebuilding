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
    public static @Nullable Holder<Potion> CRAFTY_SHULKER_POTION;
    public static @Nullable Holder<Potion> LONG_CRAFTY_SHULKER_POTION;

    private ModEffects() {
    }

    public static void registerEffects() {
        if (!McVersion.CRAFTY_SHULKER || CRAFTY_SHULKER != null) {
            return;
        }
        CRAFTY_SHULKER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, CRAFTY_SHULKER_ID,
                new CraftyShulkerEffect(CRAFTY_SHULKER_COLOR));
    }

    public static void registerPotions() {
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
