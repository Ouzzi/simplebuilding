package com.simplefun.platform;

import com.simplefun.entity.PiggyTracked;
import com.simplefun.platform.services.IPlatformHelper;
import com.simplefun.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;

public class ForgePlatformHelper implements IPlatformHelper {

  @Override
  public boolean isPiggySynced(LivingEntity entity) {
    // Players carry synced entity data (Forge PlayerEntityMixin); mobs sync their effects via the entity
    // tracker.
    if (entity instanceof PiggyTracked tracked) {
      return tracked.simplefun$isPiggyTracked();
    }
    return entity.hasEffect(ModEffects.holder());
  }
}
