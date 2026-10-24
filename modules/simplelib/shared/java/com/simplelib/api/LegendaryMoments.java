package com.simplelib.api;

import com.simplelib.SimpleLib;
import com.simplelib.config.LibConfig;
import com.simplelib.registry.LibSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Legendary moments (owner SP22-2, 2026-10-22, docs/ai/PLAN-A-LEGEND22-2026-10-22.md): finishing one of the hardest
 * advancements or making an item with one of the hardest enchantments at its maximum level plays the fanfare
 * {@code simplelib:legendary} at the player and a two-second particle show around them, seen and heard by everyone near.
 * No text on screen (rule 3.4). Server side only; switch {@code legendaryEffects} in {@code simplelib-server.json}.
 *
 * <p><b>Advancements</b> (no registry, so no tag): the built-in Vanilla collect-everything challenges {@link #DEFAULT_ADVANCEMENTS},
 * the config list {@code legendaryAdvancements}, ids a mod adds with {@link #addAdvancement}, and with
 * {@code legendaryAllChallenges} every advancement with the challenge frame. Detected where Vanilla grants the rewards,
 * i.e. exactly once when the advancement becomes done ({@code LegendaryAdvancementMixin}).
 *
 * <p><b>Enchantments:</b> tag {@code #simplelib:legendary} (Vanilla treasure enchantments that are neither traded nor in
 * random loot: Swift Sneak, Soul Speed, Wind Burst; mods add theirs to the tag) plus the config list
 * {@code legendaryEnchantments}. They count when the player makes the item - an enchanting table (or any mod table that fires
 * {@code minecraft:enchanted_item}) or taking an anvil result - and the result carries the enchantment at its maximum level
 * while the input did not. Picking up loot never counts.
 */
public final class LegendaryMoments {
    public static final TagKey<Enchantment> ENCHANTMENTS = TagKey.create(Registries.ENCHANTMENT, SimpleLib.id("legendary"));
    /** The Vanilla challenges that ask for everything of a kind (all effects, biomes, mobs, foods ...). */
    public static final List<Identifier> DEFAULT_ADVANCEMENTS = List.of(
            vanilla("nether/all_effects"), vanilla("nether/all_potions"), vanilla("adventure/adventuring_time"),
            vanilla("adventure/kill_all_mobs"), vanilla("husbandry/balanced_diet"), vanilla("husbandry/bred_all_animals"),
            vanilla("husbandry/complete_catalogue"), vanilla("husbandry/whole_pack"),
            vanilla("adventure/trim_with_all_exclusive_armor_patterns"), vanilla("husbandry/obtain_netherite_hoe"),
            vanilla("nether/explore_nether"));
    /** Length of the particle show in ticks. */
    public static final int SHOW_TICKS = 40;

    private static final Set<Identifier> ADDED = ConcurrentHashMap.newKeySet();
    private static final List<Show> SHOWS = new ArrayList<>();
    /** Celebrations per player, read by the GameTests. */
    private static final Map<ServerPlayer, Integer> COUNTS = new WeakHashMap<>();

    private record Show(UUID player, int[] age) {}

    private static Identifier vanilla(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    /** A mod marks one of its advancements as legendary (call during init; the id may name any namespace). */
    public static void addAdvancement(Identifier advancement) {
        ADDED.add(advancement);
    }

    public static boolean isLegendary(AdvancementHolder holder) {
        if (!LibConfig.legendaryEffects) return false;
        Identifier id = holder.id();
        if (DEFAULT_ADVANCEMENTS.contains(id) || ADDED.contains(id) || LibConfig.legendaryAdvancements.contains(id)) return true;
        return LibConfig.legendaryAllChallenges
                && holder.value().display().map(d -> d.type() == AdvancementType.CHALLENGE).orElse(false);
    }

    /** Whether this enchantment at this level is a legendary one (in the tag or the config list, at least its maximum level). */
    public static boolean isLegendary(Holder<Enchantment> enchantment, int level) {
        if (!LibConfig.legendaryEffects || level < enchantment.value().getMaxLevel()) return false;
        return enchantment.is(ENCHANTMENTS)
                || enchantment.unwrapKey().map(k -> LibConfig.legendaryEnchantments.contains(k.identifier())).orElse(false);
    }

    /**
     * W24-3: whether Vanilla's challenge toast of this advancement stays silent ({@code LegendaryToastMixin}, client) -
     * switch {@code legendaryMuteToast} and only where the legendary choir plays instead.
     */
    public static boolean muteToast(AdvancementHolder holder) {
        return LibConfig.legendaryMuteToast && isLegendary(holder);
    }

    /** Called when an advancement just became done for the player. */
    public static void advancementDone(ServerPlayer player, AdvancementHolder holder) {
        if (isLegendary(holder)) celebrate(player);
    }

    /**
     * The player made {@code result} out of {@code input} (enchanting table: input empty, anvil: the left slot). Celebrates
     * once when the result carries a legendary enchantment at its maximum level that the input did not have at that level.
     */
    public static void enchanted(ServerPlayer player, ItemStack input, ItemStack result) {
        if (!LibConfig.legendaryEffects || result.isEmpty()) return;
        var before = EnchantmentHelper.getEnchantmentsForCrafting(input);
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(result).entrySet()) {
            int level = entry.getIntValue();
            if (isLegendary(entry.getKey(), level) && before.getLevel(entry.getKey()) < level) {
                celebrate(player);
                return;
            }
        }
    }

    /** Fanfare at the player and the particle show; public for mods that grant something legendary their own way. */
    public static void celebrate(ServerPlayer player) {
        if (!LibConfig.legendaryEffects) return;
        synchronized (COUNTS) {
            COUNTS.merge(player, 1, Integer::sum);
        }
        if (LibSounds.LEGENDARY != null) {
            player.level().playSound(null, player.getX(), player.getY() + 1, player.getZ(), LibSounds.LEGENDARY, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        var level = player.level();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 60, 0.4, 0.6, 0.4, 0.55);
        synchronized (SHOWS) {
            SHOWS.add(new Show(player.getUUID(), new int[] {0}));
        }
    }

    /** How often this player was celebrated since the server started (GameTests). */
    public static int celebrations(ServerPlayer player) {
        synchronized (COUNTS) {
            return COUNTS.getOrDefault(player, 0);
        }
    }

    /**
     * Every server tick: two golden spirals ({@code end_rod}) climb from the feet over the head, glyphs ({@code enchant})
     * stream to the player, and a ring of sparks ({@code firework}) closes the show.
     */
    public static void tick(MinecraftServer server) {
        List<Show> shows;
        synchronized (SHOWS) {
            if (SHOWS.isEmpty()) return;
            shows = new ArrayList<>(SHOWS);
        }
        List<Show> done = new ArrayList<>();
        for (Show show : shows) {
            int age = show.age()[0]++;
            ServerPlayer player = server.getPlayerList().getPlayer(show.player());
            if (player == null || age >= SHOW_TICKS) {
                done.add(show);
                continue;
            }
            var level = player.level();
            double rise = (double) age / SHOW_TICKS;
            double y = player.getY() + 0.1 + rise * 2.4;
            double radius = 0.9 - rise * 0.45;
            for (int strand = 0; strand < 2; strand++) {
                double angle = age * 0.55 + strand * Math.PI;
                level.sendParticles(ParticleTypes.END_ROD, player.getX() + Math.cos(angle) * radius, y, player.getZ() + Math.sin(angle) * radius,
                        1, 0, 0, 0, 0);
            }
            if (age % 4 == 0) {
                level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.2, player.getZ(), 12, 1.4, 0.8, 1.4, 0.6);
            }
            if (age == SHOW_TICKS - 1) {
                for (int i = 0; i < 16; i++) {
                    double angle = i * Math.PI / 8;
                    level.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 2.5, player.getZ(), 0,
                            Math.cos(angle), 0.15, Math.sin(angle), 0.18);
                }
            }
        }
        if (!done.isEmpty()) {
            synchronized (SHOWS) {
                SHOWS.removeAll(done);
            }
        }
    }

    private LegendaryMoments() {}
}
