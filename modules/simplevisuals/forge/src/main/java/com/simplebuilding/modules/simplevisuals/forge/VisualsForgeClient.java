package com.simplebuilding.modules.simplevisuals.forge;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="simplevisuals",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class VisualsForgeClient {
 @net.minecraftforge.eventbus.api.listener.SubscribeEvent
 public static void commands(net.minecraftforge.client.event.RegisterClientCommandsEvent e){com.simplevisuals.client.LocalCommands.register(e.getDispatcher(),(source,text)->source.sendSuccess(()->text,false));}
}
