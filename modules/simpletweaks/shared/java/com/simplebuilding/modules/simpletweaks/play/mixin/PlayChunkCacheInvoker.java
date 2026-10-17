package com.simplebuilding.modules.simpletweaks.play.mixin;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Pregeneration (T33): the non-blocking chunk request of the main thread (the public one waits for the result). */
@Mixin(ServerChunkCache.class)
public interface PlayChunkCacheInvoker {
    @Invoker("getChunkFutureMainThread")
    CompletableFuture<ChunkResult<ChunkAccess>> simpletweaks$request(int x, int z, ChunkStatus status, boolean load);
}
