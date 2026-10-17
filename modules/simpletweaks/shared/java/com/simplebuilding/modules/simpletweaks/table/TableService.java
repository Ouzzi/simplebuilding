package com.simplebuilding.modules.simpletweaks.table;

import com.simplebuilding.modules.simpletweaks.claims.ClaimStore;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedNet;
import com.simplebuilding.modules.simpletweaks.table.net.TableAreaPayload;
import com.simplebuilding.modules.simpletweaks.table.net.TableListPayload;
import com.simplebuilding.modules.simpletweaks.table.net.TableOpenPayload;
import com.simplebuilding.modules.simpletweaks.table.net.TableRequestPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * Server side of the claim table. <b>The client is never trusted</b>: a request counts only while the player's last
 * opened table is still there and within reach, the dimension is the table's, sizes and rate are bounded, and what
 * is sent is filtered by the configured {@link TableVisibility}. No ids of other players, no member lists, no codes.
 */
public final class TableService {
    /** Minimum ticks between two table packets of one player (at most 4 per second). */
    public static final int REQUEST_TICKS = 5;
    /** Blocks (squared) a player may stand from the table. */
    public static final double REACH_SQ = 8.0 * 8.0;
    /** How far from the table's chunk (in chunks) a requested area may reach. */
    public static final int RADIUS = 64;
    private record Session(BlockPos pos, String dimension) {}
    private static final Map<UUID,Session> SESSIONS = new HashMap<>();
    private static final Map<UUID,Integer> LAST = new HashMap<>();

    private TableService() {}

    /** Tests only. */
    public static void reset() { SESSIONS.clear(); LAST.clear(); }

    /** Tests only: forget the packet times (a game test sends several packets in one tick). */
    public static void resetThrottle() { LAST.clear(); }

    private static boolean throttled(ServerPlayer player) {
        int now = player.level().getServer().getTickCount();
        Integer last = LAST.put(player.getUUID(), now);
        if (LAST.size() > 4096) {
            var online = new HashSet<UUID>();
            player.level().getServer().getPlayerList().getPlayers().forEach(p -> online.add(p.getUUID()));
            LAST.keySet().retainAll(online); SESSIONS.keySet().retainAll(online);
        }
        return last != null && now >= last && now - last < REQUEST_TICKS;
    }

    private static boolean usable(ServerPlayer player, BlockPos pos) {
        var level = player.level();
        return level.isLoaded(pos) && level.getBlockState(pos).is(ClaimTableBlock.block()) && player.distanceToSqr(Vec3.atCenterOf(pos)) <= REACH_SQ;
    }

    private static TableVisibility visibility(ServerPlayer player, Claims claims) {
        return Claims.admin(player) ? TableVisibility.ALL : claims.config.visibility();
    }

    /** Opens the table for the player; false if the player's client cannot show it. */
    public static boolean open(ServerPlayer player, BlockPos pos) {
        var server = player.level().getServer();
        var claims = Claims.get(server);
        if (claims == null || !Claims.enabled(server) || !usable(player, pos)) return false;
        String dimension = player.level().dimension().identifier().toString();
        var chunk = ChunkPos.containing(pos);
        var vis = visibility(player, claims);
        boolean sent = DeedNet.send(player, openPayload(claims, player, pos, vis, chunk, dimension));
        if (sent) SESSIONS.put(player.getUUID(), new Session(pos.immutable(), dimension));
        return sent;
    }

    /** The table's opening data including the player's land budget and the next expansion step. */
    public static TableOpenPayload openPayload(Claims claims, ServerPlayer player, BlockPos pos, TableVisibility vis, ChunkPos chunk, String dimension) {
        var id = player.getUUID();
        int budget = claims.budget(id);
        int state = TableOpenPayload.NONE, next = 0, levels = 0;
        var costs = new ArrayList<TableOpenPayload.Cost>();
        if (claims.config.expansionEnabled()) {
            var tier = claims.config.expansionTiers().stream().filter(t -> t.chunks() > budget).findFirst().orElse(null);
            if (tier == null) state = TableOpenPayload.MAX;
            else if (tier.chunks() > claims.config.maxClaimsPerPlayer()) state = TableOpenPayload.LIMIT;
            else {
                state = TableOpenPayload.BUY; next = tier.chunks(); levels = tier.levels();
                tier.items().forEach(c -> costs.add(new TableOpenPayload.Cost(c.id(), c.count())));
            }
        }
        return new TableOpenPayload(pos.immutable(), vis.ordinal(), chunk.x(), chunk.z(), claims.chunksOf(id), budget, dimension, state, next, levels, costs);
    }

    /** One request from the open screen. Always bounded; anything wrong is dropped without an answer. */
    public static void handle(TableRequestPayload req, ServerPlayer player) {
        var server = player.level().getServer();
        var claims = Claims.get(server);
        if (claims == null || !Claims.enabled(server) || throttled(player)) return;
        var session = SESSIONS.get(player.getUUID());
        if (session == null || !session.pos().equals(req.pos()) || !usable(player, session.pos())
                || !session.dimension().equals(player.level().dimension().identifier().toString())) return;
        var vis = visibility(player, claims);
        if (req.kind() == TableRequestPayload.EXPAND) { expand(player, claims, session, vis); return; }
        if (vis == TableVisibility.OFF) return;
        boolean op = Claims.admin(player);
        if (req.kind() == TableRequestPayload.AREA) {
            var table = ChunkPos.containing(session.pos());
            long w = req.c(), h = req.d(), x = req.a(), z = req.b();
            if (w < 1 || h < 1 || w > ClaimGrid.MAX_SIDE || h > ClaimGrid.MAX_SIDE) return;
            if (Math.abs(x - table.x()) > RADIUS || Math.abs(z - table.z()) > RADIUS
                    || Math.abs(x + w - 1 - table.x()) > RADIUS || Math.abs(z + h - 1 - table.z()) > RADIUS) return;
            DeedNet.send(player, area(server, claims, player, vis, op, session.dimension(), (int) x, (int) z, (int) w, (int) h));
        } else if (req.kind() == TableRequestPayload.LIST) {
            if (req.a() < 0 || req.a() > 100000) return;
            DeedNet.send(player, list(server, claims, player, vis, op, session.dimension(), req.a(), req.b() == 1));
        }
    }

