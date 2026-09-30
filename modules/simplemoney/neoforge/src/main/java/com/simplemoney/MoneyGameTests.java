package com.simplemoney;
import com.simplemoney.testing.MoneyTests;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import java.util.List;
public final class MoneyGameTests {
 private static Identifier id(String name){return Identifier.fromNamespaceAndPath("simplemoney",name);}
 public static void register(IEventBus bus) {
  bus.addListener((RegisterEvent event)->event.register(Registries.TEST_FUNCTION,r->MoneyTests.ALL.forEach((name,body)->r.register(id(name),body))));
  bus.addListener((RegisterGameTestsEvent event)-> {var env=event.registerEnvironment(id("default"),new TestEnvironmentDefinition.AllOf(List.of()));MoneyTests.ALL.forEach((name,body)->event.registerTest(id(name),new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id(name)),new TestData<>(env,Level.OVERWORLD,Identifier.parse("simplebuilding:empty"),100,0,true,Rotation.NONE,false,1,1,false,1))));});
 }
}
