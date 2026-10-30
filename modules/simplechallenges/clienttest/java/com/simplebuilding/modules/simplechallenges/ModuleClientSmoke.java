package com.simplebuilding.modules.simplechallenges;

import com.simplebuilding.modules.simplechallenges.client.ChallengeScreenAccess;
import com.simplebuilding.modules.simplechallenges.client.CreateChallenge;
import com.simplebuilding.modules.simplechallenges.client.SelectChallengeScreen;
import com.simplebuilding.modules.simplechallenges.client.StartAreaEditorScreen;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

/** Client smoke: the module boots, a world without a chosen challenge plays as usual (safety lock), the status command answers; the world creation screen offers Simple Challenges and its screens open. */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplechallenges")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplechallenges-title");
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("simplechallenges status");
            context.takeScreenshot("simplechallenges-no-challenge");
        }
        // S3: the world type "Simple Challenges", its row on the World tab, the Select Challenge screen and the start area editor open
        context.runOnClient(client -> CreateWorldScreen.openFresh(client, () -> client.gui.setScreen(null)));
        context.waitFor(client -> client.gui.screen() instanceof CreateWorldScreen);
        context.runOnClient(client -> {
            CreateWorldScreen screen = (CreateWorldScreen) client.gui.screen();
            var entry = screen.getUiState().getNormalPresetList().stream().filter(e -> e.preset() != null && e.preset().is(CreateChallenge.WORLD_TYPE)).findFirst()
                    .orElseThrow(() -> new AssertionError("world type Simple Challenges is missing"));
            screen.getUiState().setWorldType(entry);
            ((ChallengeScreenAccess) screen).simplechallenges$openWorldTab();
        });
        context.waitTicks(5);
        context.takeScreenshot("simplechallenges-create-world");
        context.runOnClient(client -> client.gui.setScreen(new SelectChallengeScreen((CreateWorldScreen) client.gui.screen())));
        context.waitTicks(5);
        context.takeScreenshot("simplechallenges-select-challenge");
        context.runOnClient(client -> client.gui.setScreen(new StartAreaEditorScreen(client.gui.screen(), 16, List.of(new int[]{-1, -1, 0, 0}), rects -> {})));
        context.waitTicks(5);
        context.takeScreenshot("simplechallenges-start-area");
        context.runOnClient(client -> client.gui.setScreen(null));
    }
}
