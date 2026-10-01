package com.simplebuilding.modules.simpletweaks;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import java.util.List;
public final class ModuleNeoTests {
 private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("simpletweaks",name); }
 public static void register(net.neoforged.bus.api.IEventBus bus) {
  bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e)->e.register(Registries.TEST_FUNCTION,r->com.simplebuilding.modules.simpletweaks.claims.ClaimTests.TESTS.forEach((name,test)->r.register(id("claims_game_test_"+name),test::accept))));
  bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e)->e.register(Registries.TEST_FUNCTION,r->CompatibilityTests.TESTS.forEach((name,test)->r.register(id("module_game_test_"+name),test::accept))));
  bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e)-> {
   var environment=e.registerEnvironment(id("default"),new TestEnvironmentDefinition.AllOf(List.of()));
   var data=new TestData<>(environment,net.minecraft.world.level.Level.OVERWORLD,id("empty"),100,0,true,net.minecraft.world.level.block.Rotation.NONE,false,1,1,false,1);
   CompatibilityTests.TESTS.forEach((name,test)-> {var key=id("module_game_test_"+name);e.registerTest(key,new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,key),data));});
   com.simplebuilding.modules.simpletweaks.claims.ClaimTests.TESTS.forEach((name,test)-> {var key=id("claims_game_test_"+name);e.registerTest(key,new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,key),data));});
  });
 }
}
