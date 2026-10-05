package com.simplebuilding.modules.simplevisuals.forge;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.gametest.framework.*;
public final class ModuleForgeTests {
 public static boolean enabled(){return net.minecraftforge.gametest.ForgeGameTestHooks.isGametestServer();}
 public static Identifier id(String path){return Identifier.fromNamespaceAndPath("simplevisuals",path);}
 public static Map<String,Consumer<GameTestHelper>> cases(){var out=new LinkedHashMap<String,Consumer<GameTestHelper>>();out.put("module_game_test_guide_book",h->{com.simplevisuals.guide.VisualsGuide.gameTest(h);});out.put("module_game_test_launch_smoke",h->{com.simplevisuals.test.VisualsTests.launch(h);});out.put("module_game_test_config_bounds_and_legacy_keys",h->{com.simplevisuals.test.VisualsTests.config(h);});out.put("module_game_test_config_and_language_completeness",h->{com.simplevisuals.test.VisualsTests.language(h);});out.put("module_game_test_particle_flood_safety",h->{com.simplevisuals.test.VisualsTests.budgets(h);});out.put("module_game_test_server_anvil_and_cross_mod",h->{com.simplevisuals.test.VisualsTests.formatting(h);});out.put("module_game_test_asset_decompression_bounds",h->{com.simplevisuals.test.VisualsTests.assetBounds(h);});out.put("module_game_test_effect_footstep_dust",h->{com.simplevisuals.test.VisualsTests.effect(h,"footstep_dust");});out.put("module_game_test_effect_cold_breath",h->{com.simplevisuals.test.VisualsTests.effect(h,"cold_breath");});out.put("module_game_test_effect_fireflies",h->{com.simplevisuals.test.VisualsTests.effect(h,"fireflies");});out.put("module_game_test_effect_pollen",h->{com.simplevisuals.test.VisualsTests.effect(h,"pollen");});out.put("module_game_test_effect_fire_sparks",h->{com.simplevisuals.test.VisualsTests.effect(h,"fire_sparks");});out.put("module_game_test_effect_water_ripples",h->{com.simplevisuals.test.VisualsTests.effect(h,"water_ripples");});out.put("module_game_test_effect_water_droplets",h->{com.simplevisuals.test.VisualsTests.effect(h,"water_droplets");});out.put("module_game_test_effect_leaf_fall",h->{com.simplevisuals.test.VisualsTests.effect(h,"leaf_fall");});out.put("module_game_test_effect_enchanted_items",h->{com.simplevisuals.test.VisualsTests.effect(h,"enchanted_items");});out.put("module_game_test_effect_beacon_aura",h->{com.simplevisuals.test.VisualsTests.effect(h,"beacon_aura");});out.put("module_game_test_effect_damage_feedback",h->{com.simplevisuals.test.VisualsTests.effect(h,"damage_feedback");});out.put("module_game_test_effect_healing_feedback",h->{com.simplevisuals.test.VisualsTests.effect(h,"healing_feedback");});return out;}
 public static void register(net.minecraftforge.eventbus.api.bus.BusGroup bus){
  if(enabled()) net.minecraftforge.registries.RegisterEvent.getBus(bus).addListener(e->e.<Consumer<GameTestHelper>>register(Registries.TEST_FUNCTION,r->cases().forEach((name,body)->r.register(id(name),body))));
 }
 public static void instances(Registry<TestEnvironmentDefinition<?>> environments,Registry<GameTestInstance> instances){
  Holder<TestEnvironmentDefinition<?>> env=environments.getOrThrow(GameTestEnvironments.DEFAULT_KEY);
  var data=new TestData<>(env,net.minecraft.world.level.Level.OVERWORLD,Identifier.parse("simplevisuals:empty"),100,0,true,net.minecraft.world.level.block.Rotation.NONE,false,1,1,false,1);
  cases().forEach((name,body)->Registry.register(instances,id(name),new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id(name)),data)));
 }
}
