package com.simplebuilding.clienttest;

import java.nio.file.Path;

import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.guiDrawHelper;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

/**
 * Proves that {@code BlockHighlightRenderer} still puts geometry on screen on the 1.21.11 line,
 * where the in-world hook is {@code WorldRenderEvents.BEFORE_ENTITIES} (there is no
 * {@code LevelRenderEvents.COLLECT_SUBMITS} here) feeding
 * {@code SubmitNodeCollector.submitCustomGeometry} through {@code WorldRenderContext#commandQueue}.
 *
 * <p>Both of the renderer's two independent passes are covered:
 * <ul>
 *   <li><b>Sledgehammer highlight</b> - needs a {@link SledgehammerItem} in the <em>main</em> hand
 *       (offhand is not checked) and a block hit result. With no Radius/Break Through enchantment
 *       the pattern is the 3x3 plane around the target block on the hit side; the target block
 *       itself is skipped, so exactly the eight neighbours are outlined and filled.</li>
 *   <li><b>Octant highlight</b> - needs an {@link OctantItem} in the main or offhand carrying at
 *       least {@code Pos1} or {@code Pos2} in its custom data. The corner outlines are drawn
 *       unconditionally; the area fill additionally needs Constructor's Touch (config
 *       {@code invertOctantSneak} is false by default), so this test deliberately only asserts on
 *       the corner outlines and puts them on free air positions in front of the wall where all
 *       twelve edges of each box are visible.</li>
 * </ul>
 *
 * <p>Between the two, the item is removed again and the screen has to return to the baseline. That
 * control rules out the alternative explanation "the picture drifts anyway" for the differences.
 */
public final class BlockHighlightClientGameTest implements FabricClientGameTest {

    /** Free air positions, 2.5 blocks in front of the camera - a fully visible box outline each. */
    private static final BlockPos OCTANT_POS_1 = new BlockPos(9, 1, RendererTestScene.FRONT_Z);
    private static final BlockPos OCTANT_POS_2 = new BlockPos(11, 1, RendererTestScene.FRONT_Z);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            RendererTestScene.build(context, singleplayer, "minecraft:stone", "creative");

