package com.simplelib.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.RecipeViewer;
import com.simplelib.api.SimpleLibApi;
import com.simplelib.compat.jei.InWorldUpgrade;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * JEI24: SimpleLib's recipe viewer data ({@link RecipeViewer}) without a client - the upgrade table as tab entries, the
 * loot table reader and that every effective table entry reaches the tab.
 */
public final class RecipeViewerTests {
    private RecipeViewerTests() {
    }

    /**
     * A table entry becomes one tab entry: block + material (count) in, the next block out; the tools follow the tool
     * tag and, while the axe ways are on, the axes within the {@code min_tool} grade. Every entry of the effective table
     * that has items, a tool and is not the partner's reaches {@link RecipeViewer#tableEntries}.
     */
    public static void tableEntries(GameTestHelper h) {
        Map<Identifier, JsonElement> files = Map.of(Identifier.fromNamespaceAndPath("jeitest", "a"), JsonParser.parseString("{\"upgrades\":["
                + "{\"from\":\"minecraft:cauldron\",\"to\":\"minecraft:barrel\",\"material\":\"minecraft:iron_ingot\",\"material_count\":2,"
                + "\"min_tool\":\"diamond\",\"damage_per_hit\":3},"
                + "{\"from\":\"minecraft:furnace\",\"to\":\"minecraft:smoker\",\"material\":\"minecraft:coal\",\"tool\":\"#minecraft:pickaxes\"}]}"));
        Map<Block, InWorldUpgrades.Entry> table = InWorldUpgrades.parse(files);
        List<InWorldUpgrade> entries = RecipeViewer.fromTable(table);
        InWorldUpgrade cauldron = entries.stream().filter(e -> e.id().equals("table/minecraft/cauldron")).findFirst().orElse(null);
        boolean axes = SimpleLibApi.axeWaysEnabled();
        if (axes || !RecipeViewer.tagged(InWorldUpgrades.UPGRADE_TOOLS).isEmpty()) {
            h.assertTrue(cauldron != null, "cauldron entry missing: " + entries);
            h.assertTrue(cauldron.inputs().size() == 2 && cauldron.inputs().get(0).items().equals(List.of(Items.CAULDRON))
                            && cauldron.inputs().get(1).items().equals(List.of(Items.IRON_INGOT)) && cauldron.inputs().get(1).count() == 2
                            && cauldron.output().is(Items.BARREL) && cauldron.notes().size() == 3,
                    "cauldron entry misbuilt: " + cauldron);
            if (axes) {
                h.assertTrue(cauldron.tools().contains(Items.DIAMOND_AXE) && cauldron.tools().contains(Items.NETHERITE_AXE)
                        && !cauldron.tools().contains(Items.IRON_AXE), "axes within the diamond grade: " + cauldron.tools());
            }
        }
        InWorldUpgrade furnace = entries.stream().filter(e -> e.id().equals("table/minecraft/furnace")).findFirst().orElse(null);
        h.assertTrue(furnace != null && furnace.tools().contains(Items.IRON_PICKAXE) && !furnace.tools().contains(Items.DIAMOND_AXE),
                "a tool tag of its own takes only its tools: " + furnace);
        // The effective table: every entry with items and a tool is in the tab, none twice.
        List<InWorldUpgrade> live = RecipeViewer.tableEntries();
        for (InWorldUpgrades.Entry entry : InWorldUpgrades.table().values()) {
            if (entry.from().asItem() == Items.AIR || entry.to().asItem() == Items.AIR || RecipeViewer.tools(entry).isEmpty()) continue;
            Identifier key = BuiltInRegistries.BLOCK.getKey(entry.from());
            long found = live.stream().filter(e -> e.id().equals("table/" + key.getNamespace() + "/" + key.getPath())).count();
            h.assertTrue(found <= 1, "table entry twice: " + key);
        }
        h.succeed();
    }

    /** The loot reader: item entries, item tags, nested children, a referenced table; the Vanilla zombie from the jar. */
    public static void lootItems(GameTestHelper h) {
        JsonElement table = JsonParser.parseString("{\"type\":\"minecraft:entity\",\"pools\":["
                + "{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"minecraft:bone\"},"
                + "{\"type\":\"minecraft:empty\"},{\"type\":\"minecraft:item\",\"name\":\"nomod:nothing\"}]},"
                + "{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:alternatives\",\"children\":["
                + "{\"type\":\"minecraft:tag\",\"name\":\"minecraft:wool\",\"expand\":true},"
                + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:string\"}]}]},"
                + "{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:loot_table\",\"value\":\"minecraft:entities/zombie\"}]}]}");
        List<Item> items = RecipeViewer.lootItems(table, "minecraft", Items.class, 0);
        h.assertTrue(items.contains(Items.BONE) && items.contains(Items.STRING) && items.stream().anyMatch(i -> new net.minecraft.world.item.ItemStack(i).is(net.minecraft.tags.ItemTags.WOOL))
                && items.contains(Items.ROTTEN_FLESH) && !items.contains(Items.AIR), "loot items misread: " + items);
        h.assertTrue(items.indexOf(Items.BONE) == items.lastIndexOf(Items.BONE), "each item once: " + items);
        JsonElement zombie = RecipeViewer.readLootTable(Identifier.withDefaultNamespace("entities/zombie"), Items.class);
        h.assertTrue(zombie != null, "Vanilla loot table readable from the game jar");
        h.assertTrue(RecipeViewer.readLootTable(Identifier.fromNamespaceAndPath("nomod", "entities/none"), Items.class) == null,
                "a missing table is null, no exception");
        h.succeed();
    }

    /** The partner's blocks stay out of the shared tab; everything else stays in. */
    public static void partnerShown(GameTestHelper h) {
        Map<Identifier, JsonElement> files = Map.of(Identifier.fromNamespaceAndPath("jeitest", "b"), JsonParser.parseString("{\"upgrades\":["
                + "{\"from\":\"minecraft:dirt\",\"to\":\"minecraft:coarse_dirt\",\"material\":\"minecraft:stick\",\"tool\":\"#minecraft:shovels\"},"
                + "{\"from\":\"minecraft:gravel\",\"to\":\"minecraft:sand\",\"material\":\"minecraft:stick\",\"tool\":\"#minecraft:shovels\"}]}"));
        Map<Block, InWorldUpgrades.Entry> table = InWorldUpgrades.parse(files);
        java.util.function.Predicate<Block> keep = RecipeViewer.partnerShowsNow();
        try {
            RecipeViewer.shownByPartner(block -> keep.test(block) || block == Blocks.DIRT);
            List<InWorldUpgrade> entries = RecipeViewer.fromTable(table);
            h.assertTrue(entries.stream().noneMatch(e -> e.id().equals("table/minecraft/dirt"))
                    && entries.stream().anyMatch(e -> e.id().equals("table/minecraft/gravel")), "partner exclusion: " + entries);
        } finally {
            RecipeViewer.shownByPartner(keep);
        }
        h.succeed();
    }
}
