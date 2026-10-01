package com.simplebuilding.modules.simpletweaks.forge;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.gametest.framework.*;

public final class ModuleForgeTests {
 public static boolean enabled(){return net.minecraftforge.gametest.ForgeGameTestHooks.isGametestServer();}
 public static Identifier id(String path){return Identifier.fromNamespaceAndPath("simpletweaks",path);}
 public static Map<String,Consumer<GameTestHelper>> cases(){var out=new LinkedHashMap<String,Consumer<GameTestHelper>>();com.simplebuilding.modules.simpletweaks.CompatibilityTests.TESTS.forEach((name,body)->out.put("module_game_test_"+name,body));com.simplebuilding.modules.simpletweaks.claims.ClaimTests.TESTS.forEach((name,body)->out.put("claims_game_test_"+name,body));return out;}
 public static void register(net.minecraftforge.eventbus.api.bus.BusGroup bus){
  if(enabled()) net.minecraftforge.registries.RegisterEvent.getBus(bus).addListener(e->e.<Consumer<GameTestHelper>>register(Registries.TEST_FUNCTION,r->cases().forEach((name,body)->r.register(id(name),body))));
 }
 public static void instances(Registry<TestEnvironmentDefinition<?>> environments,Registry<GameTestInstance> instances){
  Holder<TestEnvironmentDefinition<?>> env=environments.getOrThrow(GameTestEnvironments.DEFAULT_KEY);
  var data=new TestData<>(env,net.minecraft.world.level.Level.OVERWORLD,id("empty"),100,0,true,net.minecraft.world.level.block.Rotation.NONE,false,1,1,false,1);
  cases().forEach((name,body)->{
   TestData<Holder<TestEnvironmentDefinition<?>>> timing=name.equals("claims_game_test_claims_portal_flow")?new TestData<>(environments.getOrThrow(ResourceKey.create(Registries.TEST_ENVIRONMENT,id("claims_portal"))),net.minecraft.world.level.Level.OVERWORLD,id("empty"),400,0,true,net.minecraft.world.level.block.Rotation.NONE,false,1,1,false,1):data;
   Registry.register(instances,id(name),new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id(name)),timing));
  });
 }
}
