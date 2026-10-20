package com.simplemaps.net;

import com.simplebuilding.framework.api.MapLayerSources;
import com.simplemaps.MapsConfig;
import com.simplemaps.MapsItems;
import com.simplemaps.MapsComponents;
import com.simplemaps.SimpleMaps;
import com.simplemaps.WayfinderData;
import com.simplemaps.WayfinderIds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Server side of the map layers (loaded chunks and the sources other modules register, see {@link MapLayerSources}).
 * Authority stays here: the player must hold that map, the area is capped, requests are rate limited, and only chunks
 * and markers inside tiles the map has already explored are answered (the map never shows more than it knows).
 * Loaded chunks are only the viewer's own (inside the server view distance around them, same dimension) and forced ones
 * (chunk loaders, {@code /forceload}): chunks another player keeps loaded never show, the map must not betray where
 * somebody (maybe vanished) is (owner 19.10.).
 */
public final class LayerService {
    /** Minimum ticks between two layer requests of one player; the largest area (blocks per side) one answer covers. */
    public static final int REQUEST_INTERVAL = 10, MAX_SPAN = LayerDataPayload.MAX_SPAN_CHUNKS * 16;
    private static final Map<UUID, Integer> LAST = new HashMap<>();

    private LayerService() {}

    public static void handle(LayerRequestPayload p, ServerPlayer player) {
        if (!MapsConfig.enabled || !MapsConfig.layers) return;
        MinecraftServer server = player.level().getServer();
        int now = server.getTickCount();
        Integer last = LAST.get(player.getUUID());
        if (last != null && now - last >= 0 && now - last < REQUEST_INTERVAL) return;
        LAST.put(player.getUUID(), now);
        if (LAST.size() > 4096) LAST.keySet().retainAll(server.getPlayerList().getPlayers().stream().map(ServerPlayer::getUUID).toList());
        if (!holds(player, p.mapId()) || !WayfinderIds.exists(server, p.mapId())) return;
        WayfinderData data = WayfinderData.get(server, p.mapId());
        List<LayerDataPayload.Source> available = new ArrayList<>();
        for (MapLayerSources.Source s : MapLayerSources.all()) {
            if (available.size() < LayerRequestPayload.MAX_SOURCES && s.id().length() <= LayerRequestPayload.MAX_ID && s.titleKey().length() <= 96) {
                available.add(new LayerDataPayload.Source(s.id(), s.titleKey()));
            }
        }
        long w = (long) p.maxX() - p.minX(), h = (long) p.maxZ() - p.minZ();
        ResourceKey<Level> key = data.dimension().orElse(null);
        ServerLevel level = key == null ? null : server.getLevel(key);
        if (level == null || w < 0 || h < 0 || w > MAX_SPAN || h > MAX_SPAN
                || Math.abs((long) p.minX()) > MapsNetwork.MAX_COORD || Math.abs((long) p.minZ()) > MapsNetwork.MAX_COORD
                || Math.abs((long) p.maxX()) > MapsNetwork.MAX_COORD || Math.abs((long) p.maxZ()) > MapsNetwork.MAX_COORD) {
            SimpleMaps.toClient.accept(player, new LayerDataPayload(p.mapId(), new LayerDataPayload.Area(0, 0, 0, 0), new byte[0], List.of(), available));
            return;
        }
        int minCx = p.minX() >> 4, minCz = p.minZ() >> 4, cw = (p.maxX() >> 4) - minCx + 1, ch = (p.maxZ() >> 4) - minCz + 1;
        byte[] loaded = new byte[0];
        if (p.chunks()) {
            loaded = new byte[(cw * ch + 7) / 8];
            for (int dz = 0; dz < ch; dz++) {
                for (int dx = 0; dx < cw; dx++) {
                    int cx = minCx + dx, cz = minCz + dz;
                    if (explored(data, cx << 4, cz << 4) && shownChunk(level, player, cx, cz)) {
                        int bit = dz * cw + dx;
                        loaded[bit >> 3] |= (byte) (1 << (bit & 7));
                    }
                }
            }
        }
        List<LayerDataPayload.Marker> markers = new ArrayList<>();
        for (String id : p.sources()) {
            MapLayerSources.Source source = MapLayerSources.get(id);
            if (source == null) continue;
            List<MapLayerSources.Marker> found;
            try {
                found = MapLayerSources.collect(source, level, player.getUUID(), p.minX(), p.minZ(), p.maxX(), p.maxZ());
            } catch (RuntimeException e) {
                SimpleMaps.LOG.warn("Map layer {} failed: {}", id, e.toString());
                continue;
            }
            for (MapLayerSources.Marker m : found) {
                if (markers.size() >= LayerDataPayload.MAX_MARKERS) break;
                if (!explored(data, m.x(), m.z())) continue;
                List<String> lines = new ArrayList<>();
                for (String line : m.lines()) {
                    if (lines.size() < LayerDataPayload.MAX_LINES) lines.add(line.length() > LayerDataPayload.MAX_LINE ? line.substring(0, LayerDataPayload.MAX_LINE) : line);
                }
                markers.add(new LayerDataPayload.Marker(id, m.x(), m.z(), m.kind().length() > LayerDataPayload.Marker.MAX_KIND ? "" : m.kind(), lines));
            }
        }
        SimpleMaps.toClient.accept(player, new LayerDataPayload(p.mapId(), new LayerDataPayload.Area(minCx, minCz, cw, ch), loaded, markers, available));
    }

    /**
     * Whether the loaded-chunks layer may flag this chunk for {@code viewer}: loaded and either forced (chunk loader,
     * {@code /forceload}) or within the server view distance around the viewer in the same dimension.
     */
    public static boolean shownChunk(ServerLevel level, ServerPlayer viewer, int cx, int cz) {
        if (level.getChunkSource().getChunkNow(cx, cz) == null) return false;
        if (level.getForceLoadedChunks().contains(net.minecraft.world.level.ChunkPos.pack(cx, cz))) return true;
        return viewer.level() == level && net.minecraft.server.level.ChunkTrackingView.isInViewDistance(
                viewer.getBlockX() >> 4, viewer.getBlockZ() >> 4, level.getServer().getPlayerList().getViewDistance(), cx, cz);
    }

    private static boolean explored(WayfinderData data, int x, int z) {
        return data.tile(Math.floorDiv(x, WayfinderData.TILE), Math.floorDiv(z, WayfinderData.TILE)) != null;
    }

    private static boolean holds(ServerPlayer player, int mapId) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            MapsComponents.MapRef ref = stack.get(MapsComponents.MAP_ID);
            if (MapsItems.isWayfinder(stack) && ref != null && ref.id() == mapId) return true;
        }
        return false;
    }
}
