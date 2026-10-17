package com.simplebuilding.modules.simpletweaks.admin.mixin;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Vanish: {@code ChunkMap.TrackedEntity.updatePlayers}. */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public interface AdminTrackedEntityInvoker {
    @Invoker("updatePlayers") void simpletweaks$updatePlayers(List<ServerPlayer> players);
}
