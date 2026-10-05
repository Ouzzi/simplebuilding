package com.simplebuilding.modules.simplesounds.forge;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.gametest.framework.*;
import com.simplebuilding.modules.simplesounds.SoundTests;
public final class ModuleForgeTests {
 public static boolean enabled(){return net.minecraftforge.gametest.ForgeGameTestHooks.isGametestServer();}
 public static Identifier id(String path){return Identifier.fromNamespaceAndPath("simplesounds",path);}
 private static void integration(GameTestHelper h){
  h.assertTrue(net.minecraftforge.fml.ModList.isLoaded("simplevisuals")==
   (com.simplebuilding.framework.api.CosmeticIntensity.current("simplevisuals")!=null),"Loaded optional Visuals publishes through the shared API");
  SoundTests.integration(h);
 }
 public static Map<String,Consumer<GameTestHelper>> cases(){var out=new LinkedHashMap<String,Consumer<GameTestHelper>>();out.put("module_game_test_guide_book",h->{com.simplebuilding.modules.simplesounds.guide.SoundsGuide.gameTest(h);});out.put("module_game_test_launch_smoke",h->{SoundTests.launch(h);});out.put("module_game_test_config_bounds",h->{SoundTests.bounds(h);});out.put("module_game_test_sound_flood_safety",h->{SoundTests.flood(h);});out.put("module_game_test_cooldown_and_world_reset",h->{SoundTests.cooldown(h);});out.put("module_game_test_simple_building_integration",ModuleForgeTests::integration);out.put("module_game_test_effect_footstep_dust",h->{SoundTests.effect(h,"footstep_dust");});out.put("module_game_test_effect_cold_breath",h->{SoundTests.effect(h,"cold_breath");});out.put("module_game_test_effect_fireflies",h->{SoundTests.effect(h,"fireflies");});out.put("module_game_test_effect_pollen",h->{SoundTests.effect(h,"pollen");});out.put("module_game_test_effect_fire_sparks",h->{SoundTests.effect(h,"fire_sparks");});out.put("module_game_test_effect_water_ripples",h->{SoundTests.effect(h,"water_ripples");});out.put("module_game_test_effect_water_droplets",h->{SoundTests.effect(h,"water_droplets");});out.put("module_game_test_effect_leaf_fall",h->{SoundTests.effect(h,"leaf_fall");});out.put("module_game_test_effect_enchanted_items",h->{SoundTests.effect(h,"enchanted_items");});out.put("module_game_test_effect_beacon_aura",h->{SoundTests.effect(h,"beacon_aura");});out.put("module_game_test_effect_damage_feedback",h->{SoundTests.effect(h,"damage_feedback");});out.put("module_game_test_effect_healing_feedback",h->{SoundTests.effect(h,"healing_feedback");});return out;}
 public static void register(net.minecraftforge.eventbus.api.bus.BusGroup bus){
  if(enabled()) net.minecraftforge.registries.RegisterEvent.getBus(bus).addListener(e->e.<Consumer<GameTestHelper>>register(Registries.TEST_FUNCTION,r->cases().forEach((name,body)->r.register(id(name),body))));
 }
 public static void instances(Registry<TestEnvironmentDefinition<?>> environments,Registry<GameTestInstance> instances){
  Holder<TestEnvironmentDefinition<?>> env=environments.getOrThrow(GameTestEnvironments.DEFAULT_KEY);
  var data=new TestData<>(env,net.minecraft.world.level.Level.OVERWORLD,id("empty"),100,0,true,net.minecraft.world.level.block.Rotation.NONE,false,1,1,false,1);
  cases().forEach((name,body)->Registry.register(instances,id(name),new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id(name)),data)));
 }
}
