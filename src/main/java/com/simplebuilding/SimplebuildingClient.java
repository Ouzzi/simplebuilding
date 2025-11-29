package com.simplebuilding;

import com.simplebuilding.client.gui.RangefinderHudOverlay;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.util.BundleTooltipAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.gui.tooltip.BundleTooltipComponent;

public class SimplebuildingClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(new RangefinderHudOverlay());
        //WorldRenderEvents.END_MAIN.register(new BlockHighlightRendererBackup());

        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof ReinforcedBundleTooltipData reinforcedData) {
                // 1. Erstelle den normalen Vanilla Renderer
                BundleTooltipComponent component = new BundleTooltipComponent(reinforcedData.contents());

                // 2. Berechne Scale (z.B. 256 / 64 = 4.0)
                float scale = (float) reinforcedData.maxCapacity() / 64.0f;

                // 3. Setze Scale über unser Mixin-Interface
                ((BundleTooltipAccessor) component).simplebuilding$setCapacityScale(scale);

                return component;
            }
            return null;
        });
    }

}
