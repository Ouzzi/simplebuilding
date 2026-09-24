package com.simplebuilding.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.compat.InWorldRecipeCatalog;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.InWorldTransformations;
import com.simplebuilding.util.SledgehammerUpgrades;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;

/**
 * The wiki's "In-world transformation" category is exported by {@link InWorldTransformations}
 * (through the Fabric datagen provider into {@code src/main/generated/wiki/inworld.json}). These
 * tests hold the export against the game itself, so the wiki cannot quietly show numbers the game
 * does not use.
 *
 * <p><b>The expectations are written out here</b>, not read from the constants the export uses -
 * a test that read {@code SledgehammerUpgrades.UPGRADE_TICKS} would follow any edit and prove
 * nothing. Where a value is compared to the game, the game side is asked through its own path
 * ({@code hammerRank(ItemStack)}, {@code getUseDuration}), not through the export's helpers.
 */
public final class InWorldExportTests {

    private InWorldExportTests() {
    }

    /**
     * Every machine step names its target, the nugget and the weakest hammer that can do it; that
     * hammer really has the rank the step needs, and the next weaker one does not.
     */
    public static void upgradeStepsNameTheWeakestHammerThatWorks(GameTestHelper helper) {
        JsonObject up = InWorldTransformations.sledgehammerUpgrade();
        List<String> summary = new ArrayList<>();
        for (JsonElement element : up.getAsJsonArray("steps")) {
            JsonObject step = element.getAsJsonObject();
            Block from = block(step.get("from").getAsString());
            SledgehammerUpgrades.Upgrade upgrade = SledgehammerUpgrades.upgradeOf(from);
            helper.assertTrue(upgrade != null, "exported step from " + step.get("from") + " is no upgrade in the game");
            helper.assertTrue(InWorldTransformations.id(upgrade.to()).equals(step.get("to").getAsString()),
                    "step " + step.get("from") + " leads to " + step.get("to") + " in the export but to "
                            + InWorldTransformations.id(upgrade.to()) + " in the game");
            Item weakest = item(step.get("minimumHammer").getAsString());
            int rank = SledgehammerUpgrades.hammerRank(new ItemStack(weakest));
            helper.assertTrue(rank >= upgrade.minHammerRank(),
                    step.get("minimumHammer") + " cannot do the step from " + step.get("from"));
            summary.add(step.get("from").getAsString().replace("simplebuilding:", "") + ">"
                    + step.get("minimumHammer").getAsString().replace("simplebuilding:", "")
                    + "/" + step.get("nugget").getAsString().replace("simplebuilding:", "")
                    + "x" + step.get("nuggetCount").getAsInt()
                    + "/" + step.get("damagePerHit").getAsInt() + "per" + "/" + step.get("totalDamage").getAsInt());
        }
        String expected = "[netherite_blast_furnace>netherite_sledgehammer/enderite_nuggetx1/10per/50, "
                + "netherite_furnace>netherite_sledgehammer/enderite_nuggetx1/10per/50, "
                + "netherite_hopper>netherite_sledgehammer/enderite_nuggetx1/10per/50, "
                + "netherite_piston>netherite_sledgehammer/enderite_nuggetx1/10per/50, "
                + "netherite_smoker>netherite_sledgehammer/enderite_nuggetx1/10per/50, "
                + "reinforced_blast_furnace>diamond_sledgehammer/netherite_nuggetx1/4per/20, "
                + "reinforced_furnace>diamond_sledgehammer/netherite_nuggetx1/4per/20, "
                + "reinforced_hopper>diamond_sledgehammer/netherite_nuggetx1/4per/20, "
                + "reinforced_piston>diamond_sledgehammer/netherite_nuggetx1/4per/20, "
                + "reinforced_smoker>diamond_sledgehammer/netherite_nuggetx1/4per/20]";
        helper.assertTrue(summary.toString().equals(expected), "upgrade steps: expected " + expected + " but were " + summary);
        String timing = up.get("durationTicks").getAsInt() + "/" + up.get("hitIntervalTicks").getAsInt() + "/" + up.get("hits").getAsInt();
        helper.assertTrue(timing.equals("100/20/5"), "duration/interval/blows: expected 100/20/5 but were " + timing);
        // Die stapellose Rangfrage (fuer den Export) und die mit Stapel (im Spiel) sagen dasselbe.
        List<String> ranks = new ArrayList<>();
        for (Item hammer : List.of(ModItems.STONE_SLEDGEHAMMER, ModItems.IRON_SLEDGEHAMMER, ModItems.DIAMOND_SLEDGEHAMMER,
                ModItems.NETHERITE_SLEDGEHAMMER, ModItems.ENDERITE_SLEDGEHAMMER)) {
            int byItem = SledgehammerUpgrades.hammerRank(hammer);
            int byStack = SledgehammerUpgrades.hammerRank(new ItemStack(hammer));
            ranks.add(byItem + "=" + byStack);
        }
        helper.assertTrue(ranks.toString().equals("[0=0, 0=0, 1=1, 2=2, 3=3]"), "hammer ranks item=stack: " + ranks);
        TestCleanup.succeed(helper);
    }

