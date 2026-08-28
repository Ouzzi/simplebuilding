package com.simplebuilding.clienttest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.simplebuilding.items.custom.SledgehammerItem;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.core.BlockPos;

/**
 * Proves that {@code MultiBlockBreakingSupport} still contributes the extra breaking cracks after
 * the 26.2 rewrite onto {@code LevelExtractionEvents.END_EXTRACTION} /
 * {@code LevelRenderState.blockBreakingRenderStates}.
 *
 * <p>Unlike the other two renderers this one draws nothing itself: it appends
 * {@link BlockBreakingRenderState} entries which vanilla then renders as the usual crack overlay.
 * The verifiable output of the mod is therefore the content of
 * {@code levelState.blockBreakingRenderStates} at the end of extraction, and that is what this test
 * observes - through a second {@code END_EXTRACTION} listener registered from the test mod, which
 * runs after the mod's own listener and therefore sees its additions.
 *
 * <p>A screenshot difference test was deliberately not used here: mining continuously spawns block
 * crumble particles through {@code Minecraft.continueAttack}, which are random and cannot be
 * switched off, so pixel differences between two mining screenshots would not be attributable.
 * The screenshots taken below are for visual inspection only and are not asserted on.
 *
 * <p>Test structure:
 * <ul>
 *   <li><b>Control</b> - mine the wall with a vanilla netherite pickaxe. None of the eight
 *       neighbours of the target block may ever show up in the breaking render states.</li>
 *   <li><b>Signal</b> - mine the same block with the netherite sledgehammer. All eight neighbours
 *       have to show up. This also proves the observing listener itself works, so the control
 *       cannot pass trivially.</li>
 * </ul>
 *
 * <p>Obsidian is used as the wall material so that a single block takes roughly 167 ticks to break:
 * every measurement window stays far away from the block actually breaking, and the destroy stage
 * changes slowly enough to be stable. A netherite pickaxe and the netherite sledgehammer are both
 * correct tools for it, which the mod's {@code SledgehammerUtils.shouldBreak} requires at
 * Override level 0.
 */
public final class MultiBlockBreakingClientGameTest implements FabricClientGameTest {

    private static final int MEASURE_TICKS = 20;

