package com.simplebuilding.modules.simpletweaks.claims;

import com.simplebuilding.modules.simpletweaks.claims.ClaimRights.Rules;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedNet;
import com.simplebuilding.modules.simpletweaks.table.ClaimGrid;
import com.simplebuilding.modules.simpletweaks.table.ClaimTableBlock;
import com.simplebuilding.modules.simpletweaks.table.TableService;
import com.simplebuilding.modules.simpletweaks.table.TableVisibility;
import com.simplebuilding.modules.simpletweaks.table.net.TableAreaPayload;
import com.simplebuilding.modules.simpletweaks.table.net.TableListPayload;
import com.simplebuilding.modules.simpletweaks.table.net.TableOpenPayload;
import com.simplebuilding.modules.simpletweaks.table.net.TableRequestPayload;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimRightsTests.roomy;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimRightsTests.seeded;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;

/** Phase 3 of the claim deed plan: the claim table's server side (open, area, list, visibility, bounds). */
public final class TableTests {
    private TableTests() {}

    private static void sending(List<CustomPacketPayload> sent, Runnable body) {
        var old = DeedNet.toClient;
        DeedNet.toClient = (p, payload) -> { sent.add(payload); return true; };
        try { body.run(); } finally { DeedNet.toClient = old; TableService.reset(); }
    }
    private static TableAreaPayload area(List<CustomPacketPayload> sent) { return (TableAreaPayload) sent.get(sent.size() - 1); }
    private static TableListPayload list(List<CustomPacketPayload> sent) { return (TableListPayload) sent.get(sent.size() - 1); }

    /** Cells of an answer that hold a palette entry, and the entries those cells show. */
    private static int filled(TableAreaPayload a) {
        byte[] cells = ClaimGrid.decode(a.cells(), a.w() * a.h());
        int n = 0;
        for (byte b : cells) if (b != 0) n++;
        return n;
    }
    private static int mineCells(TableAreaPayload a) {
        byte[] cells = ClaimGrid.decode(a.cells(), a.w() * a.h());
        int n = 0;
        for (byte b : cells) if (b != 0 && a.palette().get((b & 0xFF) - 1).mine()) n++;
        return n;
    }

    private static void area(GameTestHelper h, ServerPlayer p, BlockPos table, int w, int hh, int dx, int dz) {
        var c = ChunkPos.containing(table);
        TableService.resetThrottle();
        TableService.handle(new TableRequestPayload(table, TableRequestPayload.AREA, c.x() + dx, c.z() + dz, w, hh), p);
    }
    private static void ask(ServerPlayer p, BlockPos table, int kind, int a, int b) {
        TableService.resetThrottle();
        TableService.handle(new TableRequestPayload(table, kind, a, b, 0, 0), p);
    }

    /** Wire format bounds, block and item, and the visibility rules (pure). */
    public static void basics(GameTestHelper h) {
        yes(h, ClaimTableBlock.block() != net.minecraft.world.level.block.Blocks.AIR && ClaimTableBlock.item() != net.minecraft.world.item.Items.AIR, "Claim table block and item are registered");
        yes(h, TableVisibility.parse("all") == TableVisibility.ALL && TableVisibility.parse("owners_hidden") == TableVisibility.OWNERS_HIDDEN
                && TableVisibility.parse("own") == TableVisibility.OWN && TableVisibility.parse("off") == TableVisibility.OFF && TableVisibility.parse("x") == null, "Visibility values");
        yes(h, TableVisibility.ALL.shows(false, false) && TableVisibility.OWNERS_HIDDEN.shows(false, false) && !TableVisibility.OWN.shows(false, false) && TableVisibility.OWN.shows(false, true)
                && !TableVisibility.OFF.shows(false, true), "Who sees which claim");
        yes(h, TableVisibility.OFF.shows(true, false) && TableVisibility.OWN.shows(true, false) && TableVisibility.OWNERS_HIDDEN.showsName(true, false) && !TableVisibility.OWNERS_HIDDEN.showsName(false, false), "An operator sees everything with names");
        boolean refused = false;
        try { ClaimConfig.DEFAULT.withVisibility("everyone"); } catch (IllegalArgumentException e) { refused = true; }
        yes(h, refused && ClaimConfig.DEFAULT.visibility() == TableVisibility.ALL, "Config: unknown visibility refused, default all");
        var buf = Unpooled.buffer();
        TableRequestPayload.CODEC.encode(buf, new TableRequestPayload(new BlockPos(1, 2, 3), TableRequestPayload.AREA, -5, 6, 64, 64));
        var req = TableRequestPayload.CODEC.decode(buf);
        yes(h, req.pos().equals(new BlockPos(1, 2, 3)) && req.a() == -5 && req.b() == 6 && req.c() == 64 && req.d() == 64, "Request round trip");
        var cells = new byte[64 * 64];
        for (int i = 0; i < cells.length; i++) cells[i] = (byte) ((i / 7) % 3);
        var area = new TableAreaPayload(-3, 4, 64, 64, List.of(new TableAreaPayload.Entry("Ouzzi", 0x336699, true), new TableAreaPayload.Entry("", 0x808080, false)), ClaimGrid.encode(cells));
        buf = Unpooled.buffer();
        TableAreaPayload.CODEC.encode(buf, area);
        var back = TableAreaPayload.CODEC.decode(buf);
        yes(h, back.minX() == -3 && back.palette().equals(area.palette()) && java.util.Arrays.equals(ClaimGrid.decode(back.cells(), 4096), cells), "Area round trip incl. run-length cells");
        var rows = new ArrayList<TableListPayload.Row>();
        for (int i = 0; i < TableListPayload.PAGE; i++) rows.add(new TableListPayload.Row("P" + i, i + 1, 1, -i, i, 0x112233, i == 0));
        buf = Unpooled.buffer();
        TableListPayload.CODEC.encode(buf, new TableListPayload(2, 120, true, rows));
        var listBack = TableListPayload.CODEC.decode(buf);
        yes(h, listBack.page() == 2 && listBack.total() == 120 && listBack.onlyMine() && listBack.rows().equals(rows), "List round trip");
        rows.add(rows.get(0));
        boolean tooMany = false;
        try { TableListPayload.CODEC.encode(Unpooled.buffer(), new TableListPayload(0, 51, false, rows)); } catch (RuntimeException e) { tooMany = true; }
        yes(h, tooMany, "A page of 51 rows cannot be encoded");
        var tooLong = new ArrayList<TableAreaPayload.Entry>();
        for (int i = 0; i < 256; i++) tooLong.add(new TableAreaPayload.Entry("x", 0, false));
        boolean palette = false;
        try { TableAreaPayload.CODEC.encode(Unpooled.buffer(), new TableAreaPayload(0, 0, 1, 1, tooLong, new byte[2])); } catch (RuntimeException e) { palette = true; }
        yes(h, palette, "A palette of 256 entries cannot be encoded");
        h.succeed();
    }

