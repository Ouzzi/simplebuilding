package com.simplebuilding.modules.simplemobheads;

import com.simplemobheads.test.MobHeadTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: one @GameTest per entry of {@link MobHeadTests#ALL} (ids module_game_test_*). */
public final class ModuleGameTest {
    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void registered(GameTestHelper helper) {
        MobHeadTests.ALL.get("registered").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void mobsHaveHeads(GameTestHelper helper) {
        MobHeadTests.ALL.get("mobs_have_heads").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void soundsAndNames(GameTestHelper helper) {
        MobHeadTests.ALL.get("sounds_and_names").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void creeperDropsEveryHead(GameTestHelper helper) {
        MobHeadTests.ALL.get("creeper_drops_every_head").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void variantsDropWithTheHead(GameTestHelper helper) {
        MobHeadTests.ALL.get("variants_drop_with_the_head").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void babiesDropTheNormalHead(GameTestHelper helper) {
        MobHeadTests.ALL.get("babies_drop_the_normal_head").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void otherDeathsDropNothing(GameTestHelper helper) {
        MobHeadTests.ALL.get("other_deaths_drop_nothing").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void bossHeads(GameTestHelper helper) {
        MobHeadTests.ALL.get("boss_heads").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void playerKillChance(GameTestHelper helper) {
        MobHeadTests.ALL.get("player_kill_chance").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void pvpPlayerHead(GameTestHelper helper) {
        MobHeadTests.ALL.get("pvp_player_head").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void placedHeadKeepsItsVariant(GameTestHelper helper) {
        MobHeadTests.ALL.get("placed_head_keeps_its_variant").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void oldIdsMove(GameTestHelper helper) {
        MobHeadTests.ALL.get("old_ids_move").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void funConfigIsAdopted(GameTestHelper helper) {
        MobHeadTests.ALL.get("fun_config_is_adopted").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void movedAbilities(GameTestHelper helper) {
        MobHeadTests.ALL.get("moved_abilities").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void animalAbilities(GameTestHelper helper) {
        MobHeadTests.ALL.get("animal_abilities").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void newAbilities(GameTestHelper helper) {
        MobHeadTests.ALL.get("new_abilities").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void extraHeadSlot(GameTestHelper helper) {
        MobHeadTests.ALL.get("extra_head_slot").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void extraHeadGreeting(GameTestHelper helper) {
        MobHeadTests.ALL.get("extra_head_greeting").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void extraHeadVariantLook(GameTestHelper helper) {
        MobHeadTests.ALL.get("extra_head_variant_look").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void abilitySwitches(GameTestHelper helper) {
        MobHeadTests.ALL.get("ability_switches").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void greeting(GameTestHelper helper) {
        MobHeadTests.ALL.get("greeting").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void commands(GameTestHelper helper) {
        MobHeadTests.ALL.get("commands").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void creativeTabs(GameTestHelper helper) {
        MobHeadTests.ALL.get("creative_tabs").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void subModSwitch(GameTestHelper helper) {
        MobHeadTests.ALL.get("sub_mod_switch").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void guideBook(GameTestHelper helper) {
        MobHeadTests.ALL.get("guide_book").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void discoverRecipeUnlocks(GameTestHelper helper) {
        MobHeadTests.ALL.get("discover_recipe_unlocks").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void discoverBehaviourHints(GameTestHelper helper) {
        MobHeadTests.ALL.get("discover_behaviour_hints").accept(helper);
    }
}
