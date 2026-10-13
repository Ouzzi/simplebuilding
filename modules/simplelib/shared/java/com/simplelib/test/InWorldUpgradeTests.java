package com.simplelib.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.OctetCells;
import com.simplelib.api.SimpleLibApi;
import com.simplelib.crucible.CrucibleBlockEntity;
import com.simplelib.registry.LibBlocks;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The SimpleLib services of the module split, stage 3c: the {@code in_world_upgrades} data type, the upgrade
 * handlers, the tool tags and the octet cells. Every test also runs standalone (no SimpleBuilding) and checks the
 * partner's entries only when SimpleBuilding is there (looked up by id, principle 3).
 */
public final class InWorldUpgradeTests {
    private static final String TEST_DEFAULTS = "simpletest_defaults";

    private InWorldUpgradeTests() {
    }

    private static boolean simplebuilding() {
        return BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath("simplebuilding", "iron_sledgehammer"));
    }

    private static JsonElement json(String text) {
        return JsonParser.parseString(text);
    }

    /**
     * Files apply defaults first, then the rest in id order; later entries win, {@code remove} takes entries out,
     * unknown ids skip the entry, a broken file keeps what it had before the break; defaults and the old field name
     * {@code min_hammer} are read; the effective table is untouched by {@link InWorldUpgrades#parse}.
     */
    public static void dataType(GameTestHelper h) {
        InWorldUpgrades.registerDefaults(TEST_DEFAULTS);
        Map<Block, InWorldUpgrades.Entry> before = InWorldUpgrades.table();
        Map<Identifier, JsonElement> files = new HashMap<>();
        files.put(Identifier.fromNamespaceAndPath(TEST_DEFAULTS, "base"), json("{\"upgrades\":["
                + "{\"from\":\"minecraft:cauldron\",\"to\":\"simplelib:reinforced_cauldron\",\"material\":\"minecraft:diamond\","
                + "\"material_count\":8,\"min_hammer\":\"diamond\",\"tier\":\"reinforced\"},"
                + "{\"from\":\"minecraft:furnace\",\"to\":\"minecraft:blast_furnace\",\"material\":\"minecraft:iron_ingot\",\"damage_per_hit\":4},"
                + "{\"from\":\"minecraft:anvil\",\"to\":\"minecraft:chipped_anvil\",\"material\":\"minecraft:iron_ingot\"}]}"));
        // "apack" sorts before "simpletest_defaults", yet applies after it: defaults first.
        files.put(Identifier.fromNamespaceAndPath("apack", "extra"), json("{\"upgrades\":["
                + "{\"from\":\"minecraft:furnace\",\"to\":\"minecraft:smoker\",\"material\":\"minecraft:gold_ingot\","
                + "\"tool\":\"#minecraft:pickaxes\",\"min_tool\":\"netherite\",\"duration_factor\":3},"
                + "{\"from\":\"nomod:thing\",\"to\":\"minecraft:stone\",\"material\":\"minecraft:stick\"},"
                + "{\"from\":\"minecraft:stone\",\"to\":\"minecraft:cobblestone\",\"material\":\"nomod:item\"}],"
                + "\"remove\":[\"minecraft:anvil\"]}"));
        files.put(Identifier.fromNamespaceAndPath("apack", "broken"), json("{\"upgrades\":["
                + "{\"from\":\"minecraft:dirt\",\"to\":\"minecraft:coarse_dirt\",\"material\":\"minecraft:stick\"},"
                + "{\"to\":\"minecraft:gravel\"}]}"));
        Map<Block, InWorldUpgrades.Entry> table = InWorldUpgrades.parse(files);

        InWorldUpgrades.Entry cauldron = table.get(Blocks.CAULDRON);
        h.assertTrue(cauldron != null && cauldron.to() == LibBlocks.REINFORCED_CAULDRON && cauldron.material() == Items.DIAMOND
                        && cauldron.materialCount() == 8 && cauldron.durationFactor() == 1 && cauldron.damagePerHit() == 1
                        && cauldron.minTool().equals("diamond") && cauldron.tier().equals("reinforced")
                        && cauldron.tool().equals(InWorldUpgrades.UPGRADE_TOOLS),
                "cauldron entry misread (defaults, min_hammer alias): " + cauldron);
        InWorldUpgrades.Entry furnace = table.get(Blocks.FURNACE);
        h.assertTrue(furnace != null && furnace.to() == Blocks.SMOKER && furnace.material() == Items.GOLD_INGOT
                        && furnace.minTool().equals("netherite") && furnace.durationFactor() == 3 && furnace.damagePerHit() == 1
                        && furnace.tool().equals(TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("pickaxes"))),
                "the later file did not win for the furnace: " + furnace);
        h.assertTrue(!table.containsKey(Blocks.ANVIL), "\"remove\" did not take the anvil out");
        h.assertTrue(!table.containsKey(Blocks.STONE), "an entry with an unknown material was kept");
        h.assertTrue(table.get(Blocks.DIRT) != null && table.get(Blocks.DIRT).minTool().equals("any"),
                "a broken file lost the entries before its break");
        h.assertValueEqual(table.size(), 3, "entries");
        h.assertTrue(InWorldUpgrades.table() == before, "parse changed the effective table");

        // The tool: an own tag, or the default tag (hammers; axes only while the Vanilla axe ways are on).
        h.assertTrue(InWorldUpgrades.toolFits(furnace, new ItemStack(Items.DIAMOND_PICKAXE)), "pickaxe tag not honoured");
        h.assertTrue(!InWorldUpgrades.toolFits(furnace, new ItemStack(Items.STICK)), "a stick fits the pickaxe tag");
        h.assertValueEqual(InWorldUpgrades.toolFits(cauldron, new ItemStack(Items.IRON_AXE)), SimpleLibApi.axeWaysEnabled(),
                "an axe fits the default tool exactly while the axe ways are on");
        h.assertTrue(!SimpleLibApi.isHammer(new ItemStack(Items.IRON_AXE)), "an axe counts as a hammer");
        if (simplebuilding()) {
            ItemStack hammer = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simplebuilding", "iron_sledgehammer")));
            h.assertTrue(SimpleLibApi.isHammer(hammer) && SimpleLibApi.isUpgradeTool(hammer) && InWorldUpgrades.toolFits(cauldron, hammer),
                    "SimpleBuilding's sledgehammer is no #simplelib:hammers / upgrade tool");
            h.assertTrue(!SimpleLibApi.axeWaysEnabled(), "SimpleBuilding is loaded, yet the axe ways are on");
            InWorldUpgrades.Entry chest = InWorldUpgrades.entryFor(BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("copper_chest")));
            h.assertTrue(chest != null && chest.material() == BuiltInRegistries.ITEM.getValue(
                            Identifier.fromNamespaceAndPath("simplebuilding", "cracked_diamond")) && chest.tier().equals("reinforced"),
                    "SimpleBuilding's in_world_upgrades files did not reach the effective table: " + chest);
        } else {
            h.assertTrue(SimpleLibApi.axeWaysEnabled() && SimpleLibApi.isUpgradeTool(new ItemStack(Items.IRON_AXE)),
                    "standalone: the axe is no upgrade tool");
            h.assertTrue(!SimpleLibApi.isUpgradeTool(new ItemStack(Items.DIAMOND_PICKAXE)), "standalone: a pickaxe is an upgrade tool");
        }
        h.succeed();
    }

    /**
     * {@link InWorldUpgrades#replace}: SimpleLib's own handler moves a crucible's contents into the next tier; a block
     * without handler keeps its properties; a registered handler is asked for its targets and reports its block count.
     */
    public static void handlers(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        h.setBlock(rel.below(), Blocks.STONE);
        h.setBlock(rel, LibBlocks.IRON_CRUCIBLE.defaultBlockState());
        BlockPos abs = h.absolutePos(rel);
        CrucibleBlockEntity be = (CrucibleBlockEntity) h.getLevel().getBlockEntity(abs);
        h.assertTrue(be != null, "no crucible block entity");
        be.setItem(4, new ItemStack(Items.RAW_COPPER, 3));
        h.assertTrue(InWorldUpgrades.hasHandler(LibBlocks.REINFORCED_CRUCIBLE), "SimpleLib registered no crucible handler");
        h.assertValueEqual(InWorldUpgrades.replace(h.getLevel(), abs, LibBlocks.REINFORCED_CRUCIBLE), 1, "crucible blocks changed");
        CrucibleBlockEntity fresh = (CrucibleBlockEntity) h.getLevel().getBlockEntity(abs);
        h.assertTrue(h.getLevel().getBlockState(abs).is(LibBlocks.REINFORCED_CRUCIBLE) && fresh != null
                        && fresh.getItem(4).is(Items.RAW_COPPER) && fresh.getItem(4).getCount() == 3,
                "the crucible handler lost the contents");

        BlockPos furnaceRel = new BlockPos(3, 2, 1);
        h.setBlock(furnaceRel, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.EAST));
        BlockPos furnaceAbs = h.absolutePos(furnaceRel);
        h.assertTrue(!InWorldUpgrades.hasHandler(Blocks.BLAST_FURNACE), "a handler claims the blast furnace");
        h.assertValueEqual(InWorldUpgrades.replace(h.getLevel(), furnaceAbs, Blocks.BLAST_FURNACE), 1, "default blocks changed");
        BlockState blast = h.getLevel().getBlockState(furnaceAbs);
        h.assertTrue(blast.is(Blocks.BLAST_FURNACE) && blast.getValue(FurnaceBlock.FACING) == Direction.EAST,
                "the default move lost the facing: " + blast);

        // A partner handler (target: a block nothing upgrades into), asked before the default move.
        SimpleLibApi.registerUpgradeHandler(block -> block == Blocks.REINFORCED_DEEPSLATE, (level, pos, to) -> {
            level.setBlock(pos, Blocks.REINFORCED_DEEPSLATE.defaultBlockState(), Block.UPDATE_ALL);
            return 2;
        });
        BlockPos stoneRel = new BlockPos(1, 2, 3);
        h.setBlock(stoneRel, Blocks.STONE);
        h.assertValueEqual(InWorldUpgrades.replace(h.getLevel(), h.absolutePos(stoneRel), Blocks.REINFORCED_DEEPSLATE), 2,
                "the partner handler was not asked");
        h.assertBlockPresent(Blocks.REINFORCED_DEEPSLATE, stoneRel);
        h.succeed();
    }

    /** {@link OctetCells}: register, look up both ways, unregister; SimpleBuilding's wood octets when it is there. */
    public static void octetCells(GameTestHelper h) {
        int size = OctetCells.size();
        Item piece = Items.STRUCTURE_VOID;
        OctetCells.register(Blocks.BEDROCK, Blocks.STRUCTURE_VOID, () -> piece);
        try {
            h.assertTrue(OctetCells.cellFor(Blocks.BEDROCK) == Blocks.STRUCTURE_VOID, "cellFor misses the registered cell");
            h.assertTrue(OctetCells.pieceOf(Blocks.STRUCTURE_VOID) == piece, "pieceOf misses the registered piece");
            h.assertTrue(OctetCells.baseOf(Blocks.STRUCTURE_VOID) == Blocks.BEDROCK, "baseOf misses the base");
            h.assertValueEqual(OctetCells.size(), size + 1, "materials after register");
        } finally {
            OctetCells.unregister(Blocks.BEDROCK);
        }
        h.assertTrue(OctetCells.cellFor(Blocks.BEDROCK) == null && OctetCells.pieceOf(Blocks.STRUCTURE_VOID) == null,
                "unregister left the cell behind");
        h.assertTrue(OctetCells.cellFor(null) == null && OctetCells.pieceOf(Blocks.STONE) == null, "lookups of nothing answered");
        if (simplebuilding()) {
            Block cell = OctetCells.cellFor(Blocks.OAK_PLANKS);
            h.assertTrue(cell != null && OctetCells.pieceOf(cell) != null && OctetCells.cellFor(Blocks.MELON) != null,
                    "SimpleBuilding's octet cells are not registered");
        }
        h.succeed();
    }
}
