package com.simplemaps.net;

import com.simplemaps.CubeNet;
import com.simplemaps.MapsComponents;
import com.simplemaps.MapsItems;
import com.simplemaps.Waypoint;
import com.simplemaps.Waypoints;
import com.simplemaps.WayfinderData;
import com.simplemaps.WayfinderIds;
import com.simplemaps.SimpleMaps;
import com.simplemaps.mixin.ItemFrameAccessor;
import com.simplebuilding.framework.api.Protection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
    /** Per player (a new join is a new object) and map: the structure-marks version the client already has. */
    private static final Map<ServerPlayer, Map<Integer, Integer>> SENT_MARKS = new java.util.WeakHashMap<>();

    private MapsNetwork() {}

    public static void handleTiles(TileRequestPayload payload, ServerPlayer player) {
        if (!com.simplemaps.MapsConfig.enabled) return;
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
                Map<Integer, Integer> seen = SENT_MARKS.computeIfAbsent(player, p -> new HashMap<>());
                boolean marksChanged = !Integer.valueOf(data.marksVersion()).equals(seen.get(e.mapId()));
                if (marksChanged) seen.put(e.mapId(), data.marksVersion());
                SimpleMaps.toClient.accept(player, new MapStatePayload(e.mapId(),
                        data.dimension().map(k -> k.identifier().toString()).orElse(""), data.tileCount(),
                        marksChanged, marksChanged ? data.marks() : java.util.List.of()));
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
            Waypoint cleaned = clean(w);
            if (cleaned == null) return;
            next = current.with(cleaned);
        }
        if (next.list().isEmpty()) stack.remove(MapsComponents.WAYPOINTS);
        else stack.set(MapsComponents.WAYPOINTS, next);
    }

    /**
     * Coordinates inside the world; the icon any registered item but air (owner N65); a valid side, and the 3D block model only
     * for an item with a block (anything else is shown from the front). Returns the cleaned waypoint, or null when refused.
     */
    public static Waypoint clean(Waypoint w) {
        if (Math.abs(w.x()) > MAX_COORD || Math.abs(w.z()) > MAX_COORD) return null;
        java.util.Optional<net.minecraft.world.item.Item> item = BuiltInRegistries.ITEM.getOptional(w.icon());
        if (item.isEmpty() || item.get() == net.minecraft.world.item.Items.AIR || !CubeNet.validFace(w.face())) return null;
        boolean block = item.get() instanceof net.minecraft.world.item.BlockItem;
        return block ? w : new Waypoint(w.slot(), w.x(), w.z(), w.name(), w.color(), w.icon(), CubeNet.FRONT);
    }

    public static boolean valid(Waypoint w) {
        return clean(w) != null;
    }

    public static void handleDisplay(DisplayPayload payload, ServerPlayer player) {
        if (payload.hand() < 0 || payload.hand() > 1) return;
        ItemStack stack = player.getItemInHand(payload.hand() == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        if (!MapsItems.isWayfinder(stack)) return;
        if (payload.display().equals(MapsComponents.Display.DEFAULT)) stack.remove(MapsComponents.DISPLAY);
        else stack.set(MapsComponents.DISPLAY, payload.display());
    }

    /** Owner B23-K1: the mega map in that hand shows another (visited) dimension; everything is checked by {@code select}. */
    public static void handleSelectDimension(SelectDimensionPayload payload, ServerPlayer player) {
        if (!com.simplemaps.MapsConfig.enabled || payload.hand() < 0 || payload.hand() > 1) return;
        ItemStack stack = player.getItemInHand(payload.hand() == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        com.simplemaps.MegaWayfinderMapItem.select(stack, payload.dimension());
    }

    /** Outcome of saving a framed view: stored, refused by a right (claim, fixed frame, adventure, spawn), or a bad request. */
    public enum FrameSave { SAVED, DENIED, INVALID }

    public static void handleFrameView(FrameViewPayload payload, ServerPlayer player) {
        FrameSave result = saveFrameView(player, payload.entityId(), payload.view());
        SimpleMaps.toClient.accept(player, new FrameSavedPayload(payload.entityId(), result.ordinal()));
    }

    /**
     * Owner 21.10.: the framed map takes the view only when the player saves (two-step confirmation on the client). The
     * request is checked here: the frame exists in the player's level within reach, holds a wayfinder map, the centre lies
     * inside the world; and the player may change the frame: no spectator, not a fixed frame, may build (not adventure),
     * outside spawn protection, and every framework {@link Protection} provider allows the container right there (claims
     * of Simple Tweaks protect item frames with that right). The new stack goes into the frame's synced data, so every
     * player watching the frame sees the new view.
     */
    public static FrameSave saveFrameView(ServerPlayer player, int entityId, MapsComponents.View view) {
        if (!com.simplemaps.MapsConfig.enabled || !(player.level().getEntity(entityId) instanceof ItemFrame frame) || frame.isRemoved()) return FrameSave.INVALID;
        if (player.distanceToSqr(frame) > FRAME_REACH_SQR || !MapsItems.isWayfinder(frame.getItem())) return FrameSave.INVALID;
        if (Math.abs(view.x()) > MAX_COORD || Math.abs(view.z()) > MAX_COORD) return FrameSave.INVALID;
        if (!mayChange(player, frame)) return FrameSave.DENIED;
        ItemStack copy = frame.getItem().copy();
        copy.set(MapsComponents.VIEW, view);
        frame.getEntityData().set(ItemFrameAccessor.simplemaps$dataItem(), copy);
        return FrameSave.SAVED;
    }

    /** Whether this player may change what the frame shows (the rights listed at {@link #saveFrameView}). */
    public static boolean mayChange(ServerPlayer player, ItemFrame frame) {
        BlockPos pos = frame.blockPosition();
        if (player.isSpectator() || ((ItemFrameAccessor) frame).simplemaps$fixed() || !player.mayBuild()) return false;
        if (!(frame.level() instanceof ServerLevel level) || !level.mayInteract(player, pos)) return false;
        MinecraftServer server = level.getServer();
        if (!Protection.active(server)) return true;
        boolean admin = Commands.hasPermission(Commands.LEVEL_OWNERS).test(player.createCommandSourceStack());
        return Protection.allows(server, new Protection.Target(player.getUUID(), level.dimension().identifier().toString(),
                pos.getX(), pos.getY(), pos.getZ(), admin), Protection.Action.CONTAINER);
    }

    /** What became of a {@link PictureMapPayload}. */
    public enum PictureResult { OK, OFF, PERMISSION, SIZE, INVALID, RATE }

    /** Window and per-player tile count of the picture rate limit: at most twice the largest wall per minute. */
    private static final int PICTURE_WINDOW = 1200;
    private static final Map<UUID, int[]> PICTURES = new HashMap<>();

    public static void handlePicture(PictureMapPayload payload, ServerPlayer player) {
        PictureResult result = createPictureMap(player, payload);
        if (result != PictureResult.OK) {
            String key = result.name().toLowerCase(java.util.Locale.ROOT);
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatableWithFallback("message.simplemaps.picture." + key,
                    switch (result) {
                        case OFF -> "Picture maps are off (pictureMap in simplemaps-server.json).";
                        case PERMISSION -> "You may not make picture maps on this server.";
                        case SIZE -> "That picture wall is larger than this server allows.";
                        case RATE -> "Too many picture maps in a short time, wait a minute.";
                        default -> "The picture data was refused.";
                    }));
        }
    }

    /**
     * One map of a picture wall (AUD-6): refused unless picture maps are on, the player has the permission level, the wall is
     * at most {@code pictureMapMaxSide} maps per edge, the index lies in it, exactly one map of colour bytes arrives with only
     * valid map colours, and the per-minute budget lasts. Then a locked, 1:1 Vanilla map with these colours goes to the
     * player's inventory (dropped at the feet when full).
     */
    public static PictureResult createPictureMap(ServerPlayer player, PictureMapPayload p) {
        if (!com.simplemaps.MapsConfig.pictureMap) return PictureResult.OFF;
        if (!hasLevel(player, com.simplemaps.MapsConfig.pictureMapPermission)) return PictureResult.PERMISSION;
        int max = com.simplemaps.MapsConfig.pictureMapMaxSide;
        if (p.cols() < 1 || p.rows() < 1 || p.cols() > max || p.rows() > max) return PictureResult.SIZE;
        if (p.col() < 0 || p.row() < 0 || p.col() >= p.cols() || p.row() >= p.rows() || p.colors().length != com.simplemaps.PictureMaps.BYTES) return PictureResult.INVALID;
        for (byte b : p.colors()) if (!com.simplemaps.PictureMaps.valid(b)) return PictureResult.INVALID;
        int now = player.level().getServer().getTickCount();
        int[] window = PICTURES.computeIfAbsent(player.getUUID(), k -> new int[]{now, 0});
        if (now - window[0] < 0 || now - window[0] >= PICTURE_WINDOW) { window[0] = now; window[1] = 0; }
        if (window[1] >= 2 * max * max) return PictureResult.RATE;
        window[1]++;
        if (PICTURES.size() > 4096) PICTURES.keySet().retainAll(player.level().getServer().getPlayerList().getPlayers().stream().map(ServerPlayer::getUUID).toList());
        ServerLevel level = player.level();
        ItemStack stack = net.minecraft.world.item.MapItem.create(level, 0, 0, (byte) 0, false, false);
        net.minecraft.world.level.saveddata.maps.MapItemSavedData data = net.minecraft.world.item.MapItem.getSavedData(stack, level);
        if (data == null) return PictureResult.INVALID;
        System.arraycopy(p.colors(), 0, data.colors, 0, com.simplemaps.PictureMaps.BYTES);
        data.locked = true;
        data.setDirty();
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.translatableWithFallback(
                "item.simplemaps.picture_map", "Picture %s/%s", p.col() + 1 + p.row() * p.cols(), p.cols() * p.rows()));
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        return PictureResult.OK;
    }

    /** Vanilla permission level 0..4 (0 = everyone), fresh from the operator list like commands. */
    static boolean hasLevel(ServerPlayer player, int level) {
        if (level <= 0) return true;
        var check = switch (level) {
            case 1 -> Commands.LEVEL_MODERATORS;
            case 2 -> Commands.LEVEL_GAMEMASTERS;
            case 3 -> Commands.LEVEL_ADMINS;
            default -> Commands.LEVEL_OWNERS;
        };
        return check.check(player.createCommandSourceStack().permissions());
    }
}
