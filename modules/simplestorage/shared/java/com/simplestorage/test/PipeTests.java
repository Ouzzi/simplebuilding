package com.simplestorage.test;

import com.simplestorage.pipe.ItemPipeBlock;
import com.simplestorage.pipe.ItemPipeBlockEntity;
import com.simplestorage.pipe.PipeMenu;
import com.simplestorage.pipe.PipeTier;
import com.simplestorage.pipe.Pipes;
import com.simplestorage.registry.StorageBlockEntities;
import com.simplestorage.registry.StorageBlocks;
import com.simplestorage.registry.StorageItems;
import com.simplestorage.registry.StorageMenus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Module tests of the pipe (ST27-1). The numbers are this file's own constants, not read from the mod. Coordinates stay inside
 * the 8 x 8 x 8 test structure.
 */
public final class PipeTests {
    /** The transport tests wait for a few transfers of the slow first step. */
    public static final int MOVE_MAX_TICKS = 200;
    private static final BlockPos PIPE = new BlockPos(2, 2, 2);

    private PipeTests() {
    }

    private static Block pipe(String id) {
        return Refs.block(id);
    }

    private static BlockState pipeState(String id, Direction facing) {
        BlockState state = pipe(id).defaultBlockState().setValue(ItemPipeBlock.FACING, facing);
        for (Direction d : Direction.values()) state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d), d == facing);
        return state;
    }

    /** Places a pipe the way an item does: the arms follow the neighbours that are there already. */
    private static void place(GameTestHelper h, BlockPos pos, String id, Direction facing) {
        h.setBlock(pos, ItemPipeBlock.withArms(pipeState(id, facing), h.getLevel(), h.absolutePos(pos)));
    }

    private static ItemPipeBlockEntity entity(GameTestHelper h, BlockPos pos) {
        return h.getBlockEntity(pos, ItemPipeBlockEntity.class);
    }

    private static Container container(GameTestHelper h, BlockPos pos) {
        return (Container) h.getBlockEntity(pos, net.minecraft.world.level.block.entity.BlockEntity.class);
    }

    /** Four pipes with their block, item, block entity type and menus; the tier numbers. */
    public static void registered(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        int[][] numbers = {{8, 1, 0}, {6, 1, 1}, {4, 2, 1}, {2, 4, 1}};
        String[] ids = {"pipe", "reinforced_pipe", "netherite_pipe", "enderite_pipe"};
        for (int i = 0; i < ids.length; i++) {
            if (!(pipe(ids[i]) instanceof ItemPipeBlock block)) {
                problems.add("no pipe block " + ids[i]);
                continue;
            }
            PipeTier tier = PipeTier.values()[i];
            if (block.tier() != tier) problems.add(ids[i] + " tier " + block.tier());
            if (tier.cooldown() != numbers[i][0] || tier.amount() != numbers[i][1] || tier.canPointUp() != (numbers[i][2] == 1)) {
                problems.add(ids[i] + " numbers " + tier.cooldown() + "/" + tier.amount() + "/" + tier.canPointUp());
            }
            if (!BuiltInRegistries.ITEM.containsKey(com.simplestorage.SimpleStorage.id(ids[i]))) problems.add("no item " + ids[i]);
            if (StorageItems.PIPES.get(tier) == null || StorageBlocks.PIPES.get(tier) != block) problems.add("registry map " + ids[i]);
            BlockState state = block.defaultBlockState();
            if (!state.hasProperty(ItemPipeBlock.FACING) || !state.getValue(PipeBlock.DOWN)) problems.add(ids[i] + " default state " + state);
        }
        if (StorageBlockEntities.PIPE == null || !StorageBlockEntities.PIPE.isValid(pipe("pipe").defaultBlockState())
                || !StorageBlockEntities.PIPE.isValid(pipe("enderite_pipe").defaultBlockState())) problems.add("pipe block entity type");
        if (StorageMenus.PIPE == null) problems.add("pipe menu type");
        var manager = h.getLevel().getServer().getRecipeManager();
        for (String recipe : ids) {
            if (manager.byKey(ResourceKey.create(Registries.RECIPE, com.simplestorage.SimpleStorage.id(recipe))).isEmpty()) problems.add("recipe " + recipe);
        }
        h.assertTrue(problems.isEmpty(), "pipes: " + problems);
        h.succeed();
    }

    /** One transfer step per tier: 1, 1, 2 and 4 items from a full stack into the chest below. */
    public static void transferStepFollowsTheTier(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        int[] expected = {1, 1, 2, 4};
        String[] ids = {"pipe", "reinforced_pipe", "netherite_pipe", "enderite_pipe"};
        for (int i = 0; i < ids.length; i++) {
            BlockPos pos = new BlockPos(1 + i, 3, 1);
            h.setBlock(pos.below(), Blocks.CHEST);
            place(h, pos, ids[i], Direction.DOWN);
            ItemPipeBlockEntity pipe = entity(h, pos);
            pipe.setItem(0, new ItemStack(Items.STONE, 20));
            boolean moved = pipe.push(h.getLevel(), h.absolutePos(pos));
            int got = container(h, pos.below()).countItem(Items.STONE);
            if (!moved || got != expected[i]) problems.add(ids[i] + " moved " + got + " expected " + expected[i]);
            if (pipe.getItem(0).getCount() != 20 - expected[i]) problems.add(ids[i] + " left " + pipe.getItem(0).getCount());
        }
        h.assertTrue(problems.isEmpty(), "transfer step: " + problems);
        h.succeed();
    }

    /** The pipe carries its items down into the chest at its output, one every eight ticks, all of them in the end. */
    public static void carriesItemsToTheOutputContainer(GameTestHelper h) {
        h.setBlock(PIPE.below(), Blocks.CHEST);
        place(h, PIPE, "pipe", Direction.DOWN);
        entity(h, PIPE).setItem(0, new ItemStack(Items.STONE, 3));
        h.succeedWhen(() -> {
            int got = container(h, PIPE.below()).countItem(Items.STONE);
            h.assertTrue(got == 3 && entity(h, PIPE).isEmpty(), "the chest holds " + got + " of 3 stone, the pipe " + entity(h, PIPE).getItem(0).getCount());
        });
    }

    /** It pulls from the container beside it (any side but the output) and pushes to the output; the output container is not pulled from. */
    public static void pullsFromBesideAndPushesToTheOutput(GameTestHelper h) {
        BlockPos west = PIPE.west();
        BlockPos east = PIPE.east();
        h.setBlock(west, Blocks.CHEST);
        h.setBlock(east, Blocks.CHEST);
        container(h, west).setItem(0, new ItemStack(Items.COBBLESTONE, 3));
        container(h, east).setItem(0, new ItemStack(Items.DIRT, 2));
        place(h, PIPE, "pipe", Direction.EAST);
        BlockState state = h.getBlockState(PIPE);
        h.assertTrue(state.getValue(PipeBlock.WEST) && state.getValue(PipeBlock.EAST) && !state.getValue(PipeBlock.NORTH),
                "arms west (input) and east (output) expected, got " + state);
        h.succeedWhen(() -> {
            int moved = container(h, east).countItem(Items.COBBLESTONE);
            h.assertTrue(moved == 3 && container(h, west).countItem(Items.COBBLESTONE) == 0, "cobblestone moved " + moved + " of 3");
            h.assertTrue(container(h, east).countItem(Items.DIRT) == 2, "the output chest's own dirt must stay (" + container(h, east).countItem(Items.DIRT) + ")");
        });
    }

    /** The output turns through the allowed sides: the first step never up, Reinforced and better do; the arms follow. */
    public static void outputTurnsAndTheFirstStepNeverPointsUp(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        List<Direction> basic = new ArrayList<>();
        Direction d = Direction.DOWN;
        for (int i = 0; i < 6; i++) {
            d = Pipes.next(PipeTier.BASIC, d);
            basic.add(d);
        }
        // down -> north -> east -> south -> west -> (up skipped) down -> north
        if (basic.contains(Direction.UP) || basic.get(3) != Direction.WEST || basic.get(4) != Direction.DOWN || basic.get(5) != Direction.NORTH) problems.add("basic order " + basic);
        List<Direction> reinforced = new ArrayList<>();
        d = Direction.DOWN;
        for (int i = 0; i < 6; i++) {
            d = Pipes.next(PipeTier.REINFORCED, d);
            reinforced.add(d);
        }
        if (!reinforced.contains(Direction.UP) || reinforced.get(4) != Direction.UP || reinforced.get(5) != Direction.DOWN) problems.add("reinforced order " + reinforced);
        // The real block: turning moves the output arm.
        place(h, PIPE, "pipe", Direction.DOWN);
        ItemPipeBlock.turnOutput(h.getLevel(), h.absolutePos(PIPE), h.getBlockState(PIPE), null);
        BlockState turned = h.getBlockState(PIPE);
        if (turned.getValue(ItemPipeBlock.FACING) != Direction.NORTH || !turned.getValue(PipeBlock.NORTH) || turned.getValue(PipeBlock.DOWN)) problems.add("turned state " + turned);
        h.assertTrue(problems.isEmpty(), "output turning: " + problems);
        h.succeed();
    }

    /** A neighbour with items grows an arm, a solid block does not; a placed pipe points into the clicked side. */
    public static void connectionsGrowArms(GameTestHelper h) {
        h.setBlock(PIPE.north(), Blocks.CHEST);
        place(h, PIPE.south(), "pipe", Direction.DOWN);
        h.setBlock(PIPE.east(), Blocks.STONE);
        place(h, PIPE, "pipe", Direction.DOWN);
        BlockState state = ItemPipeBlock.withArms(h.getBlockState(PIPE), h.getLevel(), h.absolutePos(PIPE));
        List<String> problems = new ArrayList<>();
        if (!state.getValue(PipeBlock.NORTH)) problems.add("no arm to the chest");
        if (!state.getValue(PipeBlock.SOUTH)) problems.add("no arm to the other pipe");
        if (state.getValue(PipeBlock.EAST)) problems.add("arm to solid stone");
        if (!state.getValue(PipeBlock.DOWN)) problems.add("the output is always an arm");
        // after a neighbour changes, the placed block follows by itself
        h.setBlock(PIPE.west(), Blocks.FURNACE);
        if (!h.getBlockState(PIPE).getValue(PipeBlock.WEST)) problems.add("the furnace placed later did not grow an arm");
        h.assertTrue(problems.isEmpty(), "arms: " + problems);
        h.succeed();
    }

    /** Filter principle: one real item stays, automation fills only matches and never an empty filter slot, a player may fill it. */
    public static void filterKeepsOneItemAndFollowsThePrinciple(GameTestHelper h) {
        h.setBlock(PIPE.below(), Blocks.CHEST);
        place(h, PIPE, "reinforced_pipe", Direction.DOWN);
        ItemPipeBlockEntity pipe = entity(h, PIPE);
        pipe.setItem(0, new ItemStack(Items.STONE, 5));
        pipe.cycleFilterMode(); // exact
        List<String> problems = new ArrayList<>();
        if (pipe.filterMode() != 1) problems.add("mode after one press " + pipe.filterMode());
        if (pipe.canPlaceItem(0, new ItemStack(Items.DIRT))) problems.add("automation added dirt to a stone filter");
        if (!pipe.canPlaceItem(0, new ItemStack(Items.STONE))) problems.add("automation refused a match");
        if (!pipe.canTakeItemThroughFace(0, new ItemStack(Items.STONE), Direction.DOWN)) problems.add("the second and further items may be taken");
        ItemStack renamed = new ItemStack(Items.STONE);
        renamed.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("x"));
        if (pipe.canPlaceItem(0, renamed)) problems.add("exact mode took a renamed stone");
        pipe.cycleFilterMode(); // same kind
        if (!pipe.canPlaceItem(0, renamed)) problems.add("same kind mode refused a renamed stone");
        pipe.cycleFilterMode(); // off
        if (pipe.filterMode() != 0 || !pipe.canPlaceItem(0, new ItemStack(Items.DIRT))) problems.add("off mode must take anything");
        pipe.cycleFilterMode(); // exact again
        if (!pipe.mayPlayerPlace(new ItemStack(Items.STONE)) || pipe.mayPlayerPlace(new ItemStack(Items.DIRT))) problems.add("a player may only add a match to a filled slot");
        if (!problems.isEmpty()) {
            h.fail("filter: " + problems);
            return;
        }
        h.succeedWhen(() -> {
            h.assertTrue(pipe.getItem(0).getCount() == 1 && pipe.getItem(0).is(Items.STONE), "the filter item must stay (" + pipe.getItem(0) + ")");
            int got = container(h, PIPE.below()).countItem(Items.STONE);
            h.assertTrue(got == 4, "the chest holds " + got + " of the 4 movable stone");
        });
    }

    /** An empty filter slot takes nothing from automation but anything from a player (that sets the filter). */
    public static void emptyFilterSlotOnlyTakesPlayerItems(GameTestHelper h) {
        place(h, PIPE, "reinforced_pipe", Direction.DOWN);
        ItemPipeBlockEntity pipe = entity(h, PIPE);
        pipe.cycleFilterMode();
        List<String> problems = new ArrayList<>();
        if (pipe.canPlaceItem(0, new ItemStack(Items.STONE))) problems.add("automation filled an empty filter slot");
        if (!pipe.mayPlayerPlace(new ItemStack(Items.STONE))) problems.add("a player could not set the filter");
        // The menu: its one slot follows the same rule, the data slots show mode, output and tier.
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        PipeMenu menu = PipeMenu.server(1, player.getInventory(), pipe);
        if (menu.filterMode() != 1 || menu.output() != Direction.DOWN || menu.tier() != PipeTier.REINFORCED) problems.add("menu data " + menu.filterMode() + "/" + menu.output() + "/" + menu.tier());
        if (!menu.getSlot(0).mayPlace(new ItemStack(Items.STONE))) problems.add("menu slot refused the first item");
        menu.getSlot(0).set(new ItemStack(Items.STONE, 2));
        if (menu.getSlot(0).mayPlace(new ItemStack(Items.DIRT))) problems.add("menu slot took dirt into a stone filter");
        if (!menu.clickMenuButton(player, PipeMenu.BUTTON_FILTER) || menu.filterMode() != 2) problems.add("filter button did not cycle (" + menu.filterMode() + ")");
        if (!menu.clickMenuButton(player, PipeMenu.BUTTON_OUTPUT) || h.getBlockState(PIPE).getValue(ItemPipeBlock.FACING) != Direction.NORTH) problems.add("output button did not turn");
        h.assertTrue(problems.isEmpty(), "empty filter: " + problems);
        h.succeed();
    }

    /**
     * IG31-5: the six side keys of the window point the output at exactly that side (all other sides stay inputs), a key for a side
     * the step may not point to (up on the first step) does nothing, a key for the current output changes nothing.
     */
    public static void sideKeysChooseTheOutput(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        place(h, PIPE, "reinforced_pipe", Direction.DOWN);
        PipeMenu menu = PipeMenu.server(1, player.getInventory(), entity(h, PIPE));
        for (Direction side : Direction.values()) {
            if (!menu.clickMenuButton(player, PipeMenu.BUTTON_SIDE + side.ordinal()) && side != menu.output()) problems.add("key " + side + " refused on a reinforced pipe");
            BlockState state = h.getBlockState(PIPE);
            if (state.getValue(ItemPipeBlock.FACING) != side) problems.add("key " + side + " gave output " + state.getValue(ItemPipeBlock.FACING));
            if (menu.output() != side) problems.add("menu shows " + menu.output() + " for " + side);
            if (!state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side))) problems.add("no arm on the chosen output " + side);
        }
        place(h, PIPE, "pipe", Direction.NORTH);
        PipeMenu basic = PipeMenu.server(2, player.getInventory(), entity(h, PIPE));
        if (basic.clickMenuButton(player, PipeMenu.BUTTON_SIDE + Direction.UP.ordinal())) problems.add("the first step took up as output");
        if (h.getBlockState(PIPE).getValue(ItemPipeBlock.FACING) != Direction.NORTH) problems.add("the first step moved its output up");
        if (!basic.clickMenuButton(player, PipeMenu.BUTTON_SIDE + Direction.WEST.ordinal()) || h.getBlockState(PIPE).getValue(ItemPipeBlock.FACING) != Direction.WEST) problems.add("west key on the first step");
        if (basic.clickMenuButton(player, PipeMenu.BUTTON_SIDE + 6)) problems.add("a seventh key exists");
        h.assertTrue(problems.isEmpty(), "side keys: " + problems);
        h.succeed();
    }

    /** Upgrading a pipe in the world keeps its item, filter and output. */
    public static void upgradeKeepsItemFilterAndOutput(GameTestHelper h) {
        place(h, PIPE, "reinforced_pipe", Direction.EAST);
        ItemPipeBlockEntity pipe = entity(h, PIPE);
        pipe.setItem(0, new ItemStack(Items.STONE, 5));
        pipe.cycleFilterMode();
        pipe.cycleFilterMode(); // kind
        List<String> problems = new ArrayList<>();
        BlockPos abs = h.absolutePos(PIPE);
        Pipes.upgradeInPlace(h.getLevel(), abs, pipe("netherite_pipe"));
        BlockState now = h.getBlockState(PIPE);
        if (now.getBlock() != pipe("netherite_pipe") || now.getValue(ItemPipeBlock.FACING) != Direction.EAST) problems.add("state after the upgrade " + now);
        ItemPipeBlockEntity upgraded = entity(h, PIPE);
        if (upgraded.getItem(0).getCount() != 5 || !upgraded.getItem(0).is(Items.STONE) || upgraded.filterMode() != 2) problems.add("contents " + upgraded.getItem(0) + " mode " + upgraded.filterMode());
        var entry = com.simplelib.api.InWorldUpgrades.entryFor(pipe("pipe"));
        if (entry == null || entry.to() != pipe("reinforced_pipe")) problems.add("no in-world upgrade entry pipe -> reinforced pipe");
        var next = com.simplelib.api.InWorldUpgrades.entryFor(pipe("reinforced_pipe"));
        if (next == null || next.to() != pipe("netherite_pipe")) problems.add("no entry reinforced -> netherite");
        var last = com.simplelib.api.InWorldUpgrades.entryFor(pipe("netherite_pipe"));
        if (last == null || last.to() != pipe("enderite_pipe")) problems.add("no entry netherite -> enderite");
        h.assertTrue(problems.isEmpty(), "pipe upgrade: " + problems);
        h.succeed();
    }

    /** A pipe pushes into the next pipe, which carries on into its chest (chains work, the second pipe takes only what its filter allows). */
    public static void pipesChain(GameTestHelper h) {
        BlockPos second = PIPE.east();
        h.setBlock(second.below(), Blocks.CHEST);
        place(h, PIPE, "enderite_pipe", Direction.EAST);
        place(h, second, "enderite_pipe", Direction.DOWN);
        entity(h, PIPE).setItem(0, new ItemStack(Items.STONE, 12));
        h.succeedWhen(() -> {
            int got = container(h, second.below()).countItem(Items.STONE);
            h.assertTrue(got == 12, "the chest at the end of the chain holds " + got + " of 12");
        });
    }

    private static ItemStack craft(GameTestHelper h, java.util.List<ItemStack> grid) {
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
        return h.getLevel().getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, h.getLevel())
                .map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    /** Eight iron ingots around a chest (the Vanilla hopper's material) make 4 pipes; the old copper-and-hopper grid and a ring around a hopper make none. */
    public static void recipeIsIronAroundChest(GameTestHelper h) {
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack chest = new ItemStack(Items.CHEST);
        List<String> problems = new ArrayList<>();
        ItemStack ring = craft(h, List.of(iron, iron, iron, iron, chest, iron, iron, iron, iron));
        if (!ring.is(StorageItems.PIPES.get(PipeTier.BASIC)) || ring.getCount() != 4) problems.add("iron ring around a chest gave " + ring);
        ItemStack hopper = craft(h, List.of(iron, iron, iron, iron, new ItemStack(Items.HOPPER), iron, iron, iron, iron));
        if (!hopper.isEmpty()) problems.add("a ring around a hopper gave " + hopper);
        ItemStack copper = craft(h, List.of(ItemStack.EMPTY, new ItemStack(Items.COPPER_INGOT), ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.HOPPER),
                ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.COPPER_INGOT), ItemStack.EMPTY));
        if (!copper.isEmpty()) problems.add("the old copper recipe still crafts " + copper);
        ItemStack gap = craft(h, List.of(iron, iron, iron, iron, chest, iron, iron, iron, ItemStack.EMPTY));
        if (!gap.isEmpty()) problems.add("seven iron gave " + gap);
        h.assertTrue(problems.isEmpty(), "pipe recipe: " + problems);
        h.succeed();
    }

    /** The filter is a feature of Reinforced and up: the first step ignores the key and moves everything, even with a stored mode. */
    public static void firstStepHasNoFilter(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (PipeTier tier : PipeTier.values()) {
            if (tier.canFilter() != (tier != PipeTier.BASIC)) problems.add(tier + " canFilter " + tier.canFilter());
        }
        place(h, PIPE, "pipe", Direction.DOWN);
        ItemPipeBlockEntity pipe = entity(h, PIPE);
        pipe.setItem(0, new ItemStack(Items.STONE, 3));
        pipe.cycleFilterMode();
        if (pipe.filterMode() != 0) problems.add("the first step took a filter mode (" + pipe.filterMode() + ")");
        if (!pipe.canPlaceItem(0, new ItemStack(Items.DIRT))) problems.add("the first step refused dirt");
        if (!pipe.canTakeItemThroughFace(0, new ItemStack(Items.STONE), Direction.DOWN)) problems.add("the first step keeps a last item back");
        pipe.receiveUpgraded(new ItemStack(Items.STONE, 3), 1); // a mode that came along from somewhere
        if (pipe.mode() != 0 || !pipe.canPlaceItem(0, new ItemStack(Items.DIRT))) problems.add("a stored mode counts on the first step");
        Pipes.upgradeInPlace(h.getLevel(), h.absolutePos(PIPE), pipe("reinforced_pipe"));
        ItemPipeBlockEntity upgraded = entity(h, PIPE);
        if (upgraded.mode() != 1 || upgraded.canPlaceItem(0, new ItemStack(Items.DIRT))) problems.add("the mode did not start working after the upgrade (" + upgraded.mode() + ")");
        h.assertTrue(problems.isEmpty(), "first step: " + problems);
        h.succeed();
    }

    /** The window has the Vanilla hopper's rows: slot row at y 20, inventory at y 51, one slot in the middle of the five. */
    public static void windowFollowsTheHopperLayout(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        if (PipeMenu.SLOT_Y != 20 || PipeMenu.INVENTORY_Y != 51 || PipeMenu.IMAGE_WIDTH != 176 || PipeMenu.SLOT_X != 80) problems.add("layout constants");
        place(h, PIPE, "reinforced_pipe", Direction.DOWN);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        PipeMenu menu = PipeMenu.server(1, player.getInventory(), entity(h, PIPE));
        if (menu.getSlot(0).x != 80 || menu.getSlot(0).y != 20) problems.add("pipe slot at " + menu.getSlot(0).x + "," + menu.getSlot(0).y);
        if (menu.slots.size() != 1 + 36 || menu.getSlot(1).x != 8 || menu.getSlot(1).y != 51) problems.add("inventory at " + menu.getSlot(1).x + "," + menu.getSlot(1).y + " slots " + menu.slots.size());
        h.assertTrue(problems.isEmpty(), "window layout: " + problems);
        h.succeed();
    }
}
