package com.simplebuilding.tweaks;

import com.simplebuilding.framework.api.MapLayerSources;
import com.simplebuilding.tweaks.block.entity.ChunkLoaderRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

/** Wayfinder map layer (Simple Maps, framework contract, no import of the map module): active chunk loaders by dimension. 26.3 only (the framework module). */
final class ChunkLoaderMapLayer {
    private ChunkLoaderMapLayer() {
    }

    static void register() {
        MapLayerSources.register("simplebuilding", "chunk_loaders", "layer.simplebuilding.chunk_loaders",
                (level, minX, minZ, maxX, maxZ) -> {
                    ServerLevel serverLevel = (ServerLevel) level;
                    String dimension = serverLevel.dimension().identifier().toString();
                    List<MapLayerSources.Marker> out = new ArrayList<>();
                    for (ChunkLoaderRegistry.Entry e : ChunkLoaderRegistry.all(serverLevel.getServer())) {
                        if (!e.active() || !e.dimension().equals(dimension)) continue;
                        int x = e.pos().getX(), z = e.pos().getZ();
                        if (x < minX || x > maxX || z < minZ || z > maxZ) continue;
                        out.add(new MapLayerSources.Marker(x, z, "chunk_loader", List.of("Chunk loader (tier " + e.tier() + ")")));
                    }
                    return out;
                });
    }
}
