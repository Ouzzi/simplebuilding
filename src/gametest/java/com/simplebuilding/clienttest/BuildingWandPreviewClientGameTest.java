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
 * after the 26.2 rewrite onto {@code SubmitNodeCollector.submitCustomGeometry}, and that each of the
 * four conditions it checks really has to hold before it draws.
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
 *
 * <p>Three negative cases follow the positive one. Each of them is a real branch in the renderer
 * and each has to leave the baseline picture untouched:
 * <ul>
 *   <li><b>wand in the offhand only</b> - the renderer reads {@code player.getMainHandItem()} and
 *       nothing else, so an offhand wand must draw nothing;</li>
 *   <li><b>no material anywhere</b> - {@code getPreviewStates} finds no block item, returns an empty
 *       map and the renderer returns before submitting anything. The emptiness of that map is
 *       asserted before the screenshot, so an unchanged picture cannot mean "the test forgot to
 *       remove the material";</li>
 *   <li><b>ghost blocks are scaled down</b> - see below.</li>
 * </ul>
 *
 * <p><b>How the 0.5 ghost scale is measured.</b> The renderer shrinks every ghost block around its
 * block centre by {@code GHOST_SCALE = 0.5}, which leaves a visible gap between neighbouring ghosts.
 * At the fixed camera of {@link RendererTestScene} the 11x11 preview plane covers the whole viewport
 * (at 2.5 blocks distance and 70 degrees of vertical field of view, only about 3.5 blocks fit on
 * screen), so an unscaled preview would repaint essentially every pixel, while half sized cubes
 * repaint roughly a quarter to a third of them - a third or so once the side faces of the off axis
 * cubes are counted. The test therefore requires the painted area to stay clearly below the full
 * viewport. A bright red material is used so that "painted" is not a question of texture contrast:
 * a redstone block ghost differs from the stone wall behind it in every channel.
 *
 * <p><b>Not covered.</b> The ghost alpha itself ({@code GHOST_ALPHA = 180}). Reading a blend factor
 * back out of the framebuffer would require knowing the exact lit colour of both the ghost model and
 * the wall texel behind it; the value would then be pinned to one resource pack and one light level.
 * The gap in the ghost grid is the strongest statement a screenshot can make here.
 */
public final class BuildingWandPreviewClientGameTest implements FabricClientGameTest {

    /**
     * Upper bound for the share of the viewport the shrunk ghost blocks may repaint. Half sized
     * cubes cover at most ~40% of their cell once perspective shows their side faces; full sized
     * ones would tile the plane and cover over 90%.
     */
    private static final double MAX_PAINTED_PERCENT = 65.0;

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
                assertLooksIdentical(noiseFloor, residual,
                        "Removing the wand has to restore the baseline image");

