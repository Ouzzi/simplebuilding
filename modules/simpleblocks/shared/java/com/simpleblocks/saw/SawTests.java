package com.simpleblocks.saw;

import com.simpleblocks.BlocksRegistry;
import com.simpleblocks.SimpleBlocks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

/**
 * GameTests of the Shapecutter (principle A4b). They also run in the standalone targets (Simple Blocks + Vanilla only):
 * the optional cuts of Simple End and SimpleBuilding must then be absent, all others present.
 */
public final class SawTests {
    /** Vanilla wood families (planks prefix, log, wood); bamboo is checked on its own. */
    static final List<String[]> WOODS = List.of(
            new String[]{"oak", "log", "wood"}, new String[]{"spruce", "log", "wood"}, new String[]{"birch", "log", "wood"},
            new String[]{"jungle", "log", "wood"}, new String[]{"acacia", "log", "wood"}, new String[]{"dark_oak", "log", "wood"},
            new String[]{"mangrove", "log", "wood"}, new String[]{"cherry", "log", "wood"}, new String[]{"pale_oak", "log", "wood"},
            new String[]{"poplar", "log", "wood"}, new String[]{"crimson", "stem", "hyphae"}, new String[]{"warped", "stem", "hyphae"});

    private static final TagKey<Item> STAIRS = TagKey.create(net.minecraft.core.registries.Registries.ITEM, Identifier.withDefaultNamespace("stairs"));
    private static final TagKey<Item> SLABS = TagKey.create(net.minecraft.core.registries.Registries.ITEM, Identifier.withDefaultNamespace("slabs"));

    private SawTests() {}

    /** Ticks for the N62 machine tests on NeoForge/Forge (Fabric: the adapter's maxTicks). */
    public static final int MACHINE_TICKS = 200;

    public static void register(Map<String, Consumer<GameTestHelper>> all, Map<String, Integer> ticks) {
        register(all);
        for (String name : List.of("saw_pulse_one_cut_per_pulse", "saw_pulse_no_cut_without_signal", "saw_pulse_ejects_without_container",
                "saw_auto_cuts_without_signal")) {
            ticks.put(name, MACHINE_TICKS);
        }
    }

