package com.simplebuilding.modules.simplemobheads.forge;

import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.test.MobHeadTests;
import java.util.LinkedHashMap;
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
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** Forge adapter: the shared suite as {@code simplemobheads:module_game_test_<name>} (instances via {@code RegistryLoadTaskMixin}). */
public final class ModuleForgeTests {
    private ModuleForgeTests() {
    }

    public static boolean enabled() {
        return net.minecraftforge.gametest.ForgeGameTestHooks.isGametestServer();
    }

    public static Map<String, Consumer<GameTestHelper>> cases() {
        Map<String, Consumer<GameTestHelper>> out = new LinkedHashMap<>();
        MobHeadTests.ALL.forEach((name, body) -> out.put("module_game_test_" + name, body));
        return out;
    }

    public static void register(net.minecraftforge.eventbus.api.bus.BusGroup bus) {
        if (enabled()) {
            net.minecraftforge.registries.RegisterEvent.getBus(bus).addListener(event -> event.<Consumer<GameTestHelper>>register(
                    Registries.TEST_FUNCTION, registry -> cases().forEach((name, body) -> registry.register(SimpleMobHeads.id(name), body))));
        }
    }

    public static void instances(Registry<TestEnvironmentDefinition<?>> environments, Registry<GameTestInstance> instances) {
        Holder<TestEnvironmentDefinition<?>> environment = environments.getOrThrow(GameTestEnvironments.DEFAULT_KEY);
        var data = new TestData<>(environment, net.minecraft.world.level.Level.OVERWORLD, Identifier.parse("simplemobheads:empty"), 400, 0, true,
                net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 1);
        cases().forEach((name, body) -> Registry.register(instances, SimpleMobHeads.id(name),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, SimpleMobHeads.id(name)), data)));
    }
}
