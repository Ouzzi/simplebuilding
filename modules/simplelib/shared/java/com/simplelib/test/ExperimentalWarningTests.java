package com.simplelib.test;

import com.mojang.serialization.Lifecycle;
import com.simplelib.api.ExperimentalWarning;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * EXP52 = A (docs/ai/PLAN-B-EXP61-2026-11-15.md): the decision behind skipping the "experimental settings" question - only
 * experimental entries of Simple mods are skipped; foreign mods, changed Vanilla dimensions, deprecated entries, a missing
 * Vanilla dimension and an unreadable mod list keep it. Pure logic plus the collector on a small hand-made registry.
 */
final class ExperimentalWarningTests {
    private ExperimentalWarningTests() {}

    static void register() {
        LibTests.ALL.put("experimental_warning_own_dimensions", ExperimentalWarningTests::ownDimensions);
    }

    static void ownDimensions(GameTestHelper h) {
        Set<String> own = ExperimentalWarning.ownNamespaces();
        h.assertTrue(own.containsAll(List.of("simplelib", "simplelayers", "simplevault", "simplebuilding")),
                "own namespaces come from the module list: " + own);
        h.assertTrue(!own.contains("minecraft") && !own.contains("c"), "Vanilla and common namespaces are not own: " + own);

        List<String> ours = List.of("simplelayers:backrooms_stack", "simplelayers:cloud_sea", "simplevault:bedrock_vault");
        h.assertTrue(!ExperimentalWarning.shouldWarn(ours, false, own), "only Simple dimensions: no warning");
        h.assertTrue(!ExperimentalWarning.shouldWarn(List.of(), false, own), "nothing unstable: no warning");
        h.assertTrue(ExperimentalWarning.shouldWarn(append(ours, "othermod:moon"), false, own), "a foreign dimension warns");
        h.assertTrue(ExperimentalWarning.shouldWarn(List.of("minecraft:overworld"), false, own), "a changed Vanilla dimension warns");
        h.assertTrue(ExperimentalWarning.shouldWarn(List.of("overworld"), false, own), "an id without namespace is Vanilla's and warns");
        h.assertTrue(ExperimentalWarning.shouldWarn(ours, true, own), "another reason (deprecated, missing dimension) warns");
        h.assertTrue(ExperimentalWarning.shouldWarn(ours, false, Set.of()), "unreadable mod list: warns");
        h.assertTrue(ExperimentalWarning.shouldWarn(List.of("simplelayersx:void"), false, own), "a look-alike namespace warns");

        h.assertTrue(!ExperimentalWarning.missingVanillaDimension(
                List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end", "simplevault:bedrock_vault")), "all three Vanilla stems");
        h.assertTrue(ExperimentalWarning.missingVanillaDimension(List.of("minecraft:overworld", "minecraft:the_end")), "no Nether: unstable");

        // the collector on a hand-made registry (own test key, plain values: no loader hooks on real registries)
        ResourceKey<Registry<Object>> key = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("simplelib", "experimental_warning_test"));
        MappedRegistry<Object> stable = new MappedRegistry<>(key, Lifecycle.stable());
        stable.register(ResourceKey.create(key, Identifier.withDefaultNamespace("overworld")), new Object(), RegistrationInfo.BUILT_IN);
        ExperimentalWarning.Scan none = ExperimentalWarning.scan(Stream.of(stable));
        h.assertTrue(none.experimentalIds().isEmpty() && !none.otherReason(), "stable registry: nothing collected " + none);

        MappedRegistry<Object> mixed = new MappedRegistry<>(key, Lifecycle.stable());
        RegistrationInfo experimental = new RegistrationInfo(java.util.Optional.empty(), Lifecycle.experimental());
        mixed.register(ResourceKey.create(key, Identifier.withDefaultNamespace("overworld")), new Object(), RegistrationInfo.BUILT_IN);
        mixed.register(ResourceKey.create(key, Identifier.fromNamespaceAndPath("simplelayers", "cloud_sea")), new Object(), experimental);
        ExperimentalWarning.Scan scan = ExperimentalWarning.scan(Stream.of(mixed));
        h.assertTrue(scan.experimentalIds().equals(List.of("simplelayers:cloud_sea")) && !scan.otherReason(), "own entry collected " + scan);
        h.assertTrue(!ExperimentalWarning.shouldWarn(scan.experimentalIds(), scan.otherReason(), own), "own registry entry: no warning");

        mixed.register(ResourceKey.create(key, Identifier.fromNamespaceAndPath("othermod", "moon")), new Object(), experimental);
        mixed.register(ResourceKey.create(key, Identifier.fromNamespaceAndPath("simplevault", "old")), new Object(),
                new RegistrationInfo(java.util.Optional.empty(), Lifecycle.deprecated(1)));
        scan = ExperimentalWarning.scan(Stream.of(mixed));
        h.assertTrue(scan.experimentalIds().contains("othermod:moon") && scan.otherReason(), "foreign + deprecated collected " + scan);
        h.assertTrue(ExperimentalWarning.shouldWarn(scan.experimentalIds(), scan.otherReason(), own), "foreign / deprecated entry: warns");

        // created unstable although every entry is stable: "another reason", warns
        MappedRegistry<Object> bornUnstable = new MappedRegistry<>(key, Lifecycle.experimental());
        bornUnstable.register(ResourceKey.create(key, Identifier.withDefaultNamespace("overworld")), new Object(), RegistrationInfo.BUILT_IN);
        ExperimentalWarning.Scan born = ExperimentalWarning.scan(Stream.of(bornUnstable));
        h.assertTrue(born.experimentalIds().isEmpty() && born.otherReason(), "registry created unstable counts as another reason " + born);
        h.succeed();
    }

    private static List<String> append(List<String> list, String id) {
        return Stream.concat(list.stream(), Stream.of(id)).toList();
    }
}
