package com.simplebuilding.modules.simplefarming;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceKey;

public final class ModuleNeoTests {
    private ModuleNeoTests() {}

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e) -> e.register(Registries.TEST_FUNCTION,
                r -> FarmingTests.ALL.forEach((n, b) -> r.register(FarmingTests.id("module_game_test_" + n), b))));
        bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e) -> {
            var env = e.registerEnvironment(FarmingTests.id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
            FarmingTests.ALL.forEach((n, b) -> {
                var id = FarmingTests.id("module_game_test_" + n);
                var data = new TestData<>(env, net.minecraft.world.level.Level.OVERWORLD, FarmingTests.id("empty"), 100, 0, true,
                        net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 1);
                e.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), data));
            });
        });
    }
}
