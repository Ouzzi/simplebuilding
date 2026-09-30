package com.simplebuilding.modules.simpledimensions;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import java.util.*;
public final class ModuleNeoTests {
 public static void register(net.neoforged.bus.api.IEventBus bus){
  bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e)->e.register(Registries.TEST_FUNCTION,r->DimensionTests.ALL.forEach((name,body)->r.register(DimensionRegistry.id("module_game_test_"+name),body))));
  bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e)->{
   var env=e.registerEnvironment(DimensionRegistry.id("default"),new TestEnvironmentDefinition.AllOf(List.of()));
   DimensionTests.ALL.forEach((name,body)->{var id=DimensionRegistry.id("module_game_test_"+name);var data=new TestData<>(env,net.minecraft.world.level.Level.OVERWORLD,Identifier.parse("simpledimensions:empty"),240,0,true,net.minecraft.world.level.block.Rotation.NONE,false,1,1,false,1);e.registerTest(id,new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id),data));});
  });
 }
}
