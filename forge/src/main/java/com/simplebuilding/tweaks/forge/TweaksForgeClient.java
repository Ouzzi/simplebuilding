package com.simplebuilding.tweaks.forge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.client.LaserRenderer;
import com.simplebuilding.tweaks.client.TweaksClient;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Forge-Client-Anbindung des Simple-Tweaks-Teils; der Laser zeichnet ueber TweaksLevelRendererMixin. */
@Mod.EventBusSubscriber(modid = Simplebuilding.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TweaksForgeClient {
    private TweaksForgeClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        TweaksClient.init();
    }

    @Mod.EventBusSubscriber(modid = Simplebuilding.MOD_ID, value = Dist.CLIENT)
    public static final class DefaultBusEvents {
        private DefaultBusEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
            TweaksClient.tick(Minecraft.getInstance());
        }

        @SubscribeEvent
        public static void onAddGuiLayers(AddGuiOverlayLayersEvent event) {
            event.getLayeredDraw().addAbove(ForgeLayeredDraw.POST_SLEEP_STACK, SimpleTweaks.id("laser_distance"),
                    ForgeLayeredDraw.CHAT_OVERLAY, (graphics, deltaTracker) -> LaserRenderer.renderHud(graphics));
        }
    }
}
