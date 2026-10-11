package com.simplemaps.test;

import com.google.gson.JsonObject;
import com.mojang.serialization.Dynamic;
import com.simplemaps.Cartography;
import com.simplemaps.MapsComponents;
import com.simplemaps.MapsConfig;
import com.simplemaps.MapsItems;
import com.simplemaps.MapsLoot;
import com.simplemaps.Reveal;
import com.simplemaps.SimpleMaps;
import com.simplemaps.Waypoint;
import com.simplemaps.Waypoints;
import com.simplemaps.WayfinderData;
import com.simplemaps.WayfinderMapItem;
import com.simplemaps.net.FrameViewPayload;
import com.simplemaps.net.MapsNetwork;
import com.simplemaps.net.TileCodec;
import com.simplemaps.net.WaypointEditPayload;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.phys.Vec3;

/** Server tests of Simple Maps; the loader adapters register them as {@code simplemaps:module_game_test_<name>}. */
public final class MapsTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    static {
        ALL.put("registered", MapsTests::registered);
        ALL.put("config_bounds", MapsTests::configBounds);
        ALL.put("waypoints", MapsTests::waypoints);
        ALL.put("tile_storage", MapsTests::tileStorage);
        ALL.put("reveal_explores", MapsTests::revealExplores);
        ALL.put("dimension_binding", MapsTests::dimensionBinding);
        ALL.put("cartography_copy", MapsTests::cartographyCopy);
        ALL.put("cartography_extend", MapsTests::cartographyExtend);
        ALL.put("cartography_combine", MapsTests::cartographyCombine);
        ALL.put("cartography_switches", MapsTests::cartographySwitches);
        ALL.put("waypoint_edit", MapsTests::waypointEdit);
        ALL.put("frame_view", MapsTests::frameView);
        ALL.put("loot", MapsTests::loot);
        ALL.put("structure_marks", MapsTests::structureMarks);
        ALL.put("host_switch", MapsTests::hostSwitch);
    }

    private MapsTests() {}

    private static void registered(GameTestHelper h) {
        for (String id : List.of("wayfinder_map", "nether_wayfinder_map", "end_wayfinder_map")) {
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleMaps.id(id)), "item missing: " + id);
            h.assertTrue(h.getLevel().getServer().getRecipeManager()
                    .byKey(ResourceKey.create(Registries.RECIPE, SimpleMaps.id(id))).isPresent(), "recipe missing: " + id);
        }
        ServerLevel nether = h.getLevel().getServer().getLevel(Level.NETHER), end = h.getLevel().getServer().getLevel(Level.END);
        h.assertTrue(nether != null && nether.dimensionTypeRegistration().is(MapsItems.NETHER_DIMENSIONS), "nether tag");
        h.assertTrue(end != null && end.dimensionTypeRegistration().is(MapsItems.END_DIMENSIONS), "end tag");
        h.assertTrue(new ItemStack(Items.ZOMBIE_HEAD).is(MapsItems.WAYPOINT_HEADS) && new ItemStack(Items.DRAGON_HEAD).is(MapsItems.WAYPOINT_HEADS),
                "every mob head can be a waypoint icon");
        h.assertTrue(!new ItemStack(Items.PUMPKIN).is(MapsItems.WAYPOINT_HEADS), "pumpkin is no head");
        h.succeed();
    }

    private static void configBounds(GameTestHelper h) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("revealRadius", 100000);
            json.addProperty("maxTilesPerMap", -5);
            json.addProperty("allowCopy", false);
            MapsConfig.apply(json);
            h.assertTrue(MapsConfig.revealRadius == MapsConfig.REVEAL_RADIUS_MAX, "radius clamped");
            h.assertTrue(MapsConfig.maxTilesPerMap == MapsConfig.MAX_TILES_MIN, "tiles clamped");
            h.assertTrue(!MapsConfig.allowCopy && MapsConfig.allowExtend && MapsConfig.allowCombine, "switches");
        } finally {
            MapsConfig.apply(new JsonObject());
        }
        h.assertTrue(MapsConfig.revealRadius == MapsConfig.REVEAL_RADIUS_DEFAULT && MapsConfig.allowCopy, "defaults back");
        h.succeed();
    }

    private static void waypoints(GameTestHelper h) {
        Waypoints w = Waypoints.EMPTY.with(Waypoint.fresh(3, 10, 20)).with(Waypoint.fresh(0, 1, 2)).with(Waypoint.fresh(3, 5, 6));
        h.assertTrue(w.list().size() == 2 && w.list().get(0).slot() == 0 && w.get(3).get().x() == 5, "one per slot, sorted, replaced");
        h.assertTrue(w.without(3).get(3).isEmpty(), "delete");
        Waypoints other = Waypoints.EMPTY.with(Waypoint.fresh(0, 100, 100)).with(Waypoint.fresh(1, 200, 200));
        Waypoints merged = w.mergedWith(other);
        h.assertTrue(merged.get(0).get().x() == 1 && merged.get(3).get().x() == 5, "first map's waypoints stay (F4)");
        h.assertTrue(merged.get(1).get().x() == 100 && merged.get(2).get().x() == 200, "free slots take the second map's");
        Waypoint longName = new Waypoint(1, 0, 0, "x".repeat(80), 0x123456, Optional.empty());
        h.assertTrue(longName.name().length() == Waypoint.MAX_NAME && (longName.color() >>> 24) == 0xFF, "name cut, colour opaque");
        h.assertTrue(MapsNetwork.valid(new Waypoint(0, 0, 0, "", 0, Optional.of(Identifier.withDefaultNamespace("creeper_head")))), "head ok");
        h.assertTrue(!MapsNetwork.valid(new Waypoint(0, 0, 0, "", 0, Optional.of(Identifier.withDefaultNamespace("diamond")))), "no head");
        h.assertTrue(!MapsNetwork.valid(Waypoint.fresh(0, 40_000_000, 0)), "outside the world");
        var encoded = Waypoints.CODEC.encodeStart(NbtOps.INSTANCE, merged).getOrThrow();
        h.assertTrue(Waypoints.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow().equals(merged), "codec round trip");
        h.succeed();
    }

    private static void tileStorage(GameTestHelper h) {
        WayfinderData data = new WayfinderData();
        int color = MapColor.GOLD.getPackedId(MapColor.Brightness.NORMAL) & 0xFF;
        h.assertTrue(data.set(-1, -1, color, 40, true), "write");
        h.assertTrue(!data.set(-1, -1, color, 40, true), "same value is no change");
        h.assertTrue(data.color(-1, -1) == color && data.height(-1, -1) == 40 && data.color(0, 0) == 0, "read back, tile -1/-1");
        h.assertTrue(data.tileCount() == 1 && data.tile(-1, -1) != null, "one tile");
        h.assertTrue(!data.set(-1, -1, 1, 1, false), "fill-only keeps known pixels");
        Tag tag = WayfinderData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        WayfinderData back = WayfinderData.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, tag)).getOrThrow();
        h.assertTrue(back.color(-1, -1) == color && back.height(-1, -1) == 40, "codec round trip");
        WayfinderData.Rendered far = data.render(64, -1, -1);
        h.assertTrue(far.colors() != null && far.version() != 0, "zoom 64 sees the tile");
        h.assertTrue(data.render(64, 5, 5).version() == 0, "empty zoom tile");
        WayfinderData.Rendered near = data.render(1, -1, -1);
        byte[] raw = TileCodec.unpack(TileCodec.pack(near.colors(), near.heights()));
        h.assertTrue(raw != null && (raw[WayfinderData.AREA - 1] & 0xFF) == color && (raw[2 * WayfinderData.AREA - 1] & 0xFF) == 40, "network round trip");
        int limit = MapsConfig.maxTilesPerMap;
        try {
            MapsConfig.maxTilesPerMap = 2;
            data.set(1000, 1000, color, 1, true);
            h.assertTrue(!data.set(5000, 5000, color, 1, true) && data.tileCount() == 2, "tile limit (Feature 8)");
        } finally {
            MapsConfig.maxTilesPerMap = limit;
        }
        h.succeed();
    }

    private static void revealExplores(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        h.setBlock(new BlockPos(1, 1, 1), Blocks.GOLD_BLOCK);
        // The shared gametest world may hand out a chunk whose surface heightmap is stale at tick 0 (seen once in the gate): rebuild it.
        net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(h.getLevel().getChunkAt(pos),
                java.util.EnumSet.of(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE));
        WayfinderData data = new WayfinderData();
        for (int phase = 0; phase < 16; phase++) Reveal.step(h.getLevel(), pos.getX(), pos.getY(), pos.getZ(), data, 8, phase);
        int c = data.color(pos.getX(), pos.getZ());
        h.assertTrue(c >> 2 == MapColor.GOLD.id, "gold block mapped, got colour id " + (c >> 2));
        h.assertTrue(data.height(pos.getX(), pos.getZ()) == (pos.getY() - h.getLevel().getMinY()) / 2 + 1, "height stored");
        h.assertTrue(data.color(pos.getX() + 30, pos.getZ()) == 0, "outside the radius stays unknown");
        h.succeed();
    }

    private static void dimensionBinding(GameTestHelper h) {
        var server = h.getLevel().getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        WayfinderMapItem plain = (WayfinderMapItem) MapsItems.WAYFINDER_MAP, netherMap = (WayfinderMapItem) MapsItems.NETHER_WAYFINDER_MAP;
        h.assertTrue(plain.accepts(h.getLevel()) && !plain.accepts(nether), "plain map: not in the Nether (F8)");
        h.assertTrue(netherMap.accepts(nether) && !netherMap.accepts(h.getLevel()), "Nether map only in the Nether");
        ItemStack stack = new ItemStack(MapsItems.WAYFINDER_MAP);
        WayfinderData data = plain.dataFor(stack, h.getLevel());
        h.assertTrue(data != null && data.dimension().orElseThrow().equals(h.getLevel().dimension()), "binds on first use");
        h.assertTrue(stack.has(MapsComponents.MAP_ID), "id assigned");
        h.succeed();
    }

    private static ItemStack bound(GameTestHelper h, Waypoints waypoints) {
        ItemStack stack = new ItemStack(MapsItems.WAYFINDER_MAP);
        ((WayfinderMapItem) MapsItems.WAYFINDER_MAP).dataFor(stack, h.getLevel());
        if (!waypoints.list().isEmpty()) stack.set(MapsComponents.WAYPOINTS, waypoints);
        return stack;
    }

    private static CartographyTableMenu table(GameTestHelper h, ServerPlayer player) {
        return new CartographyTableMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(BlockPos.ZERO)));
    }

    private static void cartographyCopy(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        CartographyTableMenu menu = table(h, player);
        ItemStack map = bound(h, Waypoints.EMPTY.with(Waypoint.fresh(2, 7, 8)));
        h.assertTrue(menu.getSlot(0).mayPlace(map) && menu.getSlot(1).mayPlace(map), "wider slots");
        menu.container.setItem(0, map);
        menu.container.setItem(1, new ItemStack(Items.MAP));
        ItemStack result = menu.getSlot(2).getItem();
        h.assertTrue(MapsItems.isWayfinder(result) && result.getCount() == 2, "two copies");
        h.assertTrue(result.get(MapsComponents.MAP_ID).equals(map.get(MapsComponents.MAP_ID)), "same map");
        h.assertTrue(result.getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY).get(2).isPresent(), "waypoints copied (Feature 1)");
        menu.getSlot(2).onTake(player, result);
        h.assertTrue(menu.container.getItem(0).isEmpty() && menu.container.getItem(1).isEmpty(), "inputs used");
        h.succeed();
    }

    private static void cartographyExtend(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        BlockPos center = h.absolutePos(BlockPos.ZERO);
        ItemStack filled = MapItem.create(h.getLevel(), center.getX(), center.getZ(), (byte) 0, true, false);
        MapItemSavedData vanilla = MapItem.getSavedData(filled, h.getLevel());
        byte gold = MapColor.GOLD.getPackedId(MapColor.Brightness.HIGH);
        vanilla.setColor(64, 64, gold);
        ItemStack map = bound(h, Waypoints.EMPTY);
        CartographyTableMenu menu = table(h, player);
        // Either order works: the filled map in the map slot, the wayfinder in the additional slot.
        menu.container.setItem(0, filled);
        menu.container.setItem(1, map);
        ItemStack result = menu.getSlot(2).getItem();
        h.assertTrue(MapsItems.isWayfinder(result) && result.getCount() == 1 && result.has(MapsComponents.PENDING), "extended map");
        menu.getSlot(2).onTake(player, result);
        h.assertTrue(!result.has(MapsComponents.PENDING), "applied when taken");
        WayfinderData data = WayfinderData.get(h.getLevel().getServer(), result.get(MapsComponents.MAP_ID).id());
        int x = vanilla.centerX, z = vanilla.centerZ;
        h.assertTrue(data.color(x, z) == (gold & 0xFF), "filled map's area taken over at " + x + "," + z);
        h.succeed();
    }

    private static void cartographyCombine(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack first = bound(h, Waypoints.EMPTY.with(Waypoint.fresh(0, 1, 1)));
        ItemStack second = bound(h, Waypoints.EMPTY.with(Waypoint.fresh(0, 9, 9)));
        var server = h.getLevel().getServer();
        int color = MapColor.SNOW.getPackedId(MapColor.Brightness.NORMAL) & 0xFF;
        WayfinderData.get(server, second.get(MapsComponents.MAP_ID).id()).set(300, 300, color, 10, true);
        int firstId = first.get(MapsComponents.MAP_ID).id();
        CartographyTableMenu menu = table(h, player);
        menu.container.setItem(0, first);
        menu.container.setItem(1, second);
        ItemStack result = menu.getSlot(2).getItem();
        h.assertTrue(result.get(MapsComponents.MAP_ID).id() == firstId, "the first map stays");
        Waypoints w = result.getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY);
        h.assertTrue(w.get(0).get().x() == 1 && w.get(1).get().x() == 9, "first keeps slot 1, second fills slot 2");
        menu.getSlot(2).onTake(player, result);
        h.assertTrue(WayfinderData.get(server, firstId).color(300, 300) == color, "areas united");
        h.assertTrue(menu.container.getItem(1).isEmpty(), "second map used up");
        ItemStack nether = new ItemStack(MapsItems.NETHER_WAYFINDER_MAP);
        h.assertTrue(Cartography.result(bound(h, Waypoints.EMPTY), nether, h.getLevel()).isEmpty(), "different kinds do not combine");
        h.succeed();
    }

    private static void cartographySwitches(GameTestHelper h) {
        ItemStack map = bound(h, Waypoints.EMPTY);
        try {
            MapsConfig.allowCopy = false;
            MapsConfig.allowCombine = false;
            h.assertTrue(Cartography.result(map, new ItemStack(Items.MAP), h.getLevel()).isEmpty(), "copy off");
            h.assertTrue(Cartography.result(map, bound(h, Waypoints.EMPTY), h.getLevel()).isEmpty(), "combine off");
            h.assertTrue(Cartography.result(map, new ItemStack(Items.PAPER), h.getLevel()).isEmpty(), "no scaling");
        } finally {
            MapsConfig.allowCopy = true;
            MapsConfig.allowCombine = true;
        }
        h.succeed();
    }

    private static void waypointEdit(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack map = bound(h, Waypoints.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, map);
        MapsNetwork.handleWaypoint(new WaypointEditPayload(1, false, Waypoint.fresh(4, 11, 12)), player);
        ItemStack held = player.getItemInHand(InteractionHand.OFF_HAND);
        h.assertTrue(held.getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY).get(4).map(w -> w.x() == 11).orElse(false), "created");
        MapsNetwork.handleWaypoint(new WaypointEditPayload(1, false,
                new Waypoint(4, 11, 12, "Home", 0xFF00FF00, Optional.of(Identifier.withDefaultNamespace("stone")))), player);
        h.assertTrue(held.get(MapsComponents.WAYPOINTS).get(4).get().name().isEmpty(), "invalid head refused");
        MapsNetwork.handleWaypoint(new WaypointEditPayload(1, false,
                new Waypoint(4, 11, 12, "Home", 0xFF00FF00, Optional.of(Identifier.withDefaultNamespace("piglin_head")))), player);
        h.assertTrue(held.get(MapsComponents.WAYPOINTS).get(4).get().name().equals("Home"), "configured");
        MapsNetwork.handleWaypoint(new WaypointEditPayload(0, false, Waypoint.fresh(1, 0, 0)), player);
        h.assertTrue(held.get(MapsComponents.WAYPOINTS).get(1).isEmpty(), "main hand holds no map");
        MapsNetwork.handleWaypoint(new WaypointEditPayload(1, true, Waypoint.fresh(4, 0, 0)), player);
        h.assertTrue(!held.has(MapsComponents.WAYPOINTS), "deleted");
        h.succeed();
    }

    private static void frameView(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, 2, 1));
        player.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5);
        ItemFrame frame = new ItemFrame(h.getLevel(), pos, Direction.SOUTH);
        h.getLevel().addFreshEntity(frame);
        frame.setItem(bound(h, Waypoints.EMPTY));
        InteractionResult click = frame.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(click.consumesAction() && frame.getRotation() == 0, "right-click opens instead of rotating (Feature 4)");
        MapsNetwork.handleFrameView(new FrameViewPayload(frame.getId(), new MapsComponents.View(500, -40, 8)), player);
        MapsComponents.View view = frame.getItem().get(MapsComponents.VIEW);
        h.assertTrue(view != null && view.x() == 500 && view.zoom() == 8, "view stored in the frame");
        MapsNetwork.handleFrameView(new FrameViewPayload(frame.getId(), new MapsComponents.View(1, 1, 3)), player);
        h.assertTrue(frame.getItem().get(MapsComponents.VIEW).zoom() == 1, "invalid zoom falls back to 1:1");
        frame.discard();
        h.succeed();
    }

    private static void loot(GameTestHelper h) {
        var registries = h.getLevel().getServer().reloadableRegistries().lookup();
        for (String table : MapsLoot.TABLES.keySet()) {
            List<LootPool.Builder> pools = new ArrayList<>();
            MapsLoot.apply(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(table)), pools::add, registries);
            h.assertTrue(pools.size() == 1, "one pool for " + table);
        }
        List<LootPool.Builder> none = new ArrayList<>();
        MapsLoot.apply(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse("minecraft:chests/simple_dungeon")), none::add, registries);
        h.assertTrue(none.isEmpty(), "other tables untouched");
        h.assertTrue(MapsLoot.TABLES.get("minecraft:chests/village/village_cartographer")[1] * 20
                == MapsLoot.TABLES.get("minecraft:chests/village/village_cartographer")[1] + MapsLoot.TABLES.get("minecraft:chests/village/village_cartographer")[2],
                "cartographer 5 %");
        h.succeed();
    }

    /** Feature 3: discovered structures are stored once, saved, merged and sent; nothing is found where none stands. */
    private static void structureMarks(GameTestHelper h) {
        WayfinderData data = new WayfinderData();
        int v0 = data.marksVersion();
        h.assertTrue(data.addMark("minecraft:village_plains", 100, 200) && data.marksVersion() != v0, "new mark");
        h.assertTrue(!data.addMark("minecraft:village_plains", 120, 190), "same structure nearby is one mark");
        h.assertTrue(data.addMark("minecraft:village_plains", 900, 200) && data.addMark("minecraft:ancient_city", 100, 200), "far or other id is new");
        Tag tag = WayfinderData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        WayfinderData back = WayfinderData.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, tag)).getOrThrow();
        h.assertTrue(back.marks().equals(data.marks()) && back.marks().size() == 3, "codec round trip");
        WayfinderData other = new WayfinderData();
        other.addMark("minecraft:stronghold", -50, -50);
        data.mergeFrom(other);
        h.assertTrue(data.marks().size() == 4, "combining keeps both maps' structures");
        var payload = new com.simplemaps.net.MapStatePayload(7, "minecraft:overworld", 3, true, data.marks());
        var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        com.simplemaps.net.MapStatePayload.CODEC.encode(buf, payload);
        h.assertTrue(com.simplemaps.net.MapStatePayload.CODEC.decode(buf).equals(payload), "network round trip");
        h.assertTrue(!com.simplemaps.StructureMarks.scanAt(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)), new WayfinderData()),
                "no structure here, no mark");
        WayfinderData full = new WayfinderData();
        for (int i = 0; i < WayfinderData.Mark.MAX_PER_MAP + 5; i++) full.addMark("minecraft:mineshaft", i * 1000, 0);
        h.assertTrue(full.marks().size() == WayfinderData.Mark.MAX_PER_MAP, "mark limit");
        h.succeed();
    }

    /** Bundled in SimpleBuilding the host config can switch the module off; everything then goes inert. */
    private static void hostSwitch(GameTestHelper h) {
        boolean before = MapsConfig.enabled;
        try {
            java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("simplemaps-host");
            h.assertTrue(MapsConfig.hostAllows(dir), "no host file: on");
            java.nio.file.Files.writeString(dir.resolve(MapsConfig.HOST_FILE), "{\"enableSimpleMaps\": false}");
            h.assertTrue(!MapsConfig.hostAllows(dir), "host says off");
            java.nio.file.Files.writeString(dir.resolve(MapsConfig.HOST_FILE), "{\"enableSimpleMaps\": true}");
            h.assertTrue(MapsConfig.hostAllows(dir), "host says on");
            java.nio.file.Files.writeString(dir.resolve(MapsConfig.HOST_FILE), "not json");
            h.assertTrue(MapsConfig.hostAllows(dir), "unreadable host file: on");
            ItemStack stack = new ItemStack(MapsItems.WAYFINDER_MAP);
            MapsConfig.enabled = false;
            h.assertTrue(!MapsItems.isWayfinder(stack) && MapsItems.tabStacks().isEmpty(), "disabled: not offered, not a wayfinder");
            MapsConfig.enabled = true;
            h.assertTrue(MapsItems.isWayfinder(stack) && MapsItems.tabStacks().size() == 3, "enabled again");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        } finally {
            MapsConfig.enabled = before;
        }
        h.succeed();
    }
}
