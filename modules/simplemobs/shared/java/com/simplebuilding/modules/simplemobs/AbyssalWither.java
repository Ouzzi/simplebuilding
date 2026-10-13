package com.simplebuilding.modules.simplemobs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;

/**
 * Abyssal Wither (owner N50): the second, stronger stage of the Blighted Wither, summoned with an Enderite Helmet on the
 * middle skull (item tag {@code simplemobs:abyssal_wither_crowns}). Same attacks and the same warnings, larger numbers:
 * {@link WitherLogic#ENDERITE}.
 */
public class AbyssalWither extends BlightedWither {
    public AbyssalWither(EntityType<? extends WitherBoss> type, Level level) {
        super(type, level);
    }

    @Override
    public WitherLogic.Stage stage() {
        return WitherLogic.ENDERITE;
    }
}
