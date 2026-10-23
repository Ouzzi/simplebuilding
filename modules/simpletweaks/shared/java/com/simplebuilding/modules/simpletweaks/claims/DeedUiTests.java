package com.simplebuilding.modules.simpletweaks.claims;

import com.simplebuilding.modules.simpletweaks.claims.ClaimConfig.Cost;
import com.simplebuilding.modules.simpletweaks.claims.ClaimConfig.Tier;
import com.simplebuilding.modules.simpletweaks.deed.ClaimDeedItem;
import com.simplebuilding.modules.simpletweaks.deed.ClaimPictures;
import com.simplebuilding.modules.simpletweaks.deed.DeedData;
import com.simplebuilding.modules.simpletweaks.deed.DeedService;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedActionPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedChunksPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedChunksRequest;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedMap;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedNet;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedOpenPayload;
import io.netty.buffer.Unpooled;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;

/** SP22-8 branch 2: the deed's map view - chunk pictures (only loaded chunks), the tile packets, the buttons as packets. */
public final class DeedUiTests {
    private DeedUiTests() {}

    private static final List<Tier> TIERS = List.of(new Tier(6, List.of(new Cost("minecraft:iron_ingot", 2)), 1));
    /** Start 4, one step to 6, server limit 8, every dimension. */
    private static ClaimConfig config() { return new ClaimConfig(true, 8, 8, 50, 20, false, 0, List.of(ClaimConfig.ALL_DIMENSIONS)).withExpansion(4, TIERS); }
    private static ClaimStore.Key key(String dim, int x, int z) { return new ClaimStore.Key(dim, ChunkPos.pack(x, z)); }
    private static ItemStack deedOf(UUID owner) { var s = new ItemStack(ClaimDeedItem.item()); s.set(DeedData.TYPE, new DeedData(owner, "Owner", 0)); return s; }

    /** Records what the server sends while the body runs. */
    private static List<CustomPacketPayload> sending(Runnable body) {
        var sent = new ArrayList<CustomPacketPayload>();
        var old = DeedNet.toClient;
        DeedNet.toClient = (p, payload) -> { sent.add(payload); return true; };
        try { body.run(); } finally { DeedNet.toClient = old; DeedService.resetThrottle(); }
        return sent;
    }
    private static DeedOpenPayload lastOpen(List<CustomPacketPayload> sent) {
        for (int i = sent.size() - 1; i >= 0; i--) if (sent.get(i) instanceof DeedOpenPayload o) return o;
        return null;
    }
    private static List<DeedChunksPayload> chunks(List<CustomPacketPayload> sent) {
        return sent.stream().filter(p -> p instanceof DeedChunksPayload).map(p -> (DeedChunksPayload) p).toList();
    }