    /** Open, area, list, per visibility mode, and every refusal. */
    public static void service(GameTestHelper h) {
        var server = h.getLevel().getServer(); var previous = Claims.get(server); var level = h.getLevel();
        var base = h.absolutePos(new BlockPos(2, 3, 2));
        var table = new BlockPos((base.getX() >> 4) * 16 + 16 + 4, base.getY(), (base.getZ() >> 4) * 16 + 8);
        var oldState = level.getBlockState(table);
        String dim = level.dimension().identifier().toString();
        var a = player(h, table.east(2)); var b = player(h, table.east(2)); var g = player(h, table.east(2)); var s = player(h, table.east(2));
        var chunk = ChunkPos.containing(table);
        try {
            level.setBlock(table, ClaimTableBlock.block().defaultBlockState(), 3);
            var root = temp();
            var key0 = new ClaimStore.Key(dim, ChunkPos.pack(chunk.x(), chunk.z()));
            var c = seeded(h, root, key0, a.getUUID(), new Rules(0, Map.of(g.getUUID(), ClaimRights.USE)), Map.of());
            yes(h, c.create(new ClaimStore.Key(dim, ChunkPos.pack(chunk.x() + 1, chunk.z())), a.getUUID(), 100), "A's second chunk");
            yes(h, c.create(new ClaimStore.Key(dim, ChunkPos.pack(chunk.x() + 3, chunk.z() + 2)), b.getUUID(), 100), "B's chunk");
            var sent = new ArrayList<CustomPacketPayload>();
            // mode all
            Claims.install(server, new Claims(roomy().withVisibility("all"), root, Map.of()));
            sending(sent, () -> {
                yes(h, TableService.open(s, table), "A table within reach opens");
                var open = (TableOpenPayload) sent.get(0);
                yes(h, open.pos().equals(table) && open.visibility() == TableVisibility.ALL.ordinal() && open.chunkX() == chunk.x() && open.chunkZ() == chunk.z() && open.dimension().equals(dim) && open.ownChunks() == 0, "Open payload");
                area(h, s, table, 8, 6, -2, -1);
                yes(h, filled(area(sent)) == 3 && area(sent).palette().size() == 2 && area(sent).palette().stream().noneMatch(e -> e.mine() || e.name().isEmpty()), "all: three claimed chunks, two owners with names, none is mine");
                sent.clear(); TableService.open(a, table); area(h, a, table, 8, 6, -2, -1);
                yes(h, mineCells(area(sent)) == 2 && filled(area(sent)) == 3, "The owner's own chunks are marked");
                sent.clear(); TableService.open(s, table);
                ask(s, table, TableRequestPayload.LIST, 0, 0);
                var l = list(sent);
                yes(h, l.total() == 2 && l.rows().size() == 2 && l.rows().get(0).chunks() == 2 && l.rows().get(1).chunks() == 1 && l.rows().get(0).dimensions() == 1 && l.rows().get(0).chunkX() != TableListPayload.NONE, "List: owners by size, with a chunk to center on");
                ask(s, table, TableRequestPayload.LIST, 1, 0);
                yes(h, list(sent).rows().isEmpty() && list(sent).total() == 2, "Page 2 is empty");
                sent.clear(); TableService.open(a, table);
                ask(a, table, TableRequestPayload.LIST, 0, 1);
                yes(h, list(sent).rows().size() == 1 && list(sent).rows().get(0).mine() && list(sent).rows().get(0).chunks() == 2, "Only mine");
            });
            // owners hidden
            Claims.install(server, new Claims(roomy().withVisibility("owners_hidden"), root, Map.of()));
            sent.clear();
            sending(sent, () -> {
                TableService.open(s, table); area(h, s, table, 8, 6, -2, -1);
                yes(h, filled(area(sent)) == 3 && area(sent).palette().stream().allMatch(e -> e.name().isEmpty()), "owners_hidden: outlines, no names for a stranger");
                ask(s, table, TableRequestPayload.LIST, 0, 0);
                yes(h, list(sent).rows().size() == 2 && list(sent).rows().stream().allMatch(r -> r.name().isEmpty()), "owners_hidden list: no names");
                sent.clear(); TableService.open(g, table); area(h, g, table, 8, 6, -2, -1);
                yes(h, area(sent).palette().stream().filter(e -> !e.name().isEmpty()).count() == 1 && area(sent).palette().size() == 2, "owners_hidden: a person with rights sees that owner's name only");
            });
            // own
            Claims.install(server, new Claims(roomy().withVisibility("own"), root, Map.of()));
            sent.clear();
            sending(sent, () -> {
                TableService.open(s, table); area(h, s, table, 8, 6, -2, -1);
                yes(h, filled(area(sent)) == 0 && area(sent).palette().isEmpty(), "own: a stranger sees nothing");
                ask(s, table, TableRequestPayload.LIST, 0, 0);
                yes(h, list(sent).total() == 0, "own: empty list for a stranger");
                TableService.open(g, table); area(h, g, table, 8, 6, -2, -1);
                yes(h, filled(area(sent)) == 2 && area(sent).palette().size() == 1, "own: rights holder sees the land he may use");
                TableService.open(b, table); area(h, b, table, 8, 6, -2, -1);
                yes(h, filled(area(sent)) == 1 && mineCells(area(sent)) == 1, "own: B sees only his chunk");
            });
            // off
            Claims.install(server, new Claims(roomy().withVisibility("off"), root, Map.of()));
            sent.clear();
            sending(sent, () -> {
                TableService.open(s, table);
                yes(h, ((TableOpenPayload) sent.get(0)).visibility() == TableVisibility.OFF.ordinal(), "off: the screen is told");
                int before = sent.size();
                area(h, s, table, 8, 6, -2, -1); ask(s, table, TableRequestPayload.LIST, 0, 0);
                yes(h, sent.size() == before, "off: no map data at all");
            });
            // refusals (mode all)
            Claims.install(server, new Claims(roomy(), root, Map.of()));
            sent.clear();
            sending(sent, () -> {
                area(h, s, table, 8, 6, -2, -1);
                yes(h, sent.isEmpty(), "No answer without an open table");
                TableService.open(s, table); sent.clear();
                area(h, s, table.above(), 8, 6, -2, -1);
                yes(h, sent.isEmpty(), "A different position than the opened table is refused");
                area(h, s, table, 65, 6, -2, -1); area(h, s, table, 8, 0, -2, -1); area(h, s, table, -3, 6, -2, -1);
                yes(h, sent.isEmpty(), "Sizes over 64 or below 1 are refused");
                area(h, s, table, 8, 6, 70, 0); area(h, s, table, 8, 6, -70, 0); area(h, s, table, 8, 6, 0, -TableService.RADIUS - 1);
                yes(h, sent.isEmpty(), "Areas beyond 64 chunks of the table are refused");
                area(h, s, table, 64, 64, -32, -32);
                yes(h, sent.size() == 1 && area(sent).w() == 64, "The largest allowed area is answered");
                sent.clear();
                ask(s, table, TableRequestPayload.LIST, -1, 0); ask(s, table, 7, 0, 0);
                yes(h, sent.isEmpty(), "Negative page and unknown request kinds are refused");
                s.setPos(table.getX() + 20.5, table.getY(), table.getZ() + .5);
                area(h, s, table, 8, 6, -2, -1);
                yes(h, sent.isEmpty(), "Out of reach: refused");
                s.setPos(table.getX() + 2.5, table.getY(), table.getZ() + .5);
                area(h, s, table, 8, 6, -2, -1);
                yes(h, sent.size() == 1, "Back in reach: answered");
                sent.clear();
                TableService.handle(new TableRequestPayload(table, TableRequestPayload.AREA, chunk.x(), chunk.z(), 2, 2), s);
                TableService.handle(new TableRequestPayload(table, TableRequestPayload.AREA, chunk.x(), chunk.z(), 2, 2), s);
                yes(h, sent.size() == 1, "A second request in the same tick is dropped (rate limit)");
                level.setBlock(table, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                area(h, s, table, 8, 6, -2, -1);
                yes(h, sent.size() == 1 && !TableService.open(s, table), "A removed table answers nothing and cannot be opened");
            });
            yes(h, DeedNet.toClient != null, "Hook restored");
        } finally {
            level.setBlock(table, oldState, 3);
            Claims.install(server, previous);
        }
        h.succeed();
    }
}
