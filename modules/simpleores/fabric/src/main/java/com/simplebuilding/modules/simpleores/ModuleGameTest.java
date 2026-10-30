package com.simplebuilding.modules.simpleores;

import com.simpleores.test.OresTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link OresTests}, all on the 16 x 8 x 16 stone arena. */
public final class ModuleGameTest {
    @GameTest public void configCommand(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.module(h, "simpleores", false); }

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
    public void silverArrowRecipe(GameTestHelper h) {
        OresTests.ALL.get("silver_arrow_recipe").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void silverArrowUndeadBonus(GameTestHelper h) {
        OresTests.ALL.get("silver_arrow_undead_bonus").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void silverMirrorPlacement(GameTestHelper h) {
        OresTests.ALL.get("silver_mirror_placement").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void leadWorldgen(GameTestHelper h) {
        OresTests.ALL.get("lead_worldgen").accept(h);
    }
}