    @Override
    public void runTest(ClientGameTestContext context) {
        BreakingStateRecorder.install(context);

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            RendererTestScene.build(context, singleplayer, "minecraft:obsidian", "survival");

            try {
                Set<BlockPos> expectedNeighbours = neighboursOfTarget();

                Set<BlockPos> withPickaxe = mineAndRecord(context, singleplayer,
                        "minecraft:netherite_pickaxe", "breaking-a-vanilla-pickaxe", false);
                System.out.println("[simplebuilding-test] breaking states while mining with a vanilla pickaxe: "
                        + withPickaxe);

                Set<BlockPos> leaked = new LinkedHashSet<>(withPickaxe);
                leaked.retainAll(expectedNeighbours);

                if (!leaked.isEmpty()) {
                    throw new AssertionError("Control failed: mining with a plain netherite pickaxe already "
                            + "produced breaking cracks on connected blocks " + leaked
                            + ". The sledgehammer measurement below would not be attributable to the mod.");
                }

                Set<BlockPos> withSledgehammer = mineAndRecord(context, singleplayer,
                        "simplebuilding:netherite_sledgehammer", "breaking-b-sledgehammer", true);
                System.out.println("[simplebuilding-test] breaking states while mining with the sledgehammer: "
                        + withSledgehammer);

                Set<BlockPos> missing = new LinkedHashSet<>(expectedNeighbours);
                missing.removeAll(withSledgehammer);

                if (!missing.isEmpty()) {
                    throw new AssertionError("MultiBlockBreakingSupport did not contribute anything: the "
                            + "breaking render states are missing " + missing.size() + " of the "
                            + expectedNeighbours.size() + " connected blocks " + missing
                            + ". Recorded were: " + withSledgehammer
                            + ". Trigger conditions were asserted before measuring.");
                }

                System.out.println("[simplebuilding-test] MultiBlockBreakingSupport added "
                        + expectedNeighbours.size() + " connected blocks to the breaking render states");
            } finally {
                context.getInput().releaseMouse(0);
                RendererTestScene.showHudAgain(context);
            }
        }
    }

    /** The eight blocks the sledgehammer breaks alongside the target, in the wall plane. */
    private static Set<BlockPos> neighboursOfTarget() {
        Set<BlockPos> positions = new LinkedHashSet<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    positions.add(RendererTestScene.TARGET.offset(dx, dy, 0));
                }
            }
        }

        return positions;
    }

    /**
     * Equips {@code itemId}, mines the target block for a while and returns every block position
     * that showed up in the breaking render states during the measurement window.
     */
    private Set<BlockPos> mineAndRecord(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                        String itemId, String screenshotName, boolean expectSledgehammer) {
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getServer().runCommand("item replace entity @a weapon.mainhand with " + itemId);
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);

        RendererTestScene.assertAimedAt(context, RendererTestScene.TARGET, RendererTestScene.TARGET_FACE);
        assertMainHand(context, itemId, expectSledgehammer);

        context.getInput().holdMouse(0);

        try {
            waitUntilDestroying(context, itemId);
            context.waitTicks(5);

            BreakingStateRecorder.arm();
            Set<BlockPos> seen = new LinkedHashSet<>();

            for (int tick = 0; tick < MEASURE_TICKS; tick++) {
                context.waitTick();
                seen.addAll(BreakingStateRecorder.lastSeen());
            }

            BreakingStateRecorder.disarm();

            context.takeScreenshot(screenshotName);
            return seen;
        } finally {
            context.getInput().releaseMouse(0);
            context.waitTicks(5);
        }
    }

    private void waitUntilDestroying(ClientGameTestContext context, String itemId) {
        for (int tick = 0; tick < 100; tick++) {
            boolean destroying = context.computeOnClient(client -> client.gameMode != null
                    && client.gameMode.isDestroying()
                    && client.gameMode.getDestroyStage() >= 0
                    && client.gameMode.getDestroyStage() <= 9);

            if (destroying) {
                return;
            }

            context.waitTick();
        }

        throw new AssertionError("Player never started destroying the target block with " + itemId
                + ". " + RendererTestScene.describeAim(context));
    }

    private void assertMainHand(ClientGameTestContext context, String itemId, boolean expectSledgehammer) {
        boolean isSledgehammer = context.computeOnClient(client -> client.player != null
                && client.player.getMainHandItem().getItem() instanceof SledgehammerItem);

        if (isSledgehammer != expectSledgehammer) {
            throw new AssertionError("Wrong tool in the main hand for " + itemId
                    + " (SledgehammerItem=" + isSledgehammer + "). " + RendererTestScene.describeAim(context));
        }
    }

    /**
     * Records {@code levelState.blockBreakingRenderStates} at the end of every extraction pass.
     *
     * <p>Registered lazily from the test thread: Fabric events keep their registration order, and
     * the mod registers its own {@code END_EXTRACTION} listener during client initialisation, so
     * this listener always runs after it and sees the entries the mod appended.
     */
    private static final class BreakingStateRecorder {

        private static volatile boolean registered;
        private static volatile boolean armed;
        private static volatile List<BlockPos> lastSeen = List.of();

        private BreakingStateRecorder() {
        }

        static void install(ClientGameTestContext context) {
            context.runOnClient(client -> {
                if (registered) {
                    return;
                }

                registered = true;
                LevelExtractionEvents.END_EXTRACTION.register(extraction -> {
                    if (!armed) {
                        return;
                    }

                    List<BlockPos> positions = new ArrayList<>();

                    for (BlockBreakingRenderState state : extraction.levelState().blockBreakingRenderStates) {
                        positions.add(state.blockPos());
                    }

                    lastSeen = List.copyOf(positions);
                });
            });
        }

        static void arm() {
            lastSeen = List.of();
            armed = true;
        }

        static void disarm() {
            armed = false;
        }

        static Set<BlockPos> lastSeen() {
            return new HashSet<>(lastSeen);
        }
    }
}