    /** The exported reshape charge time of every hammer is what {@code getUseDuration} gives in the game. */
    public static void reshapeTicksMatchTheUseDurationOfEveryHammer(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        TreeMap<String, String> seen = new TreeMap<>();
        for (JsonElement element : InWorldTransformations.sledgehammerReshape().getAsJsonArray("hammers")) {
            JsonObject hammer = element.getAsJsonObject();
            Item item = item(hammer.get("id").getAsString());
            int game = ((SledgehammerItem) item).getUseDuration(new ItemStack(item), player);
            seen.put(hammer.get("id").getAsString().replace("simplebuilding:", ""), hammer.get("ticks").getAsInt() + "=" + game);
        }
        String expected = "{copper_sledgehammer=40=40, diamond_sledgehammer=25=25, enderite_sledgehammer=20=20, "
                + "gold_sledgehammer=16=16, iron_sledgehammer=33=33, netherite_sledgehammer=22=22, stone_sledgehammer=40=40}";
        helper.assertTrue(seen.toString().equals(expected), "export=game charge ticks: expected " + expected + " but were " + seen);
        JsonObject reshape = InWorldTransformations.sledgehammerReshape();
        String wear = reshape.get("damage").getAsInt() + "/" + reshape.get("reverseDamage").getAsInt();
        helper.assertTrue(wear.equals("1/2"), "reshape durability forward/backward: expected 1/2 but was " + wear);
        TestCleanup.succeed(helper);
    }

    /**
     * The stone chisel's table has the stone-tier step and not the iron-tier one, the iron chisel
     * has both; a step that points at its own block (mud brick stairs) does nothing and is left
     * out; the enderite chisel shares the netherite table, and no legacy spatula is a chisel.
     */
    public static void chiselTablesFollowTheToolTiers(GameTestHelper helper) {
        JsonObject chisel = InWorldTransformations.chisel();
        JsonArray tables = chisel.getAsJsonArray("tables");
        TreeMap<String, Integer> tableOf = new TreeMap<>();
        for (JsonElement element : chisel.getAsJsonArray("tools")) {
            JsonObject tool = element.getAsJsonObject();
            tableOf.put(tool.get("id").getAsString().replace("simplebuilding:", ""), tool.get("table").getAsInt());
        }
        JsonObject stone = tables.get(tableOf.get("stone_chisel")).getAsJsonObject();
        JsonObject iron = tables.get(tableOf.get("iron_chisel")).getAsJsonObject();
        String stoneStep = "[\"minecraft:stone\",\"minecraft:chiseled_stone_bricks\"]";
        String ironStep = "[\"minecraft:chiseled_stone_bricks\",\"minecraft:stone_bricks\"]";
        String selfStep = "[\"minecraft:mud_brick_stairs\",\"minecraft:mud_brick_stairs\"]";
        String found = contains(stone, "forward", stoneStep) + "/" + contains(stone, "forward", ironStep) + "/"
                + contains(iron, "forward", stoneStep) + "/" + contains(iron, "forward", ironStep) + "/"
                + contains(stone, "touchForward", selfStep);
        helper.assertTrue(found.equals("true/false/true/true/false"),
                "stone has stone step / stone has iron step / iron has stone step / iron has iron step / self step listed: "
                        + "expected true/false/true/true/false but was " + found);
        String backward = contains(stone, "backward", "[\"minecraft:chiseled_stone_bricks\",\"minecraft:stone\"]") + "";
        helper.assertTrue(backward.equals("true"), "the stone chisel walks chiseled stone bricks back to stone: " + backward);
        String shared = (tableOf.get("enderite_chisel").equals(tableOf.get("netherite_chisel"))) + "/"
                + (tableOf.get("copper_chisel").equals(tableOf.get("iron_chisel"))) + "/"
                + (tableOf.get("stone_chisel").equals(tableOf.get("iron_chisel")));
        helper.assertTrue(shared.equals("true/true/false"), "enderite=netherite / copper=iron / stone=iron tables: " + shared);
        String wear = chisel.get("damage").getAsInt() + "/" + chisel.get("reverseDamage").getAsInt();
        helper.assertTrue(wear.equals("1/2"), "chisel durability forward/backward: expected 1/2 but was " + wear);
        // Die Tabellen im Export sind dieselben Objekte wie am Werkzeug.
        ChiselItem stoneChisel = (ChiselItem) ModItems.STONE_CHISEL;
        helper.assertTrue(stoneChisel.getForwardMap().get(net.minecraft.world.level.block.Blocks.STONE)
                        == net.minecraft.world.level.block.Blocks.CHISELED_STONE_BRICKS,
                "the stone chisel's own table turns stone into chiseled stone bricks");
        TestCleanup.succeed(helper);
    }

