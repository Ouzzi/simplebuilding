package com.simplebuilding;

import com.simplebuilding.block.entity.ModBlockEntities;
import com.simplebuilding.client.gui.RangefinderHudOverlay;
import com.simplebuilding.client.gui.SpeedometerHudOverlay;
import com.simplebuilding.client.render.BuildingWandOutlineRenderer;
import com.simplebuilding.block.entity.renderer.NetheriteShulkerBoxBlockRenderer;
import com.simplebuilding.client.render.SledgehammerOutlineRenderer;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.util.BundleTooltipAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.gui.tooltip.BundleTooltipComponent;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class SimplebuildingClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(new RangefinderHudOverlay());
        HudRenderCallback.EVENT.register(new SpeedometerHudOverlay());

        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof ReinforcedBundleTooltipData reinforcedData) {
                BundleTooltipComponent component = new BundleTooltipComponent(reinforcedData.contents());
                float scale = (float) reinforcedData.maxCapacity() / 64.0f;
                ((BundleTooltipAccessor) component).simplebuilding$setCapacityScale(scale);
                return component;
            }
            return null;
        });

        SledgehammerOutlineRenderer.register();
        BuildingWandOutlineRenderer.register();

        // 2. Renderer registrieren
        BlockEntityRendererFactories.register(ModBlockEntities.NETHERITE_SHULKER_BE, NetheriteShulkerBoxBlockRenderer::new);
    }

}
