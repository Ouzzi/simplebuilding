package com.simplefun.platform;

import com.simplefun.SimplefunAttachments;
import com.simplefun.platform.services.IPlatformHelper;
import com.simplefun.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public boolean isPiggySynced(LivingEntity entity) {
        // Players carry a synced data attachment; mobs sync their effects via the entity tracker.
        if (entity instanceof Player) {
            return entity.getData(SimplefunAttachments.PIGGY);
        }
        return entity.hasEffect(ModEffects.holder());
    }
}