    public static void register(Map<String, Consumer<GameTestHelper>> all) {
        all.put("saw_registered", SawTests::registered);
        all.put("saw_wood_cuts", SawTests::woodCuts);
        all.put("saw_superset_of_stonecutter", SawTests::superset);
        all.put("saw_cuts_every_shape", SawTests::everyShape);
        all.put("saw_menu_cuts", SawTests::menuCuts);
        all.put("saw_optional_mods", SawTests::optionalMods);
        all.put("saw_ui_contrast", SawTests::uiContrast);
        all.put("saw_menu_layout", SawTests::menuLayout);
        all.put("saw_id_moved", SawTests::idMoved);
        all.put("saw_upgrade_axe", SawTests::upgradeAxe);
        all.put("saw_upgrade_hammer", SawTests::upgradeHammer);
        all.put("saw_upgrade_mode", SawTests::upgradeMode);
        all.put("saw_metal_finish", SawTests::metalFinish);
        all.put("saw_upgrade_respects_protection", SawTests::upgradeRespectsProtection);
        // N62: Pulse Shapecutter (one cut per redstone pulse) and Auto Shapecutter (cuts by itself).
        all.put("saw_machines_registered", SawTests::machinesRegistered);
        all.put("saw_pulse_one_cut_per_pulse", SawTests::pulseOneCutPerPulse);
        all.put("saw_pulse_no_cut_without_signal", SawTests::pulseNoCutWithoutSignal);
        all.put("saw_pulse_ejects_without_container", SawTests::pulseEjectsWithoutContainer);
        all.put("saw_auto_cuts_without_signal", SawTests::autoCutsWithoutSignal);
        all.put("saw_machine_selection_saved", SawTests::machineSelectionSaved);
        all.put("saw_machine_menu", SawTests::machineMenu);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private static boolean has(String id) {
        return BuiltInRegistries.ITEM.containsKey(Identifier.parse(id));
    }

    /** Count of {@code result} the saw makes from one {@code input}, 0 if it cannot. */
    static int cut(ServerLevel level, Item input, Item result) {
        for (Sawing.Entry e : Sawing.forInput(Sawing.entries(level), new ItemStack(input))) {
            if (e.result().item().value() == result) return e.result().count();
        }
        return 0;
    }

    private static void expect(GameTestHelper h, ServerLevel level, String input, String result, int count) {
        int got = cut(level, item(input), item(result));
        h.assertTrue(got == count, input + " -> " + count + " " + result + ", got " + got);
    }

    private static void registered(GameTestHelper h) {
        h.assertTrue(BuiltInRegistries.BLOCK.containsKey(SimpleBlocks.id(Sawing.NAME)) && BuiltInRegistries.ITEM.containsKey(SimpleBlocks.id(Sawing.NAME)), "saw block and item");
        h.assertTrue(BuiltInRegistries.MENU.containsKey(SimpleBlocks.id(Sawing.NAME)), "saw menu");
        h.assertTrue(BuiltInRegistries.RECIPE_TYPE.containsKey(Sawing.TYPE_ID) && BuiltInRegistries.RECIPE_SERIALIZER.containsKey(Sawing.TYPE_ID), "recipe type and serializer simpleblocks:sawing");
        h.assertTrue(BlocksRegistry.tabStacks().stream().anyMatch(s -> s.is(Sawing.SAW_ITEM)), "saw in the Simple Blocks tab");
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, Sawing.SAW);
        h.assertTrue(h.getBlockState(pos).getBlock() instanceof ShapecutterBlock, "saw placed");
        h.assertTrue(h.getBlockState(pos).getMenuProvider(h.getLevel(), h.absolutePos(pos)) != null, "saw opens a menu");
        var level = h.getLevel();
        // BS25-3: stonecutter in the middle, the tool above, a U of planks; with Simple Hammers a hammer, else an axe.
        String id = SawUpgrade.hammerMode ? "shapecutter_with_hammers" : "shapecutter";
        var holder = level.getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, SimpleBlocks.id(id)));
        h.assertTrue(holder.isPresent(), "crafting recipe " + id);
        var ctx = SlotDisplayContext.fromLevel(level);
        h.assertTrue(holder.get().value().display().stream().anyMatch(d -> d.result().resolveForFirstStack(ctx).is(Sawing.SAW_ITEM)), id + " makes the saw");
        var ingredients = holder.get().value().placementInfo().ingredients();
        h.assertTrue(ingredients.stream().anyMatch(i -> i.test(new ItemStack(Items.STONECUTTER))), id + " needs the stonecutter");
        h.assertTrue(ingredients.stream().anyMatch(i -> i.test(new ItemStack(Items.OAK_PLANKS))), id + " needs planks");
        h.assertTrue(ingredients.stream().noneMatch(i -> i.test(new ItemStack(Items.COPPER_INGOT))), id + " has no copper any more");
        h.assertTrue(ingredients.stream().anyMatch(i -> i.test(new ItemStack(Items.IRON_AXE))) != SawUpgrade.hammerMode, id + " takes the axe exactly without a hammer mod");
        h.succeed();
    }

    private static void woodCuts(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (String[] w : WOODS) {
            String planks = "minecraft:" + w[0] + "_planks";
            expect(h, level, planks, "minecraft:" + w[0] + "_stairs", 1);
            expect(h, level, planks, "minecraft:" + w[0] + "_slab", 2);
            expect(h, level, planks, "minecraft:" + w[0] + "_fence", 1);
            expect(h, level, planks, "minecraft:stick", 2);
            for (String log : List.of(w[0] + "_" + w[1], "stripped_" + w[0] + "_" + w[1], w[0] + "_" + w[2], "stripped_" + w[0] + "_" + w[2])) {
                expect(h, level, "minecraft:" + log, planks, 4);
            }
            expect(h, level, "minecraft:" + w[0] + "_" + w[1], "minecraft:stripped_" + w[0] + "_" + w[1], 1);
        }
        expect(h, level, "minecraft:bamboo_block", "minecraft:bamboo_planks", 2);
        expect(h, level, "minecraft:bamboo_planks", "minecraft:bamboo_slab", 2);
        expect(h, level, "minecraft:bamboo_planks", "minecraft:bamboo_mosaic", 1);
        expect(h, level, "minecraft:bamboo_mosaic", "minecraft:bamboo_mosaic_stairs", 1);
        // Stone stays as at the stonecutter.
        expect(h, level, "minecraft:stone", "minecraft:stone_stairs", 1);
        expect(h, level, "minecraft:stone", "minecraft:stone_slab", 2);
        h.succeed();
    }

    /** Every Vanilla (and loaded mods') stonecutting recipe is a cut of the saw with the same yield. */
    private static void superset(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        List<String> missing = new ArrayList<>();
        int checked = 0;
        for (var holder : level.getServer().getRecipeManager().getRecipes()) {
            if (holder.value().getType() != RecipeType.STONECUTTING || !(holder.value() instanceof SingleItemRecipe recipe)) continue;
            for (var input : recipe.input().items().toList()) {
                ItemStack stack = new ItemStack(input.value());
                ItemStack out = recipe.assemble(new SingleRecipeInput(stack));
                if (cut(level, input.value(), out.getItem()) != out.getCount()) missing.add(holder.id().identifier() + " (" + input.getRegisteredName() + ")");
                checked++;
            }
        }
        h.assertTrue(checked > 200, "stonecutting recipes found: " + checked);
        h.assertTrue(missing.isEmpty(), "stonecutting cuts the saw lacks: " + missing);
        h.succeed();
    }

    /** Principle A4b: every crafted stairs or slab item can be cut in the saw (the stonecutter cannot cut wood or wool). */
    private static void everyShape(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var ctx = SlotDisplayContext.fromLevel(level);
        Set<Item> crafted = new HashSet<>();
        for (var holder : level.getServer().getRecipeManager().getRecipes()) {
            if (holder.value().getType() != RecipeType.CRAFTING) continue;
            for (var display : holder.value().display()) crafted.add(display.result().resolveForFirstStack(ctx).getItem());
        }
        Set<Item> sawn = new HashSet<>();
        for (Sawing.Entry e : Sawing.entries(level)) sawn.add(e.result().item().value());
        List<String> missing = new ArrayList<>();
        int shapes = 0;
        for (Item it : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(it);
            if (!(stack.is(STAIRS) || stack.is(SLABS)) || !crafted.contains(it)) continue;
            shapes++;
            if (!sawn.contains(it)) missing.add(BuiltInRegistries.ITEM.getKey(it).toString());
        }
        h.assertTrue(shapes > 100, "crafted stairs/slabs found: " + shapes);
        h.assertTrue(missing.isEmpty(), "stairs/slabs the saw cannot cut: " + missing);
        // The ones the stonecutter cannot: wood, wool, the nether brick fence.
        expect(h, level, "minecraft:white_wool", "minecraft:white_wool_stairs", 1);
        expect(h, level, "minecraft:red_wool", "minecraft:red_wool_slab", 2);
        expect(h, level, "minecraft:nether_bricks", "minecraft:nether_brick_fence", 1);
        h.succeed();
    }

    /** The menu really cuts on the server: wool in, wool stairs out, one wool used. */
    private static void menuCuts(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        Item woolStairs = item("minecraft:white_wool_stairs");
        ShapecutterMenu menu = new ShapecutterMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), pos));
        menu.container.setItem(0, new ItemStack(item("minecraft:white_wool"), 2));
        int index = -1;
        for (int i = 0; i < menu.cuts().size(); i++) if (menu.cuts().get(i).result().item().value() == woolStairs) index = i;
        h.assertTrue(index >= 0, "wool stairs offered, cuts " + menu.cuts().size());
        menu.clickMenuButton(player, index);
        ItemStack result = menu.getSlot(ShapecutterMenu.RESULT_SLOT).getItem();
        h.assertTrue(result.is(woolStairs) && result.getCount() == 1, "result slot: " + result);
        menu.quickMoveStack(player, ShapecutterMenu.RESULT_SLOT);
        h.assertTrue(player.getInventory().countItem(woolStairs) >= 1, "stairs in the inventory");
        h.assertTrue(menu.container.getItem(0).getCount() <= 1, "wool used: " + menu.container.getItem(0));
        menu.container.setItem(0, new ItemStack(Items.DIAMOND));
        h.assertTrue(menu.cuts().isEmpty(), "a diamond has no cuts");
        h.succeed();
    }

    /** Optional cuts only with their mod: Simple End's woods; the natural slabs are this module's since b-blocksmove (always). */
    private static void optionalMods(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean end = has("simpleend:ashwood_planks"), sb = BuiltInRegistries.ITEM.keySet().stream().anyMatch(id -> id.getNamespace().equals("simplebuilding"));
        if (end) {
            for (String w : List.of("ashwood", "duskwood")) {
                expect(h, level, "simpleend:" + w + "_planks", "simpleend:" + w + "_stairs", 1);
                expect(h, level, "simpleend:" + w + "_planks", "simpleend:" + w + "_slab", 2);
                expect(h, level, "simpleend:" + w + "_log", "simpleend:" + w + "_planks", 4);
            }
        }
        expect(h, level, "minecraft:dirt", "simpleblocks:dirt_slab", 2);
        for (Sawing.Entry e : Sawing.entries(level)) {
            String ns = BuiltInRegistries.ITEM.getKey(e.result().item().value()).getNamespace();
            h.assertTrue(!ns.equals("simpleend") || end, "Simple End cut without Simple End: " + e.id());
            h.assertTrue(!ns.equals("simplebuilding") || sb, "SimpleBuilding cut without SimpleBuilding: " + e.id());
        }
        h.succeed();
    }

    /** AGENTS.md rule 15: the title and inventory label of the saw screen against Vanilla's container grey. */
    private static void uiContrast(GameTestHelper h) {
        double label = SawPalette.contrast(SawPalette.LABEL, SawPalette.FILL);
        h.assertTrue(label >= SawPalette.MIN_CONTRAST, "label contrast " + label);
        h.succeed();
    }

    /** Owner 21.10.: the same UI as the stonecutter - every slot where Vanilla's StonecutterMenu has it, same slot count. */
    private static void menuLayout(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        var saw = new ShapecutterMenu(1, player.getInventory());
        var vanilla = new net.minecraft.world.inventory.StonecutterMenu(2, player.getInventory());
        h.assertTrue(saw.slots.size() == vanilla.slots.size(), "slot count " + saw.slots.size() + " vs " + vanilla.slots.size());
        List<String> moved = new ArrayList<>();
        for (int i = 0; i < saw.slots.size(); i++) {
            var a = saw.slots.get(i);
            var b = vanilla.slots.get(i);
            if (a.x != b.x || a.y != b.y) moved.add(i + ": " + a.x + "," + a.y + " vs " + b.x + "," + b.y);
        }
        h.assertTrue(moved.isEmpty(), "slots off the Vanilla stonecutter layout: " + moved);
        h.assertTrue(ShapecutterMenu.WIDTH == 176 && ShapecutterMenu.HEIGHT == 166, "screen size of the stonecutter (176 x 166)");
        h.succeed();
    }

    /** The rename Shaping Saw -> Shapecutter (owner 21.10.): old ids of block, item, recipe and recipe advancement resolve to the new ones. */
    private static void idMoved(GameTestHelper h) {
        Identifier old = SimpleBlocks.id(Sawing.OLD_NAME), now = SimpleBlocks.id(Sawing.NAME);
        h.assertTrue(!old.equals(now), "the id changed");
        h.assertTrue(BuiltInRegistries.BLOCK.getValue(old) == Sawing.SAW, "old block id -> " + BuiltInRegistries.BLOCK.getValue(old));
        h.assertTrue(BuiltInRegistries.ITEM.getValue(old) == Sawing.SAW_ITEM, "old item id -> " + BuiltInRegistries.ITEM.getValue(old));
        h.assertTrue(now.equals(com.simplelib.api.IdMoves.target(net.minecraft.core.registries.Registries.RECIPE, old)), "old recipe id");
        h.assertTrue(SimpleBlocks.id(Sawing.ADVANCEMENT).equals(com.simplelib.api.IdMoves.advancement(SimpleBlocks.id(Sawing.OLD_ADVANCEMENT))), "old recipe advancement");
        var advancements = h.getLevel().getServer().getAdvancements();
        // BS25-3: the axe recipe keeps the id shapecutter; with Simple Hammers the hammer recipe replaces it (own id).
        if (SawUpgrade.hammerMode) {
            h.assertTrue(advancements.get(SimpleBlocks.id(Sawing.ADVANCEMENT + "_with_hammers")) != null, "new recipe advancement exists (hammer recipe)");
        } else {
            h.assertTrue(advancements.get(SimpleBlocks.id(Sawing.ADVANCEMENT)) != null, "new recipe advancement exists");
            var viaOld = advancements.get(SimpleBlocks.id(Sawing.OLD_ADVANCEMENT));
            h.assertTrue(viaOld != null && viaOld.id().equals(SimpleBlocks.id(Sawing.ADVANCEMENT)), "old advancement progress finds the new one: " + viaOld);
        }
        h.succeed();
    }

    /** Places a stonecutter facing east and a survival player holding {@code tool} with {@code nuggets} iron nuggets. */
    private static ServerPlayer upgradeScene(GameTestHelper h, BlockPos pos, ItemStack tool, int nuggets) {
        h.setBlock(pos, net.minecraft.world.level.block.Blocks.STONECUTTER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.StonecutterBlock.FACING, net.minecraft.core.Direction.EAST));
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tool);
        if (nuggets > 0) player.getInventory().add(new ItemStack(Items.IRON_NUGGET, nuggets));
        return player;
    }

    private static void checkPath(GameTestHelper h, boolean hammer, ItemStack tool, java.util.function.Predicate<ItemStack> rule, String name) {
        ServerLevel level = h.getLevel();
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        BlockPos rel = new BlockPos(3, 1, 3), pos = h.absolutePos(rel);
        // Too few nuggets: nothing happens, nothing is used.
        ServerPlayer poor = upgradeScene(h, rel, tool.copy(), SawUpgrade.NUGGETS - 1);
        h.assertTrue(!SawUpgrade.tryUpgrade(level, pos, poor, hand, hammer, rule), name + ": refused with 3 nuggets");
        h.assertTrue(h.getBlockState(rel).is(net.minecraft.world.level.block.Blocks.STONECUTTER) && poor.getInventory().countItem(Items.IRON_NUGGET) == 3, name + ": nothing used");
        // REV2-A: a player who may not build (Adventure) changes nothing, even with tool and nuggets.
        ServerPlayer adventurer = upgradeScene(h, rel, tool.copy(), SawUpgrade.NUGGETS + 1);
        adventurer.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
        h.assertTrue(!SawUpgrade.tryUpgrade(level, pos, adventurer, hand, hammer, rule), name + ": refused in Adventure mode");
        h.assertTrue(h.getBlockState(rel).is(net.minecraft.world.level.block.Blocks.STONECUTTER)
                && adventurer.getInventory().countItem(Items.IRON_NUGGET) == SawUpgrade.NUGGETS + 1, name + ": Adventure uses nothing");
        // Enough nuggets: the stonecutter becomes the saw facing the same way, 4 nuggets are used.
        ServerPlayer player = upgradeScene(h, rel, tool.copy(), SawUpgrade.NUGGETS + 1);
        h.assertTrue(SawUpgrade.tryUpgrade(level, pos, player, hand, hammer, rule), name + ": upgrade runs");
        var state = h.getBlockState(rel);
        h.assertTrue(state.is(Sawing.SAW) && state.getValue(net.minecraft.world.level.block.StonecutterBlock.FACING) == net.minecraft.core.Direction.EAST, name + ": saw, same facing: " + state);
        h.assertTrue(player.getInventory().countItem(Items.IRON_NUGGET) == 1, name + ": 4 nuggets used, left " + player.getInventory().countItem(Items.IRON_NUGGET));
        // A saw is not upgraded again.
        h.assertTrue(!SawUpgrade.tryUpgrade(level, pos, player, hand, hammer, rule), name + ": no second upgrade");
    }

    /** Path without Simple Hammers: an axe and 4 iron nuggets; a hammer-mode rule refuses the axe. */
    private static void upgradeAxe(GameTestHelper h) {
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        h.assertTrue(SawUpgrade.toolFits(axe, false) && !SawUpgrade.toolFits(axe, true), "axe fits only the axe path");
        h.assertTrue(!SawUpgrade.toolFits(new ItemStack(Items.IRON_PICKAXE), false), "a pickaxe is no axe");
        checkPath(h, false, axe, t -> SawUpgrade.toolFits(t, false), "axe");
        h.succeed();
    }

    /**
     * Path with Simple Hammers: a hammer ({@code #simplelib:hammers}) and 4 iron nuggets. With a hammer mod loaded a real
     * hammer is used; standalone (no hammer exists) the same flow runs with a stand-in tool for the hammer rule.
     */
    private static void upgradeHammer(GameTestHelper h) {
        ItemStack hammer = null;
        for (Item it : BuiltInRegistries.ITEM) {
            if (new ItemStack(it).is(SawUpgrade.HAMMERS)) {
                hammer = new ItemStack(it);
                break;
            }
        }
        if (hammer != null) {
            h.assertTrue(SawUpgrade.toolFits(hammer, true) && !SawUpgrade.toolFits(new ItemStack(Items.IRON_AXE), true), "hammer path takes the hammer, not the axe");
            checkPath(h, true, hammer, t -> SawUpgrade.toolFits(t, true), "hammer");
        } else {
            ItemStack standIn = new ItemStack(Items.IRON_SHOVEL);
            checkPath(h, true, standIn, t -> t.is(Items.IRON_SHOVEL), "hammer (stand-in, no hammer mod)");
        }
        h.succeed();
    }

    /**
     * a-audsb31 simpleblocks-P2-1: the upgrade and the metal finish are Fabric use-block events and ask the framework protection
     * (claims) themselves; a provider that denies everything stops both, nothing is used, and the same click works again without it.
     */
    private static void upgradeRespectsProtection(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var server = level.getServer();
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        BlockPos rel = new BlockPos(3, 1, 3), pos = h.absolutePos(rel);
        java.util.function.Predicate<ItemStack> axe = t -> SawUpgrade.toolFits(t, false);
        String provider = "audsb31_saw";
        ServerPlayer player = upgradeScene(h, rel, new ItemStack(Items.IRON_AXE), SawUpgrade.NUGGETS);
        com.simplebuilding.framework.api.Protection.register(server, provider, t -> false);
        try {
            h.assertTrue(!SawUpgrade.tryUpgrade(level, pos, player, hand, false, axe), "upgrade refused: the area is protected");
        } finally {
            com.simplebuilding.framework.api.Protection.unregister(server, provider);
        }
        h.assertTrue(h.getBlockState(rel).is(net.minecraft.world.level.block.Blocks.STONECUTTER)
                && player.getInventory().countItem(Items.IRON_NUGGET) == SawUpgrade.NUGGETS, "protected: block kept, nuggets kept");
        h.assertTrue(SawUpgrade.tryUpgrade(level, pos, player, hand, false, axe), "without the provider the same click upgrades");
        // metal finish on the new saw: 2 heavy plates in the off hand
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.HEAVY_WEIGHTED_PRESSURE_PLATE, SawUpgrade.PLATES));
        com.simplebuilding.framework.api.Protection.register(server, provider, t -> false);
        try {
            h.assertTrue(!SawUpgrade.tryMetal(level, pos, player, hand, false, axe), "metal finish refused: the area is protected");
        } finally {
            com.simplebuilding.framework.api.Protection.unregister(server, provider);
        }
        h.assertTrue(!h.getBlockState(rel).getValue(ShapecutterBlock.METAL)
                && player.getOffhandItem().getCount() == SawUpgrade.PLATES, "protected: finish and plates kept");
        h.succeed();
    }

    /** The loaders pick the hammer path exactly when Simple Hammers is loaded (split stage 7; before: SimpleBuilding). */
    private static void upgradeMode(GameTestHelper h) {
        boolean hammers = BuiltInRegistries.ITEM.keySet().stream().anyMatch(id -> id.getNamespace().equals("simplehammers"));
        h.assertTrue(SawUpgrade.hammerMode == hammers, "hammer mode " + SawUpgrade.hammerMode + " but Simple Hammers loaded " + hammers);
        h.succeed();
    }

    /**
     * BS25-3: the metal finish. The default state is wood; the tool in the main hand plus 2 heavy weighted pressure plates in the
     * off hand turn it to metal (plates used up, facing kept); 1 plate, other plates or no tool change nothing; a metal one is
     * not converted again; breaking it gives the item and 2 plates (loot table), a wood one only the item.
     */
    private static void metalFinish(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        BlockPos rel = new BlockPos(3, 1, 3), pos = h.absolutePos(rel);
        java.util.function.Predicate<ItemStack> axe = t -> SawUpgrade.toolFits(t, false);
        h.setBlock(rel, Sawing.SAW.defaultBlockState().setValue(net.minecraft.world.level.block.StonecutterBlock.FACING, net.minecraft.core.Direction.EAST));
        h.assertTrue(!h.getBlockState(rel).getValue(ShapecutterBlock.METAL), "default finish is wood");
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setItemInHand(hand, new ItemStack(Items.IRON_AXE));
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.HEAVY_WEIGHTED_PRESSURE_PLATE, 1));
        h.assertTrue(!SawUpgrade.tryMetal(level, pos, player, hand, false, axe), "1 plate is not enough");
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.LIGHT_WEIGHTED_PRESSURE_PLATE, 2));
        h.assertTrue(!SawUpgrade.tryMetal(level, pos, player, hand, false, axe), "gold plates do not count");
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.HEAVY_WEIGHTED_PRESSURE_PLATE, 3));
        h.assertTrue(!h.getBlockState(rel).getValue(ShapecutterBlock.METAL), "still wood");
        h.assertTrue(SawUpgrade.tryMetal(level, pos, player, hand, false, axe), "2 plates + axe convert");
        var state = h.getBlockState(rel);
        h.assertTrue(state.getValue(ShapecutterBlock.METAL) && state.getValue(net.minecraft.world.level.block.StonecutterBlock.FACING) == net.minecraft.core.Direction.EAST, "metal, same facing: " + state);
        h.assertTrue(player.getOffhandItem().getCount() == 1, "2 of 3 plates used, left " + player.getOffhandItem().getCount());
        h.assertTrue(!SawUpgrade.tryMetal(level, pos, player, hand, false, axe), "no second conversion");
        // Loot: metal gives the saw and 2 plates.
        var drops = net.minecraft.world.level.block.Block.getDrops(state, level, pos, null);
        h.assertTrue(drops.stream().anyMatch(s -> s.is(Sawing.SAW_ITEM)) && drops.stream().filter(s -> s.is(Items.HEAVY_WEIGHTED_PRESSURE_PLATE)).mapToInt(ItemStack::getCount).sum() == 2,
                "metal drops the saw and 2 plates: " + drops);
        var woodDrops = net.minecraft.world.level.block.Block.getDrops(state.setValue(ShapecutterBlock.METAL, false), level, pos, null);
        h.assertTrue(woodDrops.size() == 1 && woodDrops.getFirst().is(Sawing.SAW_ITEM), "wood drops only the saw: " + woodDrops);
        h.succeed();
    }

    // ------------------------------------------------------------------ N62: Pulse and Auto Shapecutter

    /** Places a machine facing east (front = east) with {@code count} stone and the stone stairs chosen. */
    private static ShapecutterMachineBlockEntity machine(GameTestHelper h, BlockPos pos, net.minecraft.world.level.block.Block block, int count) {
        h.setBlock(pos, block.defaultBlockState().setValue(ShapecutterMachineBlock.FACING, net.minecraft.core.Direction.EAST));
        ShapecutterMachineBlockEntity machine = h.getBlockEntity(pos, ShapecutterMachineBlockEntity.class);
        machine.setItem(0, new ItemStack(Items.STONE, count));
        Identifier stairs = cutId(machine, Items.STONE_STAIRS);
        h.assertTrue(stairs != null, "stone has the stone stairs cut, cuts " + machine.cuts().size());
        machine.select(stairs);
        return machine;
    }

    private static Identifier cutId(ShapecutterMachineBlockEntity machine, Item result) {
        for (Sawing.Entry e : machine.cuts()) if (e.result().item().value() == result) return e.id();
        return null;
    }

    private static int countIn(net.minecraft.world.Container container, Item item) {
        int n = 0;
        for (int i = 0; i < container.getContainerSize(); i++) if (container.getItem(i).is(item)) n += container.getItem(i).getCount();
        return n;
    }

    private static net.minecraft.world.Container chest(GameTestHelper h, BlockPos pos) {
        return h.getBlockEntity(pos, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
    }

    /**
     * Blocks, items, the shared block entity type and menu; both recipes: the Pulse Shapecutter is the crafter's pattern with the
     * Shapecutter in place of the crafting table (iron, redstone, dropper), the Auto Shapecutter = Pulse Shapecutter + hopper + comparator
     * (the crafter -> Auto Crafter step); both in the Simple Blocks tab right behind the Shapecutter.
     */
    private static void machinesRegistered(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (String name : List.of(Sawing.PULSE_NAME, Sawing.AUTO_NAME)) {
            h.assertTrue(BuiltInRegistries.BLOCK.containsKey(SimpleBlocks.id(name)) && BuiltInRegistries.ITEM.containsKey(SimpleBlocks.id(name)), name + " block and item");
        }
        h.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(SimpleBlocks.id(Sawing.MACHINE_NAME)) == Sawing.MACHINE_ENTITY, "machine block entity type");
        h.assertTrue(Sawing.MACHINE_ENTITY.isValid(Sawing.PULSE_SAW.defaultBlockState()) && Sawing.MACHINE_ENTITY.isValid(Sawing.AUTO_SAW.defaultBlockState()), "block entity accepts both");
        h.assertTrue(BuiltInRegistries.MENU.getValue(SimpleBlocks.id(Sawing.MACHINE_NAME)) == Sawing.MACHINE_MENU, "machine menu");
        h.assertTrue(!((ShapecutterMachineBlock) Sawing.PULSE_SAW).auto() && ((ShapecutterMachineBlock) Sawing.AUTO_SAW).auto(), "pulse vs auto");
        var tab = BlocksRegistry.tabStacks();
        int saw = -1;
        for (int i = 0; i < tab.size(); i++) if (tab.get(i).is(Sawing.SAW_ITEM)) saw = i;
        h.assertTrue(saw >= 0 && saw + 2 < tab.size() && tab.get(saw + 1).is(Sawing.PULSE_SAW_ITEM) && tab.get(saw + 2).is(Sawing.AUTO_SAW_ITEM), "tab: saw, pulse, auto in a row");
        var ctx = SlotDisplayContext.fromLevel(level);
        record Want(String id, Item makes, List<Item> needs) {}
        for (Want want : List.of(new Want(Sawing.PULSE_NAME, Sawing.PULSE_SAW_ITEM, List.of(Sawing.SAW_ITEM, Items.DROPPER, Items.REDSTONE, Items.IRON_INGOT)),
                new Want(Sawing.AUTO_NAME, Sawing.AUTO_SAW_ITEM, List.of(Sawing.PULSE_SAW_ITEM, Items.HOPPER, Items.COMPARATOR)))) {
            var holder = level.getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, SimpleBlocks.id(want.id())));
            h.assertTrue(holder.isPresent(), "crafting recipe " + want.id());
            h.assertTrue(holder.get().value().display().stream().anyMatch(d -> d.result().resolveForFirstStack(ctx).is(want.makes())), want.id() + " makes its block");
            var ingredients = holder.get().value().placementInfo().ingredients();
            for (Item need : want.needs()) h.assertTrue(ingredients.stream().anyMatch(i -> i.test(new ItemStack(need))), want.id() + " needs " + need);
        }
        h.succeed();
    }

    /** Crafter rule: one rising edge = exactly one cut into the chest in front; a lasting signal cuts no more; off and on again = one more. */
    private static void pulseOneCutPerPulse(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2), front = pos.east(), power = pos.west();
        h.setBlock(front, net.minecraft.world.level.block.Blocks.CHEST);
        ShapecutterMachineBlockEntity machine = machine(h, pos, Sawing.PULSE_SAW, 4);
        h.setBlock(power, net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        h.startSequence()
                .thenIdle(ShapecutterMachineBlock.PULSE_DELAY + 4)
                .thenExecute(() -> {
                    h.assertValueEqual(countIn(chest(h, front), Items.STONE_STAIRS), 1, "stairs after the first pulse");
                    h.assertValueEqual(machine.getItem(0).getCount(), 3, "stone left after the first pulse");
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertValueEqual(countIn(chest(h, front), Items.STONE_STAIRS), 1, "a lasting signal cut again");
                    h.setBlock(power, net.minecraft.world.level.block.Blocks.AIR);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    h.assertTrue(!machine.isTriggered(), "still triggered without the redstone block");
                    h.setBlock(power, net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(ShapecutterMachineBlock.PULSE_DELAY + 4)
                .thenExecute(() -> {
                    h.assertValueEqual(countIn(chest(h, front), Items.STONE_STAIRS), 2, "stairs after the second pulse");
                    h.assertValueEqual(machine.getItem(0).getCount(), 2, "stone left after the second pulse");
                })
                .thenSucceed();
    }

    /** No signal, no cut - however long it waits (the Pulse Shapecutter has no ticker). */
    private static void pulseNoCutWithoutSignal(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2), front = pos.east();
        h.setBlock(front, net.minecraft.world.level.block.Blocks.CHEST);
        ShapecutterMachineBlockEntity machine = machine(h, pos, Sawing.PULSE_SAW, 4);
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertValueEqual(countIn(chest(h, front), Items.STONE_STAIRS), 0, "stairs without a signal");
                    h.assertValueEqual(machine.getItem(0).getCount(), 4, "stone used without a signal");
                })
                .thenSucceed();
    }

    /** Without a container in front the cut flies out of the front like the crafter's output. */
    private static void pulseEjectsWithoutContainer(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        ShapecutterMachineBlockEntity machine = machine(h, pos, Sawing.PULSE_SAW, 2);
        h.setBlock(pos.west(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        h.startSequence()
                .thenIdle(ShapecutterMachineBlock.PULSE_DELAY + 4)
                .thenExecute(() -> {
                    h.assertItemEntityPresent(Items.STONE_STAIRS, pos.east(), 1.5);
                    h.assertValueEqual(machine.getItem(0).getCount(), 1, "one stone used");
                })
                .thenSucceed();
    }

    /**
     * Auto Shapecutter: cuts by itself into the chest in front until the input is used up; a redstone signal stops it; without a
     * container in front it does not cut (no items piling up in the world, like the Auto Crafter without a hopper).
     */
    private static void autoCutsWithoutSignal(GameTestHelper h) {
        BlockPos free = new BlockPos(1, 1, 1), stopped = new BlockPos(1, 1, 4), open = new BlockPos(5, 1, 1);
        h.setBlock(free.east(), net.minecraft.world.level.block.Blocks.CHEST);
        h.setBlock(stopped.east(), net.minecraft.world.level.block.Blocks.CHEST);
        h.setBlock(stopped.west(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        ShapecutterMachineBlockEntity a = machine(h, free, Sawing.AUTO_SAW, 3);
        ShapecutterMachineBlockEntity b = machine(h, stopped, Sawing.AUTO_SAW, 3);
        ShapecutterMachineBlockEntity c = machine(h, open, Sawing.AUTO_SAW, 3);
        h.assertTrue(b.isTriggered(), "the redstone block did not trigger the stopped saw");
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(countIn(chest(h, free.east()), Items.STONE_STAIRS), 3, "stairs from the Auto Shapecutter"))
                .thenExecute(() -> h.assertTrue(a.getItem(0).isEmpty(), "stone left: " + a.getItem(0)))
                .thenIdle(ShapecutterMachineBlockEntity.AUTO_INTERVAL_TICKS * 3)
                .thenExecute(() -> {
                    h.assertValueEqual(countIn(chest(h, stopped.east()), Items.STONE_STAIRS), 0, "the powered Auto Shapecutter cut");
                    h.assertValueEqual(b.getItem(0).getCount(), 3, "the powered Auto Shapecutter used stone");
                    h.assertValueEqual(c.getItem(0).getCount(), 3, "the Auto Shapecutter cut without a container in front");
                })
                .thenSucceed();
    }

    /** The chosen cut (a recipe id) and the input survive saving and loading, and the choice stays while the input runs empty. */
    private static void machineSelectionSaved(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        ShapecutterMachineBlockEntity machine = machine(h, pos, Sawing.PULSE_SAW, 5);
        Identifier chosen = machine.selected();
        var registries = h.getLevel().registryAccess();
        net.minecraft.nbt.CompoundTag saved = machine.saveWithFullMetadata(registries);
        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(h.absolutePos(pos), h.getBlockState(pos), saved, registries);
        h.assertTrue(loaded instanceof ShapecutterMachineBlockEntity, "loaded as " + loaded);
        ShapecutterMachineBlockEntity copy = (ShapecutterMachineBlockEntity) loaded;
        h.assertTrue(chosen != null && chosen.equals(copy.selected()), "chosen cut after loading: " + copy.selected() + " vs " + chosen);
        h.assertTrue(copy.getItem(0).is(Items.STONE) && copy.getItem(0).getCount() == 5, "input after loading: " + copy.getItem(0));
        machine.setItem(0, ItemStack.EMPTY);
        h.assertTrue(chosen.equals(machine.selected()) && machine.previewResult().isEmpty(), "choice kept while empty, nothing to cut");
        machine.setItem(0, new ItemStack(Items.STONE, 2));
        h.assertTrue(machine.previewResult().is(Items.STONE_STAIRS), "refilled: the same cut again, preview " + machine.previewResult());
        h.succeed();
    }

    /**
     * The machine menu: the stonecutter's slots (same as the Shapecutter's), a click chooses the cut in the block, slot 1 previews
     * the result and can be neither taken nor filled; automation can only fill the input, not from the front, and never take.
     */
    private static void machineMenu(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        BlockPos pos = new BlockPos(2, 1, 2);
        ShapecutterMachineBlockEntity machine = machine(h, pos, Sawing.AUTO_SAW, 4);
        machine.select(null);
        var menu = new ShapecutterMachineMenu(1, player.getInventory(), machine);
        var vanilla = new net.minecraft.world.inventory.StonecutterMenu(2, player.getInventory());
        h.assertTrue(menu.slots.size() == vanilla.slots.size(), "slot count " + menu.slots.size() + " vs " + vanilla.slots.size());
        for (int i = 0; i < menu.slots.size(); i++) {
            var a = menu.slots.get(i);
            var b = vanilla.slots.get(i);
            h.assertTrue(a.x == b.x && a.y == b.y, "slot " + i + " at " + a.x + "," + a.y + " vs " + b.x + "," + b.y);
        }
        h.assertTrue(menu.hasInput() && menu.selectedIndex() == -1, "stone in, nothing chosen");
        int index = -1;
        for (int i = 0; i < menu.cuts().size(); i++) if (menu.cuts().get(i).result().item().value() == Items.STONE_SLAB) index = i;
        h.assertTrue(index >= 0, "stone slab offered");
        h.assertTrue(menu.clickMenuButton(player, index), "click on a cut");
        h.assertTrue(menu.cuts().get(index).id().equals(machine.selected()) && menu.selectedIndex() == index, "the click chose the slab in the block");
        menu.broadcastChanges();
        var preview = menu.getSlot(ShapecutterMachineMenu.PREVIEW_SLOT);
        h.assertTrue(preview.getItem().is(Items.STONE_SLAB) && preview.getItem().getCount() == 2, "preview: " + preview.getItem());
        h.assertTrue(!preview.mayPickup(player) && !preview.mayPlace(new ItemStack(Items.STONE)), "preview can be neither taken nor filled");
        h.assertTrue(menu.quickMoveStack(player, ShapecutterMachineMenu.PREVIEW_SLOT).isEmpty(), "shift-click on the preview does nothing");
        var east = net.minecraft.core.Direction.EAST;
        h.assertTrue(machine.getSlotsForFace(east).length == 0 && machine.getSlotsForFace(net.minecraft.core.Direction.UP).length == 1, "input from every side but the front");
        h.assertTrue(!machine.canTakeItemThroughFace(0, machine.getItem(0), net.minecraft.core.Direction.DOWN), "automation cannot take the input");
        h.assertTrue(!machine.canPlaceItem(0, new ItemStack(Items.DIAMOND)), "a diamond has no cuts and is refused");
        h.assertTrue(machine.stillValid(player) == menu.stillValid(player), "menu validity follows the block");
        h.succeed();
    }
}
