package com.simplebuilding.forge.gametest;

import com.simplebuilding.gametest.GameTestSpec;
import com.simplebuilding.gametest.SimpleBuildingGameTests;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
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
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FormattedCharSequence;
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
        injectModEnglish();
    }

    /**
     * The Forge gametest server knows only vanilla's English, while the Fabric and NeoForge servers
     * also load the mod's {@code en_us.json}. Tests that read tooltip or message text then saw bare
     * keys on Forge only. Falls back to the mod's English for keys vanilla does not have.
     */
    private static void injectModEnglish() {
        Map<String, String> modEnglish = new HashMap<>();
        try (InputStream in = ForgeGameTests.class.getResourceAsStream("/assets/simplebuilding/lang/en_us.json")) {
            if (in == null) {
                return;
            }
            Language.loadFromJson(in, modEnglish::put);
        } catch (IOException e) {
            return;
        }
        Language vanilla = Language.getInstance();
        Language.inject(new Language() {
            @Override
            public String getOrDefault(String key, String defaultValue) {
                return vanilla.has(key) ? vanilla.getOrDefault(key, defaultValue) : modEnglish.getOrDefault(key, defaultValue);
            }

            @Override
            public boolean has(String key) {
                return vanilla.has(key) || modEnglish.containsKey(key);
            }

            @Override
            public boolean isDefaultRightToLeft() {
                return vanilla.isDefaultRightToLeft();
            }

            @Override
            public FormattedCharSequence getVisualOrder(FormattedText text) {
                return vanilla.getVisualOrder(text);
            }
        });
    }

    private static void registerTestFunctions(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.TEST_FUNCTION)) {
            return;
        }
        event.<Consumer<GameTestHelper>>register(Registries.TEST_FUNCTION, helper -> {
            SimpleBuildingGameTests.forEach((name, spec) -> helper.register(id(name), test -> {
                if (name.equals("config_option_game_test_every_config_option_keeps_its_persisted_name_and_default")) ForgeConfigChecks.verify(test);
                spec.body().accept(test);
            }));
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
        return com.simplebuilding.version.McVersion.testData(
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
