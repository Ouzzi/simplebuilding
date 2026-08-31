package com.simplebuilding.clienttest;

import java.nio.file.Path;
import java.util.Map;

import com.simplebuilding.items.custom.BuildingWandItem;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Proves that {@code BuildingWandPreviewRenderer} still puts its translucent ghost blocks on screen
 * after the 26.2 rewrite onto {@code SubmitNodeCollector.submitCustomGeometry}.
 *
 * <p>Trigger conditions taken from the renderer:
 * <ul>
 *   <li>a {@link BuildingWandItem} in the <em>main</em> hand,</li>
 *   <li>a block hit result (the preview plane is derived from hit position and hit face),</li>
 *   <li>a non-empty preview map, which needs a block item as material - the wand looks in the
 *       offhand first, then the hotbar, so the test puts stone in the offhand,</li>
 *   <li>replaceable blocks at the preview positions; the renderer skips everything else.</li>
 * </ul>
 *
 * <p>The netherite wand is used on purpose: its diameter of 11 produces an 11x11 plane of ghost
 * blocks one block in front of the wall, which covers a large part of the viewport. Together with
 * the offhand material being present <em>before</em> the baseline screenshot, the only thing that
 * changes between the two screenshots is the wand in the main hand.
 */
public final class BuildingWandPreviewClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            RendererTestScene.build(context, singleplayer, "minecraft:stone", "creative");

            try {
                // Material for the preview goes in before the baseline so it is not part of the diff.
                singleplayer.getServer().runCommand(
                        "item replace entity @a weapon.offhand with minecraft:stone 64");
                singleplayer.getConnection().waitForClientboundPackets();
                context.waitTicks(15);

                Path baseline = context.takeScreenshot("wand-a-no-wand");
                context.waitTicks(10);
                Path baselineAgain = context.takeScreenshot("wand-b-no-wand-again");

                ScreenshotDiff.Diff noiseFloor =
                        ScreenshotDiff.compare("noise floor (no wand vs. no wand)", baseline, baselineAgain);
                ScreenshotDiff.assertUnchanged(noiseFloor);

                singleplayer.getServer().runCommand(
                        "item replace entity @a weapon.mainhand with simplebuilding:netherite_building_wand");
                singleplayer.getConnection().waitForClientboundPackets();
                context.waitTicks(15);

                int previewSize = assertPreviewTriggerConditions(context);
                System.out.println("[simplebuilding-test] building wand preview positions: " + previewSize);

                Path withWand = context.takeScreenshot("wand-c-preview");
                ScreenshotDiff.Diff signal =
                        ScreenshotDiff.compare("building wand ghost preview", baseline, withWand);
                ScreenshotDiff.assertDrew("BuildingWandPreviewRenderer", noiseFloor, signal);

                // Control: taking the wand away has to restore the baseline picture.
                singleplayer.getServer().runCommand("clear @a");
                singleplayer.getConnection().waitForClientboundPackets();
                context.waitTicks(15);

                Path withoutWand = context.takeScreenshot("wand-d-removed-again");
                ScreenshotDiff.Diff residual =
                        ScreenshotDiff.compare("control (wand removed again)", baseline, withoutWand);
                int allowed = Math.max(noiseFloor.changedPixels() * 4 + 200, residual.totalPixels() / 20000);

                if (residual.changedPixels() > allowed) {
                    throw new AssertionError("Control step failed: removing the wand did not restore the "
                            + "baseline image (" + residual + ", allowed " + allowed + " pixels).");
                }
            } finally {
                RendererTestScene.showHudAgain(context);
            }
        }
    }

    /**
     * Recomputes exactly what the renderer computes and asserts there is something to draw.
     * Returns the number of preview positions that are actually renderable (replaceable target
     * blocks), so a failure can never be blamed on an empty preview map.
     */
    private int assertPreviewTriggerConditions(ClientGameTestContext context) {
        RendererTestScene.assertAimedAt(context, RendererTestScene.TARGET, RendererTestScene.TARGET_FACE);

        String problem = context.computeOnClient(client -> {
            if (client.player == null || client.level == null) {
                return "no client player or level";
            }

            if (!(client.player.getMainHandItem().getItem() instanceof BuildingWandItem)) {
                return "main hand does not hold a BuildingWandItem but " + client.player.getMainHandItem();
            }

            if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
                return "no block hit result";
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Building wand preview trigger conditions not met: " + problem);
        }

        int renderable = context.computeOnClient(client -> {
            BlockHitResult hit = (BlockHitResult) client.hitResult;
            BuildingWandItem wand = (BuildingWandItem) client.player.getMainHandItem().getItem();

            Map<BlockPos, BlockState> preview = BuildingWandItem.getPreviewStates(
                    client.level, client.player, client.player.getMainHandItem(),
                    hit.getBlockPos(), hit.getDirection(), wand.getWandSquareDiameter());

            int count = 0;

            for (BlockPos pos : preview.keySet()) {
                if (client.level.getBlockState(pos).canBeReplaced()) {
                    count++;
                }
            }

            return count;
        });

        if (renderable < 9) {
            throw new AssertionError("Building wand preview trigger conditions not met: only " + renderable
                    + " renderable preview positions, expected the 11x11 plane in front of the wall. "
                    + RendererTestScene.describeAim(context));
        }

        return renderable;
    }
}
