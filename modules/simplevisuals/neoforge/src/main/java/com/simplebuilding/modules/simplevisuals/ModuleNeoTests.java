package com.simplebuilding.modules.simplevisuals;
import net.minecraft.core.registries.Registries;import net.minecraft.gametest.framework.*;import net.minecraft.resources.*;import java.util.List;
public final class ModuleNeoTests {
 private record Case(String name,java.util.function.Consumer<GameTestHelper> body){}
 private static Identifier id(String name){return Identifier.fromNamespaceAndPath("simplevisuals",name);}
 private static final List<Case> CASES=List.of(
new Case("module_game_test_launch_smoke",h->{com.simplevisuals.test.VisualsTests.launch(h);}),
new Case("module_game_test_config_bounds_and_legacy_keys",h->{com.simplevisuals.test.VisualsTests.config(h);}),
new Case("module_game_test_config_and_language_completeness",h->{com.simplevisuals.test.VisualsTests.language(h);}),
new Case("module_game_test_particle_flood_safety",h->{com.simplevisuals.test.VisualsTests.budgets(h);}),
new Case("module_game_test_server_anvil_and_cross_mod",h->{com.simplevisuals.test.VisualsTests.formatting(h);}),
new Case("module_game_test_asset_decompression_bounds",h->{com.simplevisuals.test.VisualsTests.assetBounds(h);}),
new Case("module_game_test_effect_footstep_dust",h->{com.simplevisuals.test.VisualsTests.effect(h,"footstep_dust");}),
new Case("module_game_test_effect_cold_breath",h->{com.simplevisuals.test.VisualsTests.effect(h,"cold_breath");}),
new Case("module_game_test_effect_fireflies",h->{com.simplevisuals.test.VisualsTests.effect(h,"fireflies");}),
new Case("module_game_test_effect_pollen",h->{com.simplevisuals.test.VisualsTests.effect(h,"pollen");}),
new Case("module_game_test_effect_fire_sparks",h->{com.simplevisuals.test.VisualsTests.effect(h,"fire_sparks");}),
new Case("module_game_test_effect_water_ripples",h->{com.simplevisuals.test.VisualsTests.effect(h,"water_ripples");}),
new Case("module_game_test_effect_water_droplets",h->{com.simplevisuals.test.VisualsTests.effect(h,"water_droplets");}),
new Case("module_game_test_effect_leaf_fall",h->{com.simplevisuals.test.VisualsTests.effect(h,"leaf_fall");}),
new Case("module_game_test_effect_enchanted_items",h->{com.simplevisuals.test.VisualsTests.effect(h,"enchanted_items");}),
new Case("module_game_test_effect_beacon_aura",h->{com.simplevisuals.test.VisualsTests.effect(h,"beacon_aura");}),
new Case("module_game_test_effect_damage_feedback",h->{com.simplevisuals.test.VisualsTests.effect(h,"damage_feedback");}),
new Case("module_game_test_effect_healing_feedback",h->{com.simplevisuals.test.VisualsTests.effect(h,"healing_feedback");}));
 public static void register(net.neoforged.bus.api.IEventBus bus){
  bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event)->event.register(Registries.TEST_FUNCTION,registry->{for(var test:CASES)registry.register(id(test.name),test.body);}));
  bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent event)->{
   var environment=event.registerEnvironment(id("default"),new TestEnvironmentDefinition.AllOf(List.of()));
   var data=new TestData<>(environment,net.minecraft.world.level.Level.OVERWORLD,id("empty"),100,0,true,net.minecraft.world.level.block.Rotation.NONE,false,1,1,false,1);
   for(var test:CASES)event.registerTest(id(test.name),new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id(test.name)),data));
  });
 }
}
