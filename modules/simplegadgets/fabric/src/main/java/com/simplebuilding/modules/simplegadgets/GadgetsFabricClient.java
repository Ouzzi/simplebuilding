package com.simplebuilding.modules.simplegadgets;

import com.simplegadgets.client.GaugeAutowalk;
import com.simplegadgets.client.GaugeHud;
import com.simplegadgets.client.GaugeNeedleModelProperty;
import com.simplegadgets.client.RodClient;
import com.simplegadgets.client.RodRenderer;
import com.simplegadgets.network.LaserPayload;
import com.simplegadgets.network.RodNetwork;
import com.simplegadgets.gauge.VelocityGaugeItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;

/** Fabric client entry: the Gauge's needle property, its HUD box (SimpleLib gadget stack) and auto-walk. */
public final class GadgetsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RangeSelectItemModelProperties.ID_MAPPER.put(GaugeNeedleModelProperty.ID, GaugeNeedleModelProperty.CODEC);
        net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties.ID_MAPPER.put(
                com.simplegadgets.client.TransformHintModelProperty.ID, com.simplegadgets.client.TransformHintModelProperty.CODEC);
        GaugeHud.register();
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, com.simplegadgets.SimpleGadgets.id("gauge_hud"),
                (context, tickCounter) -> GaugeHud.render(context));
        // The Resonance Rod: dots of the others, the own dot report, the dot in the world, the readout box.
        RodClient.register();
        ClientPlayNetworking.registerGlobalReceiver(LaserPayload.ID, (payload, context) -> RodNetwork.receiveLaser(payload));
        RodNetwork.setServerSender(ClientPlayNetworking::send);
        ClientTickEvents.END_CLIENT_TICK.register(RodClient::tick);
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> RodRenderer.submit(
                context.submitNodeCollector(), context.poseStack(), context.levelState().cameraRenderState.pos));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, com.simplegadgets.SimpleGadgets.id("rod_hud"),
                (context, tickCounter) -> RodRenderer.renderHud(context));
        VelocityGaugeItem.clientAutowalkToggle = GaugeAutowalk::toggle;
        ClientTickEvents.END_CLIENT_TICK.register(GaugeAutowalk::tick);
    }
}
