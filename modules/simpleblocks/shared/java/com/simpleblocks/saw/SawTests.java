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
 * GameTests of the Shaping Saw (principle A4b). They also run in the standalone targets (Simple Blocks + Vanilla only):
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

    public static void register(Map<String, Consumer<GameTestHelper>> all) {
        all.put("saw_registered", SawTests::registered);
        all.put("saw_wood_cuts", SawTests::woodCuts);
        all.put("saw_superset_of_stonecutter", SawTests::superset);
        all.put("saw_cuts_every_shape", SawTests::everyShape);
        all.put("saw_menu_cuts", SawTests::menuCuts);
        all.put("saw_optional_mods", SawTests::optionalMods);
        all.put("saw_ui_contrast", SawTests::uiContrast);
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
        h.assertTrue(h.getBlockState(pos).getBlock() instanceof ShapingSawBlock, "saw placed");
        h.assertTrue(h.getBlockState(pos).getMenuProvider(h.getLevel(), h.absolutePos(pos)) != null, "saw opens a menu");
        var level = h.getLevel();
        for (String id : List.of("shaping_saw", "shaping_saw_from_stonecutter")) {
            var holder = level.getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, SimpleBlocks.id(id)));
            h.assertTrue(holder.isPresent(), "crafting recipe " + id);
            var ctx = SlotDisplayContext.fromLevel(level);
            h.assertTrue(holder.get().value().display().stream().anyMatch(d -> d.result().resolveForFirstStack(ctx).is(Sawing.SAW_ITEM)), id + " makes the saw");
        }
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
        ShapingSawMenu menu = new ShapingSawMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), pos));
        menu.container.setItem(0, new ItemStack(item("minecraft:white_wool"), 2));
        int index = -1;
        for (int i = 0; i < menu.cuts().size(); i++) if (menu.cuts().get(i).result().item().value() == woolStairs) index = i;
        h.assertTrue(index >= 0, "wool stairs offered, cuts " + menu.cuts().size());
        menu.clickMenuButton(player, index);
        ItemStack result = menu.getSlot(ShapingSawMenu.RESULT_SLOT).getItem();
        h.assertTrue(result.is(woolStairs) && result.getCount() == 1, "result slot: " + result);
        menu.quickMoveStack(player, ShapingSawMenu.RESULT_SLOT);
        h.assertTrue(player.getInventory().countItem(woolStairs) >= 1, "stairs in the inventory");
        h.assertTrue(menu.container.getItem(0).getCount() <= 1, "wool used: " + menu.container.getItem(0));
        menu.container.setItem(0, new ItemStack(Items.DIAMOND));
        h.assertTrue(menu.cuts().isEmpty(), "a diamond has no cuts");
        h.succeed();
    }

    /** Optional cuts only with their mod: Simple End's woods, SimpleBuilding's natural slabs. */
    private static void optionalMods(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean end = has("simpleend:ashwood_planks"), sb = has("simplebuilding:dirt_slab");
        if (end) {
            for (String w : List.of("ashwood", "duskwood")) {
                expect(h, level, "simpleend:" + w + "_planks", "simpleend:" + w + "_stairs", 1);
                expect(h, level, "simpleend:" + w + "_planks", "simpleend:" + w + "_slab", 2);
                expect(h, level, "simpleend:" + w + "_log", "simpleend:" + w + "_planks", 4);
            }
        }
        if (sb) expect(h, level, "minecraft:dirt", "simplebuilding:dirt_slab", 2);
        for (Sawing.Entry e : Sawing.entries(level)) {
            String ns = BuiltInRegistries.ITEM.getKey(e.result().item().value()).getNamespace();
            h.assertTrue(!ns.equals("simpleend") || end, "Simple End cut without Simple End: " + e.id());
            h.assertTrue(!ns.equals("simplebuilding") || sb, "SimpleBuilding cut without SimpleBuilding: " + e.id());
        }
        h.succeed();
    }

    /** AGENTS.md rule 15: label and hint text of the saw screen against their backgrounds. */
    private static void uiContrast(GameTestHelper h) {
        double label = SawPalette.contrast(SawPalette.LABEL, SawPalette.FILL);
        double hint = SawPalette.contrast(SawPalette.FIELD_TEXT, SawPalette.DARK);
        double field = SawPalette.contrast(SawPalette.FIELD_TEXT, SawPalette.FIELD);
        h.assertTrue(label >= SawPalette.MIN_CONTRAST, "label contrast " + label);
        h.assertTrue(hint >= SawPalette.MIN_CONTRAST, "hint contrast " + hint);
        h.assertTrue(field >= SawPalette.MIN_CONTRAST, "search contrast " + field);
        h.succeed();
    }
}
