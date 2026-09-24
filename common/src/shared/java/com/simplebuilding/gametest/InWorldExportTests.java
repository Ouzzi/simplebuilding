package com.simplebuilding.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
        helper.succeed();
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
        helper.succeed();
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
        helper.succeed();
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
