package com.simplebuilding.modules.simplequestionary;

import com.simplequestionary.test.QuestionaryTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link QuestionaryTests}. */
public final class ModuleGameTest {
    @GameTest
    public void registered(GameTestHelper h) {
        QuestionaryTests.ALL.get("registered").accept(h);
    }

    @GameTest
    public void recipe(GameTestHelper h) {
        QuestionaryTests.ALL.get("recipe").accept(h);
    }

    @GameTest
    public void menuOpens(GameTestHelper h) {
        QuestionaryTests.ALL.get("menu_opens").accept(h);
    }

    @GameTest
    public void slotTakesItem(GameTestHelper h) {
        QuestionaryTests.ALL.get("slot_takes_item").accept(h);
    }

    @GameTest
    public void dataLoaded(GameTestHelper h) {
        QuestionaryTests.ALL.get("data_loaded").accept(h);
    }

    @GameTest
    public void fallbackDeterministic(GameTestHelper h) {
        QuestionaryTests.ALL.get("fallback_deterministic").accept(h);
    }

    @GameTest
    public void families(GameTestHelper h) {
        QuestionaryTests.ALL.get("families").accept(h);
    }

    @GameTest
    public void facts(GameTestHelper h) {
        QuestionaryTests.ALL.get("facts").accept(h);
    }

    @GameTest
    public void coverage(GameTestHelper h) {
        QuestionaryTests.ALL.get("coverage").accept(h);
    }

    @GameTest
    public void hostSwitch(GameTestHelper h) {
        QuestionaryTests.ALL.get("host_switch").accept(h);
    }
    @GameTest public void discoverRecipeUnlocks(GameTestHelper h) { com.simplequestionary.discover.QuestionaryDiscover.recipeUnlocks(h); }
    @GameTest public void discoverBehaviourHints(GameTestHelper h) { com.simplequestionary.discover.QuestionaryDiscover.behaviourHints(h); }
}
