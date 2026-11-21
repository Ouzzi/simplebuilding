package com.simplebuilding.modules.simplequalityoflife;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ModuleGameTest {
 /** N124: the Mod Menu settings screen names the title, every category and every option (no raw or humanized paths), in EN (loaded language) and DE (the jar's de_de.json), server and client config. */
 @GameTest(maxTicks=100) public void configLabels(GameTestHelper h){var missing=com.simplelib.api.config.ConfigScreenModel.missingLabels("simplequalityoflife"); h.assertTrue(missing.isEmpty(), "config screen without an English name: "+missing); java.util.List<String> de=new java.util.ArrayList<>(); try(var in=ModuleGameTest.class.getResourceAsStream("/assets/simplequalityoflife/lang/de_de.json")){h.assertTrue(in!=null, "no de_de.json"); var lang=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject(); for(boolean client:new boolean[]{false,true}){var spec=com.simplelib.api.config.ConfigCommands.spec("simplequalityoflife", client); if(spec!=null) de.addAll(com.simplelib.api.config.ConfigScreenModel.missingLabels(spec, lang::has));}}catch(java.io.IOException e){throw new RuntimeException(e);} h.assertTrue(de.isEmpty(), "config screen without a German name: "+de); h.succeed();}
 @GameTest(maxTicks=100) public void guideBook(GameTestHelper h){com.simplequalityoflife.guide.QolGuide.gameTest(h);}
 @GameTest(maxTicks=100) public void launch(GameTestHelper h){com.simplequalityoflife.test.QolTests.launch(h);}
 @GameTest(maxTicks=100) public void configLang(GameTestHelper h){com.simplequalityoflife.test.QolTests.configLang(h);}
 @GameTest public void subModSwitch(GameTestHelper h){com.simplequalityoflife.test.QolTests.subModSwitch(h);}
    @GameTest public void discoverRecipeUnlocks(GameTestHelper h) { com.simplequalityoflife.discover.QolDiscover.recipeUnlocks(h); }
    @GameTest public void discoverBehaviourHints(GameTestHelper h) { com.simplequalityoflife.discover.QolDiscover.behaviourHints(h); }
}
