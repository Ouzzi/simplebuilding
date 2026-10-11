package com.simplebuilding.modules.simpleinterfaces.forge;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.gametest.framework.*;
public final class ModuleForgeTests {
 public static boolean enabled(){return net.minecraftforge.gametest.ForgeGameTestHooks.isGametestServer();}
 public static Identifier id(String path){return Identifier.fromNamespaceAndPath("simpleinterfaces",path);}
 public static Map<String,Consumer<GameTestHelper>> cases(){var out=new LinkedHashMap<String,Consumer<GameTestHelper>>();out.put("module_game_test_box_layouts",com.simplebuilding.modules.simpleinterfaces.ContainerTests::layouts);out.put("module_game_test_box_layout_limits",com.simplebuilding.modules.simpleinterfaces.ContainerTests::layoutLimits);out.put("module_game_test_narrow_box_layouts",com.simplebuilding.modules.simpleinterfaces.ContainerTests::narrowLayouts);out.put("module_game_test_style_registry",com.simplebuilding.modules.simpleinterfaces.ContainerTests::registry);out.put("module_game_test_block_palettes",com.simplebuilding.modules.simpleinterfaces.ContainerTests::palettes);out.put("module_game_test_box_motifs",com.simplebuilding.modules.simpleinterfaces.ContainerTests::motifs);out.put("module_game_test_config_defaults",com.simplebuilding.modules.simpleinterfaces.ContainerTests::config);out.put("module_game_test_legacy_migration",com.simplebuilding.modules.simpleinterfaces.ContainerTests::legacyMigration);out.put("module_game_test_style_toggle",com.simplebuilding.modules.simpleinterfaces.ContainerTests::styleToggle);out.put("module_game_test_station_registry",com.simplebuilding.modules.simpleinterfaces.ContainerTests::stationRegistry);out.put("module_game_test_station_palettes",com.simplebuilding.modules.simpleinterfaces.ContainerTests::stationPalettes);out.put("module_game_test_station_layouts",com.simplebuilding.modules.simpleinterfaces.ContainerTests::stationLayouts);out.put("module_game_test_work_layouts",com.simplebuilding.modules.simpleinterfaces.ContainerTests::workLayouts);out.put("module_game_test_work_palettes",com.simplebuilding.modules.simpleinterfaces.ContainerTests::workPalettes);return out;}
 public static void register(net.minecraftforge.eventbus.api.bus.BusGroup bus){
  if(enabled()) net.minecraftforge.registries.RegisterEvent.getBus(bus).addListener(e->e.<Consumer<GameTestHelper>>register(Registries.TEST_FUNCTION,r->cases().forEach((name,body)->r.register(id(name),body))));
 }
 public static void instances(Registry<TestEnvironmentDefinition<?>> environments,Registry<GameTestInstance> instances){
  Holder<TestEnvironmentDefinition<?>> env=environments.getOrThrow(GameTestEnvironments.DEFAULT_KEY);
  cases().forEach((name,body)->{var data=new TestData<>(env,net.minecraft.world.level.Level.OVERWORLD,Identifier.parse("simpleinterfaces:empty"),100,0,true,net.minecraft.world.level.block.Rotation.NONE,false,1,1,false,1);Registry.register(instances,id(name),new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id(name)),data));});
 }
}
