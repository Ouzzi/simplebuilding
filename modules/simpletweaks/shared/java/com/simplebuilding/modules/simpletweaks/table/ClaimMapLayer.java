package com.simplebuilding.modules.simpletweaks.table;

import com.simplebuilding.framework.api.MapLayerSources;
import com.simplebuilding.framework.api.MapLayerSources.Marker;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Claims on the wayfinder map of Simple Maps. Simple Tweaks does not know Simple Maps (and the other way round): it only
 * registers this source with the framework's {@link MapLayerSources}; with Simple Maps absent nothing asks for it.
 * The server decides what a viewer sees, with the same {@link TableVisibility} rules as the claim table; ids, member lists
 * and codes never leave. Every dimension works the same: the map asks for its own dimension. Each marker is a strip of neighbouring chunks of one owner in one row ({@code area:RRGGBB:M:W}).
 */
public final class ClaimMapLayer implements MapLayerSources.ViewerCollector {
    /** Markers per answer (the wire limit of one layer answer). */
    public static final int MAX = 256;
    public static final String ID = "claims";

    public static void register() { MapLayerSources.register("simpletweaks", ID, "layer.simpletweaks.claims", new ClaimMapLayer()); }

    @Override
    public List<Marker> collectFor(Object levelObject, UUID viewer, int minX, int minZ, int maxX, int maxZ) {
        var out = new ArrayList<Marker>();
        if (!(levelObject instanceof ServerLevel level) || viewer == null) return out;
        var server = level.getServer();
        var claims = Claims.get(server);
        if (claims == null || !Claims.enabled(server)) return out;
        var player = server.getPlayerList().getPlayer(viewer);
        boolean op = player != null && Claims.admin(player);
        var vis = op ? TableVisibility.ALL : claims.config.visibility();
        if (vis == TableVisibility.OFF) return out;
        String dimension = level.dimension().identifier().toString();
        int minCx = minX >> 4, maxCx = maxX >> 4, minCz = minZ >> 4, maxCz = maxZ >> 4;
        var hits = new ArrayList<long[]>(); // {cz, cx, ownerIndex}
        var owners = new ArrayList<UUID>();
        for (var e : claims.view().entrySet()) {
            var key = e.getKey();
            if (!key.dimension().equals(dimension)) continue;
            int cx = ChunkPos.getX(key.chunk()), cz = ChunkPos.getZ(key.chunk());
            if (cx < minCx || cx > maxCx || cz < minCz || cz > maxCz) continue;
            UUID owner = e.getValue().owner();
            if (!vis.shows(op, TableService.involved(claims, viewer, dimension, key.chunk(), owner))) continue;
            int at = owners.indexOf(owner);
            if (at < 0) { owners.add(owner); at = owners.size() - 1; }
            hits.add(new long[]{cz, cx, at});
        }
        hits.sort((a, b) -> a[0] != b[0] ? Long.compare(a[0], b[0]) : Long.compare(a[1], b[1]));
        for (int i = 0; i < hits.size() && out.size() < MAX; ) {
            var first = hits.get(i);
            int j = i + 1;
            while (j < hits.size() && hits.get(j)[0] == first[0] && hits.get(j)[2] == first[2] && hits.get(j)[1] == first[1] + (j - i)
                    && j - i < com.simplebuilding.modules.simpletweaks.table.ClaimGrid.MAX_SIDE) j++;
            UUID owner = owners.get((int) first[2]);
            boolean involved = TableService.involved(claims, viewer, dimension, ChunkPos.pack((int) first[1], (int) first[0]), owner);
            // Own land carries the viewer's free chunk credits (SP22-8); other land the owner's name as claimVisibility allows.
            String line = owner.equals(viewer) ? ownLine(claims.credits(viewer))
                    : vis.showsName(op, involved) ? "Claim of " + TableService.nameOf(server, owner) : "Foreign claim";
            out.add(new Marker((int) first[1] * 16, (int) first[0] * 16, String.format("area:%06x:%d:%d", ClaimGrid.color(owner) & 0xFFFFFF, owner.equals(viewer) ? 1 : 0, j - i), List.of(line)));
            i = j;
        }
        return out;
    }

    /** Tooltip line of the viewer's own land (plain text, like every map marker line). */
    public static String ownLine(int credits) { return "Your land · " + credits + (credits == 1 ? " credit" : " credits"); }
}
