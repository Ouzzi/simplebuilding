package com.simplefun;

import com.simplefun.client.PigHeadFeatureRenderer;
import com.simplefun.registry.ModEntities;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only NeoForge setup. Only referenced when running on the client (dist guard in the entry point).
 */
public class SimplefunNeoForgeClient {

    public static void init(IEventBus modBus, ModContainer modContainer) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(ModEntities.BRICK_PROJECTILE, ThrownItemRenderer::new));

        // Players are rendered per skin model (slim/wide), retrieved via getPlayerRenderer - NOT getRenderer(PLAYER).
        modBus.addListener((EntityRenderersEvent.AddLayers event) -> {
            EntityModelSet models = event.getEntityModels();
            int registered = 0;
            for (PlayerModelType skin : event.getSkins()) {
                AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
                if (renderer != null) {
                    renderer.addLayer(new PigHeadFeatureRenderer<>(renderer, models));
                    registered++;
                }
            }
            Constants.LOG.info("Registered pig-head layer on {} player skin renderer(s)", registered);
        });

        // In-game config screen (Cloth Config, built via the low-level API). Shows the config
        // button in NeoForge's mod list.
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> SimplefunNeoForgeConfigScreen.build(parent));
    }
}
