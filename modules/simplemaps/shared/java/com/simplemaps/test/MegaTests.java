package com.simplemaps.test;

import com.google.gson.JsonObject;
import com.simplemaps.MapsComponents;
import com.simplemaps.MapsConfig;
import com.simplemaps.MapsItems;
import com.simplemaps.MegaMaps;
import com.simplemaps.MegaTabs;
import com.simplemaps.MegaWayfinderMapItem;
import com.simplemaps.SimpleMaps;
import com.simplemaps.Waypoint;
import com.simplemaps.Waypoints;
import com.simplemaps.WayfinderData;
import com.simplemaps.WayfinderMapItem;
import com.simplemaps.net.MapsNetwork;
import com.simplemaps.net.SelectDimensionPayload;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;

/** Mega wayfinder map (owner B23-K1..K3); registered as {@code simplemaps:module_game_test_mega_*}. */
final class MegaTests {
    private static final String OVERWORLD = "minecraft:overworld", NETHER = "minecraft:the_nether", END = "minecraft:the_end";

    private MegaTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> all) {
        all.put("mega_recipes", MegaTests::recipes);
        all.put("mega_explores", MegaTests::explores);
        all.put("mega_select", MegaTests::select);
        all.put("mega_unlock", MegaTests::unlock);
        all.put("mega_inherit", MegaTests::inherit);
        all.put("mega_cartography", MegaTests::cartography);
    }

    private static boolean recipe(GameTestHelper h, String id) {
        return h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, SimpleMaps.id(id))).isPresent();
    }

    /** K1/K3: exactly one variant of each conditional recipe is loaded (with or without enderite), never both. */
    private static void recipes(GameTestHelper h) {
        h.assertTrue(MapsItems.MEGA_WAYFINDER_MAP instanceof MegaWayfinderMapItem && new ItemStack(MapsItems.MEGA_WAYFINDER_MAP).is(MapsItems.WAYFINDER_MAPS),
                "mega map registered and tagged");
        for (String id : List.of("mega_wayfinder_map", "end_wayfinder_map")) {
            boolean with = recipe(h, id), without = recipe(h, id + "_without_enderite");
            h.assertTrue(with != without, id + ": exactly one variant loaded (with " + with + ", without " + without + ")");
        }
        h.assertTrue(recipe(h, "nether_wayfinder_map"), "nether recipe");
        var nether = h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, SimpleMaps.id("nether_wayfinder_map"))).orElseThrow();
        long echo = nether.value().placementInfo().ingredients().stream().filter(i -> i.test(new ItemStack(Items.ECHO_SHARD))).count();
        h.assertTrue(echo == 1, "Nether wayfinder: one echo shard (K3), got " + echo);
        h.succeed();
    }

    /** K1: in the hand it explores the dimension the holder stands in, each into its own map; the first one is shown. */
    private static void explores(GameTestHelper h) {
        var server = h.getLevel().getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        ItemStack stack = new ItemStack(MapsItems.MEGA_WAYFINDER_MAP);
        MegaWayfinderMapItem item = (MegaWayfinderMapItem) MapsItems.MEGA_WAYFINDER_MAP;
        WayfinderData over = item.dataFor(stack, h.getLevel());
        h.assertTrue(over != null && over.dimension().orElseThrow().equals(Level.OVERWORLD), "Overworld map bound");
        MegaMaps maps = MegaWayfinderMapItem.maps(stack);
        h.assertTrue(maps.selected().equals(OVERWORLD) && stack.get(MapsComponents.MAP_ID).id() == maps.entry(OVERWORLD).orElseThrow().mapId(),
                "first dimension is shown");
        WayfinderData net = item.dataFor(stack, nether);
        maps = MegaWayfinderMapItem.maps(stack);
        h.assertTrue(net != null && net != over && maps.entry(NETHER).orElseThrow().visited(), "own map for the Nether");
        h.assertTrue(maps.entry(NETHER).orElseThrow().kindValue() == WayfinderMapItem.Kind.NETHER, "Nether kind stored");
        h.assertTrue(maps.selected().equals(OVERWORLD), "remembers the shown dimension (no switch by itself)");
        h.assertTrue(!maps.entry(END).orElseThrow().visited(), "End not visited");
        h.assertTrue(item.accepts(stack, h.getLevel()) && !item.accepts(stack, nether), "jamming: shows the Overworld");
        h.assertTrue(item.kind(stack) == WayfinderMapItem.Kind.OVERWORLD, "kind of the shown dimension");
        h.assertTrue(item.dataFor(stack, h.getLevel()) == over, "same map again");
        h.succeed();
    }

    /** K1: switching stores the waypoints per dimension; only visited dimensions; checked on the server. */
    private static void select(GameTestHelper h) {
        var server = h.getLevel().getServer();
        ItemStack stack = new ItemStack(MapsItems.MEGA_WAYFINDER_MAP);
        MegaWayfinderMapItem item = (MegaWayfinderMapItem) MapsItems.MEGA_WAYFINDER_MAP;
        item.dataFor(stack, h.getLevel());
        item.dataFor(stack, server.getLevel(Level.NETHER));
        stack.set(MapsComponents.WAYPOINTS, Waypoints.EMPTY.with(Waypoint.fresh(1, 10, 20)));
        h.assertTrue(!MegaWayfinderMapItem.select(stack, END), "End not visited: no switch");
        h.assertTrue(!MegaWayfinderMapItem.select(stack, "minecraft:nowhere"), "unknown dimension: no switch");
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        MapsNetwork.handleSelectDimension(new SelectDimensionPayload(0, NETHER), player);
        ItemStack held = player.getMainHandItem();
        MegaMaps maps = MegaWayfinderMapItem.maps(held);
        h.assertTrue(maps.selected().equals(NETHER), "switched by the packet");
        h.assertTrue(held.get(MapsComponents.MAP_ID).id() == maps.entry(NETHER).orElseThrow().mapId(), "MAP_ID follows");
        h.assertTrue(!held.has(MapsComponents.WAYPOINTS), "the Nether has its own (no) waypoints");
        h.assertTrue(maps.entry(OVERWORLD).orElseThrow().waypoints().get(1).isPresent(), "Overworld waypoints kept in its entry");
        h.assertTrue(MegaWayfinderMapItem.select(held, OVERWORLD), "back");
        h.assertTrue(held.getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY).get(1).map(w -> w.x() == 10).orElse(false), "waypoints restored");
        MapsNetwork.handleSelectDimension(new SelectDimensionPayload(5, NETHER), player);
        h.assertTrue(MegaWayfinderMapItem.maps(held).selected().equals(OVERWORLD), "bad hand refused");
        // Stored and sent like any component.
        var tag = MegaMaps.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, MegaWayfinderMapItem.maps(held)).getOrThrow();
        h.assertTrue(MegaMaps.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).getOrThrow().equals(MegaWayfinderMapItem.maps(held)), "codec round trip");
        h.succeed();
    }

    /** K2: anvil rules (config item or "any" block where you stand), only existing dimensions, never built-in or twice. */
    private static void unlock(GameTestHelper h) {
        MegaMaps maps = MegaMaps.EMPTY;
        Set<String> existing = Set.of(OVERWORLD, NETHER, END, "mod:moon", "mod:mars", "mod:caves");
        Map<String, String> rules = Map.of("mod:moon", "minecraft:white_concrete", "mod:caves", "");
        h.assertTrue("mod:moon".equals(maps.unlockTarget(rules, MegaMaps.ANY, OVERWORLD, existing::contains, "minecraft:white_concrete", true)),
                "configured item unlocks its dimension anywhere");
        h.assertTrue("mod:mars".equals(maps.unlockTarget(rules, MegaMaps.ANY, "mod:mars", existing::contains, "minecraft:dirt", true)),
                "any block unlocks the dimension you stand in");
        h.assertTrue(maps.unlockTarget(rules, MegaMaps.ANY, "mod:mars", existing::contains, "minecraft:stick", false) == null, "any = blocks only");
        h.assertTrue(maps.unlockTarget(rules, "", "mod:mars", existing::contains, "minecraft:dirt", true) == null, "default off");
        h.assertTrue(maps.unlockTarget(rules, MegaMaps.ANY, "mod:venus", existing::contains, "minecraft:dirt", true) == null, "not on this server");
        h.assertTrue(maps.unlockTarget(rules, MegaMaps.ANY, NETHER, existing::contains, "minecraft:netherrack", true) == null, "built-in never");
        MegaMaps moon = maps.unlock("mod:moon", Identifier.parse("minecraft:white_concrete"));
        h.assertTrue(moon != null && moon.all().size() == 4 && moon.all().get(3).dimension().equals("mod:moon") && !moon.all().get(3).visited(),
                "unlocked after the three built-in ones, not visited yet");
        h.assertTrue(moon.unlock("mod:moon", Identifier.parse("minecraft:dirt")) == null && moon.unlock(END, Identifier.parse("minecraft:dirt")) == null, "no twice");
        h.assertTrue(moon.unlockTarget(rules, MegaMaps.ANY, OVERWORLD, existing::contains, "minecraft:white_concrete", true) == null, "already unlocked");
        MegaMaps many = maps;
        for (int i = 0; many.all().size() < MegaMaps.MAX; i++) many = many.unlock("mod:d" + i, Identifier.parse("minecraft:dirt"));
        h.assertTrue(many.unlock("mod:full", Identifier.parse("minecraft:dirt")) == null, "at most MAX dimensions");
        h.assertTrue(MegaTabs.paged(many.all().size()) && MegaTabs.places(6, 1)[1] == -1, "paged with placeholders");
        // The anvil itself: built-in dimensions are never offered, so mega + grass stays Vanilla's (no result).
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        AnvilMenu anvil = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(BlockPos.ZERO)));
        anvil.getSlot(0).set(new ItemStack(MapsItems.MEGA_WAYFINDER_MAP));
        anvil.getSlot(1).set(new ItemStack(Items.GRASS_BLOCK));
        anvil.createResult();
        h.assertTrue(anvil.getSlot(2).getItem().isEmpty(), "no unlock for a built-in dimension");
        try {
            JsonObject json = new JsonObject();
            JsonObject r = new JsonObject();
            r.addProperty("mod:moon", "minecraft:white_concrete");
            r.addProperty("minecraft:the_end", "minecraft:dirt");
            r.addProperty("bad id!", "minecraft:dirt");
            json.add("megaUnlockItems", r);
            json.addProperty("megaUnlockDefault", "minecraft:dirt");
            json.addProperty("megaUnlockLevels", 500);
            MapsConfig.apply(json);
            h.assertTrue(MapsConfig.megaUnlockItems.equals(Map.of("mod:moon", "minecraft:white_concrete")), "rules cleaned: " + MapsConfig.megaUnlockItems);
            h.assertTrue(MapsConfig.megaUnlockDefault.equals(MegaMaps.ANY) && MapsConfig.megaUnlockLevels == MapsConfig.MEGA_LEVELS_MAX, "clamped");
        } finally {
            MapsConfig.apply(new JsonObject());
        }
        h.assertTrue(MapsConfig.megaUnlockItems.isEmpty() && MapsConfig.megaUnlockLevels == MapsConfig.MEGA_LEVELS_DEFAULT, "defaults back");
        h.succeed();
    }

    /** K1: crafting takes over the three maps (ids, waypoints); the Overworld one is shown. */
    private static void inherit(GameTestHelper h) {
        var server = h.getLevel().getServer();
        ItemStack over = new ItemStack(MapsItems.WAYFINDER_MAP), nether = new ItemStack(MapsItems.NETHER_WAYFINDER_MAP), end = new ItemStack(MapsItems.END_WAYFINDER_MAP);
        ((WayfinderMapItem) MapsItems.WAYFINDER_MAP).dataFor(over, h.getLevel());
        ((WayfinderMapItem) MapsItems.NETHER_WAYFINDER_MAP).dataFor(nether, server.getLevel(Level.NETHER));
        nether.set(MapsComponents.WAYPOINTS, Waypoints.EMPTY.with(Waypoint.fresh(4, 1, 1)));
        over.set(MapsComponents.WAYPOINTS, Waypoints.EMPTY.with(Waypoint.fresh(2, 3, 3)));
        ItemStack mega = new ItemStack(MapsItems.MEGA_WAYFINDER_MAP);
        MegaWayfinderMapItem.inherit(mega, List.of(nether, end, over, new ItemStack(Items.ECHO_SHARD)), server);
        MegaMaps maps = MegaWayfinderMapItem.maps(mega);
        h.assertTrue(maps.entry(OVERWORLD).orElseThrow().mapId() == over.get(MapsComponents.MAP_ID).id(), "Overworld map taken over");
        h.assertTrue(maps.entry(NETHER).orElseThrow().mapId() == nether.get(MapsComponents.MAP_ID).id(), "Nether map taken over");
        h.assertTrue(maps.entry(NETHER).orElseThrow().waypoints().get(4).isPresent(), "Nether waypoints kept");
        h.assertTrue(!maps.entry(END).orElseThrow().visited(), "unused End map (no id) leaves the End unvisited");
        h.assertTrue(maps.selected().equals(OVERWORLD) && mega.get(MapsComponents.MAP_ID).id() == over.get(MapsComponents.MAP_ID).id()
                && mega.getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY).get(2).isPresent(), "Overworld shown with its waypoints");
        h.succeed();
    }

    /** The cartography table only copies a mega map. */
    private static void cartography(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack mega = new ItemStack(MapsItems.MEGA_WAYFINDER_MAP);
        ((WayfinderMapItem) MapsItems.MEGA_WAYFINDER_MAP).dataFor(mega, h.getLevel());
        CartographyTableMenu menu = new CartographyTableMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(BlockPos.ZERO)));
        menu.container.setItem(0, mega);
        menu.container.setItem(1, new ItemStack(Items.MAP));
        ItemStack copy = menu.getSlot(2).getItem();
        h.assertTrue(copy.is(MapsItems.MEGA_WAYFINDER_MAP) && MegaWayfinderMapItem.maps(copy).equals(MegaWayfinderMapItem.maps(mega)), "copy");
        BlockPos c = h.absolutePos(BlockPos.ZERO);
        menu.container.setItem(1, MapItem.create(h.getLevel(), c.getX(), c.getZ(), (byte) 0, true, false));
        h.assertTrue(menu.getSlot(2).getItem().isEmpty(), "no extending");
        ItemStack plain = new ItemStack(MapsItems.WAYFINDER_MAP);
        ((WayfinderMapItem) MapsItems.WAYFINDER_MAP).dataFor(plain, h.getLevel());
        menu.container.setItem(1, plain);
        h.assertTrue(menu.getSlot(2).getItem().isEmpty(), "no combining");
        h.succeed();
    }
}
