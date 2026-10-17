package com.simplebuilding.modules.simpletweaks.admin.vanish;

import com.simplebuilding.modules.simpletweaks.admin.mixin.*;
import net.minecraft.server.level.ServerPlayer;

/** Re-evaluates who tracks a player right now (Vanilla only does this when the player changes section). */
public final class Tracking {
    public static void refresh(ServerPlayer player) {
        var map = (AdminChunkMapAccessor) player.level().getChunkSource().chunkMap;
        Object tracked = map.simpletweaks$entityMap().get(player.getId());
        if (tracked != null) ((AdminTrackedEntityInvoker) tracked).simpletweaks$updatePlayers(player.level().players());
    }
    private Tracking() {}
}
