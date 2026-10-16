package com.simplebuilding.modules.simplemobs.mixin;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The Wither's boss bar, so the Blighted/Abyssal Wither can show its phases on it. */
@Mixin(WitherBoss.class)
public interface WitherBossAccessor {
    @Accessor("bossEvent")
    ServerBossEvent simplemobs$bossEvent();
}
