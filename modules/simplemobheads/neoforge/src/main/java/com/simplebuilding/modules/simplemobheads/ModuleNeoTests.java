package com.simplebuilding.modules.simplemobheads;

import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.test.MobHeadTests;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;

/** NeoForge adapter: the shared suite as {@code simplemobheads:module_game_test_<name>}. */
public final class ModuleNeoTests {
    private ModuleNeoTests() {
    }

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> event.register(Registries.TEST_FUNCTION,
                registry -> MobHeadTests.ALL.forEach((name, body) -> registry.register(SimpleMobHeads.id("module_game_test_" + name), body))));
        bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent event) -> {
            var environment = event.registerEnvironment(SimpleMobHeads.id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
            MobHeadTests.ALL.forEach((name, body) -> {
                var id = SimpleMobHeads.id("module_game_test_" + name);
                var data = new TestData<>(environment, net.minecraft.world.level.Level.OVERWORLD, SimpleMobHeads.id("empty"), 400, 0, true,
                        net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 1);
                event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), data));
            });
        });
    }
}
