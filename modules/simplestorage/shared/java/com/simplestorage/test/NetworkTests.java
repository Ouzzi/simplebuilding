package com.simplestorage.test;

import com.simplestorage.SimpleStorage;
import com.simplestorage.chest.ChestTier;
import com.simplestorage.chest.TieredChestBlockEntity;
import com.simplestorage.config.StorageConfig;
import com.simplestorage.network.CableBlock;
import com.simplestorage.network.ChestNetwork;
import com.simplestorage.network.ControllerChestBlock;
import com.simplestorage.network.ControllerChestBlockEntity;
import com.simplestorage.network.ControllerLevel;
import com.simplestorage.network.ControllerLoot;
import com.simplestorage.network.ControllerMenu;
import com.simplestorage.network.InterfaceBlock;
import com.simplestorage.network.NetworkSubstitutes;
import com.simplestorage.registry.StorageBlockEntities;
import com.simplestorage.registry.StorageBlocks;
import com.simplestorage.registry.StorageMenus;
import com.simplestorage.registry.StorageRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Module tests of the chest network (BS29b-3): wiring, limits, the broken state, the cached search, the window's clicks, the
 * switch in the config, the loot. The limits are this file's own constants (4 / 12 / 32 / 64 chest blocks). Coordinates stay
 * inside the 8 x 8 x 8 test structure; every test finishes in the tick it starts (the shared network version is global).
 */
public final class NetworkTests {
    private NetworkTests() {
    }

    private static final BlockPos CONTROLLER = new BlockPos(1, 2, 2);
    private static final BlockPos HUB = new BlockPos(3, 2, 2);
    private static final BlockPos UP_CHEST = new BlockPos(3, 4, 2);
    private static final BlockPos SOUTH_CHEST = new BlockPos(3, 2, 4);

    private static Block block(String id) {
        return Refs.block(id);
    }

