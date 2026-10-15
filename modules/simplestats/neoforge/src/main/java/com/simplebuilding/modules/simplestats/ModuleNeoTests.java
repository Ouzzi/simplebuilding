package com.simplebuilding.modules.simplestats;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceKey;

public final class ModuleNeoTests {
    private ModuleNeoTests() {}

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e) -> e.register(Registries.TEST_FUNCTION,
                r -> StatsTests.ALL.forEach((n, b) -> r.register(SimpleStats.id("module_game_test_" + n), b))));
        bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e) -> {
            var env = e.registerEnvironment(SimpleStats.id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
            StatsTests.ALL.forEach((n, b) -> {
                var id = SimpleStats.id("module_game_test_" + n);
                var data = new TestData<>(env, net.minecraft.world.level.Level.OVERWORLD, SimpleStats.id("empty"), 100, 0, true,
                        net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 1);
                e.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), data));
            });
        });
    }
}
