package com.simplebuilding.modules.simplebuildingtools;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuildingtools.SimpleBuildingTools;
import com.simplebuildingtools.client.BtClient;
import com.simplebuildingtools.client.ClientState;
import com.simplebuildingtools.client.gui.RangefinderHudOverlay;
import com.simplebuildingtools.client.render.BuildingWandPreviewRenderer;
import com.simplebuildingtools.client.render.OctantHighlightRenderer;
import com.simplebuildingtools.networking.BtConfigPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;

/** Fabric client entry: keys, HUD box, world previews, tooltips, the blueprint model property, the server's config. */
public final class BuildingToolsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BtClient.init();
        com.simplebuildingtools.platform.ClientNetworking.setSender(payload -> {
            if (ClientPlayNetworking.canSend(payload.type())) ClientPlayNetworking.send(payload);
        });
        KeyMapping.Category category = KeyMapping.Category.register(SimpleBuildingTools.id("general"));
        ClientState.settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.simplebuildingtools.settings", InputConstants.KEY_K, category));
        ClientState.octantFigureToggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.simplebuildingtools.toggle_octant_figure",
                InputConstants.UNKNOWN.getValue(), category));
        ClientTickEvents.END_CLIENT_TICK.register(BtClient::tick);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, SimpleBuildingTools.id("octant_hud"),
                (context, tickCounter) -> RangefinderHudOverlay.render(context));
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            OctantHighlightRenderer.renderInWorld(context.submitNodeCollector(), context.poseStack(),
                    Minecraft.getInstance().gameRenderer.mainCamera());
            BuildingWandPreviewRenderer.render(context.submitNodeCollector(), context.poseStack(), context.levelState().cameraRenderState.pos);
        });
        ClientTooltipComponentCallback.EVENT.register(data -> data instanceof com.simplebuildingtools.items.tooltip.BlueprintTooltipData blueprint
                ? com.simplebuildingtools.client.blueprint.BlueprintTooltip.create(blueprint) : null);
        SelectItemModelProperties.ID_MAPPER.put(com.simplebuildingtools.client.property.BlueprintStateModelProperty.ID,
                com.simplebuildingtools.client.property.BlueprintStateModelProperty.PROPERTY_TYPE);
        ClientPlayNetworking.registerGlobalReceiver(BtConfigPayload.ID,
                (payload, context) -> context.client().execute(() -> SimpleBuildingTools.setSynced(payload.json())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SimpleBuildingTools.setSynced(null));
    }
}
