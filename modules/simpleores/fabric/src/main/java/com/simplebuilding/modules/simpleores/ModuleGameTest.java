package com.simplebuilding.modules.simpleores;

import com.simpleores.test.OresTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link OresTests}, all on the 16 x 8 x 16 stone arena. */
public final class ModuleGameTest {
    @GameTest public void configCommand(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.module(h, "simpleores", false); }

    /** JEI24 (a-fix09b): Sage Ore, Sage Orb and Dimensional Scrap pages, N4 (b-ores13): six deposit pages; texts in EN and DE. */
    @GameTest public void jeiInfoPages(GameTestHelper h) { com.simplelib.test.ViewerInfoTests.infoPages(h, "simpleores", 9); }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void registered(GameTestHelper h) {
        OresTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void silverIsNoToolMaterial(GameTestHelper h) {
        OresTests.ALL.get("silver_is_no_tool_material").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void subModSwitch(GameTestHelper h) {
        OresTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void toolValues(GameTestHelper h) {
        OresTests.ALL.get("tool_values").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void armorValues(GameTestHelper h) {
        OresTests.ALL.get("armor_values").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void pressingRecipes(GameTestHelper h) {
        OresTests.ALL.get("pressing_recipes").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void pressingMeltsOnlyInTheBlastFurnace(GameTestHelper h) {
        OresTests.ALL.get("pressing_melts_only_in_the_blast_furnace").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void silverSmelting(GameTestHelper h) {
        OresTests.ALL.get("silver_smelting").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void oreDrops(GameTestHelper h) {
        OresTests.ALL.get("ore_drops").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void worldgenFrequency(GameTestHelper h) {
        OresTests.ALL.get("worldgen_frequency").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void guideBook(GameTestHelper h) {
        OresTests.ALL.get("guide_book").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void discoverRecipeUnlocks(GameTestHelper h) {
        OresTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void discoverBehaviourHints(GameTestHelper h) {
        OresTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void leadBlastResistance(GameTestHelper h) {
        OresTests.ALL.get("lead_blast_resistance").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void leadOccludesVibrations(GameTestHelper h) {
        OresTests.ALL.get("lead_occludes_vibrations").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void soundproofAttenuation(GameTestHelper h) {
        OresTests.ALL.get("soundproof_attenuation").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void silverMirrorRecipe(GameTestHelper h) {
        OresTests.ALL.get("silver_mirror_recipe").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void silverSheetRecipe(GameTestHelper h) {
        OresTests.ALL.get("silver_sheet_recipe").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void silverSheetPlacement(GameTestHelper h) {
        OresTests.ALL.get("silver_sheet_placement").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void silverMirrorPlacement(GameTestHelper h) {
        OresTests.ALL.get("silver_mirror_placement").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void leadPlateSoundproofing(GameTestHelper h) {
        OresTests.ALL.get("lead_plate_soundproofing").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void leadPlateRecipe(GameTestHelper h) {
        OresTests.ALL.get("lead_plate_recipe").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void leadWorldgen(GameTestHelper h) {
        OresTests.ALL.get("lead_worldgen").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void applesEatEffects(GameTestHelper h) {
        OresTests.ALL.get("apples_eat_effects").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void appleRecipes(GameTestHelper h) {
        OresTests.ALL.get("apple_recipes").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void roseGoldVeinRule(GameTestHelper h) {
        OresTests.ALL.get("rose_gold_vein_rule").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void roseGoldOreDropsAndSmelting(GameTestHelper h) {
        OresTests.ALL.get("rose_gold_ore_drops_and_smelting").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void sageCrystalChain(GameTestHelper h) {
        OresTests.ALL.get("sage_crystal_chain").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void storageBlockRecipes(GameTestHelper h) {
        OresTests.ALL.get("storage_block_recipes").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void roseCarrots(GameTestHelper h) {
        OresTests.ALL.get("rose_carrots").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void metalRecipes(GameTestHelper h) {
        OresTests.ALL.get("metal_recipes").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void metalsAreCosmetic(GameTestHelper h) {
        OresTests.ALL.get("metals_are_cosmetic").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void metalBlocks(GameTestHelper h) {
        OresTests.ALL.get("metal_blocks").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void metalWorldgenDrops(GameTestHelper h) {
        OresTests.ALL.get("metal_worldgen_drops").accept(h);
    }

    /** N4 (b-ores13): closed zinc mini geodes in the Overworld and the End, 1 to 8 ores, never at air or water. */
    @GameTest(structure = "simpleores:arena", maxTicks = 200)
    public void zincGeodes(GameTestHelper h) {
        OresTests.ALL.get("zinc_geodes").accept(h);
    }

    /** N4 (b-ores13): tin-bearing gravel/sand (shovel, 2 to 5 nuggets, rivers and beaches), End ore and crust drops. */
    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void tinPlacers(GameTestHelper h) {
        OresTests.ALL.get("tin_placers").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void riftShardSmelting(GameTestHelper h) {
        OresTests.ALL.get("rift_shard_smelting").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void riftChain(GameTestHelper h) {
        OresTests.ALL.get("rift_chain").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void riftStorageReversible(GameTestHelper h) {
        OresTests.ALL.get("rift_storage_reversible").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void riftBeaconTags(GameTestHelper h) {
        OresTests.ALL.get("rift_beacon_tags").accept(h);
    }

    /** N25 (b-metals25): nuggets of the plain alloys, stacked plates, mirror faces, bars and grates, metal sounds. */
    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void mixNuggets(GameTestHelper h) {
        OresTests.ALL.get("mix_nuggets").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void plateLayers(GameTestHelper h) {
        OresTests.ALL.get("plate_layers").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void mirrorFaces(GameTestHelper h) {
        OresTests.ALL.get("mirror_faces").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void metalDoors(GameTestHelper h) {
        OresTests.ALL.get("metal_doors").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void barsAndGrates(GameTestHelper h) {
        OresTests.ALL.get("bars_and_grates").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void metalSounds(GameTestHelper h) {
        OresTests.ALL.get("metal_sounds").accept(h);
    }
}
