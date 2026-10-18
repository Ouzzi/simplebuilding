package com.simplebuilding.modules.simpletweaks;

import com.simplebuilding.modules.simpletweaks.protect.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;

/** NeoForge wiring of the protection features: lifecycle, commands, rule packet, GameTests. */
final class ProtectNeo {
    static void init(net.neoforged.bus.api.IEventBus bus) {
        var events = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        events.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> Protect.start(e.getServer()));
        events.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> Protect.stop(e.getServer()));
        events.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> ProtectCommands.register(e.getDispatcher()));
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e) -> e.registrar("1").optional().playToClient(ProtectRulesPayload.TYPE, ProtectRulesPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> com.simplebuilding.modules.simpletweaks.protect.client.ProtectClient.rules(payload))));
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e) -> e.register(Registries.TEST_FUNCTION,
                r -> ProtectTests.TESTS.forEach((name, test) -> r.register(id("protect_game_test_" + name), test::accept))));
        bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e) -> {
            var environment = e.registerEnvironment(id("protect"), new TestEnvironmentDefinition.AllOf(java.util.List.of()));
            ProtectTests.TESTS.forEach((name, test) -> {
                var key = id("protect_game_test_" + name);
                var data = new TestData<>(environment, net.minecraft.world.level.Level.OVERWORLD, id("empty"), ProtectTests.maxTicks(name), 0, true,
                        net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 1);
                e.registerTest(key, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, key), data));
            });
        });
    }
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("simpletweaks", name); }
    private ProtectNeo() {}
}
