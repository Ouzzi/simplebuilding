package com.simplebuilding.modules.simplecontainers;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** NeoForge mirror of the Fabric catalogue {@code ModuleGameTest}. */
public final class ModuleNeoTests {
    private record Case(String name, Consumer<GameTestHelper> body) {}

    private static final List<Case> CASES = List.of(
            new Case("module_game_test_box_layouts", ContainerTests::layouts),
            new Case("module_game_test_box_layout_limits", ContainerTests::layoutLimits),
            new Case("module_game_test_narrow_box_layouts", ContainerTests::narrowLayouts),
            new Case("module_game_test_style_registry", ContainerTests::registry),
            new Case("module_game_test_block_palettes", ContainerTests::palettes),
            new Case("module_game_test_box_motifs", ContainerTests::motifs),
            new Case("module_game_test_config_defaults", ContainerTests::config));

    private ModuleNeoTests() {}

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("simplecontainers", name);
    }

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> event.register(Registries.TEST_FUNCTION,
                registry -> CASES.forEach(test -> registry.register(id(test.name()), test.body()))));
        bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent event) -> {
            var environment = event.registerEnvironment(id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
            var data = new TestData<>(environment, net.minecraft.world.level.Level.OVERWORLD, id("empty"), 100, 0, true,
                    net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 1);
            for (Case test : CASES) {
                event.registerTest(id(test.name()), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(test.name())), data));
            }
        });
    }
}
