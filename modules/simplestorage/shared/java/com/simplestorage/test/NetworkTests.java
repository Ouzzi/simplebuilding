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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

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

    /**
     * IG31-6: an interface next to a chest grows to a wall beside it (edge flag and shape reach the block's edge, no see-through
     * gap), and only there: with air beside the plate the edge flags stay off and the shape keeps the 1 px margin.
     */
    public static void interfaceReachesTheWallBesideIt(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        BlockPos at = new BlockPos(3, 2, 3);
        h.setBlock(at.north(), Blocks.CHEST);
        interfaceAt(h, at, Direction.NORTH);
        BlockPos abs = h.absolutePos(at);
        BlockState open = InterfaceBlock.withArms(h.getBlockState(at), h.getLevel(), abs);
        for (Direction side : InterfaceBlock.localSides(Direction.NORTH)) {
            if (open.getValue(edgeOf(side))) problems.add("edge " + side + " set with nothing beside the plate");
        }
        h.setBlock(at.east(), Blocks.STONE);
        BlockState walled = h.getBlockState(at);
        if (!walled.getValue(InterfaceBlock.EDGE_E)) problems.add("the stone to the east did not set edge_e: " + walled);
        if (walled.getValue(InterfaceBlock.EDGE_W) || walled.getValue(InterfaceBlock.EDGE_U) || walled.getValue(InterfaceBlock.EDGE_D)) problems.add("other edges set: " + walled);
        double reach = walled.getShape(h.getLevel(), abs).max(Direction.Axis.X);
        if (reach != 1.0) problems.add("the plate stops at x " + reach + " instead of the wall");
        double free = open.getShape(h.getLevel(), abs).max(Direction.Axis.X);
        if (free != 15.0 / 16) problems.add("the free plate is " + free + " wide, expected the 1 px margin");
        // a plate on the up side of a chest: the local edges turn (east neighbour of an UP plate is local +x again)
        BlockPos top = new BlockPos(5, 3, 5);
        h.setBlock(top.above(), Blocks.CHEST);
        interfaceAt(h, top, Direction.UP);
        h.setBlock(top.south(), Blocks.STONE);
        BlockState up = h.getBlockState(top);
        if (!up.getValue(InterfaceBlock.EDGE_U)) problems.add("a wall south of an up-plate sets the local up edge: " + up);
        h.assertTrue(problems.isEmpty(), "interface edges: " + problems);
        h.succeed();
    }

    /** An interface placed the way the item does it: its form, arms and edges from the neighbours that are there already. */
    private static BlockState placedInterface(GameTestHelper h, BlockPos pos, Direction toChest) {
        BlockState state = InterfaceBlock.withArms(StorageBlocks.CHEST_INTERFACE.defaultBlockState().setValue(InterfaceBlock.FACING, toChest),
                h.getLevel(), h.absolutePos(pos));
        h.setBlock(pos, state);
        return h.getBlockState(pos);
    }

    private static AABB bounds(GameTestHelper h, BlockPos pos) {
        return h.getBlockState(pos).getShape(h.getLevel(), h.absolutePos(pos)).bounds();
    }

    /**
     * DW03C-1/2: on a chest the plate keeps the chest's width and, beside it, only the part below the lid (front: the lock stays
     * free, its own form); under the chest it stays in its own cell (no face in the chest's cell, no z-fighting); on top of a chest
     * it cannot be placed; towards the other half of a double chest it always reaches the edge; with interfaceFillsGaps off a wall
     * beside it does not stretch it.
     */
    public static void interfaceFitsTheChest(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        // beside (back of) a chest that faces north, the interface to the south of it
        BlockPos back = new BlockPos(1, 2, 2);
        h.setBlock(back.north(), Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.NORTH));
        BlockState side = placedInterface(h, back, Direction.NORTH);
        AABB b = bounds(h, back);
        if (side.getValue(InterfaceBlock.MOUNT) != InterfaceBlock.Mount.CHEST) problems.add("back mount " + side);
        // the lid swings back over the hinge (y 9, up to 5 px behind the chest): nothing of the interface there
        if (net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(side.getShape(h.getLevel(), h.absolutePos(back)),
                Block.box(0, 9, 0, 16, 16, 5), net.minecraft.world.phys.shapes.BooleanOp.AND) || b.minY != 0.0) problems.add("back plate in the lid's way: " + b);
        if (b.minX != 1.0 / 16 || b.maxX != 15.0 / 16) problems.add("back plate not the chest's width: x " + b.minX + ".." + b.maxX);
        // in front of a chest that faces south (towards the interface)
        BlockPos front = new BlockPos(4, 2, 2);
        h.setBlock(front.north(), Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.SOUTH));
        if (placedInterface(h, front, Direction.NORTH).getValue(InterfaceBlock.MOUNT) != InterfaceBlock.Mount.CHEST_FRONT) problems.add("front mount");
        // under a floating chest: own cell only, flush with the chest (no blocks around it)
        BlockPos under = new BlockPos(1, 2, 5);
        h.setBlock(under.above(), Blocks.CHEST);
        BlockState below = placedInterface(h, under, Direction.UP);
        b = bounds(h, under);
        if (below.getValue(InterfaceBlock.MOUNT) != InterfaceBlock.Mount.CHEST) problems.add("under mount " + below);
        if (b.maxY > 1.0 || b.minX != 1.0 / 16 || b.maxX != 15.0 / 16 || b.minZ != 1.0 / 16 || b.maxZ != 15.0 / 16) problems.add("under plate " + b);
        for (Direction d : InterfaceBlock.localSides(Direction.UP)) {
            if (below.getValue(edgeOf(localName(Direction.UP, d)))) problems.add("edge " + d + " under a floating chest");
        }
        // on top of a chest: refused; on top of a barrel: fine
        if (InterfaceBlock.placeableOn(Blocks.CHEST.defaultBlockState(), Direction.DOWN)) problems.add("placeable on top of a chest");
        if (!InterfaceBlock.placeableOn(Blocks.BARREL.defaultBlockState(), Direction.DOWN)) problems.add("not placeable on top of a barrel");
        if (!InterfaceBlock.placeableOn(Blocks.CHEST.defaultBlockState(), Direction.UP)) problems.add("not placeable under a chest");
        // a double chest: the plate under it reaches across the seam
        BlockPos dl = new BlockPos(4, 4, 5), dr = dl.east();
        BlockState left = Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.SOUTH)
                .setValue(net.minecraft.world.level.block.ChestBlock.TYPE, net.minecraft.world.level.block.state.properties.ChestType.LEFT);
        Direction seam = net.minecraft.world.level.block.ChestBlock.getConnectedDirection(left);
        h.setBlock(dl, left);
        h.setBlock(dl.relative(seam), left.setValue(net.minecraft.world.level.block.ChestBlock.TYPE, net.minecraft.world.level.block.state.properties.ChestType.RIGHT));
        BlockState doubled = placedInterface(h, dl.below(), Direction.UP);
        if (!doubled.getValue(edgeOf(localName(Direction.UP, seam)))) problems.add("no edge toward the other half (" + seam + "): " + doubled);
        // config off: a wall beside the plate does not stretch it
        boolean was = StorageConfig.interfaceFillsGaps;
        try {
            StorageConfig.interfaceFillsGaps = false;
            h.setBlock(back.east(), Blocks.STONE);
            if (placedInterface(h, back, Direction.NORTH).getValue(InterfaceBlock.EDGE_E)) problems.add("edge with interfaceFillsGaps off");
            StorageConfig.interfaceFillsGaps = true;
            if (!placedInterface(h, back, Direction.NORTH).getValue(InterfaceBlock.EDGE_E)) problems.add("no edge with interfaceFillsGaps on");
        } finally {
            StorageConfig.interfaceFillsGaps = was;
        }
        h.assertTrue(problems.isEmpty(), "interface on chests: " + problems);
        h.succeed();
    }

    /**
     * DW04C-1: under a chest with walls on two neighbouring sides both edge strips grow, and the 1 px corner between them is filled
     * too (no see-through hole); with one wall only there is no corner.
     */
    public static void interfaceFillsTheCorner(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        boolean was = StorageConfig.interfaceFillsGaps;
        try {
            StorageConfig.interfaceFillsGaps = true;
            BlockPos under = new BlockPos(3, 2, 3);
            h.setBlock(under.above(), Blocks.CHEST);
            h.setBlock(under.east(), Blocks.STONE);
            h.setBlock(under.south(), Blocks.STONE);
            // facing up: local E = east, U = south; the EU corner is x 15..16, z 15..16 at the top of the cell
            VoxelShape corner = Block.box(15, 13, 15, 16, 16, 16);
            BlockState both = placedInterface(h, under, Direction.UP);
            if (!both.getValue(InterfaceBlock.EDGE_E) || !both.getValue(InterfaceBlock.EDGE_U)) problems.add("edges not grown: " + both);
            if (!Shapes.joinIsNotEmpty(both.getShape(h.getLevel(), h.absolutePos(under)), corner, BooleanOp.AND)) problems.add("corner open with both edges");
            if (Shapes.joinIsNotEmpty(both.getShape(h.getLevel(), h.absolutePos(under)), Block.box(0, 13, 0, 1, 16, 1), BooleanOp.AND)) problems.add("corner without edges");
            h.setBlock(under.south(), Blocks.AIR);
            BlockState one = placedInterface(h, under, Direction.UP);
            if (Shapes.joinIsNotEmpty(one.getShape(h.getLevel(), h.absolutePos(under)), corner, BooleanOp.AND)) problems.add("corner with one edge only");
        } finally {
            StorageConfig.interfaceFillsGaps = was;
        }
        h.assertTrue(problems.isEmpty(), "interface corner: " + problems);
        h.succeed();
    }

    /** DW03C-2/4: on a barrel (Vanilla and this module's own copper barrel, WH26-6: via c:barrels) the plate is flush, no edges. */
    public static void interfaceSitsFlushOnBarrels(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        BlockPos at = new BlockPos(3, 2, 3);
        h.setBlock(at.north(), Blocks.BARREL);
        h.setBlock(at.east(), Blocks.STONE);
        BlockState flush = placedInterface(h, at, Direction.NORTH);
        AABB b = bounds(h, at);
        if (flush.getValue(InterfaceBlock.MOUNT) != InterfaceBlock.Mount.FULL) problems.add("barrel mount " + flush);
        if (b.minX != 0.0 || b.maxX != 1.0 || b.minY != 0.0 || b.maxY != 1.0) problems.add("not flush with the barrel: " + b);
        if (flush.getValue(InterfaceBlock.EDGE_E)) problems.add("edge strip on a flush plate");
        var modBarrel = net.minecraft.resources.Identifier.fromNamespaceAndPath("simplestorage", "copper_barrel");
        if (BuiltInRegistries.BLOCK.containsKey(modBarrel)) {
            Block barrel = BuiltInRegistries.BLOCK.getValue(modBarrel);
            if (!ControllerLevel.mountable(barrel)) problems.add("the copper barrel is not mountable");
            BlockPos mod = new BlockPos(3, 2, 6);
            h.setBlock(mod.north(), barrel);
            if (placedInterface(h, mod, Direction.NORTH).getValue(InterfaceBlock.MOUNT) != InterfaceBlock.Mount.FULL) problems.add("copper barrel mount");
        } else {
            problems.add("the copper barrel is not registered");
        }
        h.assertTrue(problems.isEmpty(), "interface on barrels: " + problems);
        h.succeed();
    }

    /** The plate's local name (E/W/U/D as for a north plate) of the absolute side {@code side} for a plate on {@code facing}. */
    private static Direction localName(Direction facing, Direction side) {
        Direction[] sides = InterfaceBlock.localSides(facing);
        Direction[] names = {Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN};
        for (int i = 0; i < sides.length; i++) {
            if (sides[i] == side) return names[i];
        }
        throw new IllegalArgumentException(side + " is no edge of a plate on " + facing);
    }

    private static net.minecraft.world.level.block.state.properties.BooleanProperty edgeOf(Direction side) {
        return switch (side) {
            case EAST -> InterfaceBlock.EDGE_E;
            case WEST -> InterfaceBlock.EDGE_W;
            case UP -> InterfaceBlock.EDGE_U;
            default -> InterfaceBlock.EDGE_D;
        };
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
            if (h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, SimpleStorage.id(id))).isEmpty()
                    == (!id.contains("enderite") || com.simplelib.api.SimpleLibApi.materialPresent("enderite"))) problems.add("recipe state " + id); // MACH32-ENDSYS: Enderite recipes only with Simple Ores
        }
        if (StorageMenus.CONTROLLER == null) problems.add("controller menu type");
        if (ControllerLevel.rankOf(Blocks.CHEST) != 0 || ControllerLevel.rankOf(block("reinforced_chest")) != 1
                || ControllerLevel.rankOf(block("enderite_chest")) != 3 || ControllerLevel.rankOf(Blocks.BARREL) != 0 || ControllerLevel.rankOf(Blocks.SHULKER_BOX) != 0
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

    /** The chest the network fills second (members order), so a test can show the same-item rule beats the order. */
    private static Container secondChest(GameTestHelper h, ControllerChestBlockEntity controller) {
        return (Container) h.getLevel().getBlockEntity(ChestNetwork.members(h.getLevel(), 0, controller.scan()).chests().get(1));
    }

    private static long networkCount(GameTestHelper h, ControllerChestBlockEntity controller, net.minecraft.world.item.Item item) {
        long n = 0;
        for (BlockPos pos : ChestNetwork.members(h.getLevel(), 0, controller.scan()).chests()) n += ((Container) h.getLevel().getBlockEntity(pos)).countItem(item);
        return n;
    }

    /**
     * DW04C-2: the controller has no inventory of its own. Taking stone and putting it back (click, right click, shift-click in the
     * inventory) puts it into a chest of the network - the one that already holds stone, even when its stack there is full and it
     * is the later chest - and never into the controller.
     */
    public static void storePrefersTheChestWithTheSameItem(GameTestHelper h) {
        ControllerChestBlockEntity controller = star(h, "wooden", Blocks.CHEST, Blocks.CHEST);
        Container second = secondChest(h, controller);
        second.setItem(0, new ItemStack(Items.STONE, 64));
        second.setItem(1, new ItemStack(Items.STONE, 10));
        second.setItem(5, new ItemStack(Items.GOLD_INGOT, 1));
        ServerPlayer player = player(h);
        ControllerMenu menu = ControllerMenu.server(1, player.getInventory(), controller);
        List<String> problems = new ArrayList<>();
        // take a full stack (10 stone stay) and put it back one, then the rest: all of it goes where the stone is, past a free
        // first chest and the controller's slots
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        if (!menu.getCarried().is(Items.STONE) || menu.getCarried().getCount() != 64) problems.add("took " + menu.getCarried());
        menu.clicked(3, 1, ContainerInput.PICKUP, player);
        menu.clicked(3, 0, ContainerInput.PICKUP, player);
        if (!menu.getCarried().isEmpty()) problems.add("carried after storing " + menu.getCarried());
        if (second.countItem(Items.STONE) != 74) problems.add("the stone chest holds " + second.countItem(Items.STONE) + " stone, not 74");
        player.getInventory().setItem(9, new ItemStack(Items.GOLD_INGOT, 4));
        menu.quickMoveStack(player, ControllerMenu.CELLS);
        if (second.countItem(Items.GOLD_INGOT) != 5) problems.add("shift-clicked gold went elsewhere (" + second.countItem(Items.GOLD_INGOT) + " in the gold chest)");
        // a new kind goes to the network in its order
        menu.setCarried(new ItemStack(Items.DIRT, 3));
        menu.clicked(3, 0, ContainerInput.PICKUP, player);
        if (networkCount(h, controller, Items.DIRT) != 3) problems.add("new kind: " + networkCount(h, controller, Items.DIRT) + " dirt in the chests");
        if (!controller.isEmpty()) problems.add("the controller holds items");
        h.assertTrue(problems.isEmpty(), "storing through the controller: " + problems);
        h.succeed();
    }

    /** DW04C-2: with every chest full, a stored item stays on the cursor or in the inventory - it neither vanishes nor goes into the controller. */
    public static void fullNetworkKeepsTheItemWithThePlayer(GameTestHelper h) {
        ControllerChestBlockEntity controller = star(h, "wooden", Blocks.CHEST, Blocks.CHEST);
        for (BlockPos pos : List.of(UP_CHEST, SOUTH_CHEST)) {
            Container chest = container(h, pos);
            for (int i = 0; i < chest.getContainerSize(); i++) chest.setItem(i, new ItemStack(Items.DIRT, 64));
        }
        ServerPlayer player = player(h);
        ControllerMenu menu = ControllerMenu.server(1, player.getInventory(), controller);
        List<String> problems = new ArrayList<>();
        menu.setCarried(new ItemStack(Items.STONE, 5));
        menu.clicked(3, 0, ContainerInput.PICKUP, player);
        if (!menu.getCarried().is(Items.STONE) || menu.getCarried().getCount() != 5) problems.add("cursor after a full network " + menu.getCarried());
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 7));
        menu.quickMoveStack(player, ControllerMenu.CELLS);
        if (player.getInventory().countItem(Items.STONE) != 7) problems.add("inventory holds " + player.getInventory().countItem(Items.STONE) + " stone, not 7");
        HopperIntake intake = intake(h);
        if (intake.rest(new ItemStack(Items.STONE, 1)).getCount() != 1) problems.add("a hopper got stone into a full network");
        if (!controller.isEmpty()) problems.add("the controller holds items");
        h.assertTrue(problems.isEmpty(), "full network: " + problems);
        h.succeed();
    }

    /** What a hopper or pipe above the controller sees (both use {@code HopperBlockEntity.getContainerAt} and {@code addItem}). */
    private record HopperIntake(Container container) {
        ItemStack rest(ItemStack stack) {
            return net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(null, this.container, stack, Direction.DOWN);
        }
    }

    private static HopperIntake intake(GameTestHelper h) {
        return new HopperIntake(net.minecraft.world.level.block.entity.HopperBlockEntity.getContainerAt(h.getLevel(), h.absolutePos(CONTROLLER)));
    }

    /** DW04C-2: a hopper or pipe feeding the controller puts the item into the network (the chest with the same item first). */
    public static void controllerIntakeFeedsTheNetwork(GameTestHelper h) {
        ControllerChestBlockEntity controller = star(h, "wooden", Blocks.CHEST, Blocks.CHEST);
        Container second = secondChest(h, controller);
        second.setItem(2, new ItemStack(Items.IRON_INGOT, 1));
        List<String> problems = new ArrayList<>();
        HopperIntake intake = intake(h);
        if (intake.container() == null || intake.container() == controller) problems.add("hoppers see the controller's own slots: " + intake.container());
        else {
            ItemStack rest = intake.rest(new ItemStack(Items.IRON_INGOT, 1));
            if (!rest.isEmpty() || second.countItem(Items.IRON_INGOT) != 2) problems.add("hopper: rest " + rest + ", " + second.countItem(Items.IRON_INGOT) + " iron in its chest");
            rest = intake.rest(new ItemStack(Items.COBBLESTONE, 8));
            if (!rest.isEmpty() || networkCount(h, controller, Items.COBBLESTONE) != 8) problems.add("pipe stack: rest " + rest);
            if (net.minecraft.world.level.block.entity.HopperBlockEntity.getContainerAt(h.getLevel(), h.absolutePos(CONTROLLER)).getItem(0).getCount() != 0) problems.add("intake shows an item");
        }
        if (controller.canPlaceItem(0, new ItemStack(Items.STONE))) problems.add("the controller's own slots take items");
        if (!controller.isEmpty()) problems.add("the controller holds items");
        h.assertTrue(problems.isEmpty(), "controller intake: " + problems);
        h.succeed();
    }

    /**
     * DW04C-2, old worlds: items an older version kept in the controller move into the chests when the window opens; what does not
     * fit stays in the controller (listed in the window, never dropped or lost).
     */
    public static void oldControllerItemsMoveIntoTheNetwork(GameTestHelper h) {
        ControllerChestBlockEntity controller = star(h, "wooden", Blocks.CHEST, Blocks.CHEST);
        controller.setItem(0, new ItemStack(Items.STONE, 7));
        controller.setItem(4, new ItemStack(Items.DIRT, 3));
        ServerPlayer player = player(h);
        ControllerMenu.server(1, player.getInventory(), controller);
        List<String> problems = new ArrayList<>();
        if (!controller.isEmpty()) problems.add("controller still holds items");
        if (networkCount(h, controller, Items.STONE) != 7 || networkCount(h, controller, Items.DIRT) != 3) problems.add("chests hold " + networkCount(h, controller, Items.STONE) + " stone, " + networkCount(h, controller, Items.DIRT) + " dirt");
        // a full network: the old item stays in the controller and is still listed
        for (BlockPos pos : List.of(UP_CHEST, SOUTH_CHEST)) {
            Container chest = container(h, pos);
            for (int i = 0; i < chest.getContainerSize(); i++) chest.setItem(i, new ItemStack(Items.GOLD_BLOCK, 64));
        }
        controller.setItem(0, new ItemStack(Items.EMERALD, 5));
        ControllerMenu menu = ControllerMenu.server(2, player.getInventory(), controller);
        if (controller.countItem(Items.EMERALD) != 5) problems.add("overflow: controller keeps " + controller.countItem(Items.EMERALD) + " emeralds, not 5");
        boolean listed = false;
        for (int i = 0; i < ControllerMenu.CELLS; i++) listed |= menu.getSlot(i).getItem().is(Items.EMERALD) && menu.countAt(i) == 5;
        if (!listed) problems.add("old emeralds not listed in the window");
        h.assertTrue(problems.isEmpty(), "old controller contents: " + problems);
        h.succeed();
    }

    /**
     * IG31-37: barrels and shulker boxes (vanilla and tiered) take an interface like a chest, the terminal counts their contents,
     * take and store work through the Container interface, and no shulker box goes into a shulker box.
     */
    public static void barrelsAndShulkerBoxesJoinTheNetwork(GameTestHelper h) {
        ControllerChestBlockEntity controller = star(h, "wooden", Blocks.BARREL, Blocks.SHULKER_BOX);
        List<String> problems = new ArrayList<>();
        ServerPlayer player = player(h);
        if (!h.getBlockState(HUB.above()).canSurvive(h.getLevel(), h.absolutePos(HUB.above())) || h.getBlockState(HUB.above()).isAir()) problems.add("interface on a barrel");
        if (h.getBlockState(HUB.south()).isAir()) problems.add("interface on a shulker box");
        container(h, UP_CHEST).setItem(0, new ItemStack(Items.STONE, 3));
        container(h, SOUTH_CHEST).setItem(0, new ItemStack(Items.STONE, 5));
        ChestNetwork.Members members = ChestNetwork.members(h.getLevel(), 0, controller.scan());
        if (members.chests().size() != 2) problems.add("members " + members);
        ChestNetwork.Storage storage = new ChestNetwork.Storage(h.getLevel(), controller, members, player);
        long stone = 0;
        for (var entry : storage.entries(ChestNetwork.Sort.COUNT)) if (entry.kind().is(Items.STONE)) stone += entry.count();
        if (stone != 8) problems.add("terminal counts " + stone + " stone");
        ItemStack taken = storage.take(new ItemStack(Items.STONE), 64);
        if (taken.getCount() != 8 || container(h, SOUTH_CHEST).countItem(Items.STONE) != 0) problems.add("take got " + taken);
        // fill the controller and the barrel: only the shulker box has room left
        for (int i = 0; i < controller.getContainerSize(); i++) controller.setItem(i, new ItemStack(Items.DIRT, 1));
        Container barrel = container(h, UP_CHEST);
        for (int i = 0; i < barrel.getContainerSize(); i++) barrel.setItem(i, new ItemStack(Items.DIRT, 1));
        ItemStack shulkerItem = new ItemStack(Items.SHULKER_BOX);
        if (new ChestNetwork.Storage(h.getLevel(), controller, members, player).insert(shulkerItem).getCount() != 1) problems.add("a shulker box went into a shulker box");
        ItemStack rest = new ChestNetwork.Storage(h.getLevel(), controller, members, player).insert(new ItemStack(Items.GOLD_INGOT, 5));
        if (!rest.isEmpty() || container(h, SOUTH_CHEST).countItem(Items.GOLD_INGOT) != 5) problems.add("store into the shulker box: rest " + rest);
        // a tiered shulker box behind an interface, a double-rank controller takes it
        h.setBlock(SOUTH_CHEST, block("reinforced_shulker_box"));
        if (ControllerLevel.rankOf(block("reinforced_shulker_box")) != 1 || ControllerLevel.rankOf(Blocks.SHULKER_BOX) != 0
                || ControllerLevel.rankOf(Blocks.BARREL) != 0) problems.add("ranks of barrel/shulker boxes");
        if (ChestNetwork.members(h.getLevel(), 0, controller.scan()).rejected() != 1) problems.add("a reinforced shulker box is above a wooden controller");
        if (ChestNetwork.members(h.getLevel(), 1, controller.scan()).chests().size() != 2) problems.add("a reinforced controller takes it");
        Container tiered = container(h, SOUTH_CHEST);
        if (tiered.canPlaceItem(0, shulkerItem)) problems.add("tiered shulker box accepts a shulker box");
        h.assertTrue(problems.isEmpty(), "barrels and shulker boxes: " + problems);
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

    /** The compact totals of the list cells. */
    public static void compactCountsAreShort(GameTestHelper h) {
        Object[][] cases = {{0L, "0"}, {7L, "7"}, {999L, "999"}, {1000L, "1k"}, {1234L, "1.2k"}, {9999L, "9.9k"}, {10_000L, "10k"}, {32_000L, "32k"},
                {32_999L, "32k"}, {999_999L, "999k"}, {1_000_000L, "1M"}, {5_400_000L, "5.4M"}, {32_000_000L, "32M"}, {1_073_741_823L, "1B"}};
        List<String> problems = new ArrayList<>();
        for (Object[] c : cases) {
            String got = ChestNetwork.shortCount((Long) c[0]);
            if (!got.equals(c[1])) problems.add(c[0] + " -> " + got + " expected " + c[1]);
        }
        h.assertTrue(problems.isEmpty(), "compact counts: " + problems);
        h.succeed();
    }

    /** What a click takes: a whole stack (left), half a stack rounded up (right), at most what the network has. */
    public static void takeAmountsAreStacksAndHalves(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        int[][] cases = {{64, 100, 0, 64}, {64, 100, 1, 32}, {16, 100, 0, 16}, {16, 100, 1, 8}, {1, 5, 0, 1}, {1, 5, 1, 1}, {64, 10, 0, 10}, {64, 10, 1, 10}, {64, 3, 1, 3}};
        net.minecraft.world.item.Item[] items = {Items.STONE, Items.STONE, Items.ENDER_PEARL, Items.ENDER_PEARL, Items.DIAMOND_PICKAXE, Items.DIAMOND_PICKAXE, Items.STONE, Items.STONE, Items.STONE};
        for (int i = 0; i < cases.length; i++) {
            int got = ChestNetwork.takeAmount(new ItemStack(items[i]), cases[i][2] == 1, cases[i][1]);
            if (got != cases[i][3]) problems.add(items[i] + " have " + cases[i][1] + (cases[i][2] == 1 ? " half" : " full") + " -> " + got + " expected " + cases[i][3]);
        }
        h.assertTrue(problems.isEmpty(), "take amounts: " + problems);
        h.succeed();
    }

    /** The search: every word must match the id (underscores as spaces) or the name, @mod matches the mod id, an empty search keeps all. */
    public static void searchFiltersByWordsAndMod(GameTestHelper h) {
        List<ChestNetwork.Entry> all = new ArrayList<>();
        for (net.minecraft.world.item.Item item : List.of(Items.STONE, Items.OAK_PLANKS, Items.DIAMOND_PICKAXE, Items.GOLD_INGOT)) all.add(new ChestNetwork.Entry(new ItemStack(item), 5));
        String[][] cases = {{"", "4"}, {"   ", "4"}, {"oak", "1"}, {"OAK PLANK", "1"}, {"pickaxe diamond", "1"}, {"gold ingot", "1"}, {"ingot", "1"}, {"@minecraft", "4"},
                {"@minecraft pickaxe", "1"}, {"@simplelib", "0"}, {"zzz", "0"}, {"oak zzz", "0"}};
        List<String> problems = new ArrayList<>();
        for (String[] c : cases) {
            int got = ChestNetwork.filter(all, c[0]).size();
            if (got != Integer.parseInt(c[1])) problems.add("'" + c[0] + "' -> " + got + " expected " + c[1]);
        }
        h.assertTrue(problems.isEmpty(), "search: " + problems);
        h.succeed();
    }

    /** The window as a player drives it: search by menu clicks, backspace, clear, scrollbar jumps, half and full stacks. */
    public static void windowSearchScrollAndStackTakes(GameTestHelper h) {
        ControllerChestBlockEntity controller = star(h, "wooden", Blocks.CHEST, Blocks.CHEST);
        container(h, UP_CHEST).setItem(0, new ItemStack(Items.STONE, 64));
        container(h, UP_CHEST).setItem(1, new ItemStack(Items.STONE, 36));
        container(h, SOUTH_CHEST).setItem(0, new ItemStack(Items.GOLD_INGOT, 3));
        ServerPlayer player = player(h);
        ControllerMenu menu = ControllerMenu.server(1, player.getInventory(), controller);
        List<String> problems = new ArrayList<>();
        // 100 stone: left click a full stack, right click half
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        if (menu.getCarried().getCount() != 64) problems.add("left click took " + menu.getCarried());
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 1, ContainerInput.PICKUP, player);
        if (menu.getCarried().getCount() != 32) problems.add("right click took " + menu.getCarried());
        menu.setCarried(ItemStack.EMPTY);
        // search "gold": one kind, by one click per character
        for (char c : "gold".toCharArray()) menu.clickMenuButton(player, ControllerMenu.CHAR + c);
        if (!menu.search().equals("gold") || menu.kinds() != 1 || !menu.getSlot(0).getItem().is(Items.GOLD_INGOT)) problems.add("search gold: '" + menu.search() + "' kinds " + menu.kinds());
        menu.clickMenuButton(player, ControllerMenu.BUTTON_SEARCH_BACK);
        menu.clickMenuButton(player, ControllerMenu.BUTTON_SEARCH_BACK);
        menu.clickMenuButton(player, ControllerMenu.BUTTON_SEARCH_BACK);
        menu.clickMenuButton(player, ControllerMenu.BUTTON_SEARCH_BACK);
        if (!menu.search().isEmpty() || menu.kinds() != 2) problems.add("after backspaces: '" + menu.search() + "' kinds " + menu.kinds());
        menu.clickMenuButton(player, ControllerMenu.CHAR + 'x');
        if (menu.kinds() != 0) problems.add("search x kinds " + menu.kinds());
        menu.clickMenuButton(player, ControllerMenu.BUTTON_SEARCH_CLEAR);
        if (!menu.search().isEmpty() || menu.kinds() != 2) problems.add("after clear kinds " + menu.kinds());
        // a click on a hidden kind takes nothing from an empty cell
        menu.clickMenuButton(player, ControllerMenu.CHAR + 'q');
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        if (!menu.getCarried().isEmpty()) problems.add("an empty cell gave " + menu.getCarried());
        menu.clickMenuButton(player, ControllerMenu.BUTTON_SEARCH_CLEAR);
        // the scrollbar: more kinds than the 45 cells, the jump clamps to the last row
        int placed = 0;
        for (net.minecraft.world.item.Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR || item == Items.STONE || item == Items.GOLD_INGOT || placed >= 60) continue;
            if (placed < 36) controller.setItem(placed, new ItemStack(item));
            else container(h, SOUTH_CHEST).setItem(placed - 36 + 1, new ItemStack(item));
            placed++;
        }
        menu.clickMenuButton(player, ControllerMenu.BUTTON_SORT);
        menu.refresh();
        if (menu.maxScroll() < 1) problems.add("no scroll range for " + menu.kinds() + " kinds");
        menu.clickMenuButton(player, ControllerMenu.SCROLL_MAX);
        if (menu.scrollRow() != menu.maxScroll()) problems.add("a jump past the end gave row " + menu.scrollRow() + " of " + menu.maxScroll());
        menu.clickMenuButton(player, ControllerMenu.SCROLL_TO);
        if (menu.scrollRow() != 0) problems.add("a jump to row 0 gave " + menu.scrollRow());
        if (menu.clickMenuButton(player, 50)) problems.add("button 50 must be unknown");
        h.assertTrue(problems.isEmpty(), "window search/scroll: " + problems);
        h.succeed();
    }
}
