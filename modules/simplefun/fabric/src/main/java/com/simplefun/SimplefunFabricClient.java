package com.simplefun;

import com.simplefun.client.PigHeadFeatureRenderer;
import com.simplefun.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.EntityTypes;

public class SimplefunFabricClient implements ClientModInitializer {

  @Override
  public void onInitializeClient() {
    EntityRendererRegistry.register(ModEntities.BRICK_PROJECTILE, ThrownItemRenderer::new);

    LivingEntityRenderLayerRegistrationCallback.EVENT.register(
        (entityType, entityRenderer, registrationHelper, context) -> {
          if (entityType == EntityTypes.PLAYER
              && entityRenderer instanceof AvatarRenderer<?> avatarRenderer) {
            registrationHelper.register(
                new PigHeadFeatureRenderer<>(avatarRenderer, context.getModelSet()));
          }
        });
  }
}
