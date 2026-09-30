package com.simplebuilding.modules.simpleriding;
import com.simpleriding.*;
import com.simpleriding.test.RidingTests;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import java.util.*;
public final class RidingNeoTests {
 public static void register(net.neoforged.bus.api.IEventBus bus){
  bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e)->e.register(Registries.TEST_FUNCTION,r->RidingTests.ALL.forEach((name,body)->r.register(Riding.id("riding_game_test_"+name),body))));
  bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e)->{
   var env=e.registerEnvironment(Riding.id("default"),new TestEnvironmentDefinition.AllOf(List.of()));
   RidingTests.ALL.forEach((name,body)->{var id=Riding.id("riding_game_test_"+name);var data=new TestData<>(env,net.minecraft.world.level.Level.OVERWORLD,Riding.id("empty"),100,0,true,net.minecraft.world.level.block.Rotation.NONE,false,1,1,false,1);e.registerTest(id,new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id),data));});
  });
 }
}
