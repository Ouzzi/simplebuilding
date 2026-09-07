package com.simplebuilding.clienttest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.SledgehammerItem;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

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
 *   <li><b>Strip Miner</b> - the second of the three tools the support class handles, and the only
 *       one whose gate is a player state rather than an item type: the extra cracks appear only
 *       while the player sneaks. Mining with the enchanted pickaxe standing up must add nothing,
 *       mining while holding sneak must add exactly the blocks behind the target.</li>
 * </ul>
 *
 * <p>Obsidian is used as the wall material so that a single block takes roughly 167 ticks to break:
 * every measurement window stays far away from the block actually breaking, and the destroy stage
 * changes slowly enough to be stable. A netherite pickaxe and the netherite sledgehammer are both
 * correct tools for it, which the mod's {@code SledgehammerUtils.shouldBreak} requires at
 * Override level 0, and which the Strip Miner branch requires as well.
 *
 * <p><b>Not covered.</b> The Vein Miner branch of the same method. {@code MiningUtils} only accepts
 * ore blocks for pickaxes (and logs for axes), and every vanilla ore that a correct tool can mine
 * breaks within a handful of ticks - the block would be gone in the middle of the measurement
 * window and take the scene with it. The client only part of that branch is the extraction this
 * test already drives through the Strip Miner, and the block selection itself
 * ({@code MiningUtils.getVeinMinerBlocks}) is loader neutral and covered by the server side
 * {@code ToolBehaviourTests}.
 */
public final class MultiBlockBreakingClientGameTest implements FabricClientGameTest {

    private static final int MEASURE_TICKS = 20;

    /** Strip Miner level 2 reaches two blocks past the target, straight along the view direction. */
    private static final int STRIP_LEVEL = 2;
    private static final BlockPos STRIP_FIRST = RendererTestScene.TARGET.offset(0, 0, 1);
    private static final BlockPos STRIP_SECOND = RendererTestScene.TARGET.offset(0, 0, 2);

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

