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
    // FUN11-5/6/8 thrown toys and the FUN11-7 soap bubble (b-fun14).
    EntityRendererRegistry.register(com.simplefun.registry.ModToys.SKIPPING_STONE_ENTITY, ThrownItemRenderer::new);
    EntityRendererRegistry.register(com.simplefun.registry.ModToys.WATER_BALLOON_ENTITY, ThrownItemRenderer::new);
    EntityRendererRegistry.register(com.simplefun.registry.ModToys.PAPER_PLANE_ENTITY, ThrownItemRenderer::new);
    net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry.getInstance()
        .register(com.simplefun.registry.ModToys.SOAP_BUBBLE, com.simplefun.client.SoapBubbleParticle.Provider::new);

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
