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
import net.minecraft.world.item.Item;
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
        ALL.put("frame_save_rules", MapsTests::frameSaveRules);
        ALL.put("frame_code", MapsTests::frameCode);
        ALL.put("stack_size", MapsTests::stackSize);
        ALL.put("loot", MapsTests::loot);
        ALL.put("structure_marks", MapsTests::structureMarks);
        ALL.put("host_switch", MapsTests::hostSwitch);
        ALL.put("screen_layout", MapsTests::screenLayout);
        ALL.put("layers", MapsTests::layers);
        ALL.put("layers_own_chunks", MapsTests::layersOwnChunks);
        ALL.put("other_dimension_tiles", MapsTests::otherDimensionTiles);
        ALL.put("foreign_map_tiles_refused", MapsTests::foreignMapTilesRefused);
        ALL.put("shop_icon", MapsTests::shopIcon);
        ALL.put("claims_layer_absent", MapsTests::claimsLayerAbsent);
        WebTests.register(ALL);
        MegaTests.register(ALL);
    }

    private MapsTests() {}

    private static void registered(GameTestHelper h) {
        h.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(SimpleMaps.id("maps")), "own creative tab maps is registered");
        for (String id : List.of("wayfinder_map", "nether_wayfinder_map", "end_wayfinder_map", "mega_wayfinder_map")) {
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleMaps.id(id)), "item missing: " + id);
            // End and mega map: one of two recipes, with or without enderite (owner B23-K1/K3, mega_recipes checks which).
            boolean found = false;
            for (String r : List.of(id, id + "_without_enderite")) {
                found |= h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, SimpleMaps.id(r))).isPresent();
            }
            h.assertTrue(found, "recipe missing: " + id);
        }
        ServerLevel nether = h.getLevel().getServer().getLevel(Level.NETHER), end = h.getLevel().getServer().getLevel(Level.END);
        h.assertTrue(nether != null && nether.dimensionTypeRegistration().is(MapsItems.NETHER_DIMENSIONS), "nether tag");
        h.assertTrue(end != null && end.dimensionTypeRegistration().is(MapsItems.END_DIMENSIONS), "end tag");
        h.assertTrue(new ItemStack(Items.ZOMBIE_HEAD).is(MapsItems.WAYPOINT_HEADS) && new ItemStack(Items.DRAGON_HEAD).is(MapsItems.WAYPOINT_HEADS),
                "every mob head can be a waypoint icon");
        h.assertTrue(!new ItemStack(Items.PUMPKIN).is(MapsItems.WAYPOINT_HEADS), "pumpkin is no head");
        // Simple Mobheads (optional): its heads are waypoint icons too, only when that mod is loaded.
        Item blaze = BuiltInRegistries.ITEM.getOptional(Identifier.parse("simplemobheads:blaze_head")).orElse(null);
        h.assertTrue(blaze == null || new ItemStack(blaze).is(MapsItems.WAYPOINT_HEADS), "Simple Mobheads heads are waypoint icons");
        h.succeed();
    }

    /**
     * Owner N38: the map screen fills the window (GUI sizes of 854x480 up to 1920x1080 at the usual GUI scales), never
     * leaves it, keeps the waypoint bookmarks apart and grows with the window.
     */
    private static void screenLayout(GameTestHelper h) {
        int[][] sizes = {{320, 240}, {427, 240}, {480, 270}, {640, 360}, {854, 480}, {960, 540}, {1920, 1080}};
        for (int[] size : sizes) {
            com.simplemaps.WayfinderLayout l = com.simplemaps.WayfinderLayout.of(size[0], size[1]);
            String at = size[0] + "x" + size[1];
            h.assertTrue(l.minX() >= 0 && l.maxX() <= size[0], "inside horizontally at " + at);
            h.assertTrue(l.top() >= 0 && l.top() + l.panelH() <= size[1], "inside vertically at " + at);
            h.assertTrue(size[0] - (l.maxX() - l.minX()) < com.simplemaps.WayfinderLayout.CELL + 2 * com.simplemaps.WayfinderLayout.MARGIN,
                    "map as wide as possible at " + at);
            h.assertTrue(size[1] - l.panelH() < com.simplemaps.WayfinderLayout.CELL + 2 * com.simplemaps.WayfinderLayout.MARGIN,
                    "map as tall as possible at " + at);
            h.assertTrue(l.tab() >= 12 && l.rightTabY(com.simplemaps.WayfinderLayout.RIGHT_TABS - 1) + l.tab() <= l.top() + l.panelH() - com.simplemaps.WayfinderLayout.FRAME,
                    "bookmarks along the panel at " + at);
        }
        for (int[] size : sizes) {
            // Opened from an item frame (owner 21.10.): the code/save bar fits below the footer, the map still fills the window.
            com.simplemaps.WayfinderLayout f = com.simplemaps.WayfinderLayout.of(size[0], size[1], true);
            String at = size[0] + "x" + size[1];
            h.assertTrue(f.top() >= 0 && f.top() + f.panelH() <= size[1], "frame bar inside vertically at " + at);
            h.assertTrue(f.barY() + com.simplemaps.WayfinderLayout.BUTTON_H <= f.top() + f.panelH() - com.simplemaps.WayfinderLayout.FRAME,
                    "buttons inside the panel at " + at);
            h.assertTrue(size[1] - f.panelH() < com.simplemaps.WayfinderLayout.CELL + 2 * com.simplemaps.WayfinderLayout.MARGIN, "frame map as tall as possible at " + at);
            h.assertTrue(f.mapW() >= 200, "room for code field and buttons at " + at);
        }
        h.assertTrue(com.simplemaps.WayfinderLayout.of(960, 540).cols() > com.simplemaps.WayfinderLayout.of(427, 240).cols()
                && com.simplemaps.WayfinderLayout.of(960, 540).rows() > com.simplemaps.WayfinderLayout.of(427, 240).rows(), "grows with the window");
        h.assertTrue(com.simplemaps.WayfinderLayout.of(427, 240).cols() > 12, "larger than the old 12 x 9 map at 854x480");
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

    /** A waypoint as an older version saved it: colour and an optional mob head, no icon. */
    private static net.minecraft.nbt.CompoundTag legacyTag(String name, int color, String head) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("slot", 1);
        tag.putInt("x", 5);
        tag.putInt("z", 6);
        tag.putString("name", name);
        tag.putInt("color", color);
        if (head != null) tag.putString("head", head);
        return tag;
    }

    private static void waypoints(GameTestHelper h) {
        Waypoints w = Waypoints.EMPTY.with(Waypoint.fresh(3, 10, 20)).with(Waypoint.fresh(0, 1, 2)).with(Waypoint.fresh(3, 5, 6));
        h.assertTrue(w.list().size() == 2 && w.list().get(0).slot() == 0 && w.get(3).get().x() == 5, "one per slot, sorted, replaced");
        h.assertTrue(w.without(3).get(3).isEmpty(), "delete");
        Waypoints other = Waypoints.EMPTY.with(Waypoint.fresh(0, 100, 100)).with(Waypoint.fresh(1, 200, 200));
        Waypoints merged = w.mergedWith(other);
        h.assertTrue(merged.get(0).get().x() == 1 && merged.get(3).get().x() == 5, "first map's waypoints stay (F4)");
        h.assertTrue(merged.get(1).get().x() == 100 && merged.get(2).get().x() == 200, "free slots take the second map's");
        Waypoint longName = new Waypoint(1, 0, 0, "x".repeat(80), 0x123456, null, 0);
        h.assertTrue(longName.name().length() == Waypoint.MAX_NAME && (longName.color() >>> 24) == 0xFF, "name cut, colour opaque");
        h.assertTrue(MapsNetwork.valid(new Waypoint(0, 0, 0, "", 0, Identifier.withDefaultNamespace("creeper_head"), 0)), "head ok");
        h.assertTrue(MapsNetwork.valid(new Waypoint(0, 0, 0, "", 0, Identifier.withDefaultNamespace("diamond"), 0)), "any item is an icon (owner N65)");
        h.assertTrue(!MapsNetwork.valid(new Waypoint(0, 0, 0, "", 0, Identifier.withDefaultNamespace("air"), 0)), "air is no icon");
        h.assertTrue(!MapsNetwork.valid(new Waypoint(0, 0, 0, "", 0, Identifier.parse("nomod:missing"), 0)), "unknown item refused");
        h.assertTrue(MapsNetwork.clean(new Waypoint(0, 1, 1, "", 0, Identifier.withDefaultNamespace("diamond"), com.simplemaps.CubeNet.MODEL)).face() == com.simplemaps.CubeNet.FRONT,
                "no 3D model without a block model");
        h.assertTrue(MapsNetwork.clean(new Waypoint(0, 1, 1, "", 0, Identifier.withDefaultNamespace("furnace"), com.simplemaps.CubeNet.MODEL)).face() == com.simplemaps.CubeNet.MODEL,
                "3D model kept for a block");
        // Migration: an old waypoint with a colour and no icon becomes the wool of that colour.
        var legacy = Waypoint.CODEC.parse(NbtOps.INSTANCE, legacyTag("Old", -425955, null)).getOrThrow();
        h.assertTrue(legacy.icon().equals(Identifier.withDefaultNamespace("orange_wool")) && legacy.face() == com.simplemaps.CubeNet.FRONT, "legacy colour -> wool, got " + legacy.icon());
        var legacyHead = Waypoint.CODEC.parse(NbtOps.INSTANCE, legacyTag("", -1, "minecraft:zombie_head")).getOrThrow();
        h.assertTrue(legacyHead.icon().equals(Identifier.withDefaultNamespace("zombie_head")), "legacy head kept as icon");
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
        h.assertTrue(MapsItems.isWayfinder(result) && result.getCount() == 1, "one copy (maps do not stack, owner 21.10.)");
        h.assertTrue(result.get(MapsComponents.MAP_ID).equals(map.get(MapsComponents.MAP_ID)), "same map");
        h.assertTrue(result.getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY).get(2).isPresent(), "waypoints copied (Feature 1)");
        menu.getSlot(2).onTake(player, result);
        h.assertTrue(MapsItems.isWayfinder(menu.container.getItem(0)) && menu.container.getItem(1).isEmpty(), "original stays, empty map used");
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
                new Waypoint(4, 11, 12, "Home", 0xFF00FF00, Identifier.parse("nomod:missing"), 0)), player);
        h.assertTrue(held.get(MapsComponents.WAYPOINTS).get(4).get().name().isEmpty(), "unknown icon refused");
        MapsNetwork.handleWaypoint(new WaypointEditPayload(1, false,
                new Waypoint(4, 11, 12, "Home", 0xFF00FF00, Identifier.withDefaultNamespace("piglin_head"), 0)), player);
        h.assertTrue(held.get(MapsComponents.WAYPOINTS).get(4).get().name().equals("Home"), "configured");
        MapsNetwork.handleDisplay(new com.simplemaps.net.DisplayPayload(1, new MapsComponents.Display(true, 4)), player);
        h.assertTrue(held.getOrDefault(MapsComponents.DISPLAY, MapsComponents.Display.DEFAULT).equals(new MapsComponents.Display(true, 4)), "display mode saved");
        MapsNetwork.handleDisplay(new com.simplemaps.net.DisplayPayload(1, new MapsComponents.Display(true, 5)), player);
        h.assertTrue(held.get(MapsComponents.DISPLAY).handZoom() == 1, "invalid zoom becomes 1:1");
        MapsNetwork.handleDisplay(new com.simplemaps.net.DisplayPayload(1, MapsComponents.Display.DEFAULT), player);
        h.assertTrue(!held.has(MapsComponents.DISPLAY), "default display mode removes the component");
        MapsNetwork.handleDisplay(new com.simplemaps.net.DisplayPayload(0, new MapsComponents.Display(true, 4)), player);
        h.assertTrue(!player.getMainHandItem().has(MapsComponents.DISPLAY), "main hand holds no map");
        MapsNetwork.handleWaypoint(new WaypointEditPayload(0, false, Waypoint.fresh(1, 0, 0)), player);
        h.assertTrue(held.get(MapsComponents.WAYPOINTS).get(1).isEmpty(), "main hand holds no map");
        MapsNetwork.handleWaypoint(new WaypointEditPayload(1, true, Waypoint.fresh(4, 0, 0)), player);
        h.assertTrue(!held.has(MapsComponents.WAYPOINTS), "deleted");
        h.succeed();
    }

    /** Layers: server authority (holder, explored tiles only, capped area), loaded chunks, a source from another module. */
    private static void layers(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack map = bound(h, Waypoints.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, map);
        int id = map.get(MapsComponents.MAP_ID).id();
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        WayfinderData data = WayfinderData.get(h.getLevel().getServer(), id);
        data.set(pos.getX(), pos.getZ(), MapColor.STONE.getPackedId(MapColor.Brightness.NORMAL) & 0xFF, 10, true);
        com.simplebuilding.framework.api.MapLayerSources.register("simplemaps", "test_layer", "layer.test", (level, x0, z0, x1, z1) -> List.of(
                new com.simplebuilding.framework.api.MapLayerSources.Marker(pos.getX(), pos.getZ(), "t", List.of("one", "two")),
                new com.simplebuilding.framework.api.MapLayerSources.Marker(pos.getX() + 100_000, pos.getZ(), "t", List.of("unexplored"))));
        var sent = new ArrayList<net.minecraft.network.protocol.common.custom.CustomPacketPayload>();
        var old = SimpleMaps.toClient;
        SimpleMaps.toClient = (p, payload) -> sent.add(payload);
        try {
            int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
            com.simplemaps.net.LayerService.handle(new com.simplemaps.net.LayerRequestPayload(id, cx * 16, cz * 16, cx * 16 + 15, cz * 16 + 15, true, List.of("test_layer", "missing")), player);
            h.assertTrue(sent.size() == 1 && sent.get(0) instanceof com.simplemaps.net.LayerDataPayload, "answered once");
            var answer = (com.simplemaps.net.LayerDataPayload) sent.get(0);
            h.assertTrue(answer.loaded().length == 1 && (answer.loaded()[0] & 1) == 1, "the explored, loaded chunk is flagged");
            h.assertTrue(answer.markers().size() == 1 && answer.markers().get(0).lines().size() == 2, "only the marker inside explored ground");
            h.assertTrue(answer.available().stream().anyMatch(a -> a.id().equals("test_layer")), "source listed");
            // Rate limit, other holder, oversized area: nothing more is sent.
            com.simplemaps.net.LayerService.handle(new com.simplemaps.net.LayerRequestPayload(id, 0, 0, 15, 15, true, List.of()), player);
            h.assertTrue(sent.size() == 1, "rate limited");
            ServerPlayer other = h.makeMockServerPlayerInLevel();
            com.simplemaps.net.LayerService.handle(new com.simplemaps.net.LayerRequestPayload(id, 0, 0, 15, 15, true, List.of()), other);
            h.assertTrue(sent.size() == 1, "only the holder of the map may ask");
            // Codec round trip.
            var buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            com.simplemaps.net.LayerDataPayload.CODEC.encode(buf, answer);
            var back = com.simplemaps.net.LayerDataPayload.CODEC.decode(buf);
            h.assertTrue(back.markers().equals(answer.markers()) && back.area().equals(answer.area()), "codec round trip");
        } finally {
            SimpleMaps.toClient = old;
        }
        h.succeed();
    }

    /**
     * Owner 19.10.: the loaded-chunks layer flags only the viewer's own chunks and forced ones (chunk loaders), never a
     * chunk that only another player keeps loaded, and none of another dimension unless forced there.
     */
    private static void layersOwnChunks(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var server = level.getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        ServerPlayer viewer = h.makeMockServerPlayerInLevel(), late = h.makeMockServerPlayerInLevel();
        ItemStack map = bound(h, Waypoints.EMPTY);
        viewer.setItemInHand(InteractionHand.MAIN_HAND, map);
        late.setItemInHand(InteractionHand.MAIN_HAND, map.copy());
        int id = map.get(MapsComponents.MAP_ID).id();
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        int ocx = pos.getX() >> 4, ocz = pos.getZ() >> 4;
        // Far from every test structure and every player: loaded only for this test, as if somebody else stood there.
        int fcx = -30_000 + (ocx & 63), fcz = 30_000 + (ocz & 63);
        long far = net.minecraft.world.level.ChunkPos.pack(fcx, fcz), near = net.minecraft.world.level.ChunkPos.pack(ocx, ocz);
        h.assertTrue(!level.getForceLoadedChunks().contains(far) && !nether.getForceLoadedChunks().contains(near), "test chunks not forced yet");
        WayfinderData data = WayfinderData.get(server, id);
        data.set(fcx * 16 + 1, fcz * 16 + 1, MapColor.STONE.getPackedId(MapColor.Brightness.NORMAL) & 0xFF, 10, true);
        level.getChunk(fcx, fcz);
        var sent = new ArrayList<net.minecraft.network.protocol.common.custom.CustomPacketPayload>();
        var old = SimpleMaps.toClient;
        SimpleMaps.toClient = (p, payload) -> sent.add(payload);
        try {
            h.assertTrue(level.getChunkSource().getChunkNow(fcx, fcz) != null, "far chunk is loaded");
            h.assertTrue(com.simplemaps.net.LayerService.shownChunk(level, viewer, ocx, ocz), "own chunk shown");
            h.assertTrue(!com.simplemaps.net.LayerService.shownChunk(level, viewer, fcx, fcz), "chunk loaded by somebody else is not shown");
            var ask = new com.simplemaps.net.LayerRequestPayload(id, fcx * 16, fcz * 16, fcx * 16 + 15, fcz * 16 + 15, true, List.of());
            com.simplemaps.net.LayerService.handle(ask, viewer);
            h.assertTrue(sent.size() == 1 && ((com.simplemaps.net.LayerDataPayload) sent.get(0)).loaded()[0] == 0, "explored chunk loaded by somebody else stays unflagged");
            level.setChunkForced(fcx, fcz, true);
            h.assertTrue(com.simplemaps.net.LayerService.shownChunk(level, viewer, fcx, fcz), "forced chunk (chunk loader) shown");
            com.simplemaps.net.LayerService.handle(ask, late);
            h.assertTrue(sent.size() == 2 && (((com.simplemaps.net.LayerDataPayload) sent.get(1)).loaded()[0] & 1) == 1, "forced explored chunk flagged");
            nether.getChunk(ocx, ocz);
            h.assertTrue(!com.simplemaps.net.LayerService.shownChunk(nether, viewer, ocx, ocz), "same spot in another dimension is not the viewer's");
            nether.setChunkForced(ocx, ocz, true);
            h.assertTrue(com.simplemaps.net.LayerService.shownChunk(nether, viewer, ocx, ocz), "forced chunk of another dimension shown");
        } finally {
            SimpleMaps.toClient = old;
            level.setChunkForced(fcx, fcz, false);
            nether.setChunkForced(ocx, ocz, false);
        }
        h.succeed();
    }

    /** Review H-2: tiles and marks of a map the player neither carries nor sees in a frame are not served. */
    private static void foreignMapTilesRefused(GameTestHelper h) {
        var server = h.getLevel().getServer();
        ItemStack stack = new ItemStack(MapsItems.WAYFINDER_MAP);
        WayfinderData data = ((WayfinderMapItem) MapsItems.WAYFINDER_MAP).dataFor(stack, h.getLevel());
        h.assertTrue(data != null, "map binds");
        data.set(16, 16, MapColor.GRASS.getPackedId(MapColor.Brightness.NORMAL) & 0xFF, 10, true);
        int id = stack.get(MapsComponents.MAP_ID).id();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        var request = new com.simplemaps.net.TileRequestPayload(List.of(new com.simplemaps.net.TileRequestPayload.Entry(id, 1, 0, 0, -1)));
        var sent = new ArrayList<net.minecraft.network.protocol.common.custom.CustomPacketPayload>();
        var old = SimpleMaps.toClient;
        SimpleMaps.toClient = (p, payload) -> sent.add(payload);
        try {
            MapsNetwork.handleTiles(request, player);
            h.assertTrue(sent.isEmpty(), "a map the player does not have was served: " + sent.size() + " packets");
            player.getInventory().setItem(0, stack);
            MapsNetwork.resetThrottle(player);
            MapsNetwork.handleTiles(request, player);
            h.assertTrue(!sent.isEmpty(), "the carried map is served");
        } finally {
            SimpleMaps.toClient = old;
        }
        h.succeed();
    }

    /** Owner 19.10.: a map of another dimension is still served (End/Nether map in the Overworld), positions convert 8:1. */
    private static void otherDimensionTiles(GameTestHelper h) {
        var server = h.getLevel().getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        ItemStack stack = new ItemStack(MapsItems.NETHER_WAYFINDER_MAP);
        WayfinderData data = ((WayfinderMapItem) MapsItems.NETHER_WAYFINDER_MAP).dataFor(stack, nether);
        h.assertTrue(data != null, "Nether map binds in the Nether");
        data.set(16, 16, MapColor.NETHER.getPackedId(MapColor.Brightness.NORMAL) & 0xFF, 10, true);
        int id = stack.get(MapsComponents.MAP_ID).id();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
        h.assertTrue(player.level().dimension() != Level.NETHER, "holder is outside the Nether");
        var sent = new ArrayList<net.minecraft.network.protocol.common.custom.CustomPacketPayload>();
        var old = SimpleMaps.toClient;
        SimpleMaps.toClient = (p, payload) -> sent.add(payload);
        try {
            MapsNetwork.handleTiles(new com.simplemaps.net.TileRequestPayload(List.of(new com.simplemaps.net.TileRequestPayload.Entry(id, 1, 0, 0, -1))), player);
        } finally {
            SimpleMaps.toClient = old;
        }
        var state = sent.stream().filter(p -> p instanceof com.simplemaps.net.MapStatePayload).map(p -> (com.simplemaps.net.MapStatePayload) p).findFirst();
        var tile = sent.stream().filter(p -> p instanceof com.simplemaps.net.TileDataPayload).map(p -> (com.simplemaps.net.TileDataPayload) p).findFirst();
        h.assertTrue(state.isPresent() && state.get().dimension().equals(Level.NETHER.identifier().toString()), "map state names the Nether");
        h.assertTrue(tile.isPresent() && tile.get().data().length > 0, "explored Nether tile is sent to a holder in the Overworld");
        h.assertTrue(WayfinderMapItem.kindOf(nether.dimensionTypeRegistration()) == WayfinderMapItem.Kind.NETHER
                && WayfinderMapItem.kindOf(h.getLevel().dimensionTypeRegistration()) == WayfinderMapItem.Kind.OVERWORLD
                && WayfinderMapItem.kindOf(server.getLevel(Level.END).dimensionTypeRegistration()) == WayfinderMapItem.Kind.END, "kind of each dimension");
        h.assertTrue(WayfinderMapItem.coordScale(WayfinderMapItem.Kind.NETHER, WayfinderMapItem.Kind.OVERWORLD) == 0.125
                && WayfinderMapItem.coordScale(WayfinderMapItem.Kind.OVERWORLD, WayfinderMapItem.Kind.NETHER) == 8.0
                && WayfinderMapItem.coordScale(WayfinderMapItem.Kind.END, WayfinderMapItem.Kind.OVERWORLD) == 1.0
                && WayfinderMapItem.coordScale(WayfinderMapItem.Kind.OVERWORLD, WayfinderMapItem.Kind.END) == 1.0, "Overworld/Nether 8:1, End 1:1");
        int unexplored = com.simplemaps.JamNoise.argb(com.simplemaps.JamNoise.tint(WayfinderMapItem.Kind.END), 3, 4, 10);
        h.assertTrue(unexplored >>> 24 > 0 && unexplored >>> 24 <= com.simplemaps.JamNoise.MAX_ALPHA, "static is faint (alpha <= MAX_ALPHA)");
        h.succeed();
    }

    /** Owner 19.10.: shops carry Simple Money's bill only when Simple Money is loaded (by id, principle 4). */
    private static void shopIcon(GameTestHelper h) {
        var saved = SimpleMaps.modLoaded;
        boolean registered = BuiltInRegistries.ITEM.containsKey(com.simplemaps.MapsIcons.MONEY_BILL);
        try {
            h.assertTrue(com.simplemaps.MapsIcons.shopIcon().isPresent() == (saved.test("simplemoney") && registered), "real state: icon iff Simple Money is there");
            SimpleMaps.modLoaded = id -> false;
            h.assertTrue(com.simplemaps.MapsIcons.shopIcon().isEmpty() && com.simplemaps.MapsIcons.webIcon("shop").equals("diamond"), "without Simple Money: diamond");
            SimpleMaps.modLoaded = id -> id.equals("simplemoney");
            h.assertTrue(com.simplemaps.MapsIcons.shopIcon().isPresent() == registered, "with Simple Money: its bill when registered");
            h.assertTrue(com.simplemaps.MapsIcons.webIcon("shop").equals(registered ? "money_bill" : "diamond")
                    && com.simplemaps.MapsIcons.webIcon("chunk_loader").equals("diamond"), "only shops get the bill");
        } finally {
            SimpleMaps.modLoaded = saved;
        }
        h.succeed();
    }

    /** SP22-8: the Claims layer comes from Simple Tweaks (framework source "claims"); Simple Maps alone has none and needs no switch. */
    private static void claimsLayerAbsent(GameTestHelper h) {
        if (SimpleMaps.modLoaded.test("simpletweaks")) { h.succeed(); return; } // the layer is Simple Tweaks' to test
        h.assertTrue(com.simplebuilding.framework.api.MapLayerSources.get("claims") == null, "without Simple Tweaks there is no claims source");
        h.assertTrue(com.simplebuilding.framework.api.MapLayerSources.all().stream().noneMatch(s -> s.owner().equals("simpletweaks")), "nothing of Simple Tweaks on the map");
        h.succeed();
    }

    /** A frame with a bound wayfinder map two blocks in front of a fresh player. */
    private static ItemFrame framedMap(GameTestHelper h, ServerPlayer player) {
        BlockPos pos = h.absolutePos(new BlockPos(1, 2, 1));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5);
        ItemFrame frame = new ItemFrame(h.getLevel(), pos, Direction.SOUTH);
        h.getLevel().addFreshEntity(frame);
        frame.setItem(bound(h, Waypoints.EMPTY));
        return frame;
    }

    /**
     * Feature 4 + owner 21.10.: right-click opens the screen, nothing changes the frame until the save packet comes; the
     * packet stores the view (with magnification) in the frame's synced data and answers the player.
     */
    private static void frameView(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemFrame frame = framedMap(h, player);
        InteractionResult click = frame.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(click.consumesAction() && frame.getRotation() == 0, "right-click opens instead of rotating (Feature 4)");
        h.assertTrue(!frame.getItem().has(MapsComponents.VIEW), "opening alone does not change the frame");
        var sent = new ArrayList<net.minecraft.network.protocol.common.custom.CustomPacketPayload>();
        var saved = SimpleMaps.toClient;
        try {
            SimpleMaps.toClient = (p, payload) -> sent.add(payload);
            frame.getEntityData().packDirty();
            MapsNetwork.handleFrameView(new FrameViewPayload(frame.getId(), new MapsComponents.View(500, -40, 8)), player);
            MapsComponents.View view = frame.getItem().get(MapsComponents.VIEW);
            h.assertTrue(view != null && view.x() == 500 && view.zoom() == 8 && view.mag() == 1, "view stored in the frame");
            h.assertTrue(frame.getEntityData().isDirty(), "the change is queued for every client watching the frame");
            h.assertTrue(sent.size() == 1 && sent.get(0) instanceof com.simplemaps.net.FrameSavedPayload answer
                    && answer.entityId() == frame.getId() && answer.save() == MapsNetwork.FrameSave.SAVED, "answer: saved");
            MapsNetwork.handleFrameView(new FrameViewPayload(frame.getId(), com.simplemaps.FrameCode.view(1, 1, 0)), player);
            view = frame.getItem().get(MapsComponents.VIEW);
            h.assertTrue(view.x() == 24 && view.z() == 24 && view.zoom() == 1 && view.mag() == 8, "8:1 view of chunk 1,1 stored");
            MapsNetwork.handleFrameView(new FrameViewPayload(frame.getId(), new MapsComponents.View(1, 1, 3, 8)), player);
            h.assertTrue(frame.getItem().get(MapsComponents.VIEW).zoom() == 1 && frame.getItem().get(MapsComponents.VIEW).mag() == 8,
                    "invalid zoom falls back to 1:1, magnification kept");
            MapsNetwork.handleFrameView(new FrameViewPayload(frame.getId(), new MapsComponents.View(1, 1, 4, 8)), player);
            h.assertTrue(frame.getItem().get(MapsComponents.VIEW).mag() == 1, "magnification only at 1:1 data");
            MapsNetwork.handleFrameView(new FrameViewPayload(frame.getId(), new MapsComponents.View(1, 1, 1, 3)), player);
            h.assertTrue(frame.getItem().get(MapsComponents.VIEW).mag() == 1, "unknown magnification falls back to 1");
            sent.clear();
            MapsNetwork.handleFrameView(new FrameViewPayload(frame.getId() + 100000, new MapsComponents.View(0, 0, 1)), player);
            h.assertTrue(sent.size() == 1 && ((com.simplemaps.net.FrameSavedPayload) sent.get(0)).save() == MapsNetwork.FrameSave.INVALID, "answer: invalid");
        } finally {
            SimpleMaps.toClient = saved;
            frame.discard();
        }
        h.succeed();
    }

    /**
     * Owner 21.10.: only who may change the frame saves; the server checks the request itself (distance, frame, map,
     * coordinates) and the rights (spectator, fixed frame, adventure mode, framework Protection = claims).
     */
    private static void frameSaveRules(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemFrame frame = framedMap(h, player);
        MapsComponents.View view = new MapsComponents.View(64, 64, 2);
        Object server = h.getLevel().getServer();
        String provider = "simplemaps_test_frames";
        try {
            h.assertTrue(MapsNetwork.saveFrameView(player, frame.getId() + 100000, view) == MapsNetwork.FrameSave.INVALID, "unknown entity");
            h.assertTrue(MapsNetwork.saveFrameView(player, player.getId(), view) == MapsNetwork.FrameSave.INVALID, "not a frame");
            h.assertTrue(MapsNetwork.saveFrameView(player, frame.getId(), new MapsComponents.View(MapsNetwork.MAX_COORD + 1, 0, 1))
                    == MapsNetwork.FrameSave.INVALID, "outside the world");
            player.snapTo(frame.getX() + 9, frame.getY(), frame.getZ());
            h.assertTrue(MapsNetwork.saveFrameView(player, frame.getId(), view) == MapsNetwork.FrameSave.INVALID, "too far away");
            player.snapTo(frame.getX(), frame.getY(), frame.getZ() + 2);
            ItemStack map = frame.getItem().copy();
            frame.setItem(new ItemStack(Items.MAP));
            h.assertTrue(MapsNetwork.saveFrameView(player, frame.getId(), view) == MapsNetwork.FrameSave.INVALID, "not a wayfinder map");
            frame.setItem(map);
            ((com.simplemaps.mixin.ItemFrameAccessor) frame).simplemaps$setFixed(true);
            h.assertTrue(MapsNetwork.saveFrameView(player, frame.getId(), view) == MapsNetwork.FrameSave.DENIED, "fixed frame");
            ((com.simplemaps.mixin.ItemFrameAccessor) frame).simplemaps$setFixed(false);
            player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
            h.assertTrue(MapsNetwork.saveFrameView(player, frame.getId(), view) == MapsNetwork.FrameSave.DENIED, "adventure mode");
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            h.assertTrue(MapsNetwork.saveFrameView(player, frame.getId(), view) == MapsNetwork.FrameSave.DENIED, "spectator");
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            BlockPos at = frame.blockPosition();
            com.simplebuilding.framework.api.Protection.register(server, provider, new com.simplebuilding.framework.api.Protection.Check() {
                @Override
                public boolean allows(com.simplebuilding.framework.api.Protection.Target target) {
                    return false;
                }

                @Override
                public boolean allows(com.simplebuilding.framework.api.Protection.Target target, com.simplebuilding.framework.api.Protection.Action action) {
                    // A claim on the frame's block that refuses the container right (claims guard frames with it).
                    return !(action == com.simplebuilding.framework.api.Protection.Action.CONTAINER && target.x() == at.getX() && target.z() == at.getZ());
                }
            });
            h.assertTrue(MapsNetwork.saveFrameView(player, frame.getId(), view) == MapsNetwork.FrameSave.DENIED, "claim refuses");
            h.assertTrue(!frame.getItem().has(MapsComponents.VIEW), "nothing stored while refused");
            com.simplebuilding.framework.api.Protection.register(server, provider, t -> true);
            h.assertTrue(MapsNetwork.saveFrameView(player, frame.getId(), view) == MapsNetwork.FrameSave.SAVED
                    && view.equals(frame.getItem().get(MapsComponents.VIEW)), "claim allows: saved");
        } finally {
            com.simplebuilding.framework.api.Protection.unregister(server, provider);
            frame.discard();
        }
        h.succeed();
    }

    /** Owner 21.10.: the view code X,Z,stage (chunk of the top-left corner; FrameCode), valid and invalid input, limits. */
    private static void frameCode(GameTestHelper h) {
        int oneToOne = com.simplemaps.FrameCode.NEAR;
        var a = com.simplemaps.FrameCode.parse("1,1,8:1", oneToOne);
        h.assertTrue(a.ok() && a.chunkX() == 1 && a.chunkZ() == 1 && com.simplemaps.FrameCode.label(a.stage()).equals("8:1")
                && com.simplemaps.FrameCode.spanChunks(a.stage()) == 1, "1,1,8:1 = chunk 1,1, one chunk wide");
        MapsComponents.View va = com.simplemaps.FrameCode.view(1, 1, a.stage());
        long[] area = com.simplemaps.FrameCode.area(va);
        h.assertTrue(area[0] == 16 && area[1] == 16 && area[2] == 16, "8:1 frame shows exactly chunk 1,1");
        var b = com.simplemaps.FrameCode.parse("-1,-100,1:64", oneToOne);
        h.assertTrue(b.ok() && b.chunkX() == -1 && b.chunkZ() == -100 && com.simplemaps.FrameCode.spanChunks(b.stage()) == 512, "negative chunks at 1:64");
        MapsComponents.View vb = com.simplemaps.FrameCode.view(-1, -100, b.stage());
        h.assertTrue(vb.zoom() == 64 && vb.mag() == 1 && vb.x() == -16 + 4096 && vb.z() == -1600 + 4096, "1:64 centre");
        var back = com.simplemaps.FrameCode.of(vb);
        h.assertTrue(back.chunkX() == -1 && back.chunkZ() == -100 && back.stage() == b.stage()
                && com.simplemaps.FrameCode.format(back.chunkX(), back.chunkZ(), back.stage()).equals("-1,-100,1:64"), "round trip");
        var c = com.simplemaps.FrameCode.parse(" 3 , -4 , 1:4 ", oneToOne);
        h.assertTrue(c.ok() && c.chunkX() == 3 && c.chunkZ() == -4 && com.simplemaps.FrameCode.label(c.stage()).equals("1:4"), "spaces allowed");
        var d = com.simplemaps.FrameCode.parse("5,6", 5);
        h.assertTrue(d.ok() && d.stage() == 5, "without a stage the current one stays");
        String[][] bad = {{"", "format"}, {"abc", "format"}, {"1,2,3", "format"}, {"1,,1:1", "format"}, {"1.5,2,1:1", "format"},
                {"1,2,1:1,9", "format"}, {"1;2;1:1", "format"}, {"1,2,1:3", "scale"}, {"1,2,3:1", "scale"}, {"1,2,16:1", "scale"},
                {"1,2,1:128", "scale"}, {"99999999999999999999,0,1:1", "range"}, {"1874997,0,1:1", "range"}, {"-1875005,0,1:1", "range"},
                {"0,-1875100,1:1", "range"}};
        for (String[] t : bad) {
            var r = com.simplemaps.FrameCode.parse(t[0], oneToOne);
            h.assertTrue(!r.ok() && t[1].equals(r.error()), "'" + t[0] + "' -> " + t[1] + ", got " + r.error());
        }
        // Limits: the picture's centre must stay inside the world border (30 000 000).
        h.assertTrue(com.simplemaps.FrameCode.parse("1874996,0,1:1", oneToOne).ok() && com.simplemaps.FrameCode.parse("-1875004,0,1:1", oneToOne).ok(), "last chunks inside");
        h.assertTrue(com.simplemaps.FrameCode.clampChunk(1e9, oneToOne) == 1874996 && com.simplemaps.FrameCode.clampChunk(-1e9, oneToOne) == -1875004, "clamped to the border");
        for (int stage = 0; stage < com.simplemaps.FrameCode.STAGE_BPP.length; stage++) {
            String label = com.simplemaps.FrameCode.label(stage);
            var r = com.simplemaps.FrameCode.parse("7,-9," + label, oneToOne);
            MapsComponents.View v = com.simplemaps.FrameCode.view(7, -9, stage);
            int span = com.simplemaps.FrameCode.spanChunks(stage);
            var east = com.simplemaps.FrameCode.of(com.simplemaps.FrameCode.view(7 + span, -9, stage));
            long[] here = com.simplemaps.FrameCode.area(v), next = com.simplemaps.FrameCode.area(com.simplemaps.FrameCode.view(7 + span, -9, stage));
            h.assertTrue(r.ok() && r.stage() == stage && com.simplemaps.FrameCode.of(v).equals(r), "stage " + label + " parses and round-trips");
            h.assertTrue(east.chunkX() == 7 + span && here[0] + here[2] == next[0] && here[2] == 128L * v.zoom() / v.mag(), "stage " + label + ": neighbours touch");
        }
        // Stored views: old ones without magnification read as 1, new ones keep it (persistent and network codecs).
        net.minecraft.nbt.CompoundTag old = new net.minecraft.nbt.CompoundTag();
        old.putInt("x", 10);
        old.putInt("z", 20);
        old.putInt("zoom", 4);
        MapsComponents.View legacy = MapsComponents.View.CODEC.parse(NbtOps.INSTANCE, old).getOrThrow();
        h.assertTrue(legacy.equals(new MapsComponents.View(10, 20, 4, 1)), "old view: magnification 1");
        MapsComponents.View magnified = com.simplemaps.FrameCode.view(2, 3, 0);
        Tag tag = MapsComponents.View.CODEC.encodeStart(NbtOps.INSTANCE, magnified).getOrThrow();
        h.assertTrue(MapsComponents.View.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow().equals(magnified), "codec keeps 8:1");
        var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        MapsComponents.View.STREAM_CODEC.encode(buf, magnified);
        h.assertTrue(MapsComponents.View.STREAM_CODEC.decode(buf).equals(magnified), "network keeps 8:1");
        h.succeed();
    }

    /** Owner 21.10.: wayfinder maps stack only to 1; copying at the cartography table keeps the original. */
    private static void stackSize(GameTestHelper h) {
        for (Item item : List.of(MapsItems.WAYFINDER_MAP, MapsItems.NETHER_WAYFINDER_MAP, MapsItems.END_WAYFINDER_MAP)) {
            h.assertTrue(new ItemStack(item).getMaxStackSize() == 1 && item.getDefaultMaxStackSize() == 1, BuiltInRegistries.ITEM.getKey(item) + " stacks to 1");
        }
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        CartographyTableMenu menu = table(h, player);
        menu.container.setItem(0, bound(h, Waypoints.EMPTY));
        menu.container.setItem(1, new ItemStack(Items.MAP, 3));
        ItemStack first = menu.getSlot(2).getItem();
        h.assertTrue(first.getCount() == 1, "a single copy");
        menu.getSlot(2).onTake(player, first);
        h.assertTrue(MapsItems.isWayfinder(menu.container.getItem(0)) && menu.container.getItem(1).getCount() == 2, "one empty map used, original stays");
        h.assertTrue(menu.getSlot(2).getItem().getCount() == 1, "the next copy is ready");
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
            h.assertTrue(MapsItems.isWayfinder(stack) && MapsItems.tabStacks().size() == 4, "enabled again (3 maps + mega map)");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        } finally {
            MapsConfig.enabled = before;
        }
        h.succeed();
    }

    // Discoverability stage 1 (generated bodies, tools/discover/discover.py)
    static {
        ALL.put("discover_recipe_unlocks", com.simplemaps.discover.MapsDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplemaps.discover.MapsDiscover::behaviourHints);
        ALL.put("guide_book", com.simplebuilding.modules.simplemaps.guide.MapsGuide::gameTest);
    }
}
