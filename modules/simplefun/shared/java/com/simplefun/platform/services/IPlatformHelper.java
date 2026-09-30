package com.simplefun.platform.services;

import net.minecraft.world.entity.LivingEntity;

public interface IPlatformHelper {

  /**
   * Whether this entity should be rendered with the piggy head, using whatever client-synced
   * mechanism the loader provides (synced entity data on Fabric, a synced data attachment on
   * NeoForge). Falls back to the effect for mobs.
   */
  boolean isPiggySynced(LivingEntity entity);
}
