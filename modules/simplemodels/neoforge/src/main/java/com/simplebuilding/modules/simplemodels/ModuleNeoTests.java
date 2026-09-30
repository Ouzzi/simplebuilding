package com.simplebuilding.modules.simplemodels;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import java.util.List;
public final class ModuleNeoTests {
 private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("simplemodels",name); }
 public static void register(net.neoforged.bus.api.IEventBus bus) {
  bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e)-> e.register(Registries.TEST_FUNCTION,r-> ModelTests.ALL.forEach((name,body)->r.register(id("module_game_test_"+name),body))));
  bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e)->{
   var env=e.registerEnvironment(id("default"),new TestEnvironmentDefinition.AllOf(List.of()));
   ModelTests.ALL.forEach((name,body)->{var test=id("module_game_test_"+name);var data=new TestData<>(env,net.minecraft.world.level.Level.OVERWORLD,id("empty"),100,0,true,net.minecraft.world.level.block.Rotation.NONE,false,1,1,false,1);e.registerTest(test,new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,test),data));});
  });
 }
}