    /** Places a cable the way an item does: the arms follow the neighbours that are there already. */
    private static void cableAt(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, CableBlock.withArms(StorageBlocks.COPPER_CABLE.defaultBlockState(), h.getLevel(), h.absolutePos(pos)));
    }

    private static void interfaceAt(GameTestHelper h, BlockPos pos, Direction toChest) {
        h.setBlock(pos, StorageBlocks.CHEST_INTERFACE.defaultBlockState().setValue(InterfaceBlock.FACING, toChest));
    }

    private static Container container(GameTestHelper h, BlockPos pos) {
        return (Container) h.getBlockEntity(pos, BlockEntity.class);
    }

    /**
     * The test network: a controller at (1,2,2) of the given step, its interface at (2,2,2), a cable hub at (3,2,2) with an interface
     * above (chest at (3,4,2)) and one to the south (chest at (3,2,4)).
     */
    private static ControllerChestBlockEntity star(GameTestHelper h, String step, Block upChest, Block southChest) {
        h.setBlock(UP_CHEST, upChest);
        h.setBlock(SOUTH_CHEST, southChest);
        h.setBlock(CONTROLLER, block(step + "_controller_chest"));
        h.setBlock(HUB, StorageBlocks.COPPER_CABLE);
        interfaceAt(h, CONTROLLER.east(), Direction.WEST);
        interfaceAt(h, HUB.above(), Direction.UP);
        interfaceAt(h, HUB.south(), Direction.SOUTH);
        return h.getBlockEntity(CONTROLLER, ControllerChestBlockEntity.class);
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /** Blocks, items, block entity types, menus, steps and recipes. */
    public static void registered(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        if (!(StorageBlocks.COPPER_CABLE instanceof CableBlock)) problems.add("cable block");
        if (!(StorageBlocks.CHEST_INTERFACE instanceof InterfaceBlock)) problems.add("interface block");
        int[] limits = {4, 12, 32, 64};
        String[] steps = {"wooden", "reinforced", "netherite", "enderite"};
        for (int i = 0; i < steps.length; i++) {
            if (!(block(steps[i] + "_controller_chest") instanceof ControllerChestBlock controller)) {
                problems.add("controller " + steps[i]);
                continue;
            }
            ControllerLevel step = ControllerLevel.values()[i];
            if (controller.step() != step || step.maxChests() != limits[i] || step.rank() != i) problems.add(steps[i] + " step " + step + "/" + step.maxChests() + "/" + step.rank());
            if (!StorageBlockEntities.CONTROLLER_CHEST.isValid(controller.defaultBlockState())) problems.add(steps[i] + " block entity type");
        }
        for (String id : List.of("copper_cable", "chest_interface", "wooden_controller_chest", "reinforced_controller_chest", "netherite_controller_chest",
                "enderite_controller_chest")) {
            if (!BuiltInRegistries.ITEM.containsKey(SimpleStorage.id(id))) problems.add("no item " + id);
            if (h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, SimpleStorage.id(id))).isEmpty()) problems.add("no recipe " + id);
        }
        if (StorageMenus.CONTROLLER == null) problems.add("controller menu type");
        if (ControllerLevel.rankOf(Blocks.CHEST) != 0 || ControllerLevel.rankOf(block("reinforced_chest")) != 1
                || ControllerLevel.rankOf(block("enderite_chest")) != 3 || ControllerLevel.rankOf(Blocks.BARREL) != -1
                || ControllerLevel.rankOf(block("wooden_controller_chest")) != -1) problems.add("chest ranks");
        h.assertTrue(problems.isEmpty(), "chest network registrations: " + problems);
        h.succeed();
    }

    /** Controller -> interface -> cable hub -> interfaces -> chests: the search finds both chests; the controller's own interface is not a member. */
    public static void searchFindsChestsThroughCables(GameTestHelper h) {
        ControllerChestBlockEntity controller = star(h, "wooden", Blocks.CHEST, Blocks.CHEST);
        ChestNetwork.Scan scan = controller.scan();
        ChestNetwork.Members members = ChestNetwork.members(h.getLevel(), 0, scan);
        List<String> problems = new ArrayList<>();
        if (scan.status() != ChestNetwork.Status.OK) problems.add("status " + scan.status());
        if (scan.interfaces().size() != 2) problems.add("interfaces " + scan.interfaces());
        if (scan.ownInterfaces() != 1) problems.add("own interfaces " + scan.ownInterfaces());
        if (members.chests().size() != 2 || !members.chests().contains(h.absolutePos(UP_CHEST)) || !members.chests().contains(h.absolutePos(SOUTH_CHEST))) {
            problems.add("members " + members.chests());
        }
        h.assertTrue(problems.isEmpty(), "network search: " + problems);
        h.succeed();
    }

    /** A chest above the controller's step is ignored and counted; the same chest is fine for a higher controller. */
    public static void controllerTakesChestsOfItsStepOrLower(GameTestHelper h) {
        ControllerChestBlockEntity controller = star(h, "wooden", block("reinforced_chest"), Blocks.CHEST);
        ChestNetwork.Members wooden = ChestNetwork.members(h.getLevel(), ControllerLevel.WOODEN.rank(), controller.scan());
        ChestNetwork.Members reinforced = ChestNetwork.members(h.getLevel(), ControllerLevel.REINFORCED.rank(), controller.scan());
        List<String> problems = new ArrayList<>();
        if (wooden.chests().size() != 1 || wooden.rejected() != 1) problems.add("wooden takes " + wooden);
        if (reinforced.chests().size() != 2 || reinforced.rejected() != 0) problems.add("reinforced takes " + reinforced);
        // a copper chest counts as wood (rank 0), a double chest as two chest blocks
        h.setBlock(SOUTH_CHEST, Blocks.COPPER_CHEST.weathering().unaffected());
        if (ChestNetwork.members(h.getLevel(), 0, controller.scan()).chests().size() != 1) problems.add("copper chest above a wooden controller");
        h.assertTrue(problems.isEmpty(), "controller steps: " + problems);
        h.succeed();
    }

    /** More chest blocks than the step allows breaks the controller: broken screen state, empty list, no clicks; fewer chests heal it. */
    public static void tooManyChestsBreakTheController(GameTestHelper h) {
        BlockPos controllerPos = new BlockPos(0, 2, 2);
        h.setBlock(controllerPos, block("wooden_controller_chest"));
        for (int x = 2; x <= 6; x++) {
            h.setBlock(new BlockPos(x, 4, 2), Blocks.CHEST);
            h.setBlock(new BlockPos(x, 2, 2), StorageBlocks.COPPER_CABLE);
            interfaceAt(h, new BlockPos(x, 3, 2), Direction.UP);
        }
        interfaceAt(h, new BlockPos(1, 2, 2), Direction.WEST);
        ControllerChestBlockEntity controller = h.getBlockEntity(controllerPos, ControllerChestBlockEntity.class);
        ServerPlayer player = player(h);
        container(h, new BlockPos(3, 4, 2)).setItem(0, new ItemStack(Items.STONE, 4));
        ControllerMenu menu = ControllerMenu.server(1, player.getInventory(), controller);
        List<String> problems = new ArrayList<>();
        if (menu.status() != ChestNetwork.Status.TOO_MANY || menu.chests() != 5 || menu.limit() != 4) problems.add("status " + menu.status() + " chests " + menu.chests() + "/" + menu.limit());
        if (menu.kinds() != 0 || !menu.getSlot(0).getItem().isEmpty()) problems.add("a broken controller still lists items");
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        if (!menu.getCarried().isEmpty() || container(h, new BlockPos(3, 4, 2)).countItem(Items.STONE) != 4) problems.add("a broken controller gave access");
        h.setBlock(new BlockPos(6, 4, 2), Blocks.AIR);
        menu.refresh();
        if (menu.status() != ChestNetwork.Status.OK || menu.chests() != 4 || menu.kinds() != 1) problems.add("after removing a chest: " + menu.status() + " " + menu.chests() + " kinds " + menu.kinds());
        h.assertTrue(problems.isEmpty(), "too many chests: " + problems);
        h.succeed();
    }

    /** A second controller on the same network breaks both. */
    public static void secondControllerConflicts(GameTestHelper h) {
        ControllerChestBlockEntity first = star(h, "wooden", Blocks.CHEST, Blocks.CHEST);
        BlockPos secondPos = new BlockPos(3, 2, 0);
        h.setBlock(secondPos, block("reinforced_controller_chest"));
        interfaceAt(h, new BlockPos(3, 2, 1), Direction.NORTH);
        ControllerChestBlockEntity second = h.getBlockEntity(secondPos, ControllerChestBlockEntity.class);
        ServerPlayer player = player(h);
        ControllerMenu one = ControllerMenu.server(1, player.getInventory(), first);
        ControllerMenu two = ControllerMenu.server(2, player.getInventory(), second);
        List<String> problems = new ArrayList<>();
        if (one.status() != ChestNetwork.Status.CONFLICT) problems.add("first " + one.status());
        if (two.status() != ChestNetwork.Status.CONFLICT) problems.add("second " + two.status());
        h.assertTrue(problems.isEmpty(), "two controllers: " + problems);
        h.succeed();
    }

    /** The search runs again only after the wiring changed - never because of time or a new window. */
    public static void searchIsCachedUntilTheWiringChanges(GameTestHelper h) {
        ControllerChestBlockEntity controller = star(h, "wooden", Blocks.CHEST, Blocks.CHEST);
        ChestNetwork.Scan first = controller.scan();
        int count = controller.scanCount();
        ChestNetwork.Scan again = controller.scan();
        ServerPlayer player = player(h);
        ControllerMenu.server(1, player.getInventory(), controller);
        List<String> problems = new ArrayList<>();
        if (again != first || controller.scanCount() != count) problems.add("scanned again without a change (" + controller.scanCount() + " vs " + count + ")");
        h.setBlock(HUB.west().above(), StorageBlocks.COPPER_CABLE); // a cable next to the interface: placing it tells the network
        ChestNetwork.Scan renewed = controller.scan();
        if (renewed == first || controller.scanCount() != count + 1) problems.add("no new search after a cable was placed (" + controller.scanCount() + ")");
        ChestNetwork.changed();
        if (controller.scan() == renewed) problems.add("no new search after changed()");
        h.assertTrue(problems.isEmpty(), "search cache: " + problems);
        h.succeed();
    }

    /** The window lists the kinds with their totals, a click takes a stack, a click with an item stores it, shift-click works both ways. */
    public static void windowListsTakesAndStores(GameTestHelper h) {
        ControllerChestBlockEntity controller = star(h, "wooden", Blocks.CHEST, Blocks.CHEST);
        container(h, UP_CHEST).setItem(0, new ItemStack(Items.STONE, 3));
        container(h, SOUTH_CHEST).setItem(0, new ItemStack(Items.STONE, 5));
        container(h, SOUTH_CHEST).setItem(1, new ItemStack(Items.GOLD_INGOT, 2));
        controller.setItem(0, new ItemStack(Items.DIRT, 7));
        ServerPlayer player = player(h);
        ControllerMenu menu = ControllerMenu.server(1, player.getInventory(), controller);
        List<String> problems = new ArrayList<>();
        if (menu.status() != ChestNetwork.Status.OK || menu.kinds() != 3) problems.add("status " + menu.status() + " kinds " + menu.kinds());
        if (!menu.getSlot(0).getItem().is(Items.STONE) || menu.countAt(0) != 8) problems.add("cell 0: " + menu.getSlot(0).getItem() + " x" + menu.countAt(0));
        if (!menu.getSlot(1).getItem().is(Items.DIRT) || menu.countAt(1) != 7) problems.add("cell 1: " + menu.getSlot(1).getItem() + " x" + menu.countAt(1));
        // take all 8 stone with the cursor
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        if (!menu.getCarried().is(Items.STONE) || menu.getCarried().getCount() != 8) problems.add("carried " + menu.getCarried());
        if (container(h, UP_CHEST).countItem(Items.STONE) + container(h, SOUTH_CHEST).countItem(Items.STONE) != 0) problems.add("stone is still in the chests");
        // put them back with a click on any cell, one by one with the right button first
        menu.clicked(10, 1, ContainerInput.PICKUP, player);
        if (menu.getCarried().getCount() != 7) problems.add("a right click stores one (carried " + menu.getCarried().getCount() + ")");
        menu.clicked(10, 0, ContainerInput.PICKUP, player);
        if (!menu.getCarried().isEmpty()) problems.add("a left click stores the rest (carried " + menu.getCarried() + ")");
        int stone = 0;
        for (var entry : new ChestNetwork.Storage(h.getLevel(), controller, ChestNetwork.members(h.getLevel(), 0, controller.scan()), player).entries(ChestNetwork.Sort.COUNT)) {
            if (entry.kind().is(Items.STONE)) stone += (int) entry.count();
        }
        if (stone != 8) problems.add("the network holds " + stone + " stone after storing");
        // shift-click a cell: a stack of gold into the inventory
        int gold = -1;
        for (int i = 0; i < ControllerMenu.CELLS; i++) {
            if (menu.getSlot(i).getItem().is(Items.GOLD_INGOT)) gold = i;
        }
        if (gold < 0) {
            problems.add("no gold cell");
        } else {
            menu.clicked(gold, 0, ContainerInput.QUICK_MOVE, player);
            if (player.getInventory().countItem(Items.GOLD_INGOT) != 2) problems.add("shift-click gave " + player.getInventory().countItem(Items.GOLD_INGOT) + " gold");
        }
        // shift-click in the inventory stores
        player.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 4));
        ItemStack moved = menu.quickMoveStack(player, ControllerMenu.CELLS);
        if (moved.isEmpty() || !player.getInventory().getItem(9).isEmpty()) problems.add("shift-click in the inventory did not store");
        h.assertTrue(problems.isEmpty(), "window clicks: " + problems);
        h.succeed();
    }

    /** An interface sits only on a chest and drops off when the chest goes; the cable draws arms like a chorus plant. */
    public static void interfaceNeedsChestAndCableDrawsArms(GameTestHelper h) {
        BlockPos chest = new BlockPos(2, 2, 2);
        BlockPos plate = new BlockPos(3, 2, 2);
        h.setBlock(chest, Blocks.CHEST);
        interfaceAt(h, plate, Direction.WEST);
        List<String> problems = new ArrayList<>();
        if (!h.getBlockState(plate).canSurvive(h.getLevel(), h.absolutePos(plate))) problems.add("an interface on a chest cannot survive");
        h.setBlock(chest, Blocks.AIR);
        if (!h.getBlockState(plate).isAir()) problems.add("the interface stayed without its chest");
        // cables: a vertical pillar and a side neighbour
        BlockPos low = new BlockPos(5, 2, 5);
        cableAt(h, low);
        cableAt(h, low.above());
        h.setBlock(low.east(), Blocks.STONE);
        BlockState lower = h.getBlockState(low);
        BlockState upper = h.getBlockState(low.above());
        if (!lower.getValue(PipeBlock.UP) || lower.getValue(PipeBlock.EAST) || !upper.getValue(PipeBlock.DOWN) || upper.getValue(PipeBlock.UP)) problems.add("cable arms " + lower + " / " + upper);
        h.assertTrue(problems.isEmpty(), "interface and cable: " + problems);
        h.succeed();
    }

    /** Switched off in the config, the network blocks turn into Vanilla blocks; a controller keeps its contents (what does not fit pops out). */
    public static void switchedOffTheBlocksBecomeSubstitutes(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        BlockPos cable = new BlockPos(5, 2, 5);
        BlockPos chest = new BlockPos(2, 2, 5);
        BlockPos plate = new BlockPos(3, 2, 5);
        BlockPos wooden = new BlockPos(5, 2, 1);
        BlockPos reinforced = new BlockPos(1, 2, 1);
        h.setBlock(cable, StorageBlocks.COPPER_CABLE);
        h.setBlock(chest, Blocks.CHEST);
        interfaceAt(h, plate, Direction.WEST);
        h.setBlock(wooden, block("wooden_controller_chest"));
        h.setBlock(reinforced, block("reinforced_controller_chest"));
        Container w = container(h, wooden);
        Container r = container(h, reinforced);
        for (int i = 0; i < 30; i++) w.setItem(i, new ItemStack(Items.STONE, 1));
        for (int i = 0; i < 30; i++) r.setItem(i, new ItemStack(Items.DIRT, 1));
        boolean saved = StorageConfig.chestNetwork;
        try {
            StorageConfig.chestNetwork = false;
            if (!StorageRegistry.networkStacks().isEmpty()) problems.add("network items stay in the tab while off");
            if (NetworkSubstitutes.enabled()) problems.add("switch not read");
            if (!NetworkSubstitutes.replaceIfDisabled(h.getLevel(), h.absolutePos(cable), h.getBlockState(cable)) || !h.getBlockState(cable).is(Blocks.COPPER_BARS.weathering().unaffected())) problems.add("cable -> " + h.getBlockState(cable));
            if (!NetworkSubstitutes.replaceIfDisabled(h.getLevel(), h.absolutePos(plate), h.getBlockState(plate)) || !h.getBlockState(plate).is(Blocks.GLASS_PANE)) problems.add("interface -> " + h.getBlockState(plate));
            if (!NetworkSubstitutes.replaceIfDisabled(h.getLevel(), h.absolutePos(wooden), h.getBlockState(wooden)) || !h.getBlockState(wooden).is(Blocks.CHEST)) problems.add("wooden controller -> " + h.getBlockState(wooden));
            if (!NetworkSubstitutes.replaceIfDisabled(h.getLevel(), h.absolutePos(reinforced), h.getBlockState(reinforced)) || !h.getBlockState(reinforced).is(block("reinforced_chest"))) problems.add("reinforced controller -> " + h.getBlockState(reinforced));
            if (NetworkSubstitutes.replaceIfDisabled(h.getLevel(), h.absolutePos(chest), h.getBlockState(chest))) problems.add("a Vanilla chest was replaced");
        } finally {
            StorageConfig.chestNetwork = saved;
        }
        Container plain = container(h, wooden);
        int kept = 0;
        for (int i = 0; i < plain.getContainerSize(); i++) kept += plain.getItem(i).getCount();
        int popped = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(wooden)).inflate(2.0)).stream().mapToInt(e -> e.getItem().getCount()).sum();
        if (kept != 27 || popped != 3) problems.add("wooden controller: kept " + kept + " (27 expected), popped " + popped + " (3 expected)");
        Container plainReinforced = container(h, reinforced);
        int keptDirt = 0;
        for (int i = 0; i < plainReinforced.getContainerSize(); i++) keptDirt += plainReinforced.getItem(i).getCount();
        if (keptDirt != 30) problems.add("reinforced controller kept " + keptDirt + " of 30");
        h.assertTrue(problems.isEmpty(), "switched off: " + problems);
        h.succeed();
    }

    /** The loot find: a controller chest of the step, two interfaces and four cables go into free slots of a better chest. */
    public static void betterChestsHoldControllerFinds(GameTestHelper h) {
        h.setBlock(CONTROLLER, block("reinforced_chest"));
        TieredChestBlockEntity chest = h.getBlockEntity(CONTROLLER, TieredChestBlockEntity.class);
        ControllerLoot.add(chest, ControllerLevel.REINFORCED, RandomSource.create(7));
        int controllers = 0, interfaces = 0, cables = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack stack = chest.getItem(i);
            if (stack.is(com.simplestorage.registry.StorageItems.CONTROLLERS.get(ControllerLevel.REINFORCED))) controllers += stack.getCount();
            if (stack.is(com.simplestorage.registry.StorageItems.CHEST_INTERFACE)) interfaces += stack.getCount();
            if (stack.is(com.simplestorage.registry.StorageItems.COPPER_CABLE)) cables += stack.getCount();
        }
        List<String> problems = new ArrayList<>();
        if (controllers != 1 || interfaces != 2 || cables != 4) problems.add("find " + controllers + "/" + interfaces + "/" + cables);
        if (ControllerLoot.stepOf(ChestTier.NETHERITE) != ControllerLevel.NETHERITE || ControllerLoot.stepOf(ChestTier.ENDERITE) != ControllerLevel.ENDERITE) problems.add("steps of the tiers");
        if (StorageConfig.controllerLootChance() < 0.0 || StorageConfig.controllerLootChance() > 0.5) problems.add("loot chance " + StorageConfig.controllerLootChance());
        // the config keys and clamp
        var saved = StorageConfig.toJson();
        try {
            var json = new com.google.gson.JsonObject();
            json.addProperty("controllerLootPercent", 99.0);
            json.addProperty("chestNetwork", false);
            StorageConfig.apply(json);
            if (StorageConfig.controllerLootPercent != 50.0 || StorageConfig.chestNetwork) problems.add("config apply " + StorageConfig.controllerLootPercent + "/" + StorageConfig.chestNetwork);
            StorageConfig.apply(new com.google.gson.JsonObject());
            if (!StorageConfig.chestNetwork || StorageConfig.controllerLootPercent != 10.0) problems.add("config defaults");
        } finally {
            StorageConfig.apply(saved);
        }
        h.assertTrue(problems.isEmpty(), "controller loot: " + problems);
        h.succeed();
    }
}
