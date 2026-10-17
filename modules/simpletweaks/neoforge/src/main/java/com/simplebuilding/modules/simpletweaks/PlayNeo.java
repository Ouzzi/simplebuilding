package com.simplebuilding.modules.simpletweaks;

import com.simplebuilding.modules.simpletweaks.play.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;

/** NeoForge wiring of the player/world/event features: lifecycle, commands, GameTests. */
final class PlayNeo {
    static void init(net.neoforged.bus.api.IEventBus bus) {
        Mods.check = id -> net.neoforged.fml.ModList.get().isLoaded(id);
        var events = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        events.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> Play.start(e.getServer()));
        events.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> Play.stop(e.getServer()));
        events.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> PlayCommands.register(e.getDispatcher()));
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e) -> e.register(Registries.TEST_FUNCTION,
                r -> PlayTests.TESTS.forEach((name, test) -> r.register(id("play_game_test_" + name), test::accept))));
        bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e) -> {
            var environment = e.registerEnvironment(id("play"), new TestEnvironmentDefinition.AllOf(java.util.List.of()));
            PlayTests.TESTS.forEach((name, test) -> {
                var key = id("play_game_test_" + name);
                var data = new TestData<>(environment, net.minecraft.world.level.Level.OVERWORLD, id("empty"), PlayTests.maxTicks(name), 0, true,
                        net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 1);
                e.registerTest(key, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, key), data));
            });
        });
    }
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("simpletweaks", name); }
    private PlayNeo() {}
}
