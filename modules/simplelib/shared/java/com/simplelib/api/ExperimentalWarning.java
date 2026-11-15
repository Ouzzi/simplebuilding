package com.simplelib.api;

import com.mojang.serialization.Lifecycle;
import com.simplelib.SimpleLib;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * EXP52 = A (owner 15.11., docs/ai/PLAN-B-EXP61-2026-11-15.md): Vanilla calls a world "experimental" as soon as it has a
 * LevelStem that is not Vanilla's (Simple Layers' backrooms_stack and cloud_sea, Simple Vault's bedrock_vault) and asks the
 * player before creating or opening it. The client skips that question only when every registry entry that makes the world
 * experimental belongs to a Simple mod; foreign mods, changed Vanilla dimensions and real experiments still ask. Only the
 * question changes - the lifecycle saved with the world stays experimental.
 *
 * <p>"Our" namespaces are the module ids of the repository ({@code creative_tab_order.json}, generated from
 * {@code modules/modules.json}, read through {@link CreativeTabPolicy#order()}), not a hand-kept list. Unreadable list: empty
 * set, so the warning stays.
 */
public final class ExperimentalWarning {
    private static final Set<String> VANILLA_DIMENSIONS = Set.of(LevelStem.OVERWORLD.identifier().toString(),
            LevelStem.NETHER.identifier().toString(), LevelStem.END.identifier().toString());

    private ExperimentalWarning() {}

    /** What makes a set of registries unstable: ids of experimental entries, and whether anything else does. */
    public record Scan(List<String> experimentalIds, boolean otherReason) {}

    /** The module ids of the Simple mods (all of the repository, loaded or not). */
    public static Set<String> ownNamespaces() {
        return Set.copyOf(CreativeTabPolicy.order());
    }

    /** Namespace of an id string; without a colon it is Vanilla's (as {@code Identifier.parse} reads it). */
    public static String namespace(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? "minecraft" : id.substring(0, colon);
    }

    /**
     * The decision (pure): warn when something besides experimental entries is unstable, or when one experimental entry is not
     * of a Simple mod. No unstable entry at all: no warning (a stable world never asks anyway).
     */
    public static boolean shouldWarn(Collection<String> experimentalIds, boolean otherReason, Set<String> ownNamespaces) {
        if (otherReason) return true;
        for (String id : experimentalIds) {
            if (!ownNamespaces.contains(namespace(id))) return true;
        }
        return false;
    }

    /** Vanilla's own rule for the dimension registry: without Overworld, Nether and End it is experimental from the start. */
    public static boolean missingVanillaDimension(Collection<String> stemIds) {
        return !stemIds.containsAll(VANILLA_DIMENSIONS);
    }

    /** Collects what makes these registries unstable (only registries whose lifecycle is not stable are looked at). */
    public static Scan scan(Stream<? extends Registry<?>> registries) {
        List<String> ids = new ArrayList<>();
        boolean[] other = {false};
        registries.forEach(registry -> {
            Registry<?> r = registry;
            collect(r, ids, other);
        });
        return new Scan(List.copyOf(ids), other[0]);
    }

    private static <T> void collect(Registry<T> registry, List<String> ids, boolean[] other) {
        if (registry.registryLifecycle() == Lifecycle.stable()) return;
        boolean unstableEntry = false;
        for (ResourceKey<T> key : registry.registryKeySet()) {
            Lifecycle lifecycle = registry.registrationInfo(key).map(RegistrationInfo::lifecycle).orElse(Lifecycle.experimental());
            if (lifecycle == Lifecycle.stable()) continue;
            unstableEntry = true;
            if (lifecycle == Lifecycle.experimental()) ids.add(key.identifier().toString());
            else other[0] = true; // deprecated
        }
        // unstable without an unstable entry: the registry itself was created unstable
        if (!unstableEntry) other[0] = true;
        if (registry.key().equals(Registries.LEVEL_STEM)
                && missingVanillaDimension(registry.registryKeySet().stream().map(k -> k.identifier().toString()).toList())) {
            other[0] = true;
        }
    }

    /**
     * Whether the experimental warning for these registries may be skipped (every unstable entry is a Simple mod's). Logs the
     * decision once per call ({@code what} = "create" / "open"), so a probe or a bug report can see why.
     */
    public static boolean ownOnly(RegistryAccess access, String what) {
        Scan scan = scan(access.registries().map(RegistryAccess.RegistryEntry::value));
        if (scan.experimentalIds().isEmpty() && !scan.otherReason()) return false; // stable: Vanilla does not ask, nothing to skip
        Set<String> own = ownNamespaces();
        boolean warn = shouldWarn(scan.experimentalIds(), scan.otherReason(), own);
        if (!warn) {
            SimpleLib.LOG.info("Experimental warning skipped ({}): only Simple mod entries {}", what, abbreviated(scan.experimentalIds()));
        } else {
            SimpleLib.LOG.info("Experimental warning kept ({}): other={}, foreign {}", what, scan.otherReason(),
                    abbreviated(scan.experimentalIds().stream().filter(id -> !own.contains(namespace(id))).toList()));
        }
        return !warn;
    }

    private static String abbreviated(List<String> ids) {
        return ids.size() <= 8 ? ids.toString() : ids.stream().limit(8).collect(Collectors.joining(", ", "[", ", ... +" + (ids.size() - 8) + "]"));
    }
}
