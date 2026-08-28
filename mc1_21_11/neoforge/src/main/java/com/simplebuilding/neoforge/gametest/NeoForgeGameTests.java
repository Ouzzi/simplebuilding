package com.simplebuilding.neoforge.gametest;

import com.simplebuilding.gametest.GameTestSpec;
import com.simplebuilding.gametest.SimpleBuildingGameTests;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge adapter for the shared in-game test suite of the MC 1.21.11 line.
 *
 * <p>Holds no test logic: every body comes from {@link SimpleBuildingGameTests}, the same
 * catalogue the Fabric adapter classes delegate to. This class only performs the two
 * registrations NeoForge needs:
 *
 * <ol>
 *   <li>Each test body goes into the built-in {@code minecraft:test_function} registry under
 *       {@code simplebuilding:<name>}. That registry is a plain {@code registerSimple} registry, so
 *       NeoForge's {@link RegisterEvent} (fired by {@code GameData#postRegisterEvents} for every
 *       entry of {@code BuiltInRegistries.REGISTRY}) is the way in. {@code TestFunctionLoader} is
 *       not usable here: nothing on NeoForge calls {@code TestFunctionLoader.runLoaders}.</li>
 *   <li>Each test is then registered as a {@link FunctionGameTestInstance} pointing at that key,
 *       through {@link RegisterGameTestsEvent} - the same instance type Fabric's annotation
 *       processor builds, so both loaders run structurally identical tests.</li>
 * </ol>
 *
 * <p>Two differences to the 26.2 copy of this class, both read off the 1.21.11 bytecode:
 * {@code TestEnvironmentDefinition} is not generic yet (26.2 has {@code TestEnvironmentDefinition<?>}),
 * and {@code TestData} has no {@code padding} component, so its widest constructor takes ten
 * arguments instead of eleven.
 *
 * <p>The empty 8x8x8 room Fabric supplies as {@code fabric-gametest-api-v1:empty} is shipped here as
 * {@code data/simplebuilding/structure/empty.nbt}.
 */
public final class NeoForgeGameTests {

    /** Empty 8x8x8 air room, the NeoForge counterpart of {@code fabric-gametest-api-v1:empty}. */
    private static final Identifier DEFAULT_STRUCTURE =
            Identifier.fromNamespaceAndPath(SimpleBuildingGameTests.MOD_ID, "empty");

    /**
     * Our own copy of {@code minecraft:default} (an {@code all_of} with no members).
     * {@link RegisterGameTestsEvent} hands out holders only for environments it registers itself,
     * so the suite brings its own no-op environment instead of looking one up.
     */
    private static final Identifier ENVIRONMENT =
            Identifier.fromNamespaceAndPath(SimpleBuildingGameTests.MOD_ID, "default");

    private NeoForgeGameTests() {
    }

    /** Hooks both registration passes onto the mod event bus. */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeGameTests::registerTestFunctions);
        modEventBus.addListener(NeoForgeGameTests::registerTestInstances);
    }

    private static void registerTestFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry ->
                SimpleBuildingGameTests.forEach((name, spec) -> registry.register(id(name), spec.body())));
    }

    private static void registerTestInstances(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment =
                event.registerEnvironment(ENVIRONMENT, new TestEnvironmentDefinition.AllOf(List.of()));

        SimpleBuildingGameTests.forEach((name, spec) ->
                event.registerTest(id(name), new FunctionGameTestInstance(functionKey(name), testData(spec, environment))));
    }

    private static TestData<Holder<TestEnvironmentDefinition>> testData(
            GameTestSpec spec, Holder<TestEnvironmentDefinition> environment) {
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
                spec.skyAccess());
    }

    private static ResourceKey<Consumer<GameTestHelper>> functionKey(String name) {
        return ResourceKey.create(Registries.TEST_FUNCTION, id(name));
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(SimpleBuildingGameTests.MOD_ID, name);
    }
}