                testOffhandWandDrawsNothing(context, singleplayer, baseline, noiseFloor);
                testWithoutMaterialDrawsNothing(context, singleplayer, baseline, noiseFloor);
                testGhostBlocksLeaveGaps(context, singleplayer, baseline, noiseFloor);
            } finally {
                RendererTestScene.showHudAgain(context);
            }
        }
    }

    /**
     * The renderer only ever looks at the main hand. Material goes into the main hand and the wand
     * into the offhand, so everything else the renderer needs is in place and only the hand is wrong.
     *
     * <p><b>What breaks this test:</b> scanning both hands for the wand, for example by copying the
     * offhand fallback that {@code BlockHighlightRenderer.findOctantStack} uses for the octant.
     */
    private void testOffhandWandDrawsNothing(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                             Path baseline, ScreenshotDiff.Diff noiseFloor) {
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:stone 64");
        singleplayer.getServer().runCommand(
                "item replace entity @a weapon.offhand with simplebuilding:netherite_building_wand");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);

        RendererTestScene.assertAimedAt(context, RendererTestScene.TARGET, RendererTestScene.TARGET_FACE);

        String problem = context.computeOnClient(client -> {
            if (client.player == null) {
                return "no client player";
            }

            if (client.player.getMainHandItem().getItem() instanceof BuildingWandItem) {
                return "the wand ended up in the main hand, which is the case the positive test covers";
            }

            if (!(client.player.getOffhandItem().getItem() instanceof BuildingWandItem)) {
                return "the offhand does not hold a BuildingWandItem but " + client.player.getOffhandItem();
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Offhand wand setup failed: " + problem);
        }

        Path offhandOnly = context.takeScreenshot("wand-e-offhand-only");
        ScreenshotDiff.Diff diff =
                ScreenshotDiff.compare("wand in the offhand only", baseline, offhandOnly);
        assertLooksIdentical(noiseFloor, diff,
                "A building wand in the offhand must not draw a preview");
    }

    /**
     * Without a block item in offhand or hotbar {@code getPreviewStates} returns an empty map and
     * the renderer bails out before it submits anything. The wand itself sits in hotbar slot 0 and
     * is not a block item, so clearing the inventory really does leave no material.
     *
     * <p><b>What breaks this test:</b> falling back to a default block when no material is found, or
     * moving the {@code previewMap.isEmpty()} check behind the geometry submission.
     */
    private void testWithoutMaterialDrawsNothing(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                                 Path baseline, ScreenshotDiff.Diff noiseFloor) {
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getServer().runCommand(
                "item replace entity @a weapon.mainhand with simplebuilding:netherite_building_wand");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);

        RendererTestScene.assertAimedAt(context, RendererTestScene.TARGET, RendererTestScene.TARGET_FACE);

        String problem = context.computeOnClient(client -> {
            if (client.player == null || client.level == null) {
                return "no client player or level";
            }

            if (!(client.player.getMainHandItem().getItem() instanceof BuildingWandItem wand)) {
                return "main hand does not hold a BuildingWandItem but " + client.player.getMainHandItem();
            }

            if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
                return "no block hit result";
            }

            Map<BlockPos, BlockState> preview = BuildingWandItem.getPreviewStates(
                    client.level, client.player, client.player.getMainHandItem(),
                    hit.getBlockPos(), hit.getDirection(), wand.getWandSquareDiameter());

            if (!preview.isEmpty()) {
                return "the preview map still holds " + preview.size() + " positions, so some block "
                        + "item is left in the inventory and this would not test the empty branch";
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Missing material setup failed: " + problem);
        }

        Path noMaterial = context.takeScreenshot("wand-f-no-material");
        ScreenshotDiff.Diff diff =
                ScreenshotDiff.compare("wand without any material", baseline, noMaterial);
        assertLooksIdentical(noiseFloor, diff,
                "A building wand without material must not draw a preview");
    }

    /**
     * The ghost blocks are drawn at half size, so the wall stays visible between them. See the class
     * javadoc for the geometry behind the bound.
     *
     * <p><b>What breaks this test:</b> dropping the {@code GHOST_SCALE} translate/scale/translate
     * block around the model, or raising the scale towards 1.0 - the ghosts would then tile the whole
     * preview plane and repaint nearly the entire viewport.
     */
    private void testGhostBlocksLeaveGaps(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                          Path baseline, ScreenshotDiff.Diff noiseFloor) {
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getServer().runCommand(
                "item replace entity @a weapon.offhand with minecraft:redstone_block 64");
        singleplayer.getServer().runCommand(
                "item replace entity @a weapon.mainhand with simplebuilding:netherite_building_wand");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);

        int renderable = assertPreviewTriggerConditions(context);

        if (renderable < 100) {
            throw new AssertionError("Ghost scale measurement needs the full 11x11 plane in front of "
                    + "the wall, but only " + renderable + " preview positions are replaceable. "
                    + RendererTestScene.describeAim(context));
        }

        Path ghosts = context.takeScreenshot("wand-g-ghost-gaps");
        ScreenshotDiff.Diff painted =
                ScreenshotDiff.compare("ghost blocks against the wall", baseline, ghosts);
        ScreenshotDiff.assertDrew("BuildingWandPreviewRenderer (scaled ghosts)", noiseFloor, painted);

        if (painted.percent() > MAX_PAINTED_PERCENT) {
            throw new AssertionError("The ghost preview repainted " + painted
                    + " of the viewport. The 11x11 plane covers the whole screen at this camera, so "
                    + "anything above " + MAX_PAINTED_PERCENT + "% means the ghost blocks are no longer "
                    + "shrunk to GHOST_SCALE = 0.5 and tile their cells without a gap.");
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

    /**
     * Two screenshots have to show the same thing. The tolerance is the measured noise floor with
     * headroom, not zero: the comparison is only ever used where the claim is "nothing was drawn".
     */
    private void assertLooksIdentical(ScreenshotDiff.Diff noiseFloor, ScreenshotDiff.Diff diff, String claim) {
        int allowed = Math.max(noiseFloor.changedPixels() * 4 + 200, diff.totalPixels() / 20000);

        if (diff.changedPixels() > allowed) {
            throw new AssertionError(claim + ", but the two pictures differ: " + diff
                    + " (allowed " + allowed + " pixels, noise floor was " + noiseFloor.changedPixels()
                    + " pixels).");
        }
    }
}
