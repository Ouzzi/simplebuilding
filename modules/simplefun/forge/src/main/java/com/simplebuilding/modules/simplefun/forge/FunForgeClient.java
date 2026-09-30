package com.simplebuilding.modules.simplefun.forge;
import com.simplefun.Constants;

import com.simplefun.client.PigHeadFeatureRenderer;
import com.simplefun.registry.ModEntities;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraftforge.eventbus.api.bus.BusGroup;

import net.minecraftforge.client.event.EntityRenderersEvent;


/**
 * Client-only Forge setup. Only referenced when running on the client (dist guard in the entry
 * point).
 */
public class FunForgeClient {

  public static void init(BusGroup modBus) {
    EntityRenderersEvent.RegisterRenderers.BUS.addListener(
        (EntityRenderersEvent.RegisterRenderers event) ->
            event.registerEntityRenderer(ModEntities.BRICK_PROJECTILE, ThrownItemRenderer::new));

    // Players are rendered per skin model (slim/wide), retrieved via getPlayerRenderer - NOT
    // getRenderer(PLAYER).
    EntityRenderersEvent.AddLayers.BUS.addListener(
        (EntityRenderersEvent.AddLayers event) -> {
          EntityModelSet models = event.getEntityModels();
          int registered = 0;
          for (PlayerModelType skin : event.getModelTypes()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
              renderer.addLayer(new PigHeadFeatureRenderer<>(renderer, models));
              registered++;
            }
          }
          Constants.LOG.info("Registered pig-head layer on {} player skin renderer(s)", registered);
        });

  }
}
