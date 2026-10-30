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

/** Client smoke: the module boots, a world without a chosen challenge plays as usual (safety lock), the status command answers; the world creation screen offers Simple Challenges and its screens open, a Skyline choice makes the dimensions void (S5-S8). */
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
        java.util.concurrent.atomic.AtomicReference<CreateWorldScreen> create = new java.util.concurrent.atomic.AtomicReference<>();
        context.runOnClient(client -> {
            create.set((CreateWorldScreen) client.gui.screen());
            client.gui.setScreen(new SelectChallengeScreen(create.get()));
        });
        context.waitTicks(5);
        context.takeScreenshot("simplechallenges-select-challenge");
        // S5-S8: a Skyline choice empties Overworld and Nether (the End stays Vanilla), a plain choice puts the normal world back
        context.runOnClient(client -> {
            if (!CreateChallenge.choose(create.get(), "skyline_block")) throw new AssertionError("skyline_block cannot be chosen");
            var dims = create.get().getUiState().getSettings().selectedDimensions();
            if (!(dims.overworld() instanceof net.minecraft.world.level.levelgen.FlatLevelSource)
                    || !dims.get(net.minecraft.world.level.dimension.LevelStem.NETHER).map(stem -> stem.generator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource).orElse(false)) {
                throw new AssertionError("a Skyline choice must make Overworld and Nether void");
            }
        });
        context.waitTicks(5);
        context.takeScreenshot("simplechallenges-skyline-choice");
        context.runOnClient(client -> {
            if (!CreateChallenge.choose(create.get(), "lucky_strike")) throw new AssertionError("lucky_strike cannot be chosen");
            if (create.get().getUiState().getSettings().selectedDimensions().overworld() instanceof net.minecraft.world.level.levelgen.FlatLevelSource) throw new AssertionError("Lucky plays in the normal world");
        });
        context.runOnClient(client -> client.gui.setScreen(new StartAreaEditorScreen(client.gui.screen(), 16, List.of(new int[]{-1, -1, 0, 0}), rects -> {})));
        context.waitTicks(5);
        context.takeScreenshot("simplechallenges-start-area");
        context.runOnClient(client -> client.gui.setScreen(null));
    }
}
