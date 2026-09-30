package com.simplevisuals.neoforge;
@net.neoforged.fml.common.EventBusSubscriber(modid="simplevisuals",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class VisualsNeoClient {
 @net.neoforged.bus.api.SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){com.simplevisuals.client.RenamedModels.modelsLoaded=net.neoforged.fml.ModList.get().isLoaded("simplemodels");net.neoforged.fml.ModList.get().getModContainerById("simplevisuals").orElseThrow().registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,(container,parent)->com.simplevisuals.client.VisualsConfigScreen.create(parent));}
 @net.neoforged.bus.api.SubscribeEvent public static void commands(net.neoforged.neoforge.client.event.RegisterClientCommandsEvent e){com.simplevisuals.client.LocalCommands.register(e.getDispatcher(),(source,text)->source.sendSuccess(()->text,false));}
}
