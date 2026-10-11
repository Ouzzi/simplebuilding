package com.simplebuilding.modules.simpleaesthetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import java.util.List;
public final class ModuleNeoTests {
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("simpleaesthetics", name); }
    public static void register(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> event.register(Registries.TEST_FUNCTION, registry ->
                AestheticsTests.ALL.forEach((name, body) -> registry.register(id("module_game_test_" + name), body::accept))));
        bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent event) -> {
            var environment = event.registerEnvironment(id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
            AestheticsTests.ALL.forEach((name, body) -> {
                var testId = id("module_game_test_" + name);
                var data = new TestData<>(environment, net.minecraft.world.level.Level.OVERWORLD, id("empty"), 100, 0, true, net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 1);
                event.registerTest(testId, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, testId), data));
            });
        });
    }
}