    /** Buys the next step; the answer is an action bar message plus the refreshed table data (new budget, next step). */
    private static void expand(ServerPlayer player, Claims claims, Session session, TableVisibility vis) {
        var result = claims.expand(player);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.simpletweaks.claim_table.expand." + result.name().toLowerCase(java.util.Locale.ROOT),
                claims.budget(player.getUUID())), true);
        DeedNet.send(player, openPayload(claims, player, session.pos(), vis, ChunkPos.containing(session.pos()), session.dimension()));
    }

    static boolean involved(Claims claims, UUID me, String dimension, long chunk, UUID owner) {
        return owner.equals(me) || claims.rights(dimension, me, chunk) != 0;
    }

    static String nameOf(net.minecraft.server.MinecraftServer server, UUID owner) {
        String name = Claims.nameOf(server, owner);
        return name == null ? "?" : name.length() > 16 ? name.substring(0, 16) : name;
    }

    /** The owners of one area as the viewer may see them. */
    public static TableAreaPayload area(net.minecraft.server.MinecraftServer server, Claims claims, ServerPlayer viewer, TableVisibility vis, boolean op,
                                        String dimension, int minX, int minZ, int w, int h) {
        var cells = new byte[w * h];
        var palette = new ArrayList<TableAreaPayload.Entry>();
        var index = new HashMap<UUID,Integer>();
        UUID me = viewer.getUUID();
        int overflow = 0;
        for (var e : claims.view().entrySet()) {
            var key = e.getKey();
            if (!key.dimension().equals(dimension)) continue;
            int cx = ChunkPos.getX(key.chunk()) - minX, cz = ChunkPos.getZ(key.chunk()) - minZ;
            if (cx < 0 || cz < 0 || cx >= w || cz >= h) continue;
            UUID owner = e.getValue().owner();
            boolean involved = involved(claims, me, dimension, key.chunk(), owner);
            if (!vis.shows(op, involved)) continue;
            Integer at = index.get(owner);
            if (at == null) {
                if (palette.size() < ClaimGrid.MAX_PALETTE - 1) {
                    palette.add(new TableAreaPayload.Entry(vis.showsName(op, involved) ? nameOf(server, owner) : "", ClaimGrid.color(owner), owner.equals(me)));
                    at = palette.size();
                } else {
                    if (overflow == 0) { palette.add(new TableAreaPayload.Entry("", 0x808080, false)); overflow = palette.size(); }
                    at = overflow;
                }
                index.put(owner, at);
            }
            cells[cz * w + cx] = (byte) (int) at;
        }
        return new TableAreaPayload(minX, minZ, w, h, palette, ClaimGrid.encode(cells));
    }

    /** One page of the owner list (mine first, then by size). */
    public static TableListPayload list(net.minecraft.server.MinecraftServer server, Claims claims, ServerPlayer viewer, TableVisibility vis, boolean op,
                                        String dimension, int page, boolean onlyMine) {
        UUID me = viewer.getUUID();
        class Agg { int chunks; final Set<String> dims = new HashSet<>(); long first = Long.MAX_VALUE; boolean involved; }
        var owners = new LinkedHashMap<UUID,Agg>();
        for (var e : claims.view().entrySet()) {
            UUID owner = e.getValue().owner();
            if (onlyMine && !owner.equals(me)) continue;
            var key = e.getKey();
            boolean involved = involved(claims, me, key.dimension(), key.chunk(), owner);
            if (!vis.shows(op, involved)) continue;
            var agg = owners.computeIfAbsent(owner, o -> new Agg());
            agg.chunks++; agg.dims.add(key.dimension()); agg.involved |= involved;
            if (key.dimension().equals(dimension)) agg.first = Math.min(agg.first, key.chunk());
        }
        var rows = new ArrayList<Map.Entry<UUID,Agg>>(owners.entrySet());
        rows.sort((x, y) -> {
            boolean mx = x.getKey().equals(me), my = y.getKey().equals(me);
            if (mx != my) return mx ? -1 : 1;
            int c = Integer.compare(y.getValue().chunks, x.getValue().chunks);
            return c != 0 ? c : x.getKey().compareTo(y.getKey());
        });
        var out = new ArrayList<TableListPayload.Row>();
        for (int i = page * TableListPayload.PAGE; i < rows.size() && out.size() < TableListPayload.PAGE; i++) {
            var r = rows.get(i); var a = r.getValue(); UUID owner = r.getKey();
            boolean here = a.first != Long.MAX_VALUE;
            out.add(new TableListPayload.Row(vis.showsName(op, a.involved) ? nameOf(server, owner) : "", a.chunks, a.dims.size(),
                    here ? ChunkPos.getX(a.first) : TableListPayload.NONE, here ? ChunkPos.getZ(a.first) : TableListPayload.NONE, ClaimGrid.color(owner), owner.equals(me)));
        }
        return new TableListPayload(page, rows.size(), onlyMine, out);
    }
}
