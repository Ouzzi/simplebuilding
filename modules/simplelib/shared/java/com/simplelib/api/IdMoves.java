package com.simplelib.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplelib.SimpleLib;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

/**
 * Moved ids (split plan stage 3a): one table, {@code old id -> new id} per registry, that every Simple mod feeds for the
 * ids it renamed or took over from another mod. It lives in SimpleLib because every sub-mod must be able to migrate a
 * world on its own.
 *
 * <p><b>How it works.</b> Every place that turns a saved id back into an object - block states in chunk palettes,
 * block entity ids, item stacks (chunks, containers, player data, bundles, item entities), data component types,
 * enchantments on items, entity types, tags and recipes of data packs, {@code /give} - asks the registry by name.
 * SimpleLib's registry mixins ({@code IdMovesRegistryMixin} on {@code MappedRegistry}, on Forge also
 * {@code NamespacedWrapperIdMovesMixin} plus Forge registry aliases for the explicit moves) answer a lookup that
 * <em>missed</em> with the object under its new id; the next save writes the new id. A DataFixer would not do: it only
 * runs when the Minecraft data version rises, never for a rename inside a mod on the same Minecraft version.
 * Two things are no registry lookups and get their own remap: the recipe book ({@code ServerRecipeBook#loadUntrusted},
 * table {@link Registries#RECIPE}) and advancement progress ({@code ServerAdvancementManager#get}, table
 * {@link Registries#ADVANCEMENT}).
 *
 * <p><b>Rules.</b> A move only applies when the old id is missing - while a mod still registers the old id, that entry
 * wins, so the old registration must go in the same commit that adds the new one. Moves never chain into a loop
 * ({@link #move} refuses one). Register moves in the mod constructor / initializer, i.e. before any world loads and,
 * on Forge, before the registry events (Forge aliases can only be added while a registry is open). Ids that exist cost
 * nothing; a missed lookup costs one map lookup.
 *
 * <p>Namespace moves ({@link #moveNamespace}) cover every path of a namespace at once (all registries, recipes and
 * advancements) but only through the mixins: Forge's defaulted registries answer {@code getValue}/{@code containsKey}
 * from their own alias table, which only explicit moves reach. Use explicit moves for real migrations.
 */
public final class IdMoves {
    private static final Map<ResourceKey<? extends Registry<?>>, Map<Identifier, Identifier>> MOVES = new ConcurrentHashMap<>();
    private static final List<String[]> NAMESPACES = new CopyOnWriteArrayList<>();

    private IdMoves() {}

    /** Moves {@code from} to {@code to} in {@code registry} (also {@link Registries#RECIPE} / {@link Registries#ADVANCEMENT}). */
    public static synchronized void move(ResourceKey<? extends Registry<?>> registry, Identifier from, Identifier to) {
        if (from.equals(to)) throw new IllegalArgumentException("id move onto itself: " + from);
        Map<Identifier, Identifier> table = MOVES.computeIfAbsent(registry, k -> new ConcurrentHashMap<>());
        Identifier existing = table.get(from);
        if (existing != null && !existing.equals(to)) {
            throw new IllegalArgumentException("id " + from + " in " + registry.identifier() + " already moves to " + existing + ", not " + to);
        }
        // Follow the chain from the new target: it must never lead back to the old id.
        Identifier step = to;
        for (int i = 0; step != null && i <= table.size(); i++) {
            if (step.equals(from)) throw new IllegalArgumentException("id move " + from + " -> " + to + " closes a loop in " + registry.identifier());
            step = table.get(step);
        }
        table.put(from, to);
    }

    /** {@link #move} for every entry of {@code moves}. */
    public static void moveAll(ResourceKey<? extends Registry<?>> registry, Map<Identifier, Identifier> moves) {
        moves.forEach((from, to) -> move(registry, from, to));
    }

    /** Every id of namespace {@code from} moves to the same path in {@code to} (see the class note on Forge). */
    public static synchronized void moveNamespace(String from, String to) {
        if (from.equals(to)) throw new IllegalArgumentException("namespace move onto itself: " + from);
        for (String[] rule : NAMESPACES) {
            if (rule[0].equals(to) && rule[1].equals(from)) throw new IllegalArgumentException("namespace move " + from + " -> " + to + " closes a loop");
            if (rule[0].equals(from)) {
                if (rule[1].equals(to)) return;
                throw new IllegalArgumentException("namespace " + from + " already moves to " + rule[1]);
            }
        }
        NAMESPACES.add(new String[] {from, to});
    }

    /** The new id for {@code id} in {@code registry}, or {@code null} if it never moved. */
    public static @Nullable Identifier target(ResourceKey<? extends Registry<?>> registry, @Nullable Identifier id) {
        if (id == null) return null;
        Map<Identifier, Identifier> table = MOVES.get(registry);
        Identifier now = table == null ? null : table.get(id);
        if (now != null) return now;
        for (String[] rule : NAMESPACES) {
            if (rule[0].equals(id.getNamespace())) return Identifier.fromNamespaceAndPath(rule[1], id.getPath());
        }
        return null;
    }

    /** {@link #target} for a registry instance (the mixins pass {@code this}); {@code null} for anything else. */
    public static @Nullable Identifier targetIn(Object registry, @Nullable Identifier id) {
        if (id == null || (MOVES.isEmpty() && NAMESPACES.isEmpty()) || !(registry instanceof Registry<?> r)) return null;
        return target(r.key(), id);
    }

    /** The moved key of {@code key} in its own registry ({@code ResourceKey<Recipe<?>>} for the recipe book), or {@code null}. */
    public static <T> @Nullable ResourceKey<T> target(@Nullable ResourceKey<T> key) {
        if (key == null) return null;
        Identifier now = target(key.registryKey(), key.identifier());
        return now == null ? null : ResourceKey.create(key.registryKey(), now);
    }

    /** The new id of a moved advancement, or {@code null}. */
    public static @Nullable Identifier advancement(@Nullable Identifier id) {
        return target(Registries.ADVANCEMENT, id);
    }

    /** The explicit moves of {@code registry} (a copy), e.g. for Forge registry aliases. */
    public static Map<Identifier, Identifier> explicit(ResourceKey<? extends Registry<?>> registry) {
        Map<Identifier, Identifier> table = MOVES.get(registry);
        return table == null ? Map.of() : Map.copyOf(table);
    }

    /**
     * Config takeover for a sub-mod that left a bigger mod: when {@code configDir/newFile} does not exist yet but
     * {@code configDir/oldFile} does, writes the top-level {@code keys} of the old JSON object (those present) to the
     * new file. The old file stays untouched. Returns whether a file was written. Call it before the sub-mod loads its
     * config.
     */
    public static boolean adoptConfig(Path configDir, String newFile, String oldFile, Collection<String> keys) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return com.simplebuilding.framework.api.ConfigFiles.adoptLegacy(configDir.resolve(newFile), configDir.resolve(oldFile), text -> {
            JsonElement parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) return null;
            JsonObject old = parsed.getAsJsonObject();
            JsonObject taken = new JsonObject();
            for (String key : keys) {
                if (old.has(key)) taken.add(key, old.get(key).deepCopy());
            }
            return taken.isEmpty() ? null : gson.toJson(taken);
        }, SimpleLib.LOG::info);
    }
}