    /** Pictures only of loaded chunks, nothing loaded for them, at most 4 per scan, refreshed after 6000 ticks, own file. */
    public static void pictures(GameTestHelper h) {
        var server = h.getLevel().getServer(); var level = h.getLevel();
        String dim = level.dimension().identifier().toString();
        try {
            var file = temp().resolve(ClaimPictures.FILE);
            var pictures = ClaimPictures.open(file);
            var here = ChunkPos.containing(h.absolutePos(new BlockPos(1, 2, 1)));
            var near = key(dim, here.x(), here.z());
            int fx = here.x() + 30000, fz = here.z() + 30000;
            var far = key(dim, fx, fz);
            var claim = new ClaimStore.Claim(UUID.randomUUID());
            yes(h, level.getChunkSource().getChunkNow(fx, fz) == null, "The far chunk is not loaded");
            long now = 1_000_000;
            yes(h, pictures.scan(server, Map.of(near, claim, far, claim), now) == 1, "Only the loaded chunk is pictured");
            var p = pictures.picture(near);
            yes(h, p != null && p.pixels().length == 256 && p.time() == now, "Picture: 16x16 map colours and its time");
            boolean drawn = false;
            for (byte b : p.pixels()) drawn |= b != 0;
            yes(h, drawn, "The picture shows the ground of the test area");
            yes(h, pictures.picture(far) == null && level.getChunkSource().getChunkNow(fx, fz) == null, "Unloaded chunk: no picture and still not loaded");
            yes(h, pictures.scan(server, Map.of(near, claim, far, claim), now + ClaimPictures.REFRESH_TICKS - 1) == 0, "No new picture before 6000 ticks");
            yes(h, pictures.scan(server, Map.of(near, claim, far, claim), now + ClaimPictures.REFRESH_TICKS) == 1
                    && pictures.picture(near).time() == now + ClaimPictures.REFRESH_TICKS, "After 6000 ticks the loaded chunk is pictured again");
            // at most 4 per scan: six loaded chunks, pictures missing first
            var six = new java.util.HashMap<ClaimStore.Key, ClaimStore.Claim>();
            for (int i = 0; i < 6; i++) { level.getChunk(here.x() + i, here.z() + 3); six.put(key(dim, here.x() + i, here.z() + 3), claim); }
            var fresh = ClaimPictures.open(temp().resolve(ClaimPictures.FILE));
            yes(h, fresh.scan(server, six, now) == ClaimPictures.PER_SCAN, "At most 4 pictures per scan");
            yes(h, fresh.scan(server, six, now) == 2 && fresh.size() == 6, "The next scan takes the rest");
            // given up: the picture goes with the claim
            yes(h, pictures.scan(server, Map.of(far, claim), now + 2 * ClaimPictures.REFRESH_TICKS) == 0 && pictures.picture(near) == null && pictures.size() == 0, "A chunk no longer claimed loses its picture");
            pictures.scan(server, Map.of(near, claim), now);
            pictures.save();
            yes(h, Files.exists(file), "Own picture file written");
            var loaded = ClaimPictures.open(file);
            yes(h, loaded.size() == 1 && java.util.Arrays.equals(loaded.picture(near).pixels(), pictures.picture(near).pixels()) && loaded.picture(near).time() == now, "Pictures survive a restart");
            // a broken file never blocks: it is moved aside and the deed starts without pictures
            Files.writeString(file, "not nbt at all");
            var broken = ClaimPictures.open(file);
            boolean kept;
            try (var listing = Files.list(file.getParent())) { kept = listing.anyMatch(f -> f.getFileName().toString().startsWith(ClaimPictures.FILE + ".broken-")); }
            yes(h, broken.size() == 0 && kept && !Files.exists(file), "Broken picture file: moved aside, empty");
            yes(h, broken.scan(server, Map.of(near, claim), now) == 1, "... and pictures work again");
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        h.succeed();
    }

    /** Tiles only for the owner or a holder with a right; only the owner's chunks; tabs for further dimensions with land; throttled. */
    public static void tiles(GameTestHelper h) {
        var server = h.getLevel().getServer(); var previous = Claims.get(server); var previousPictures = ClaimPictures.install(server, ClaimPictures.open(temp().resolve(ClaimPictures.FILE)));
        String dim = h.getLevel().dimension().identifier().toString();
        try {
            var c = new Claims(config(), temp(), Map.of()); Claims.install(server, c);
            var pos = h.absolutePos(new BlockPos(1, 2, 1));
            var here = ChunkPos.containing(pos);
            var owner = player(h, pos); UUID me = owner.getUUID();
            long t0 = server.overworld().getGameTime();
            yes(h, c.create(key(dim, here.x(), here.z()), me, t0 - 9000) && c.create(key(dim, here.x() + 30000, here.z()), me, t0 - 8000)
                    && c.create(key("simpledimensions:farm", 3, 3), me, t0 - 7000), "Seed: two chunks here, one in a custom dimension");
            var otherOwner = UUID.randomUUID();
            yes(h, c.create(key(dim, here.x() + 1, here.z()), otherOwner, t0 - 6000), "A neighbour's chunk");
            ClaimPictures.get(server).scan(server, c.view(), server.overworld().getGameTime());
            owner.setItemInHand(InteractionHand.MAIN_HAND, deedOf(me));
            var sent = sending(() -> DeedService.open(owner, 0));
            var open = lastOpen(sent);
            var m = open.map();
            yes(h, open.mode() == DeedOpenPayload.EDIT && m.map() && m.owns() && m.credits() == 1 && open.landChunks() == 3, "Owner: map view, 3 chunks, 1 credit");
            yes(h, m.tabs().size() == 4 && m.tabs().get(0).dimension().equals("minecraft:overworld") && m.tabs().get(1).dimension().equals("minecraft:the_nether")
                    && m.tabs().get(2).dimension().equals("minecraft:the_end") && m.tabs().get(3).equals(new DeedMap.Tab("simpledimensions:farm", 1))
                    && m.chunksIn(dim) == 2, "Tabs: Overworld, Nether, End always, the farm because of land there: " + m.tabs());
            yes(h, m.here() == DeedMap.HERE_OWN && m.buy() == DeedMap.BUY && m.nextChunks() == 6 && m.nextLevels() == 1
                    && m.nextCosts().equals(List.of(new DeedMap.Cost("minecraft:iron_ingot", 2))), "Standing on own land; next step offered");
            boolean maps = com.simplebuilding.modules.simpletweaks.play.Mods.loaded("simplemaps") && com.simplebuilding.framework.api.SubMods.enabled("simplemaps");
            yes(h, m.mapsPresent() == maps, "mapsPresent follows the loaded Simple Maps (" + maps + ")");
            sent = sending(() -> DeedService.handleChunks(new DeedChunksRequest(0, dim), owner));
            var pages = chunks(sent);
            yes(h, pages.size() == 1 && pages.get(0).pages() == 1 && pages.get(0).tiles().size() == 2, "Owner: one page with the two own chunks, not the neighbour's: " + pages);
            var tiles = pages.get(0).tiles();
            var loadedTile = tiles.stream().filter(t -> t.x() == here.x()).findFirst().orElseThrow();
            var farTile = tiles.stream().filter(t -> t.x() == here.x() + 30000).findFirst().orElseThrow();
            yes(h, loadedTile.hasPicture() && loadedTile.age() >= 0 && !farTile.hasPicture() && farTile.age() == DeedChunksPayload.NO_PICTURE, "Picture of the loaded chunk, none of the unloaded one");
            sent = sending(() -> DeedService.handleChunks(new DeedChunksRequest(0, "simpledimensions:farm"), owner));
            yes(h, chunks(sent).size() == 1 && chunks(sent).get(0).tiles().equals(List.of(new DeedChunksPayload.Tile(3, 3, DeedChunksPayload.NO_PICTURE, new byte[0]))), "Custom dimension tab: its chunk");
            sent = sending(() -> { DeedService.handleChunks(new DeedChunksRequest(0, dim), owner); DeedService.handleChunks(new DeedChunksRequest(0, dim), owner); });
            yes(h, chunks(sent).size() == 1, "A second request in the same tick is dropped");
            // a stranger with a copy of the deed but no right: nothing
            var stranger = player(h, pos);
            stranger.setItemInHand(InteractionHand.MAIN_HAND, deedOf(me));
            sent = sending(() -> { DeedService.open(stranger, 0); DeedService.handleChunks(new DeedChunksRequest(0, dim), stranger); });
            yes(h, lastOpen(sent).map().equals(DeedMap.NONE) && chunks(sent).isEmpty(), "No right: no map, no tiles (C7)");
            // a holder with a right in one chunk sees the land, but buys nothing
            var reader = player(h, pos);
            yes(h, c.trust(owner, reader.getUUID(), ClaimRights.USE), "Owner grants U here");
            reader.setItemInHand(InteractionHand.MAIN_HAND, deedOf(me));
            sent = sending(() -> { DeedService.open(reader, 0); DeedService.handleChunks(new DeedChunksRequest(0, dim), reader); });
            var rm = lastOpen(sent).map();
            yes(h, rm.map() && !rm.owns() && rm.credits() == 0 && rm.buy() == DeedMap.OFF && chunks(sent).size() == 1 && chunks(sent).get(0).tiles().size() == 2, "Reader: map and tiles, no buying");
            // empty hand, unbound deed: nothing
            var plain = player(h, pos);
            plain.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ClaimDeedItem.item()));
            sent = sending(() -> { DeedService.handleChunks(new DeedChunksRequest(0, dim), plain); DeedService.handleChunks(new DeedChunksRequest(1, dim), owner); });
            yes(h, sent.isEmpty(), "Unbound deed or empty hand: no tiles");
        } finally { Claims.install(server, previous); ClaimPictures.install(server, previousPictures); }
        h.succeed();
    }

    /** The buttons as packets: buy ahead, claim here (with its picture), give up with one credit back; only the owner. */
    public static void actions(GameTestHelper h) {
        var server = h.getLevel().getServer(); var previous = Claims.get(server);
        var pictures = ClaimPictures.open(temp().resolve(ClaimPictures.FILE)); var previousPictures = ClaimPictures.install(server, pictures);
        String dim = h.getLevel().dimension().identifier().toString();
        try {
            var root = temp(); var c = new Claims(config(), root, Map.of()); Claims.install(server, c);
            // stand in a chunk far from the world spawn (never claimable) and load it: "claim here" pictures it at once
            var pos = h.absolutePos(new BlockPos(1, 2, 1)).offset(4096, 0, 4096);
            h.getLevel().getChunk(pos);
            var here = ChunkPos.containing(pos);
            var owner = player(h, pos); UUID me = owner.getUUID();
            long t0 = server.overworld().getGameTime();
            yes(h, c.create(key(dim, here.x() + 50, here.z()), me, t0 - 9000), "Seed: one chunk elsewhere");
            owner.setItemInHand(InteractionHand.MAIN_HAND, deedOf(me));
            owner.getInventory().add(new ItemStack(Items.IRON_INGOT, 2)); owner.giveExperienceLevels(1);
            var sent = sending(() -> DeedService.handleAction(new DeedActionPayload(0, DeedActionPayload.BUY, dim, 0), owner));
            yes(h, c.budget(me) == 6 && c.credits(me) == 5 && lastOpen(sent).map().result().equals("buy.ok"), "Buy: next step, answered with a fresh snapshot");
            yes(h, owner.getInventory().countItem(Items.IRON_INGOT) == 0 && owner.experienceLevel == 0, "The step's cost was taken");
            Claims.install(server, c = new Claims(config(), root, Map.of())); // a restart clears the cooldown
            var cc = c;
            sent = sending(() -> DeedService.handleAction(new DeedActionPayload(0, DeedActionPayload.CLAIM_HERE, dim, 0), owner));
            var standing = key(dim, here.x(), here.z());
            yes(h, me.equals(cc.view().get(standing).owner()) && cc.credits(me) == 4 && lastOpen(sent).map().result().equals("claim.ok"), "Claim here with a credit");
            yes(h, pictures.picture(standing) != null, "Claiming takes the picture at once");
            yes(h, lastOpen(sent).map().here() == DeedMap.HERE_OWN, "The fresh snapshot knows the chunk is own now");
            sent = sending(() -> DeedService.handleAction(new DeedActionPayload(0, DeedActionPayload.UNCLAIM, dim, standing.chunk()), owner));
            yes(h, cc.view().containsKey(standing) && lastOpen(sent).map().result().equals("unclaim.cooldown"), "Right after a change: cooldown, nothing given up");
            Claims.install(server, c = new Claims(config(), root, Map.of()));
            var c2 = c;
            // forged: somebody holding a copy of the owner's deed cannot give up the owner's land
            var thief = player(h, pos);
            thief.setItemInHand(InteractionHand.MAIN_HAND, deedOf(me));
            sent = sending(() -> DeedService.handleAction(new DeedActionPayload(0, DeedActionPayload.UNCLAIM, dim, standing.chunk()), thief));
            yes(h, sent.isEmpty() && c2.view().containsKey(standing), "A forged deed gives nothing up and gets no answer");
            var blank = player(h, pos);
            blank.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ClaimDeedItem.item()));
            sent = sending(() -> DeedService.handleAction(new DeedActionPayload(0, DeedActionPayload.BUY, dim, 0), blank));
            yes(h, sent.isEmpty(), "An unbound deed has no map buttons");
            sent = sending(() -> DeedService.handleAction(new DeedActionPayload(0, DeedActionPayload.UNCLAIM, "../bad", 0), owner));
            yes(h, sent.isEmpty() && c2.chunksOf(me) == 2, "A bad dimension is dropped");
            sent = sending(() -> DeedService.handleAction(new DeedActionPayload(0, 9, dim, 0), owner));
            yes(h, sent.isEmpty(), "An unknown action is dropped");
            owner.getInventory().add(new ItemStack(Items.DIAMOND, 3));
            sent = sending(() -> DeedService.handleAction(new DeedActionPayload(0, DeedActionPayload.UNCLAIM, dim, standing.chunk()), owner));
            yes(h, !c2.view().containsKey(standing) && c2.credits(me) == 5 && c2.budget(me) == 6 && lastOpen(sent).map().result().equals("unclaim.ok"), "Given up: one credit back, budget stays");
            yes(h, owner.getInventory().countItem(Items.IRON_INGOT) == 0 && owner.getInventory().countItem(Items.DIAMOND) == 3 && owner.experienceLevel == 0, "No refund of anything");
            pictures.scan(server, c2.view(), server.overworld().getGameTime());
            yes(h, pictures.picture(standing) == null, "The given-up chunk loses its picture");
        } finally { Claims.install(server, previous); ClaimPictures.install(server, previousPictures); DeedService.resetThrottle(); }
        h.succeed();
    }

    /** Bounded codecs of the map packets. */
    public static void codecs(GameTestHelper h) {
        var map = new DeedMap(true, true, 2, DeedMap.HERE_FREE, DeedMap.BUY, 8, 15, List.of(new DeedMap.Cost("minecraft:diamond", 2)), false,
                List.of(new DeedMap.Tab("minecraft:overworld", 6), new DeedMap.Tab("simpledimensions:farm", 1)), "claim.ok");
        var open = new DeedOpenPayload(DeedOpenPayload.EDIT, 0, UUID.randomUUID(), "Ouzzi", "", 1, ClaimRights.ALL, 6, 8, 8, 1, 2, "minecraft:overworld", "", List.of(), List.of(), map);
        var b = Unpooled.buffer(); DeedOpenPayload.CODEC.encode(b, open);
        yes(h, DeedOpenPayload.CODEC.decode(b).equals(open), "Open packet with the map part: round trip");
        byte[] pixels = new byte[256]; pixels[7] = 42;
        var page = new DeedChunksPayload("minecraft:overworld", 0, 1, List.of(new DeedChunksPayload.Tile(-3, 9, 1200, pixels), new DeedChunksPayload.Tile(5, 5, DeedChunksPayload.NO_PICTURE, new byte[0])));
        var cb = Unpooled.buffer(); DeedChunksPayload.CODEC.encode(cb, page);
        yes(h, DeedChunksPayload.CODEC.decode(cb).equals(page), "Tile page round trip");
        var many = new ArrayList<DeedChunksPayload.Tile>();
        for (int i = 0; i <= DeedChunksPayload.PAGE; i++) many.add(new DeedChunksPayload.Tile(i, 0, 0, new byte[0]));
        boolean refused = false;
        try { DeedChunksPayload.CODEC.encode(Unpooled.buffer(), new DeedChunksPayload("minecraft:overworld", 0, 1, many)); } catch (RuntimeException e) { refused = true; }
        yes(h, refused, "At most 64 tiles per page");
        boolean big = false;
        try { DeedChunksPayload.CODEC.encode(Unpooled.buffer(), new DeedChunksPayload("minecraft:overworld", 0, 1, List.of(new DeedChunksPayload.Tile(0, 0, 0, new byte[257])))); } catch (RuntimeException e) { big = true; }
        yes(h, big, "A picture is at most 256 bytes");
        var action = new DeedActionPayload(1, DeedActionPayload.UNCLAIM, "minecraft:the_nether", ChunkPos.pack(-7, 12));
        var ab = Unpooled.buffer(); DeedActionPayload.CODEC.encode(ab, action);
        yes(h, DeedActionPayload.CODEC.decode(ab).equals(action), "Action packet round trip");
        var request = new DeedChunksRequest(0, "minecraft:the_end");
        var rb = Unpooled.buffer(); DeedChunksRequest.CODEC.encode(rb, request);
        yes(h, DeedChunksRequest.CODEC.decode(rb).equals(request), "Chunk request round trip");
        h.succeed();
    }
}
