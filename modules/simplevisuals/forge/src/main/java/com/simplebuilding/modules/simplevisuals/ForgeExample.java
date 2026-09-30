package com.simplebuilding.modules.simplevisuals;
@net.minecraftforge.fml.common.Mod("simplevisuals")
public final class ForgeExample {
 public ForgeExample(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context){
  com.simplevisuals.Visuals.initialize();
  com.simplebuilding.modules.simplevisuals.forge.ModuleForgeTests.register(context.getModBusGroup());
  net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e->com.simplevisuals.VisualsCommands.register(e.getDispatcher()));
  net.minecraftforge.registries.RegisterEvent.getBus(context.getModBusGroup()).addListener(e->{if(e.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM))ExampleItems.register();});
 }
}
