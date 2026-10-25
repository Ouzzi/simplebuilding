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
    public void configCommand(GameTestHelper helper) {
        MobHeadTests.ALL.get("config_command").accept(helper);
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

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void herobrineRitual(GameTestHelper helper) {
        MobHeadTests.ALL.get("herobrine_ritual").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void herobrineRitualWrongShrines(GameTestHelper helper) {
        MobHeadTests.ALL.get("herobrine_ritual_wrong_shrines").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void herobrineHaunt(GameTestHelper helper) {
        MobHeadTests.ALL.get("herobrine_haunt").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void herobrineGaze(GameTestHelper helper) {
        MobHeadTests.ALL.get("herobrine_gaze").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void herobrineTorchFlicker(GameTestHelper helper) {
        MobHeadTests.ALL.get("herobrine_torch_flicker").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void herobrineHintBook(GameTestHelper helper) {
        MobHeadTests.ALL.get("herobrine_hint_book").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void easterHeadsData(GameTestHelper helper) {
        MobHeadTests.ALL.get("easter_heads_data").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void technobladeCrown(GameTestHelper helper) {
        MobHeadTests.ALL.get("technoblade_crown").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void technobladeLightning(GameTestHelper helper) {
        MobHeadTests.ALL.get("technoblade_lightning").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void crownedPigAbility(GameTestHelper helper) {
        MobHeadTests.ALL.get("crowned_pig_ability").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void killerBunnySpawn(GameTestHelper helper) {
        MobHeadTests.ALL.get("killer_bunny_spawn").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void killerBunnyHead(GameTestHelper helper) {
        MobHeadTests.ALL.get("killer_bunny_head").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void giantZombieLightning(GameTestHelper helper) {
        MobHeadTests.ALL.get("giant_zombie_lightning").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void giantHeadDrop(GameTestHelper helper) {
        MobHeadTests.ALL.get("giant_head_drop").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void giantHeadWearing(GameTestHelper helper) {
        MobHeadTests.ALL.get("giant_head_wearing").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void giantCowHay(GameTestHelper helper) {
        MobHeadTests.ALL.get("giant_cow_hay").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void giantCowHead(GameTestHelper helper) {
        MobHeadTests.ALL.get("giant_cow_head").accept(helper);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void jeiChargedCreeperHeads(GameTestHelper h) {
        MobHeadTests.ALL.get("jei_charged_creeper_heads").accept(h);
    }

    @GameTest(structure = "simplemobheads:empty", maxTicks = 400)
    public void wornHeadDeath(GameTestHelper helper) {
        MobHeadTests.ALL.get("worn_head_death").accept(helper);
    }
}
