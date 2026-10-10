package com.simplebuilding.modules.simplemobs;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import java.util.List;
public final class ModuleNeoTests {
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("simplemobs", name); }
    public static void register(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> event.register(Registries.TEST_FUNCTION,
                registry -> MobsTests.ALL.forEach((n, body) -> registry.register(id("module_game_test_" + n), body))));
        bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent event) -> {
            var environment = event.registerEnvironment(id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
            MobsTests.ALL.forEach((n, body) -> {
                var testId = id("module_game_test_" + n);
                var data = new TestData<>(environment, net.minecraft.world.level.Level.OVERWORLD, id("empty"), 100, 0, true, net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 1);
                event.registerTest(testId, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, testId), data));
            });
        });
    }
}
