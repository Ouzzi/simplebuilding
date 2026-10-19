package com.simplebuilding.modules.simplestorage;

import com.simplestorage.SimpleStorage;
import com.simplestorage.test.StorageTests;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** Same ids as the Fabric adapter: simplestorage:module_game_test_<name>. */
public final class ModuleNeoTests {
    private static Identifier id(String name) {
        return SimpleStorage.id("module_game_test_" + name);
    }

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> event.register(Registries.TEST_FUNCTION,
                registry -> StorageTests.ALL.forEach((name, body) -> registry.register(id(name), body))));
        bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent event) -> {
            var environment = event.registerEnvironment(SimpleStorage.id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
            StorageTests.ALL.forEach((name, body) -> {
                int ticks = StorageTests.MAX_TICKS.getOrDefault(name, 100);
                var data = new TestData<>(environment, net.minecraft.world.level.Level.OVERWORLD, SimpleStorage.id("empty"), ticks, 0, true,
                        net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 1);
                event.registerTest(id(name), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(name)), data));
            });
        });
    }

    private ModuleNeoTests() {}
}
