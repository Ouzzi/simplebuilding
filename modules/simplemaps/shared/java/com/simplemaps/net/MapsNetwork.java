package com.simplemaps.net;

import com.simplemaps.MapsComponents;
import com.simplemaps.MapsItems;
import com.simplemaps.Waypoint;
import com.simplemaps.Waypoints;
import com.simplemaps.WayfinderData;
import com.simplemaps.WayfinderIds;
import com.simplemaps.SimpleMaps;
import com.simplemaps.mixin.ItemFrameAccessor;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;

/** Server side of the module's packets. Every handler validates its input; nothing here trusts the client. */
public final class MapsNetwork {
    /** Minimum ticks between two tile requests of one player, and the byte budget of one answer. */
    public static final int REQUEST_INTERVAL = 4, ANSWER_BYTES = 384 * 1024;
    /** Coordinates beyond the world border are refused. */
    public static final int MAX_COORD = 30_000_000;
    public static final double FRAME_REACH_SQR = 8.0 * 8.0;
    private static final Map<UUID, Integer> LAST_REQUEST = new HashMap<>();

    private MapsNetwork() {}

    public static void handleTiles(TileRequestPayload payload, ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        int now = server.getTickCount();
        Integer last = LAST_REQUEST.get(player.getUUID());
        if (last != null && now - last >= 0 && now - last < REQUEST_INTERVAL) return;
        LAST_REQUEST.put(player.getUUID(), now);
        if (LAST_REQUEST.size() > 4096) LAST_REQUEST.keySet().retainAll(server.getPlayerList().getPlayers().stream().map(ServerPlayer::getUUID).toList());
        int budget = ANSWER_BYTES;
        Set<Integer> maps = new LinkedHashSet<>();
        for (TileRequestPayload.Entry e : payload.entries()) {
            if (!WayfinderIds.exists(server, e.mapId())) continue;
            WayfinderData data = WayfinderData.get(server, e.mapId());
            if (maps.add(e.mapId())) {
                SimpleMaps.toClient.accept(player, new MapStatePayload(e.mapId(),
                        data.dimension().map(k -> k.identifier().toString()).orElse(""), data.tileCount()));
            }
            if (budget <= 0) continue;
            int zoom = WayfinderData.validZoom(e.zoom());
            WayfinderData.Rendered tile = data.render(zoom, e.tx(), e.tz());
            if (tile.version() == e.version()) continue;
            byte[] packed = tile.colors() == null ? new byte[0] : TileCodec.pack(tile.colors(), tile.heights());
            budget -= packed.length + 32;
            SimpleMaps.toClient.accept(player, new TileDataPayload(e.mapId(), zoom, e.tx(), e.tz(), tile.version(), packed));
        }
    }

    public static void handleWaypoint(WaypointEditPayload payload, ServerPlayer player) {
        if (payload.hand() < 0 || payload.hand() > 1) return;
        ItemStack stack = player.getItemInHand(payload.hand() == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        if (!MapsItems.isWayfinder(stack)) return;
        Waypoint w = payload.waypoint();
        Waypoints current = stack.getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY);
        Waypoints next;
        if (payload.delete()) {
            next = current.without(w.slot());
        } else {
            if (!valid(w)) return;
            next = current.with(w);
        }
        if (next.list().isEmpty()) stack.remove(MapsComponents.WAYPOINTS);
        else stack.set(MapsComponents.WAYPOINTS, next);
    }

    /** Coordinates inside the world, a mob head from {@code simplemaps:waypoint_heads} if any (owner F10). */
    public static boolean valid(Waypoint w) {
        if (Math.abs(w.x()) > MAX_COORD || Math.abs(w.z()) > MAX_COORD) return false;
        return w.head().map(id -> BuiltInRegistries.ITEM.getOptional(id)
                .map(item -> item.builtInRegistryHolder().is(MapsItems.WAYPOINT_HEADS)).orElse(false)).orElse(true);
    }

    public static void handleFrameView(FrameViewPayload payload, ServerPlayer player) {
        if (!(player.level().getEntity(payload.entityId()) instanceof ItemFrame frame)) return;
        if (player.distanceToSqr(frame) > FRAME_REACH_SQR || !MapsItems.isWayfinder(frame.getItem())) return;
        MapsComponents.View view = payload.view();
        if (Math.abs(view.x()) > MAX_COORD || Math.abs(view.z()) > MAX_COORD) return;
        ItemStack copy = frame.getItem().copy();
        copy.set(MapsComponents.VIEW, view);
        frame.getEntityData().set(ItemFrameAccessor.simplemaps$dataItem(), copy);
    }
}
