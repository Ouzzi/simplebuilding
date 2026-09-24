package com.simplebuilding.forge.gametest;

import com.simplebuilding.gametest.GameTestSpec;
import com.simplebuilding.gametest.SimpleBuildingGameTests;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestEnvironments;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.gametest.ForgeGameTestHooks;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Forge adapter for the shared in-game test suite - the counterpart of {@code NeoForgeGameTests}.
 *
 * <p>Holds no test logic: every body comes from {@link SimpleBuildingGameTests}. Forge 65 has no
 * {@code RegisterGameTestsEvent}; its own gametest support ({@code ForgeGameTestHooks}) only reads
 * {@code @GameTest} annotations and nothing in Forge calls it. So the two registrations are done
 * by hand, the same two NeoForge does and the same two Fabric's gametest API does:
 *
 * <ol>
 *   <li>Each body goes into the built-in {@code minecraft:test_function} registry under
 *       {@code simplebuilding:<name>}, through Forge's {@link RegisterEvent}.</li>
 *   <li>Each test becomes a {@link FunctionGameTestInstance} in the data driven
 *       {@code minecraft:test_instance} registry. That registry is loaded from data packs, so the
 *       entries are added right before it is frozen - {@code RegistryLoadTaskMixin} calls
 *       {@link #registerInstances} there. Fabric's gametest API hooks the same moment.</li>
 * </ol>
 *
 * <p>Only in the gametest server run ({@code forge.gameTestServer}, set by vanilla's
 * {@code net.minecraft.gametest.Main}): the test instance registry is synchronised to clients, and
 * a player's client would not know these entries.
 *
 * <p>Padding and the empty 8x8x8 room are pinned to Fabric's defaults as on NeoForge, so the three
 * reports compare line by line; the room ships as {@code data/simplebuilding/structure/empty.nbt}
 * in this module too.
 */
public final class ForgeGameTests {

    private static final Identifier DEFAULT_STRUCTURE =
            Identifier.fromNamespaceAndPath(SimpleBuildingGameTests.MOD_ID, "empty");

    /** Fabric's {@code @GameTest} default; vanilla {@code TestData} would use 0. */
    private static final int PADDING = 1;

    private ForgeGameTests() {
    }

    /** True in the gametest server run only. */
    public static boolean enabled() {
        return ForgeGameTestHooks.isGametestServer();
    }

    public static void register(BusGroup modBus) {
        if (!enabled()) {
            return;
        }
        RegisterEvent.getBus(modBus).addListener(ForgeGameTests::registerTestFunctions);
    }

    private static void registerTestFunctions(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.TEST_FUNCTION)) {
            return;
        }
        event.<Consumer<GameTestHelper>>register(Registries.TEST_FUNCTION, helper -> {
            SimpleBuildingGameTests.forEach((name, spec) -> helper.register(id(name), spec.body()));
            ForgeOnlyGameTests.forEach((name, spec) -> helper.register(id(name), spec.body()));
        });
    }

    /**
     * Adds every test instance to the loading test instance registry, just before it is frozen.
     *
     * @param environments the already loaded (frozen) test environment registry
     */
    public static void registerInstances(Registry<TestEnvironmentDefinition<?>> environments,
            Registry<GameTestInstance> instances) {
        Holder<TestEnvironmentDefinition<?>> environment = environments.getOrThrow(GameTestEnvironments.DEFAULT_KEY);
        SimpleBuildingGameTests.forEach((name, spec) ->
                Registry.register(instances, id(name), new FunctionGameTestInstance(functionKey(name), testData(spec, environment))));
        ForgeOnlyGameTests.forEach((name, spec) ->
                Registry.register(instances, id(name), new FunctionGameTestInstance(functionKey(name), testData(spec, environment))));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            GameTestSpec spec, Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(
                environment,
                spec.structure() == null ? DEFAULT_STRUCTURE : Identifier.parse(spec.structure()),
                spec.maxTicks(),
                spec.setupTicks(),
                spec.required(),
                spec.rotation(),
                spec.manualOnly(),
                spec.maxAttempts(),
                spec.requiredSuccesses(),
                spec.skyAccess(),
                PADDING);
    }

    private static ResourceKey<Consumer<GameTestHelper>> functionKey(String name) {
        return ResourceKey.create(Registries.TEST_FUNCTION, id(name));
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(SimpleBuildingGameTests.MOD_ID, name);
    }
}