            try {
                Path baseline = context.takeScreenshot("highlight-a-empty-hand");
                context.waitTicks(10);
                Path baselineAgain = context.takeScreenshot("highlight-b-empty-hand-again");

                ScreenshotDiff.Diff noiseFloor =
                        ScreenshotDiff.compare("noise floor (empty hand vs. empty hand)", baseline, baselineAgain);
                ScreenshotDiff.assertUnchanged(noiseFloor);

                testSledgehammerHighlight(context, singleplayer, baseline, noiseFloor);
                testItemRemovalReturnsToBaseline(context, singleplayer, baseline, noiseFloor);
                testOctantHighlight(context, singleplayer, baseline, noiseFloor);
            } finally {
                RendererTestScene.showHudAgain(context);
            }
        }
    }

    private void testSledgehammerHighlight(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                           Path baseline, ScreenshotDiff.Diff noiseFloor) {
        singleplayer.getServer().runCommand(
                "item replace entity @a weapon.mainhand with simplebuilding:netherite_sledgehammer");
        RendererTestScene.awaitOnClient(context,
                client -> client.player != null
                        && client.player.getMainHandItem().getItem() instanceof SledgehammerItem,
                "the sledgehammer arriving in the main hand");

        assertSledgehammerTriggerConditions(context);

        Path withSledgehammer = context.takeScreenshot("highlight-c-sledgehammer");
        ScreenshotDiff.Diff signal =
                ScreenshotDiff.compare("sledgehammer highlight", baseline, withSledgehammer);
        ScreenshotDiff.assertDrew("BlockHighlightRenderer (sledgehammer)", noiseFloor, signal);
    }

    private void testItemRemovalReturnsToBaseline(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                                  Path baseline, ScreenshotDiff.Diff noiseFloor) {
        singleplayer.getServer().runCommand("clear @a");
        RendererTestScene.awaitOnClient(context,
                client -> client.player != null && client.player.getMainHandItem().isEmpty(),
                "the main hand being cleared again");

        Path cleared = context.takeScreenshot("highlight-d-cleared-again");
        ScreenshotDiff.Diff residual =
                ScreenshotDiff.compare("control (highlight removed again)", baseline, cleared);

        int allowed = Math.max(noiseFloor.changedPixels() * 4 + 200, residual.totalPixels() / 20000);

        if (residual.changedPixels() > allowed) {
            throw new AssertionError("Control step failed: removing the sledgehammer did not restore the "
                    + "baseline image (" + residual + ", allowed " + allowed + " pixels). The measured "
                    + "difference cannot be attributed to the highlight renderer.");
        }
    }

    private void testOctantHighlight(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                     Path baseline, ScreenshotDiff.Diff noiseFloor) {
        singleplayer.getServer().runCommand(
                "item replace entity @a weapon.mainhand with simplebuilding:octant["
                        + "minecraft:custom_data={"
                        + "Pos1:[I;" + OCTANT_POS_1.getX() + "," + OCTANT_POS_1.getY() + "," + OCTANT_POS_1.getZ() + "],"
                        + "Pos2:[I;" + OCTANT_POS_2.getX() + "," + OCTANT_POS_2.getY() + "," + OCTANT_POS_2.getZ() + "]"
                        + "}]");
        RendererTestScene.awaitOnClient(context,
                client -> client.player != null
                        && client.player.getMainHandItem().getItem() instanceof OctantItem,
                "the octant arriving in the main hand");

        assertOctantTriggerConditions(context);

        Path withOctant = context.takeScreenshot("highlight-e-octant");
        ScreenshotDiff.Diff signal = ScreenshotDiff.compare("octant highlight", baseline, withOctant);
        ScreenshotDiff.assertDrew("BlockHighlightRenderer (octant)", noiseFloor, signal);
    }

    /**
     * Verifies everything {@code drawSledgehammerHighlights} needs before the screenshot is taken.
     * Without this a missed setup would look exactly like a broken renderer.
     */
    private void assertSledgehammerTriggerConditions(ClientGameTestContext context) {
        RendererTestScene.assertAimedAt(context, RendererTestScene.TARGET, RendererTestScene.TARGET_FACE);

        String problem = context.computeOnClient(client -> {
            if (client.player == null || client.level == null) {
                return "no client player or level";
            }

            if (!(client.player.getMainHandItem().getItem() instanceof SledgehammerItem)) {
                return "main hand does not hold a SledgehammerItem but "
                        + client.player.getMainHandItem();
            }

            // The eight highlighted positions are the neighbours of the target block in the wall
            // plane. All of them must be solid, otherwise the renderer legitimately draws nothing.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }

                    BlockPos neighbour = RendererTestScene.TARGET.offset(dx, dy, 0);

                    if (client.level.getBlockState(neighbour).isAir()) {
                        return "neighbour " + neighbour + " of the target block is air, "
                                + "so there is nothing for the highlight to outline";
                    }
                }
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Sledgehammer highlight trigger conditions not met: " + problem);
        }
    }

    /**
     * Verifies everything {@code drawOctantHighlights} needs before the screenshot is taken. The
     * custom data is read back with the very same helper the renderer uses, because the component
     * syntax in the {@code item replace} command above is the part of this setup most likely to
     * differ between Minecraft versions - a silently empty {@code Pos1}/{@code Pos2} would make the
     * renderer draw nothing for a reason that has nothing to do with the renderer.
     */
    private void assertOctantTriggerConditions(ClientGameTestContext context) {
        String problem = context.computeOnClient(client -> {
            if (client.player == null) {
                return "no client player";
            }

            var stack = client.player.getMainHandItem();

            if (!(stack.getItem() instanceof OctantItem)) {
                return "main hand does not hold an OctantItem but " + stack;
            }

            var nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            BlockPos pos1 = guiDrawHelper.getPos(nbt, "Pos1");
            BlockPos pos2 = guiDrawHelper.getPos(nbt, "Pos2");

            if (!OCTANT_POS_1.equals(pos1) || !OCTANT_POS_2.equals(pos2)) {
                return "the octant's custom data did not reach the client as expected: Pos1=" + pos1
                        + " Pos2=" + pos2 + " (expected " + OCTANT_POS_1 + " / " + OCTANT_POS_2
                        + "), raw tag " + nbt;
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Octant highlight trigger conditions not met: " + problem);
        }
    }
}
