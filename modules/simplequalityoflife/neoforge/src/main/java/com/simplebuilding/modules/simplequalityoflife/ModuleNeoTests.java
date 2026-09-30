package com.simplebuilding.modules.simplequalityoflife;
import com.simplequalityoflife.test.QolTests;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import java.util.*;
public final class ModuleNeoTests {
 public static void register(net.neoforged.bus.api.IEventBus bus){
  bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e)->e.register(Registries.TEST_FUNCTION,r->QolTests.ALL.forEach((n,b)->r.register(QolTests.id("module_game_test_"+n),b))));
  bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e)->{
   var env=e.registerEnvironment(QolTests.id("default"),new TestEnvironmentDefinition.AllOf(List.of()));
   QolTests.ALL.forEach((n,b)->{var id=QolTests.id("module_game_test_"+n);var data=new TestData<>(env,net.minecraft.world.level.Level.OVERWORLD,QolTests.id("empty"),100,0,true,net.minecraft.world.level.block.Rotation.NONE,false,1,1,false,1);e.registerTest(id,new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id),data));});
  });
 }
}
