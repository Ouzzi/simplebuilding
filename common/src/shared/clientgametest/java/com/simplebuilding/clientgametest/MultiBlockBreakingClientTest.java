package com.simplebuilding.clientgametest;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.SledgehammerItem;
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
 * the 26.2 rewrite onto the render state extraction pass.
 *
 * <p>Unlike the other two renderers this one draws nothing itself: it appends
 * {@link BlockBreakingRenderState} entries which vanilla then renders as the usual crack overlay.
 * The verifiable output of the mod is therefore the content of
 * {@code levelState.blockBreakingRenderStates} at the end of extraction, and that is what this test
 * observes - through {@link BreakingStateRecorder}, which each loader's driver feeds from its own
 * extraction listener registered to run after the mod's own (see that class for why that wiring
 * cannot live in shared code).
 *
 * <p><b>Why there is no pixel comparison in this file.</b> Mining continuously spawns block crumble
 * particles through {@code Minecraft.continueAttack}, which are random and cannot be switched off,
 * so a difference between two mining screenshots would not be attributable to the mod. This test
 * therefore does not use {@link ScreenshotDiff} at all: the four screenshots it takes are
 * checkpoints for the runner and material for visual inspection, and nothing is asserted on their
 * pixels. The {@link Later} handle {@link Script#shot} returns is deliberately dropped here - there
 * is no later step that would read it. Where a shared test <em>does</em> compare pixels, that path
 * has to come from {@code shot(...)} and be read in a later step, because the file name differs per
 * loader; this test simply has no such comparison.
 *
 * <p><b>What replaces the pixel difference is stronger.</b> The recorder names the exact block
 * positions, so the test can state which blocks got cracks rather than that "something changed".
 * The methodology is the same one the pixel tests use and has to stay in this order: a
 * <em>control</em> run that must produce nothing, every <em>trigger condition</em> asserted
 * immediately before the measurement window opens, then the <em>measurement</em>, and the trigger
 * released again afterwards. Without the control the measurement would not be attributable, and
 * without the asserted trigger conditions a run that quietly failed to equip, aim or sneak would
 * report "the mod contributes nothing".
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
 *
 * <p><b>Also not covered: what the cracks look like.</b> Nothing here asserts that the appended
 * states are actually drawn - only that they are in the list vanilla draws from, and that they all
 * carry the same destroy stage. Proving the pixels would need the particle problem solved first.
 */
public final class MultiBlockBreakingClientTest {

    /**
     * How many client ticks one measurement window records.
     *
     * <p>Twenty is chosen against the two ends it has to fit between: long enough that a block the
     * mod only contributes while the aim is exact still appears in at least one frame (the union
     * over the window is what is asserted, not a single frame), and far short of the ~167 ticks
     * obsidian needs to break, so the block under the crosshair never disappears mid-measurement.
     */
    private static final int MEASURE_TICKS = 20;

    /** Strip Miner level 2 reaches two blocks past the target, straight along the view direction. */
    private static final int STRIP_LEVEL = 2;
    private static final BlockPos STRIP_FIRST = TestScene.TARGET.offset(0, 0, 1);
    private static final BlockPos STRIP_SECOND = TestScene.TARGET.offset(0, 0, 2);

    /**
     * The GLFW key code the sneak steps press.
     *
     * <p>The shared {@link Harness} takes a raw GLFW code, where the Fabric-only version of this
     * test handed over the key <em>binding</em> ({@code options -> options.keyShift}) and let the
     * framework resolve it. A raw code is the only thing both loaders can serve - NeoForge drives
     * input through {@code KeyMapping.set(InputConstants.Type.KEYSYM.getOrCreate(code), ...)} - but
     * it silently assumes the binding still sits on this key. {@link #assertSneakKeyIsBound} is the
     * control that closes that hole: without it, a changed or unbound sneak key would turn the
     * sneaking measurement into a second standing one, and the test would report the mod as broken.
     */
    private static final int SNEAK_KEY = InputConstants.KEY_LSHIFT;

    private MultiBlockBreakingClientTest() {
    }

    /**
     * The whole test, as steps.
     *
     * <p>The two positions behind the target are built once, before the first measurement, rather
     * than in the middle of the Strip Miner section: the scene's own wall is a single layer, and
     * {@code getStripMinerBlocks} stops at the first air block, so without them the enchanted run
     * would legitimately add nothing and the test would pass for the wrong reason. They stand
     * behind the wall and are invisible from the player's position, so they cannot affect the two
     * earlier runs.
     */
    public static void inWorld(Script script) {
        TestScene.build(script, "minecraft:obsidian", "survival");

        Set<BlockPos> expectedNeighbours = neighboursOfTarget();

        // --- Control: a plain netherite pickaxe must not produce a single connected crack -------
        Later<Set<BlockPos>> withPickaxe = mineAndRecord(script, "minecraft:netherite_pickaxe",
                null, 0, "breaking-a-vanilla-pickaxe", false, false);

        script.verify("the vanilla pickaxe produced no connected cracks", () -> {
            TestLog.info("breaking states while mining with a vanilla pickaxe: " + withPickaxe.get());

            Set<BlockPos> leaked = new LinkedHashSet<>(withPickaxe.get());
            leaked.retainAll(expectedNeighbours);

            if (!leaked.isEmpty()) {
                throw new AssertionError("Control failed: mining with a plain netherite pickaxe already "
                        + "produced breaking cracks on connected blocks " + leaked
                        + ". The sledgehammer measurement below would not be attributable to the mod.");
            }
        });

        // --- Signal: the sledgehammer must produce all eight ------------------------------------
        Later<Set<BlockPos>> withSledgehammer = mineAndRecord(script,
                "simplebuilding:netherite_sledgehammer", null, 0, "breaking-b-sledgehammer", true, false);

        script.verify("the sledgehammer produced all eight connected cracks", () -> {
            TestLog.info("breaking states while mining with the sledgehammer: " + withSledgehammer.get());

            Set<BlockPos> missing = new LinkedHashSet<>(expectedNeighbours);
            missing.removeAll(withSledgehammer.get());

            if (!missing.isEmpty()) {
                throw new AssertionError("MultiBlockBreakingSupport did not contribute anything: the "
                        + "breaking render states are missing " + missing.size() + " of the "
                        + expectedNeighbours.size() + " connected blocks " + missing
                        + ". Recorded were: " + withSledgehammer.get()
                        + ". Trigger conditions were asserted before measuring.");
            }

            TestLog.info("MultiBlockBreakingSupport added " + expectedNeighbours.size()
                    + " connected blocks to the breaking render states");
        });

        stripMinerCracksNeedSneaking(script);

        // --- Cleanup ----------------------------------------------------------------------------
        // What a finally block used to do. As steps these run only when everything before them
        // passed: a failing run leaves the mouse button held and the HUD hidden, and the next
        // test's TestScene.build is what puts the world and the options right again. The mouse is
        // the one thing build() does not reset, which is why it is released here as well as at the
        // end of every measurement.
        script.harness("release the attack button", harness -> harness.setAttacking(false));
        TestScene.showHudAgain(script);
    }

    /**
     * The Strip Miner branch: {@code stack.isCorrectToolForDrops(mainState) && sneaking} plus a
     * pickaxe with the enchantment.
     *
     * <p><b>What breaks this test:</b> dropping the {@code sneaking} condition (the standing run
     * starts producing cracks), losing the Strip Miner branch or its {@code ItemTags.PICKAXES}
     * check (the sneaking run produces nothing), and handing the extra states a stage of their own
     * instead of the destroy stage of the block actually being mined (the progress values in one
     * frame stop agreeing).
     */
    /**
     * <b>Open on NeoForge:</b> the sneaking case of this test does not reach a destroy stage there.
     *
     * <p>Everything up to the measurement holds - the wall is rebuilt, the crosshair is on the
     * target, the pickaxe is in hand with Strip Miner II, the player is sneaking, all asserted.
     * Then {@code gameMode.isDestroying()} never becomes true, {@code getDestroyStage()} stays at
     * -1 for the full 200 tick window, and by the end the target block is
     * {@code minecraft:air} - so something is breaking it while vanilla's client side break never
     * starts. The standing case, three steps earlier and identical but for the sneak key, works.
     *
     * <p>Ruled out by measurement, not by guesswork: the input lockout (cleared, logged), the
     * mouse grab (grabbed, window active), a leftover screen (none), a missing block (the aim
     * assertion passes immediately before), a wall broken by an earlier case (rebuilt per case
     * now), a break that was never stopped ({@code stopDestroyBlock} added) and a click vanilla
     * never registered ({@code KeyMapping.click} added). None of the six changed it.
     *
     * <p>It costs one checkpoint on one target and it is named here rather than skipped, because
     * a difference between the loaders is exactly what sharing the test body is meant to surface.
     */
    private static void stripMinerCracksNeedSneaking(Script script) {
        // Tolerated on purpose. TestScene.build clears z = 10..20 only, so these two layers survive
        // a previous test in the shared world and "no blocks were filled" is then a command error
        // for a scene that is already exactly right. Tolerating it is safe here and only here,
        // because the two assertions below prove the blocks are present rather than assuming it.
        script.command("fill 8 -1 21 12 3 22 minecraft:obsidian", true);
        script.awaitPackets();
        script.idle("let the blocks behind the wall reach the client", 20);

        assertSolid(script, STRIP_FIRST);
        assertSolid(script, STRIP_SECOND);

        Later<Set<BlockPos>> standing = mineAndRecord(script, "minecraft:netherite_pickaxe",
                ModEnchantments.STRIP_MINER, STRIP_LEVEL, "breaking-c-strip-miner-standing", false, false);

        script.verify("the Strip Miner pickaxe produced nothing while standing", () -> {
            TestLog.info("breaking states with Strip Miner while standing: " + standing.get());

            if (standing.get().contains(STRIP_FIRST) || standing.get().contains(STRIP_SECOND)) {
                throw new AssertionError("Control failed: a Strip Miner pickaxe produced breaking cracks on "
                        + STRIP_FIRST + " / " + STRIP_SECOND + " while the player was not sneaking. Recorded: "
                        + standing.get() + ". The mod gates the extra cracks on player.isShiftKeyDown().");
            }
        });

        Later<List<BlockBreakingRenderState>> lastFrame = new Later<>(
                "the last recorded extraction pass of the sneaking measurement");
        Later<Set<BlockPos>> sneaking = mineAndRecord(script, "minecraft:netherite_pickaxe",
                ModEnchantments.STRIP_MINER, STRIP_LEVEL, "breaking-d-strip-miner-sneaking", false, true,
                lastFrame);

        script.verify("the Strip Miner pickaxe produced the blocks behind the target while sneaking", () -> {
            TestLog.info("breaking states with Strip Miner while sneaking: " + sneaking.get());

            if (!sneaking.get().contains(STRIP_FIRST) || !sneaking.get().contains(STRIP_SECOND)) {
                throw new AssertionError("MultiBlockBreakingSupport contributed no Strip Miner cracks: expected "
                        + STRIP_FIRST + " and " + STRIP_SECOND + " in the breaking render states, recorded were "
                        + sneaking.get() + ". Sneak state, tool, enchantment and aim were all asserted before "
                        + "measuring.");
            }
        });

        script.verify("every crack of one frame carries the same destroy stage",
                () -> assertOneProgressPerFrame(lastFrame.get()));
    }

    /**
     * Every entry of one extraction pass has to carry the same destroy stage: the mod copies
     * {@code gameMode.getDestroyStage()} of the block the player is actually mining onto each extra
     * block. The frame is the last one of the sneaking measurement, taken after the stage has
     * provably passed 0 (see {@link #mineAndRecord}), so a hard coded 0 cannot slip through.
     *
     * <p>That the block under the crosshair is in the list at all is vanilla's doing -
     * {@code MultiPlayerGameMode} reports the local player's destroy progress and vanilla extracts
     * it - and it is only used here as the reference value.
     */
    private static void assertOneProgressPerFrame(List<BlockBreakingRenderState> frame) {
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
                    positions.add(TestScene.TARGET.offset(dx, dy, 0));
                }
            }
        }

        return positions;
    }

    private static Later<Set<BlockPos>> mineAndRecord(Script script, String itemId,
                                                      ResourceKey<Enchantment> enchantment, int level,
                                                      String screenshotName, boolean expectSledgehammer,
                                                      boolean sneak) {
        return mineAndRecord(script, itemId, enchantment, level, screenshotName, expectSledgehammer, sneak,
                null);
    }

    /**
     * Appends one whole measurement: equip, assert every trigger condition, mine, record, shoot,
     * let go.
     *
     * <p><b>Where the result lives.</b> A step list registers every step before any of them runs,
     * so the union of positions cannot be handed back in a return value the way the straight-line
     * version did. It is collected into a set owned by these steps and published through a
     * {@link Later}, which the assertion steps registered <em>after</em> this call read. Reading it
     * earlier throws by name instead of comparing against an empty set - which would make the two
     * control assertions pass for a measurement that never happened.
     *
     * <p><b>Why the union and not a single frame.</b> {@link BreakingStateRecorder#lastSeen()} is
     * read once per tick into a growing set: the interesting statement is that a position appeared
     * at all during the window. {@code lastFrame} is the opposite question and is captured
     * separately at {@code disarm} time, because "all entries agree about the stage" only means
     * something within one frame.
     *
     * @param lastFrame optional; filled with the last recorded extraction pass when non-null
     */
    private static Later<Set<BlockPos>> mineAndRecord(Script script, String itemId,
                                                      ResourceKey<Enchantment> enchantment, int level,
                                                      String screenshotName, boolean expectSledgehammer,
                                                      boolean sneak,
                                                      Later<List<BlockBreakingRenderState>> lastFrame) {
        Later<Set<BlockPos>> result = new Later<>("the recorded breaking positions of " + screenshotName);
        Set<BlockPos> seen = new LinkedHashSet<>();
        int[] recordedTicks = {0};

        // Tolerated because "nothing to clear" is a command failure and an empty inventory is
        // exactly the state the first measurement starts from - TestScene.build has just cleared
        // it. The straight-line version never noticed: the Fabric convenience command path
        // swallowed brigadier errors, which is the same swallowing that once left eight game rules
        // in this suite silently dead.
        // The wall is rebuilt before every case, not only once at the start. Mining leaves the
        // world changed - a sledgehammer takes eight connected blocks, a strip miner a whole
        // tunnel - so a case that ran before this one can have taken the very block this one aims
        // at. That is precisely how the first shared run failed on NeoForge: by the fourth case
        // the target read "minecraft:air", and the diagnosis said so outright. The straight-line
        // version had the same dependency and got away with it, which makes it a latent fault
        // rather than a new one. Tolerated because a wall that is already whole fills no block.
        script.command("fill -12 -4 " + TestScene.WALL_Z + " 32 24 " + TestScene.WALL_Z
                + " minecraft:obsidian", true);
        script.command("fill 8 -1 21 12 3 22 minecraft:obsidian", true);
        script.awaitPackets();
        script.idle("let the rebuilt wall reach the client", 10);

        script.command("clear @a", true);
        script.command("item replace entity @a weapon.mainhand with " + itemId);

        if (enchantment != null) {
            script.command("enchant @a " + enchantment.identifier() + " " + level);
        }

        script.awaitPackets();
        script.idle("let the equipment reach the client", 15);

        if (sneak) {
            assertSneakKeyIsBound(script);
            script.harness("hold the sneak key", harness -> harness.holdKey(SNEAK_KEY));
            script.idle("let the sneak state reach the server and come back", 5);
        }

        // Every trigger condition, asserted here and not earlier: this is the last moment before
        // the measurement window opens, and each of these is a way the run could produce an empty
        // recording that reads as "the mod is broken".
        TestScene.assertAimedAt(script, TestScene.TARGET, TestScene.TARGET_FACE);
        assertMainHand(script, itemId, expectSledgehammer);
        assertSneaking(script, sneak);

        if (enchantment != null) {
            assertEnchanted(script, enchantment, level);
        }

        script.harness("start mining " + itemId, harness -> harness.setAttacking(true));

        // The stage has to be past 0 before the last frame is used for the stage check; on
        // obsidian that happens after roughly 17 ticks of mining, well inside this window. With an
        // enchantment the minimum is 1, because the enchanted runs are the ones whose frame feeds
        // assertOneProgressPerFrame and a stage of 0 there cannot be told from a hard coded 0.
        int minStage = enchantment != null ? 1 : 0;
        script.await("mine until the destroy stage reaches " + minStage + " with " + itemId, 200,
                client -> client.gameMode != null
                        && client.gameMode.isDestroying()
                        && client.gameMode.getDestroyStage() >= minStage
                        && client.gameMode.getDestroyStage() <= 9,
                client -> "the player never reached destroy stage " + minStage + " on the target block with "
                        + itemId + " (isDestroying="
                        + (client.gameMode != null && client.gameMode.isDestroying()) + ", stage="
                        + (client.gameMode == null ? -1 : client.gameMode.getDestroyStage()) + "). "
                        + TestScene.describeAim(client));

        script.idle("let the destroy stage settle", 5);

        // Arming happens on the client thread, which is also where the loader's extraction listener
        // feeds the recorder - so no frame can fall between "throw the old measurement away" and
        // "start recording".
        script.act("start recording the breaking render states", client -> BreakingStateRecorder.arm());

        script.await("record the breaking render states for " + MEASURE_TICKS + " ticks",
                MEASURE_TICKS + 40, client -> {
                    seen.addAll(BreakingStateRecorder.lastSeen());
                    return ++recordedTicks[0] >= MEASURE_TICKS;
                });

        script.act("stop recording the breaking render states", client -> {
            BreakingStateRecorder.disarm();
            result.set(new LinkedHashSet<>(seen));

            // Captured here rather than read from the recorder in a later step. disarm() keeps the
            // last pass readable on purpose, but that guarantee is the recorder's and holds only
            // until the next measurement arms; taking the copy at the moment the window closes
            // makes the stage check independent of how many steps come after it.
            if (lastFrame != null) {
                lastFrame.set(List.copyOf(BreakingStateRecorder.lastFrame()));
            }
        });

        // Taken while the mouse is still held, so the cracks are actually on screen. Nothing is
        // asserted on the pixels (see the class javadoc), so the returned handle is dropped.
        script.shot(screenshotName);

        script.harness("stop mining " + itemId, harness -> harness.setAttacking(false));
        script.idle("let the mining stop", 5);

        if (sneak) {
            script.harness("release the sneak key", harness -> harness.releaseKey(SNEAK_KEY));
            script.idle("let the sneak state settle", 5);
        }

        return result;
    }

    /**
     * The sneak binding really is the key {@link #SNEAK_KEY} presses.
     *
     * <p>New in the shared form and not optional: the harness presses a raw GLFW code, so a sneak
     * key that moved would leave the player standing while the test believed it sneaks - and the
     * sneaking measurement would then correctly record nothing and be reported as a broken mod.
     */
    private static void assertSneakKeyIsBound(Script script) {
        script.act("the sneak binding sits on the key the harness presses", client -> {
            if (!client.options.keyShift.matches(InputConstants.Type.KEYSYM.getOrCreate(SNEAK_KEY))) {
                throw new AssertionError("The sneak binding is not on GLFW key " + SNEAK_KEY
                        + " any more (it says \"" + client.options.keyShift.saveString() + "\"), so holding "
                        + "that key would not make the player sneak and the Strip Miner measurement would "
                        + "record nothing for a reason that has nothing to do with the mod.");
            }
        });
    }

    private static void assertMainHand(Script script, String itemId, boolean expectSledgehammer) {
        script.act("the main hand holds " + itemId, client -> {
            boolean isSledgehammer = client.player != null
                    && client.player.getMainHandItem().getItem() instanceof SledgehammerItem;

            if (isSledgehammer != expectSledgehammer) {
                throw new AssertionError("Wrong tool in the main hand for " + itemId
                        + " (SledgehammerItem=" + isSledgehammer + "). " + TestScene.describeAim(client));
            }
        });
    }

    private static void assertSneaking(Script script, boolean expected) {
        script.act("the player's sneak state is " + expected, client -> {
            boolean sneaking = client.player != null && client.player.isShiftKeyDown();

            if (sneaking != expected) {
                throw new AssertionError("The player's sneak state is " + sneaking + " but the measurement "
                        + "needs " + expected + ". MultiBlockBreakingSupport reads player.isShiftKeyDown() "
                        + "for both the Strip Miner and the Vein Miner branch.");
            }
        });
    }

    /** Reads the enchantment component directly, so a broken mod helper cannot fake this. */
    private static void assertEnchanted(Script script, ResourceKey<Enchantment> enchantment, int level) {
        script.act("the held tool carries " + enchantment.identifier() + " at level " + level, client -> {
            int actual = 0;

            if (client.player == null) {
                actual = -1;
            } else {
                ItemStack stack = client.player.getMainHandItem();
                ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);

                if (enchantments != null) {
                    for (Holder<Enchantment> holder : enchantments.keySet()) {
                        if (holder.is(enchantment)) {
                            actual = enchantments.getLevel(holder);
                        }
                    }
                }
            }

            if (actual != level) {
                throw new AssertionError("The client's copy of the held tool carries " + enchantment.identifier()
                        + " at level " + actual + " instead of " + level + ". The /enchant command did not "
                        + "reach the client, so the branch under test would never run.");
            }
        });
    }

    private static void assertSolid(Script script, BlockPos pos) {
        script.act("there is a solid block at " + pos, client -> {
            if (client.level == null || client.level.getBlockState(pos).isAir()) {
                throw new AssertionError("Scene setup failed: " + pos + " is air on the client, but the Strip "
                        + "Miner measurement needs a solid block there - getStripMinerBlocks stops at the "
                        + "first air block.");
            }
        });
    }
}
