package com.simplebuilding.modules.simpletweaks;

import com.simplebuilding.modules.simpletweaks.admin.*;
import com.simplebuilding.modules.simpletweaks.admin.invsee.Invsee;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;

/** NeoForge wiring of the admin features: menus, lifecycle, commands, GameTests; screens in {@link AdminNeoClient}. */
final class AdminNeo {
    static void init(net.neoforged.bus.api.IEventBus bus, net.neoforged.api.distmarker.Dist dist) {
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e) -> { if (e.getRegistryKey().equals(Registries.MENU)) Invsee.registerMenus(); });
        var events = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        events.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> Admin.start(e.getServer()));
        events.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> Admin.stop(e.getServer()));
        events.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> AdminCommands.register(e.getDispatcher()));
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e) -> e.register(Registries.TEST_FUNCTION,
                r -> AdminTests.TESTS.forEach((name, test) -> r.register(id("admin_game_test_" + name), test::accept))));
        bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e) -> {
            var environment = e.registerEnvironment(id("admin"), new TestEnvironmentDefinition.AllOf(java.util.List.of()));
            AdminTests.TESTS.forEach((name, test) -> {
                var key = id("admin_game_test_" + name);
                var data = new TestData<>(environment, net.minecraft.world.level.Level.OVERWORLD, id("empty"), AdminTests.maxTicks(name), 0, true,
                        net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 1);
                e.registerTest(key, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, key), data));
            });
        });
        if (dist == net.neoforged.api.distmarker.Dist.CLIENT) AdminNeoClient.init(bus);
    }
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("simpletweaks", name); }
    private AdminNeo() {}
}