    /**
     * The JEI plugin shows what {@link InWorldRecipeCatalog} holds, and the catalog is read from the
     * same export as the wiki. This holds the catalog against that export without JEI at runtime:
     * every section has a category, nothing failed to resolve, every machine step, chisel pair (per
     * chisel), reshape pair, the diamond block and all sixteen wools arrive - and a few entries are
     * spelled out, so a catalog that followed a broken export would still fail.
     *
     * <p>The trim template in an item frame and washing an octant in a cauldron were listed by hand
     * in the wiki until 2026-09-25; now they come from the same export. Both are held against the
     * game here: all eighteen vanilla armor trim templates (counted from the item registry by their
     * vanilla name, not by the export's rule), both off-hand materials with their result, every
     * sledgehammer the mod registers as a tool, and the sixteen coloured octants washing into the
     * plain one at a cauldron.
     */
    public static void jeiCatalogCoversEveryInWorldEntry(GameTestHelper helper) {
        InWorldRecipeCatalog.Catalog catalog = InWorldRecipeCatalog.build();
        helper.assertTrue(catalog.problems().isEmpty(), "JEI catalog problems: " + catalog.problems());
        java.util.Map<String, InWorldRecipeCatalog.Entry> byId = new java.util.HashMap<>();
        for (InWorldRecipeCatalog.Entry entry : catalog.entries()) {
            helper.assertTrue(byId.put(entry.id(), entry) == null, "JEI catalog id twice: " + entry.id());
            helper.assertTrue(!entry.tools().isEmpty() && !entry.inputs().isEmpty(), "JEI entry without tool or input: " + entry.id());
        }
        JsonObject described = InWorldTransformations.describe();
        for (String section : described.keySet()) {
            InWorldRecipeCatalog.Kind kind = InWorldRecipeCatalog.Kind.bySection(section);
            helper.assertTrue(kind != null, "in-world section " + section + " has no JEI category");
            helper.assertTrue(!catalog.of(kind).isEmpty(), "JEI category " + kind + " is empty");
        }

        JsonObject up = described.getAsJsonObject("sledgehammerUpgrade");
        for (JsonElement element : up.getAsJsonArray("steps")) {
            JsonObject step = element.getAsJsonObject();
            InWorldRecipeCatalog.Entry entry = byId.get("machine_upgrade/" + step.get("from").getAsString());
            helper.assertTrue(entry != null, "machine step " + step.get("from") + " missing in JEI");
            helper.assertTrue(entry.output().items().get(0) == block(step.get("to").getAsString()).asItem()
                            && entry.inputs().get(0).items().get(0) == block(step.get("from").getAsString()).asItem()
                            && entry.inputs().get(1).items().get(0) == item(step.get("nugget").getAsString())
                            && entry.tools().get(0) == item(step.get("minimumHammer").getAsString())
                            && entry.durationTicks() == up.get("durationTicks").getAsInt(),
                    "JEI machine step " + entry.id() + " differs from the export");
        }
        InWorldRecipeCatalog.Entry hopper = byId.get("machine_upgrade/simplebuilding:reinforced_hopper");
        String hopperTools = hopper.tools().stream().map(InWorldTransformations::id).toList().toString()
                .replace("simplebuilding:", "");
        helper.assertTrue(hopperTools.equals("[diamond_sledgehammer, netherite_sledgehammer, enderite_sledgehammer]"),
                "reinforced hopper: every hammer from diamond up, weakest first - but was " + hopperTools);

        int chiselPairs = 0;
        JsonObject chisel = described.getAsJsonObject("chisel");
        JsonArray tables = chisel.getAsJsonArray("tables");
        for (JsonElement element : chisel.getAsJsonArray("tools")) {
            JsonObject tool = element.getAsJsonObject();
            if (tool.get("spatula").getAsBoolean()) {
                continue;
            }
            Item toolItem = item(tool.get("id").getAsString());
            JsonObject table = tables.get(tool.get("table").getAsInt()).getAsJsonObject();
            for (String direction : new String[]{"forward", "backward"}) {
                String prefix = direction.equals("forward") ? "chisel/" : "chisel_reverse/";
                String touchKey = direction.equals("forward") ? "touchForward" : "touchBackward";
                java.util.Set<java.util.List<String>> normal = InWorldRecipeCatalog.pairs(table.getAsJsonArray(direction));
                java.util.Set<java.util.List<String>> touch = InWorldRecipeCatalog.pairs(table.getAsJsonArray(touchKey));
                for (java.util.List<String> pair : touch) {
                    String id = prefix + pair.get(0) + "/" + pair.get(1) + (normal.contains(pair) ? "" : "/touch");
                    InWorldRecipeCatalog.Entry entry = byId.get(id);
                    helper.assertTrue(entry != null && entry.tools().contains(toolItem),
                            "chisel pair " + id + " of " + tool.get("id") + " missing in JEI");
                    chiselPairs++;
                }
                for (java.util.List<String> pair : normal) {
                    InWorldRecipeCatalog.Entry entry = byId.get(prefix + pair.get(0) + "/" + pair.get(1));
                    helper.assertTrue(entry != null && entry.tools().contains(toolItem),
                            "chisel pair " + pair + " (" + direction + ") of " + tool.get("id") + " missing in JEI");
                }
            }
        }
        helper.assertTrue(chiselPairs > 0, "no chisel pair was checked");
        InWorldRecipeCatalog.Entry stoneStep = byId.get("chisel/minecraft:stone/minecraft:chiseled_stone_bricks");
        helper.assertTrue(stoneStep != null && stoneStep.tools().get(0) == ModItems.STONE_CHISEL,
                "stone -> chiseled stone bricks is a stone-chisel step in JEI");

        for (boolean reverse : new boolean[]{false, true}) {
            for (Block[] pair : InWorldTransformations.reshapePairs(reverse)) {
                InWorldRecipeCatalog.Entry entry = byId.get((reverse ? "reshape_reverse/" : "reshape/") + InWorldTransformations.id(pair[0]));
                helper.assertTrue(entry != null && entry.output().items().get(0) == pair[1].asItem(),
                        "reshape " + InWorldTransformations.id(pair[0]) + (reverse ? " (reverse)" : "") + " missing in JEI");
            }
        }
        String reshapes = reshapeTo(byId, "reshape/minecraft:stone") + "/" + reshapeTo(byId, "reshape/minecraft:stone_stairs")
                + "/" + reshapeTo(byId, "reshape_reverse/minecraft:stone_slab") + "/" + reshapeTo(byId, "reshape_reverse/minecraft:oak_stairs")
                + "/" + reshapeTo(byId, "reshape/minecraft:glass");
        helper.assertTrue(reshapes.equals("minecraft:stone_stairs/minecraft:stone_slab/minecraft:stone_stairs/minecraft:oak_planks/-"),
                "stone>stairs / stairs>slab / slab<stairs / oak stairs<planks / glass: expected "
                        + "minecraft:stone_stairs/minecraft:stone_slab/minecraft:stone_stairs/minecraft:oak_planks/- but was " + reshapes);

        InWorldRecipeCatalog.Entry crush = byId.get("diamond_crush/minecraft:diamond_block");
        helper.assertTrue(crush != null && crush.output().items().get(0) == ModItems.DIAMOND_PEBBLE && crush.output().count() == 81,
                "diamond block -> 81 diamond pebbles in JEI");
        InWorldRecipeCatalog.Entry wool = byId.get("shear_wool");
        helper.assertTrue(wool != null && wool.inputs().get(0).items().size() == 16
                        && wool.output().items().get(0) == net.minecraft.world.item.Items.STRING && wool.output().count() == 4
                        && wool.tools().equals(java.util.List.of(net.minecraft.world.item.Items.SHEARS)),
                "shears on any of the 16 wools -> 4 string in JEI");

        JsonObject trim = described.getAsJsonObject("trimTemplate");
        long vanillaTemplates = BuiltInRegistries.ITEM.stream()
                .filter(i -> BuiltInRegistries.ITEM.getKey(i).getPath().endsWith("_armor_trim_smithing_template")).count();
        java.util.List<Item> hammers = BuiltInRegistries.ITEM.stream()
                .filter(i -> i instanceof SledgehammerItem && BuiltInRegistries.ITEM.getKey(i).getNamespace().equals("simplebuilding"))
                .toList();
        helper.assertTrue(vanillaTemplates == 18 && trim.getAsJsonArray("templates").size() == 18,
                "18 vanilla armor trim templates, all exported: registry " + vanillaTemplates
                        + ", export " + trim.getAsJsonArray("templates").size());
        for (Object[] upgrade : new Object[][]{
                {net.minecraft.world.item.Items.GLOW_INK_SAC, ModItems.GLOWING_TRIM_TEMPLATE},
                {net.minecraft.world.item.Items.GLOWSTONE_DUST, ModItems.EMITTING_TRIM_TEMPLATE}}) {
            Item result = (Item) upgrade[1];
            InWorldRecipeCatalog.Entry entry = byId.get("trim_template/" + InWorldTransformations.id(result));
            helper.assertTrue(entry != null && entry.inputs().size() == 2
                            && entry.inputs().get(0).items().size() == 18
                            && entry.inputs().get(0).items().contains(net.minecraft.world.item.Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE)
                            && !entry.inputs().get(0).items().contains(ModItems.GLOWING_TRIM_TEMPLATE)
                            && entry.inputs().get(1).items().equals(java.util.List.of((Item) upgrade[0]))
                            && entry.inputs().get(1).count() == 1
                            && entry.output().items().get(0) == result && entry.output().count() == 1
                            && entry.tools().size() == hammers.size() && entry.tools().containsAll(hammers)
                            && entry.tools().contains(ModItems.STONE_SLEDGEHAMMER),
                    "any trim template + " + InWorldTransformations.id((Item) upgrade[0]) + " under any sledgehammer -> "
                            + InWorldTransformations.id(result) + " in JEI, but was " + describe(entry));
        }
        helper.assertTrue(catalog.of(InWorldRecipeCatalog.Kind.TRIM_TEMPLATE).size() == 2,
                "two trim template upgrades in JEI: " + catalog.of(InWorldRecipeCatalog.Kind.TRIM_TEMPLATE).size());

        InWorldRecipeCatalog.Entry wash = byId.get("cauldron_wash");
        java.util.List<Item> washed = wash == null ? java.util.List.of() : wash.inputs().get(0).items();
        helper.assertTrue(wash != null && washed.size() == 16
                        && washed.stream().allMatch(i -> i instanceof com.simplebuilding.items.custom.OctantItem && i != ModItems.OCTANT)
                        && washed.contains(ModItems.COLORED_OCTANT_ITEMS.get(net.minecraft.world.item.DyeColor.RED))
                        && wash.output().items().get(0) == ModItems.OCTANT && wash.output().count() == 1
                        && wash.tools().equals(java.util.List.of(net.minecraft.world.item.Items.CAULDRON)),
                "any of the 16 coloured octants at a cauldron -> the plain octant in JEI, but was " + describe(wash));
        helper.assertTrue(described.getAsJsonObject("cauldronWash").get("waterLevels").getAsInt() == 1,
                "washing costs one water level");
        TestCleanup.succeed(helper);
    }

    private static String describe(InWorldRecipeCatalog.Entry entry) {
        if (entry == null) {
            return "missing";
        }
        return entry.inputs().stream().map(s -> s.items().size() + "x" + s.count()).toList()
                + " tools " + entry.tools().size() + " -> " + entry.output().items() + " x" + entry.output().count();
    }

    private static String reshapeTo(java.util.Map<String, InWorldRecipeCatalog.Entry> byId, String id) {
        InWorldRecipeCatalog.Entry entry = byId.get(id);
        return entry == null ? "-" : InWorldTransformations.id(entry.output().items().get(0));
    }

    private static boolean contains(JsonObject table, String key, String pair) {
        for (JsonElement element : table.getAsJsonArray(key)) {
            if (element.toString().equals(pair)) {
                return true;
            }
        }
        return false;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
    }
}
