package com.simplebuilding.modules.simplesounds;
import net.minecraft.core.registries.Registries;import net.minecraft.gametest.framework.*;import net.minecraft.resources.*;import java.util.List;
public final class ModuleNeoTests {
 private record Case(String name,java.util.function.Consumer<GameTestHelper> body){}
 private static Identifier id(String name){return Identifier.fromNamespaceAndPath("simplesounds",name);}
 private static final List<Case> CASES=List.of(
new Case("module_game_test_launch_smoke",h->{SoundTests.launch(h);}),
new Case("module_game_test_config_bounds",h->{SoundTests.bounds(h);}),
new Case("module_game_test_sound_flood_safety",h->{SoundTests.flood(h);}),
new Case("module_game_test_cooldown_and_world_reset",h->{SoundTests.cooldown(h);}),
new Case("module_game_test_simple_building_integration",h->{
 h.assertTrue(net.neoforged.fml.ModList.get().isLoaded("simplevisuals") ==
  (com.simplebuilding.framework.api.CosmeticIntensity.current("simplevisuals") != null), "Optional provider presence matches the actual loader selection");
 SoundTests.integration(h);
}),
new Case("module_game_test_effect_footstep_dust",h->{SoundTests.effect(h,"footstep_dust");}),
new Case("module_game_test_effect_cold_breath",h->{SoundTests.effect(h,"cold_breath");}),
new Case("module_game_test_effect_fireflies",h->{SoundTests.effect(h,"fireflies");}),
new Case("module_game_test_effect_pollen",h->{SoundTests.effect(h,"pollen");}),
new Case("module_game_test_effect_fire_sparks",h->{SoundTests.effect(h,"fire_sparks");}),
new Case("module_game_test_effect_water_ripples",h->{SoundTests.effect(h,"water_ripples");}),
new Case("module_game_test_effect_water_droplets",h->{SoundTests.effect(h,"water_droplets");}),
new Case("module_game_test_effect_leaf_fall",h->{SoundTests.effect(h,"leaf_fall");}),
new Case("module_game_test_effect_enchanted_items",h->{SoundTests.effect(h,"enchanted_items");}),
new Case("module_game_test_effect_beacon_aura",h->{SoundTests.effect(h,"beacon_aura");}),
new Case("module_game_test_effect_damage_feedback",h->{SoundTests.effect(h,"damage_feedback");}),
new Case("module_game_test_effect_healing_feedback",h->{SoundTests.effect(h,"healing_feedback");}));
 public static void register(net.neoforged.bus.api.IEventBus bus){
  bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event)->event.register(Registries.TEST_FUNCTION,registry->{for(var test:CASES)registry.register(id(test.name),test.body);}));
  bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent event)->{
   var environment=event.registerEnvironment(id("default"),new TestEnvironmentDefinition.AllOf(List.of()));
   var data=new TestData<>(environment,net.minecraft.world.level.Level.OVERWORLD,id("empty"),100,0,true,net.minecraft.world.level.block.Rotation.NONE,false,1,1,false,1);
   for(var test:CASES)event.registerTest(id(test.name),new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id(test.name)),data));
  });
 }
}
