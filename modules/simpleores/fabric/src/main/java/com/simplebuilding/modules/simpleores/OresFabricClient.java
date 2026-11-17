package com.simplebuilding.modules.simpleores;

import com.simpleores.SimpleOres;
import com.simpleores.client.GrateModels;
import com.simpleores.config.OresClientConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric client entry. It drew the silver arrow until 03.11.2026; the arrow and its renderer moved to Simple Arrows
 * (docs/ai/PLAN-B-ARROWS02-2026-11-03.md). N78b: the client config "3D grates" ({@code /simpleclient simpleores}, config screen);
 * a change reloads the resources (like F3+T) so the grate models switch at once.
 */
public final class OresFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        var dir = FabricLoader.getInstance().getConfigDir();
        OresClientConfig.load(dir);
        com.simplelib.api.config.ConfigCommands.client(SimpleOres.MOD_ID).json(OresClientConfig::toJson, OresClientConfig::apply)
                .file(dir, OresClientConfig.FILE)
                .after(() -> {
                    if (!GrateModels.stale()) return;
                    var mc = net.minecraft.client.Minecraft.getInstance();
                    mc.execute(mc::reloadResourcePacks);
                }).register();
    }
}
