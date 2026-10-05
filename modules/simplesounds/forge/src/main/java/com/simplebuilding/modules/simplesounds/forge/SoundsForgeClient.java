package com.simplebuilding.modules.simplesounds.forge;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="simplesounds",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class SoundsForgeClient {
 @net.minecraftforge.eventbus.api.listener.SubscribeEvent
 public static void commands(net.minecraftforge.client.event.RegisterClientCommandsEvent e){com.simplebuilding.modules.simplesounds.client.SoundsGuideCommand.register(e.getDispatcher());}
}
