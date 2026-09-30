package com.simplebuilding.modules.simplefun.forge;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.gametest.framework.*;
public final class ModuleForgeTests {
 public static boolean enabled(){return net.minecraftforge.gametest.ForgeGameTestHooks.isGametestServer();}
 public static Identifier id(String path){return Identifier.fromNamespaceAndPath("simplefun",path);}
 public static Map<String,Consumer<GameTestHelper>> cases(){var out=new LinkedHashMap<String,Consumer<GameTestHelper>>();for(var entry:com.simplefun.test.FunTests.ALL.entrySet()) out.put("module_game_test_"+entry.getKey(),entry.getValue());return out;}
 public static void register(net.minecraftforge.eventbus.api.bus.BusGroup bus){
  if(enabled()) net.minecraftforge.registries.RegisterEvent.getBus(bus).addListener(e->e.<Consumer<GameTestHelper>>register(Registries.TEST_FUNCTION,r->cases().forEach((name,body)->r.register(id(name),body))));
 }
 public static void instances(Registry<TestEnvironmentDefinition<?>> environments,Registry<GameTestInstance> instances){
  Holder<TestEnvironmentDefinition<?>> env=environments.getOrThrow(GameTestEnvironments.DEFAULT_KEY);
  var data=new TestData<>(env,net.minecraft.world.level.Level.OVERWORLD,Identifier.parse("simplebuilding:empty"),100,0,true,net.minecraft.world.level.block.Rotation.NONE,false,1,1,false,1);
  cases().forEach((name,body)->Registry.register(instances,id(name),new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id(name)),data)));
 }
}