                testStripMinerCracksNeedSneaking(context, singleplayer);
            } finally {
                context.getInput().releaseMouse(0);
                RendererTestScene.showHudAgain(context);
            }
        }
    }

    /**
     * The Strip Miner branch: {@code stack.isCorrectToolForDrops(mainState) && sneaking} plus a
     * pickaxe with the enchantment. The two blocks behind the target are built first, because the
     * default scene's wall is a single layer and {@code getStripMinerBlocks} stops at the first air
     * block - without them the enchanted run would legitimately add nothing and the test would pass
     * for the wrong reason.
     *
     * <p><b>What breaks this test:</b> dropping the {@code sneaking} condition (the standing run
     * starts producing cracks), losing the Strip Miner branch or its {@code ItemTags.PICKAXES}
     * check (the sneaking run produces nothing), and handing the extra states a stage of their own
     * instead of the destroy stage of the block actually being mined (the progress values in one
     * frame stop agreeing).
     */
    private void testStripMinerCracksNeedSneaking(ClientGameTestContext context,
                                                  TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runCommand("fill 8 -1 21 12 3 22 minecraft:obsidian");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(20);

        assertSolid(context, STRIP_FIRST);
        assertSolid(context, STRIP_SECOND);

        Set<BlockPos> standing = mineAndRecord(context, singleplayer, "minecraft:netherite_pickaxe",
                ModEnchantments.STRIP_MINER, STRIP_LEVEL, "breaking-c-strip-miner-standing", false, false);
        System.out.println("[simplebuilding-test] breaking states with Strip Miner while standing: " + standing);

        if (standing.contains(STRIP_FIRST) || standing.contains(STRIP_SECOND)) {
            throw new AssertionError("Control failed: a Strip Miner pickaxe produced breaking cracks on "
                    + STRIP_FIRST + " / " + STRIP_SECOND + " while the player was not sneaking. Recorded: "
                    + standing + ". The mod gates the extra cracks on player.isShiftKeyDown().");
        }

        Set<BlockPos> sneaking = mineAndRecord(context, singleplayer, "minecraft:netherite_pickaxe",
                ModEnchantments.STRIP_MINER, STRIP_LEVEL, "breaking-d-strip-miner-sneaking", false, true);
        System.out.println("[simplebuilding-test] breaking states with Strip Miner while sneaking: " + sneaking);

        if (!sneaking.contains(STRIP_FIRST) || !sneaking.contains(STRIP_SECOND)) {
            throw new AssertionError("MultiBlockBreakingSupport contributed no Strip Miner cracks: expected "
                    + STRIP_FIRST + " and " + STRIP_SECOND + " in the breaking render states, recorded were "
                    + sneaking + ". Sneak state, tool, enchantment and aim were all asserted before measuring.");
        }

        assertOneProgressPerFrame(BreakingStateRecorder.lastFrame());
    }

    /**
     * Every entry of one extraction pass has to carry the same destroy stage: the mod copies
     * {@code gameMode.getDestroyStage()} of the block the player is actually mining onto each extra
     * block. The frame is the last one of the sneaking measurement, taken after the stage has
     * provably passed 0 (see {@code mineAndRecord}), so a hard coded 0 cannot slip through.
     *
     * <p>That the block under the crosshair is in the list at all is vanilla's doing -
     * {@code MultiPlayerGameMode} reports the local player's destroy progress and vanilla extracts
     * it - and it is only used here as the reference value.
     */
    private void assertOneProgressPerFrame(List<BlockBreakingRenderState> frame) {
        if (frame.isEmpty()) {
            throw new AssertionError("The last recorded extraction pass held no breaking render states at "
                    + "all, so the destroy stage of the extra cracks cannot be checked.");
        }

        int first = frame.get(0).progress();

        for (BlockBreakingRenderState state : frame) {
            if (state.progress() != first) {
                throw new AssertionError("The breaking render states of one frame disagree about the destroy "
                        + "stage: " + state.blockPos() + " is at stage " + state.progress() + " while "
                        + frame.get(0).blockPos() + " is at stage " + first + ". The extra cracks have to "
                        + "carry the stage of the block that is actually being mined. Frame: " + frame);
            }
        }

        if (first < 1) {
            throw new AssertionError("The destroy stage never passed 0 during the measurement (" + first
                    + "), so this frame cannot tell a copied stage from a hard coded one.");
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

    private Set<BlockPos> mineAndRecord(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                        String itemId, String screenshotName, boolean expectSledgehammer) {
        return mineAndRecord(context, singleplayer, itemId, null, 0, screenshotName, expectSledgehammer, false);
    }

    /**
     * Equips {@code itemId} (optionally enchanted), mines the target block for a while and returns
     * every block position that showed up in the breaking render states during the measurement
     * window. With {@code sneak} the sneak key is held for the whole run.
     */
    private Set<BlockPos> mineAndRecord(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                        String itemId, ResourceKey<Enchantment> enchantment, int level,
                                        String screenshotName, boolean expectSledgehammer, boolean sneak) {
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getServer().runCommand("item replace entity @a weapon.mainhand with " + itemId);

        if (enchantment != null) {
            singleplayer.getServer().runCommand("enchant @a " + enchantment.identifier() + " " + level);
        }

        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);

        if (sneak) {
            context.getInput().holdKey(options -> options.keyShift);
            context.waitTicks(5);
        }

        try {
            RendererTestScene.assertAimedAt(context, RendererTestScene.TARGET, RendererTestScene.TARGET_FACE);
            assertMainHand(context, itemId, expectSledgehammer);
            assertSneaking(context, sneak);

            if (enchantment != null) {
                assertEnchanted(context, enchantment, level);
            }

            context.getInput().holdMouse(0);

            try {
                // The stage has to be past 0 before the last frame is used for the stage check; on
                // obsidian that happens after roughly 17 ticks of mining, well inside this window.
                waitUntilDestroying(context, itemId, enchantment != null ? 1 : 0);
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
        } finally {
            if (sneak) {
                context.getInput().releaseKey(options -> options.keyShift);
                context.waitTicks(5);
            }
        }
    }

    private void waitUntilDestroying(ClientGameTestContext context, String itemId, int minStage) {
        for (int tick = 0; tick < 200; tick++) {
            boolean destroying = context.computeOnClient(client -> client.gameMode != null
                    && client.gameMode.isDestroying()
                    && client.gameMode.getDestroyStage() >= minStage
                    && client.gameMode.getDestroyStage() <= 9);

            if (destroying) {
                return;
            }

            context.waitTick();
        }

        throw new AssertionError("Player never reached destroy stage " + minStage + " on the target block "
                + "with " + itemId + ". " + RendererTestScene.describeAim(context));
    }

    private void assertMainHand(ClientGameTestContext context, String itemId, boolean expectSledgehammer) {
        boolean isSledgehammer = context.computeOnClient(client -> client.player != null
                && client.player.getMainHandItem().getItem() instanceof SledgehammerItem);

        if (isSledgehammer != expectSledgehammer) {
            throw new AssertionError("Wrong tool in the main hand for " + itemId
                    + " (SledgehammerItem=" + isSledgehammer + "). " + RendererTestScene.describeAim(context));
        }
    }

    private void assertSneaking(ClientGameTestContext context, boolean expected) {
        boolean sneaking = context.computeOnClient(client ->
                client.player != null && client.player.isShiftKeyDown());

        if (sneaking != expected) {
            throw new AssertionError("The player's sneak state is " + sneaking + " but the measurement needs "
                    + expected + ". MultiBlockBreakingSupport reads player.isShiftKeyDown() for both the "
                    + "Strip Miner and the Vein Miner branch.");
        }
    }

    /** Reads the enchantment component directly, so a broken mod helper cannot fake this. */
    private void assertEnchanted(ClientGameTestContext context, ResourceKey<Enchantment> enchantment, int level) {
        int actual = context.computeOnClient(client -> {
            if (client.player == null) {
                return -1;
            }

            ItemStack stack = client.player.getMainHandItem();
            ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);

            if (enchantments == null) {
                return 0;
            }

            for (Holder<Enchantment> holder : enchantments.keySet()) {
                if (holder.is(enchantment)) {
                    return enchantments.getLevel(holder);
                }
            }

            return 0;
        });

        if (actual != level) {
            throw new AssertionError("The client's copy of the held tool carries " + enchantment.identifier()
                    + " at level " + actual + " instead of " + level + ". The /enchant command did not reach "
                    + "the client, so the branch under test would never run.");
        }
    }

    private void assertSolid(ClientGameTestContext context, BlockPos pos) {
        boolean air = context.computeOnClient(client ->
                client.level == null || client.level.getBlockState(pos).isAir());

        if (air) {
            throw new AssertionError("Scene setup failed: " + pos + " is air on the client, but the Strip "
                    + "Miner measurement needs a solid block there - getStripMinerBlocks stops at the first "
                    + "air block.");
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
        private static volatile List<BlockBreakingRenderState> lastSeen = List.of();

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

                    List<BlockBreakingRenderState> states = new ArrayList<>();

                    for (BlockBreakingRenderState state : extraction.levelState().blockBreakingRenderStates) {
                        states.add(state);
                    }

                    lastSeen = List.copyOf(states);
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
            Set<BlockPos> positions = new HashSet<>();

            for (BlockBreakingRenderState state : lastSeen) {
                positions.add(state.blockPos());
            }

            return positions;
        }

        /**
         * The full content of the last recorded extraction pass. Survives {@link #disarm()} on
         * purpose, so a measurement can be evaluated after the mouse has been released again.
         */
        static List<BlockBreakingRenderState> lastFrame() {
            return lastSeen;
        }
    }
}
