package com.simplebuilding.modules.simplevisuals.forge;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="simplevisuals",value=net.minecraftforge.api.distmarker.Dist.CLIENT,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class VisualsForgeSetup {
 @net.minecraftforge.eventbus.api.listener.SubscribeEvent
 public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e){com.simplevisuals.client.RenamedModels.modelsLoaded=net.minecraftforge.fml.ModList.isLoaded("simplemodels");}
}
