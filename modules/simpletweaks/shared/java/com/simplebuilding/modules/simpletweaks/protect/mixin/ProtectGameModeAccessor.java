package com.simplebuilding.modules.simpletweaks.protect.mixin;

import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Tests: lets time pass for a mock player whose game mode is not ticked. */
@Mixin(ServerPlayerGameMode.class)
public interface ProtectGameModeAccessor {
    @Accessor("gameTicks") int simpletweaks$gameTicks();
    @Accessor("gameTicks") void simpletweaks$setGameTicks(int ticks);
}
