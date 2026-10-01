package com.simplebuilding.modules.simpledimensions;

import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceKey;

public final class ModuleForgeTests {
    public static boolean enabled() { return net.minecraftforge.gametest.ForgeGameTestHooks.isGametestServer(); }
    public static void register(net.minecraftforge.eventbus.api.bus.BusGroup bus) {
        if (enabled()) net.minecraftforge.registries.RegisterEvent.getBus(bus).addListener(e ->
                e.<Consumer<GameTestHelper>>register(Registries.TEST_FUNCTION, r -> DimensionTests.ALL.forEach((name, body) ->
                        r.register(DimensionRegistry.id("module_game_test_" + name), body))));
    }
    public static void instances(Registry<TestEnvironmentDefinition<?>> environments, Registry<GameTestInstance> instances) {
        net.minecraft.core.Holder<TestEnvironmentDefinition<?>> env = environments.getOrThrow(GameTestEnvironments.DEFAULT_KEY);
        DimensionTests.ALL.forEach((name, body) -> {
            var id = DimensionRegistry.id("module_game_test_" + name);
            var testEnv = name.startsWith("settings_") ? environments.getOrThrow(ResourceKey.create(
                    Registries.TEST_ENVIRONMENT, DimensionRegistry.id(name.equals("settings_persistence") ? "settings" : name))) : env;
            var data = new TestData<>(testEnv, net.minecraft.world.level.Level.OVERWORLD,
                    net.minecraft.resources.Identifier.parse("simpledimensions:empty"), name.startsWith("settings_") ? 420 : 240,
                    0, true, net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 1);
            Registry.register(instances, id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), data));
        });
    }
}
