package com.simplebuilding.tweaks.forge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.tweaks.client.EchoCompassFov;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * FOV-Sog des Echo-Kompasses auf Forge: Forge laedt simplebuilding.tweaks.mixins.json nicht, darum
 * das Forge-eigene Ereignis statt des EchoCompassFovMixin (Fabric/NeoForge).
 */
@Mod.EventBusSubscriber(modid = Simplebuilding.MOD_ID, value = Dist.CLIENT)
public final class EchoCompassForgeFov {
    private EchoCompassForgeFov() {
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        float factor = EchoCompassFov.factor(event.getPlayer(), event.getScale());
        if (factor != 1.0f) {
            event.setNewFovModifier(event.getNewFovModifier() * factor);
        }
    }
}
