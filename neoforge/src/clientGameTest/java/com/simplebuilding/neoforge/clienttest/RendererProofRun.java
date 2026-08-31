package com.simplebuilding.neoforge.clienttest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.SledgehammerItem;
import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Automated proof that the three in-world renderers actually draw on NeoForge.
 *
 * <h2>What is proven, and how</h2>
 * <ul>
 *   <li><b>BlockHighlightRenderer</b> and <b>BuildingWandPreviewRenderer</b> submit geometry through
 *       {@code SubmitCustomGeometryEvent}. They are proven by screenshot difference: a baseline
 *       screenshot without the triggering item, one with it, and a control screenshot after the item
 *       is taken away again which has to return to the baseline.</li>
 *   <li><b>MultiBlockBreakingSupport</b> draws nothing itself, it appends
 *       {@code BlockBreakingRenderState} entries via {@code ExtractLevelRenderStateEvent}. It is
 *       proven by reading that list back from a second listener that runs after the mod's, with a
 *       vanilla pickaxe as the control (must contribute nothing) and the sledgehammer as the signal
 *       (must contribute all eight connected blocks).</li>
 * </ul>
 *
 * <h2>Guards against false positives</h2>
 * The scene is frozen (fixed time, no weather, no mobs, no random ticks, no view bob, no dynamic
 * FOV, HUD hidden). Hiding the HUD also removes the first person hand - in 26.2
 * {@code GameRenderer.renderItemInHand} returns early when {@code GuiRenderState.isHudHidden} is
 * set - so swapping the held item cannot change pixels by itself. Every phase first measures a
 * <em>noise floor</em> from two screenshots of the unchanged scene and asserts it is essentially
 * zero, and every trigger condition the renderer checks is asserted before the screenshot is taken,
 * so a scene that was set up wrong fails as a setup error and can never be reported as "the
 * renderer draws nothing".
 *
 * <h2>Shutdown</h2>
 * The run never logs out. {@code Minecraft.disconnect} pumps client ticks itself to draw its
 * progress screen and therefore never returns when it is called from inside a client tick - that is
 * exactly how the previous attempt at this test hung. Instead the result is written and the JVM is
 * halted (exit 0 proven, non-zero not proven).
 *
 * <p>Geometry is identical to the Fabric client game tests so a result means the same on all three
 * client targets:
 * <pre>
 *   wall   z = 20, x = -12..32, y = -4..24
 *   floor  y = -1, z = 10..19
 *   player x = 10.5, y = 0.0, z = 16.5, yaw 0 (facing +Z / south), pitch 0
 *   eye    y = 1.62  ->  crosshair ray hits block (10, 1, 20) through its NORTH face
 * </pre>
 */
final class RendererProofRun {

    /** Block the crosshair is aimed at. */
    private static final BlockPos TARGET = new BlockPos(10, 1, 20);

    /** Face of {@link #TARGET} the crosshair ray enters through. */
    private static final Direction TARGET_FACE = Direction.NORTH;

    /** Z plane of the wall. */
    private static final int WALL_Z = 20;

    /** Z plane one block in front of the wall - free air, this is where octant boxes are visible. */
    private static final int FRONT_Z = 19;

    private static final BlockPos OCTANT_POS_1 = new BlockPos(9, 1, FRONT_Z);
    private static final BlockPos OCTANT_POS_2 = new BlockPos(11, 1, FRONT_Z);

    /** Ticks the breaking render states are sampled for. */
    private static final int MEASURE_TICKS = 20;

    private final TestScript script = new TestScript();

    private boolean finished;
    private boolean inWorld;
    private boolean miningActive;
    private boolean reportedInputLockout;

    private ScreenshotDiff.Diff highlightNoise;
    private ScreenshotDiff.Diff wandNoise;
    private Set<BlockPos> pickaxeStates = Set.of();
    private Set<BlockPos> sledgehammerStates = Set.of();

    private final List<String> proven = new ArrayList<>();

    RendererProofRun() {
        buildScript();
    }

    // ------------------------------------------------------------------ driving

    void onClientTick() {
        if (finished) {
            return;
        }

        try {
            closeStrayPauseScreen();

            // Runs at the tail of Minecraft.tick(), i.e. after handleKeybinds() has already acted
            // on the held attack button and before the frame is rendered. Clearing the lockout here
            // means the next tick's handleKeybinds() sees a zero and actually mines.
            if (miningActive) {
                clearInputLockout();
            }

            if (script.tick(LOGGER)) {
                succeed();
            }
        } catch (Throwable t) {
            fail(t);
        }
    }

    private static final TestScript.StepLogger LOGGER = new TestScript.StepLogger() {
        @Override
        public void stepFinished(int number, int total, String name, int ticks) {
            Log.info("step " + number + "/" + total + " done after " + ticks + " ticks: " + name);
        }

        @Override
        public void stillWaiting(int number, int total, String name, int ticks, int timeoutTicks) {
            Log.info("step " + number + "/" + total + " still waiting (" + ticks + "/" + timeoutTicks
                    + " ticks): " + name + " | " + describeAim());
        }
    };

    /**
     * The client pauses itself when the window loses focus. On an unattended run that would freeze
     * the world mid-test, so the option is switched off - this is the belt to that option's braces.
     */
    private void closeStrayPauseScreen() {
        Minecraft client = Minecraft.getInstance();

        if (inWorld && client.gui.screen() instanceof PauseScreen) {
            Log.info("a pause screen appeared (window focus lost?) - closing it again");
            client.gui.setScreen(null);
        }
    }

    // ------------------------------------------------------------------ the script

    private void buildScript() {
        script.act("freeze the client options", this::applyDeterministicOptions);
        // Not "wait for TitleScreen": NeoForge can legitimately put another screen in front of it
        // (the mod-warning screen for instance). All that matters is that loading has finished and
        // the client is idle in a menu.
        script.await("wait until the client has finished loading", ticks(180), () -> {
            Minecraft client = Minecraft.getInstance();
            return client.gui.overlay() == null && client.gui.screen() != null && client.level == null;
        });
        script.act("report the menu the client came up in", () -> Log.info("client is up, screen = "
                + Minecraft.getInstance().gui.screen().getClass().getName()
                + (Minecraft.getInstance().gui.screen() instanceof TitleScreen ? " (title screen)" : "")));

        script.act("create and open the test world", this::createTestWorld);
        script.await("wait for level, player and server", ticks(600), () -> {
            Minecraft client = Minecraft.getInstance();
            MinecraftServer server = client.getSingleplayerServer();
            return client.level != null
                    && client.player != null
                    && client.gameMode != null
                    && server != null
                    && server.isReady()
                    && client.gui.screen() == null;
        });
        script.act("mark that we are in the world", () -> inWorld = true);
        script.act("freeze the client options again", this::applyDeterministicOptions);
        script.act("release the mouse (a grabbed mouse turns the view on any desktop movement)",
                RendererProofRun::releaseMouse);

        // ---------------- phase 1+2: the two SubmitCustomGeometryEvent renderers ----------------
        buildScene("minecraft:stone", "creative");
        blockHighlightPhase();
        buildingWandPhase();

        // ---------------- phase 3: the ExtractLevelRenderStateEvent renderer --------------------
        buildScene("minecraft:obsidian", "survival");
        multiBlockBreakingPhase();
    }

    /** Wipes the working volume, builds wall and floor, positions the player, verifies the aim. */
    private void buildScene(String wallBlockId, String gameMode) {
        commands("build the scene (" + wallBlockId + ", " + gameMode + ")",
                // MC 26.2 renamed every game rule to snake_case and dropped some of the old ones
                // (doFireTick is now the integer fire_spread_radius_around_player). The names below
                // are taken from net.minecraft.world.level.gamerules.GameRules of this version, and
                // a wrong one aborts the run instead of being silently ignored.
                "gamerule advance_time false",
                "gamerule advance_weather false",
                "gamerule spawn_mobs false",
                "gamerule spawn_monsters false",
                "gamerule spawn_phantoms false",
                "gamerule spawn_patrols false",
                "gamerule spawn_wandering_traders false",
                "gamerule fire_spread_radius_around_player 0",
                "gamerule mob_griefing false",
                "gamerule random_tick_speed 0",
                "time set noon",
                "weather clear",
                "gamemode " + gameMode + " @a",
                // May legitimately match nothing, which brigadier reports as an error.
                "?kill @e[type=!minecraft:player]",
                // On the first pass the whole volume is already air (the superflat surface is far
                // below), and vanilla's fill reports "no blocks were filled" as an error.
                "?fill -12 -4 10 32 24 " + WALL_Z + " minecraft:air",
                "fill -12 -4 " + WALL_Z + " 32 24 " + WALL_Z + " " + wallBlockId,
                "fill -12 -1 10 32 -1 " + FRONT_Z + " " + wallBlockId,
                "?clear @a",
                "tp @a 10.5 0.0 16.5 0.0 0.0");

        script.idle("let the scene settle", 40);
        script.await("wait until the chunk meshes are built", ticks(180), () -> {
            Minecraft client = Minecraft.getInstance();
            return client.levelRenderer.hasRenderedAllSections()
                    && client.levelRenderer.isSectionCompiledAndVisible(TARGET);
        });
        script.act("hide the HUD (this also removes the first person hand)", this::hideHud);
        script.idle("settle after hiding the HUD", 20);
        script.await("crosshair has to be on " + TARGET + " (" + TARGET_FACE + ")", ticks(15),
                () -> aimedAt(TARGET, TARGET_FACE));
        script.act("report the aim", () -> Log.info("scene ready - " + describeAim()));
    }

    private void blockHighlightPhase() {
        Shot baseline = shot("highlight-a-empty-hand");
        script.idle("wait between the two baseline screenshots", 10);
        Shot baselineAgain = shot("highlight-b-empty-hand-again");

        script.act("measure the noise floor", () -> {
            highlightNoise = ScreenshotDiff.compare("noise floor (empty hand vs. empty hand)",
                    baseline.path(), baselineAgain.path());
            ScreenshotDiff.assertUnchanged(highlightNoise);
        });

        // --- sledgehammer highlight ---
        commands("give the sledgehammer",
                "item replace entity @a weapon.mainhand with simplebuilding:netherite_sledgehammer");
        script.idle("let the item arrive on the client", 15);
        script.act("assert the sledgehammer trigger conditions", this::assertSledgehammerTriggerConditions);

        Shot withSledgehammer = shot("highlight-c-sledgehammer");
        script.act("assert BlockHighlightRenderer (sledgehammer) drew", () -> {
            ScreenshotDiff.Diff signal = ScreenshotDiff.compare("sledgehammer highlight",
                    baseline.path(), withSledgehammer.path());
            ScreenshotDiff.assertDrew("BlockHighlightRenderer (sledgehammer)", highlightNoise, signal);
            proven.add("BlockHighlightRenderer (sledgehammer): " + signal.changedPixels() + " changed pixels");
        });

        // --- control: taking it away has to restore the baseline ---
        commands("take the sledgehammer away", "?clear @a");
        script.idle("let the empty hand arrive on the client", 15);
        script.act("assert the main hand is empty again", () -> {
            Minecraft client = Minecraft.getInstance();

            if (client.player == null || !client.player.getMainHandItem().isEmpty()) {
                throw new AssertionError("Control step failed: main hand was not cleared. " + describeAim());
            }
        });

        Shot cleared = shot("highlight-d-cleared-again");
        script.act("assert the picture is back at the baseline", () -> ScreenshotDiff.assertBackToBaseline(
                "removing the sledgehammer", highlightNoise,
                ScreenshotDiff.compare("control (highlight removed again)", baseline.path(), cleared.path())));

        // --- octant highlight ---
        commands("give the octant with both positions set",
                "item replace entity @a weapon.mainhand with simplebuilding:octant["
                        + "minecraft:custom_data={"
                        + "Pos1:[I;" + OCTANT_POS_1.getX() + "," + OCTANT_POS_1.getY() + "," + OCTANT_POS_1.getZ() + "],"
                        + "Pos2:[I;" + OCTANT_POS_2.getX() + "," + OCTANT_POS_2.getY() + "," + OCTANT_POS_2.getZ() + "]"
                        + "}]");
        script.idle("let the item arrive on the client", 15);
        script.act("assert the octant trigger conditions", this::assertOctantTriggerConditions);

        Shot withOctant = shot("highlight-e-octant");
        script.act("assert BlockHighlightRenderer (octant) drew", () -> {
            // Right half only: holding an octant also brings up the mod's rangefinder info panel in
            // the upper left (it is a HUD layer and still renders while the HUD is hidden), and that
            // panel must not be able to stand in for the in-world corner boxes. The right half holds
            // the complete box around OCTANT_POS_2 and none of the panel.
            ScreenshotDiff.Diff signal = ScreenshotDiff.compare(
                    "octant highlight (right half of the frame, no HUD panel in it)",
                    baseline.path(), withOctant.path(), true);
            ScreenshotDiff.assertDrew("BlockHighlightRenderer (octant)", highlightNoise, signal);
            proven.add("BlockHighlightRenderer (octant): " + signal.changedPixels()
                    + " changed pixels in the right half of the frame");
        });

        commands("take the octant away", "?clear @a");
        script.await("main hand empty again", ticks(15), () -> {
            Minecraft client = Minecraft.getInstance();
            return client.player != null && client.player.getMainHandItem().isEmpty();
        });
        script.idle("let the empty hand reach the renderer", 10);

        Shot octantRemoved = shot("highlight-f-octant-removed");
        script.act("assert the picture is back at the baseline", () -> ScreenshotDiff.assertBackToBaseline(
                "removing the octant", highlightNoise,
                ScreenshotDiff.compare("control (octant removed again, right half)",
                        baseline.path(), octantRemoved.path(), true)));
    }

    private void buildingWandPhase() {
        // Material for the preview goes in before the baseline so it is not part of the diff.
        commands("clear the hands and put stone in the offhand",
                "?clear @a",
                "item replace entity @a weapon.offhand with minecraft:stone 64");
        script.idle("let the offhand material arrive", 15);

        Shot baseline = shot("wand-a-no-wand");
        script.idle("wait between the two baseline screenshots", 10);
        Shot baselineAgain = shot("wand-b-no-wand-again");

        script.act("measure the noise floor", () -> {
            wandNoise = ScreenshotDiff.compare("noise floor (no wand vs. no wand)",
                    baseline.path(), baselineAgain.path());
            ScreenshotDiff.assertUnchanged(wandNoise);
        });

        commands("give the netherite building wand",
                "item replace entity @a weapon.mainhand with simplebuilding:netherite_building_wand");
        script.idle("let the wand arrive on the client", 15);
        script.act("assert the wand preview trigger conditions", this::assertPreviewTriggerConditions);

        Shot withWand = shot("wand-c-preview");
        script.act("assert BuildingWandPreviewRenderer drew", () -> {
            ScreenshotDiff.Diff signal = ScreenshotDiff.compare("building wand ghost preview",
                    baseline.path(), withWand.path());
            ScreenshotDiff.assertDrew("BuildingWandPreviewRenderer", wandNoise, signal);
            proven.add("BuildingWandPreviewRenderer: " + signal.changedPixels() + " changed pixels");
        });

        commands("take the wand away", "?clear @a",
                "item replace entity @a weapon.offhand with minecraft:stone 64");
        script.idle("let the empty main hand arrive", 15);

        Shot withoutWand = shot("wand-d-removed-again");
        script.act("assert the picture is back at the baseline", () -> ScreenshotDiff.assertBackToBaseline(
                "removing the wand", wandNoise,
                ScreenshotDiff.compare("control (wand removed again)", baseline.path(), withoutWand.path())));
    }

    private void multiBlockBreakingPhase() {
        mineAndRecord("minecraft:netherite_pickaxe", false, "breaking-a-vanilla-pickaxe",
                seen -> pickaxeStates = seen);

        script.act("assert the vanilla pickaxe contributed nothing", () -> {
            Log.info("breaking states while mining with a vanilla pickaxe: " + pickaxeStates);
            Set<BlockPos> leaked = new LinkedHashSet<>(pickaxeStates);
            leaked.retainAll(neighboursOfTarget());

            if (!leaked.isEmpty()) {
                throw new AssertionError("Control failed: mining with a plain netherite pickaxe already produced "
                        + "breaking cracks on connected blocks " + leaked + ". The sledgehammer measurement "
                        + "below would not be attributable to the mod.");
            }

            if (!pickaxeStates.contains(TARGET)) {
                throw new AssertionError("Control failed: not even the block being mined (" + TARGET + ") showed up "
                        + "in the breaking render states " + pickaxeStates + ". The observing listener or the "
                        + "mining setup is broken, so the sledgehammer measurement would prove nothing.");
            }
        });

        mineAndRecord("simplebuilding:netherite_sledgehammer", true, "breaking-b-sledgehammer",
                seen -> sledgehammerStates = seen);

        script.act("assert MultiBlockBreakingSupport contributed the connected blocks", () -> {
            Log.info("breaking states while mining with the sledgehammer: " + sledgehammerStates);
            Set<BlockPos> expected = neighboursOfTarget();
            Set<BlockPos> missing = new LinkedHashSet<>(expected);
            missing.removeAll(sledgehammerStates);

            if (!missing.isEmpty()) {
                throw new AssertionError("MultiBlockBreakingSupport did not contribute anything: the breaking "
                        + "render states are missing " + missing.size() + " of the " + expected.size()
                        + " connected blocks " + missing + ". Recorded were: " + sledgehammerStates
                        + ". Trigger conditions were asserted before measuring.");
            }

            proven.add("MultiBlockBreakingSupport: added all " + expected.size()
                    + " connected blocks to the breaking render states");
        });
    }

    private interface SetSink {
        void accept(Set<BlockPos> positions);
    }

    /** Equips {@code itemId}, mines {@link #TARGET} and records every position that shows up. */
    private void mineAndRecord(String itemId, boolean expectSledgehammer, String screenshotName, SetSink sink) {
        commands("equip " + itemId, "?clear @a", "item replace entity @a weapon.mainhand with " + itemId);
        script.idle("let the tool arrive on the client", 15);
        script.await("crosshair still on " + TARGET, ticks(15), () -> aimedAt(TARGET, TARGET_FACE));
        script.act("assert the right tool is in the main hand", () -> {
            Minecraft client = Minecraft.getInstance();
            boolean isSledgehammer = client.player != null
                    && client.player.getMainHandItem().getItem() instanceof SledgehammerItem;

            if (isSledgehammer != expectSledgehammer) {
                throw new AssertionError("Wrong tool in the main hand for " + itemId + " (SledgehammerItem="
                        + isSledgehammer + "). " + describeAim());
            }
        });

        // Mining has to go through vanilla's own input path. Minecraft.handleKeybinds() calls
        // continueAttack(false) on every tick in which the attack key is not held with the mouse
        // grabbed, and that calls MultiPlayerGameMode.stopDestroyBlock() - which resets the destroy
        // progress. Driving continueDestroyBlock() directly from a tick handler therefore never
        // gets past destroy stage -1. So the test presses the button for real.
        script.await("the game window has to be focused (the mouse cannot be grabbed otherwise)",
                ticks(20), () -> Minecraft.getInstance().isWindowActive());
        script.act("grab the mouse and hold the attack button", () -> {
            Minecraft client = Minecraft.getInstance();
            miningActive = true;
            clearInputLockout();
            client.mouseHandler.grabMouse();
            client.options.keyAttack.setDown(true);

            if (!client.mouseHandler.isMouseGrabbed()) {
                throw new AssertionError("The mouse could not be grabbed although the window reports itself as "
                        + "focused, so vanilla will never start breaking a block. This is a test environment "
                        + "problem, not a renderer problem.");
            }
        });
        script.await("wait until the client reports a destroy stage", ticks(10), () -> {
            Minecraft client = Minecraft.getInstance();
            return client.gameMode != null
                    && client.gameMode.isDestroying()
                    && client.gameMode.getDestroyStage() >= 0
                    && client.gameMode.getDestroyStage() <= 9;
        });
        script.idle("let the breaking settle", 5);
        script.await("crosshair still on " + TARGET + " while mining", ticks(5),
                () -> aimedAt(TARGET, TARGET_FACE));

        Set<BlockPos> seen = new LinkedHashSet<>();
        script.step("sample the breaking render states for " + MEASURE_TICKS + " ticks", MEASURE_TICKS + 20, t -> {
            if (t == 0) {
                seen.clear();
                BreakingStateRecorder.arm();
                return false;
            }

            seen.addAll(BreakingStateRecorder.lastSeen());
            return t >= MEASURE_TICKS;
        });

        Shot screenshot = shot(screenshotName);

        script.act("stop mining", () -> {
            BreakingStateRecorder.disarm();
            miningActive = false;
            releaseMouse();
            Minecraft client = Minecraft.getInstance();

            if (client.gameMode != null) {
                client.gameMode.stopDestroyBlock();
            }

            Log.info("screenshot for visual inspection only: " + screenshot.path());
            sink.accept(new LinkedHashSet<>(seen));
        });
        script.idle("settle after mining", 10);
    }

    /** The eight blocks the sledgehammer breaks alongside the target, in the wall plane. */
    private static Set<BlockPos> neighboursOfTarget() {
        Set<BlockPos> positions = new LinkedHashSet<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    positions.add(TARGET.offset(dx, dy, 0));
                }
            }
        }

        return positions;
    }

    // ------------------------------------------------------------------ trigger assertions

    private void assertSledgehammerTriggerConditions() {
        if (!aimedAt(TARGET, TARGET_FACE)) {
            throw new AssertionError("Sledgehammer highlight trigger conditions not met: " + describeAim());
        }

        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null) {
            throw new AssertionError("no client player or level");
        }

        if (!(client.player.getMainHandItem().getItem() instanceof SledgehammerItem)) {
            throw new AssertionError("Sledgehammer highlight trigger conditions not met: main hand does not hold a "
                    + "SledgehammerItem but " + client.player.getMainHandItem());
        }

        // The eight highlighted positions are the neighbours of the target block in the wall plane.
        // All of them must be solid, otherwise the renderer legitimately draws nothing.
        for (BlockPos neighbour : neighboursOfTarget()) {
            if (client.level.getBlockState(neighbour).isAir()) {
                throw new AssertionError("Sledgehammer highlight trigger conditions not met: neighbour " + neighbour
                        + " of the target block is air, so there is nothing for the highlight to outline");
            }
        }
    }

    private void assertOctantTriggerConditions() {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            throw new AssertionError("no client player");
        }

        if (!(client.player.getMainHandItem().getItem() instanceof OctantItem)) {
            throw new AssertionError("Octant highlight trigger conditions not met: main hand does not hold an "
                    + "OctantItem but " + client.player.getMainHandItem());
        }
    }

    /** Recomputes exactly what the wand renderer computes and asserts there is something to draw. */
    private void assertPreviewTriggerConditions() {
        if (!aimedAt(TARGET, TARGET_FACE)) {
            throw new AssertionError("Building wand preview trigger conditions not met: " + describeAim());
        }

        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null) {
            throw new AssertionError("no client player or level");
        }

        if (!(client.player.getMainHandItem().getItem() instanceof BuildingWandItem wand)) {
            throw new AssertionError("Building wand preview trigger conditions not met: main hand does not hold a "
                    + "BuildingWandItem but " + client.player.getMainHandItem());
        }

        BlockHitResult hit = (BlockHitResult) client.hitResult;
        Map<BlockPos, BlockState> preview = BuildingWandItem.getPreviewStates(client.level, client.player,
                client.player.getMainHandItem(), hit.getBlockPos(), hit.getDirection(), wand.getWandSquareDiameter());

        int renderable = 0;

        for (BlockPos pos : preview.keySet()) {
            if (client.level.getBlockState(pos).canBeReplaced()) {
                renderable++;
            }
        }

        if (renderable < 9) {
            throw new AssertionError("Building wand preview trigger conditions not met: only " + renderable
                    + " renderable preview positions, expected the 11x11 plane in front of the wall. " + describeAim());
        }

        Log.info("building wand preview positions: " + renderable);
    }

    private static boolean aimedAt(BlockPos expected, Direction expectedFace) {
        Minecraft client = Minecraft.getInstance();
        return client.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK
                && hit.getBlockPos().equals(expected)
                && hit.getDirection() == expectedFace;
    }

    private static String describeAim() {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            return "no client player";
        }

        String hit;

        if (client.hitResult == null) {
            hit = "none";
        } else if (client.hitResult instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK) {
            hit = "block " + blockHit.getBlockPos() + " face " + blockHit.getDirection();
        } else {
            hit = String.valueOf(client.hitResult.getType());
        }

        String target = client.level == null ? "?" : client.level.getBlockState(TARGET).toString();

        return String.format(Locale.ROOT,
                "player=(%.3f, %.3f, %.3f) yaw=%.2f pitch=%.2f mainHand=%s hitResult=%s target%s=%s "
                        + "windowFocused=%s mouseGrabbed=%s attackKeyDown=%s destroying=%s stage=%d "
                        + "screen=%s usingItem=%s piercingWeapon=%s mode=%s instabuild=%s "
                        + "destroySpeed=%.3f destroyProgressPerTick=%.5f overlay=%s missTime=%d",
                client.player.getX(), client.player.getY(), client.player.getZ(),
                client.player.getYRot(), client.player.getXRot(), client.player.getMainHandItem(), hit,
                TARGET, target, client.isWindowActive(), client.mouseHandler.isMouseGrabbed(),
                client.options.keyAttack.isDown(),
                client.gameMode != null && client.gameMode.isDestroying(),
                client.gameMode == null ? -99 : client.gameMode.getDestroyStage(),
                client.gui.screen() == null ? "none" : client.gui.screen().getClass().getSimpleName(),
                client.player.isUsingItem(),
                client.player.getMainHandItem().has(DataComponents.PIERCING_WEAPON),
                client.gameMode == null ? "?" : client.gameMode.getPlayerMode(),
                client.player.getAbilities().instabuild,
                client.level == null ? -1.0f : client.player.getDestroySpeed(client.level.getBlockState(TARGET)),
                client.level == null ? -1.0f
                        : client.level.getBlockState(TARGET).getDestroyProgress(client.player, client.level, TARGET),
                client.gui.overlay() == null ? "none" : client.gui.overlay().getClass().getName(),
                missTime());
    }

    // ------------------------------------------------------------------ client plumbing

    private void applyDeterministicOptions() {
        Minecraft client = Minecraft.getInstance();

        // Without this the client opens the pause menu (and stops the world) the moment the window
        // loses focus - fatal for an unattended run.
        client.options.pauseOnLostFocus = false;
        client.options.fov().set(70);
        client.options.fovEffectScale().set(0.0);
        client.options.screenEffectScale().set(0.0);
        client.options.bobView().set(false);
        client.options.entityShadows().set(false);
        client.options.cloudStatus().set(CloudStatus.OFF);
        client.options.particles().set(ParticleStatus.MINIMAL);
        client.options.framerateLimit().set(60);
        client.options.setCameraType(CameraType.FIRST_PERSON);
    }

    /**
     * Reads {@code Minecraft.missTime}, vanilla's input lockout counter.
     *
     * <p>{@code Minecraft.tick()} sets it to 10000 for as long as any screen is open and afterwards
     * only counts it down by one per tick, and {@code continueAttack} returns without doing
     * anything at all while it is above zero. It normally clears itself the moment the attack
     * button is <em>not</em> held, but the automated run reaches the mining phase with it still at
     * 10000, which would mean an eight minute wait before the client starts breaking anything.
     */
    private static int missTime() {
        try {
            return missTimeField().getInt(Minecraft.getInstance());
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Clears the input lockout so vanilla acts on the held attack button immediately.
     *
     * <p>This is the only place where the test reaches into a private vanilla field, and it fakes
     * nothing about the renderer: {@code missTime} exists purely so that the click which closed a
     * GUI does not also punch the world. The block breaking itself still runs through vanilla's own
     * {@code Minecraft.continueAttack} -> {@code MultiPlayerGameMode.continueDestroyBlock}, driven
     * by a genuinely held attack button.
     */
    private void clearInputLockout() {
        int before = missTime();

        if (before <= 0) {
            return;
        }

        try {
            missTimeField().setInt(Minecraft.getInstance(), 0);

            if (!reportedInputLockout) {
                reportedInputLockout = true;
                Log.info("cleared vanilla's input lockout (Minecraft.missTime was " + before + "); this is "
                        + "re-armed while any screen is open and only counts down by one per tick");
            }
        } catch (Throwable t) {
            throw new AssertionError("Could not clear Minecraft.missTime (" + before + "), so vanilla would "
                    + "ignore the held attack button for the next " + before + " ticks", t);
        }
    }

    private static java.lang.reflect.Field missTimeField() throws NoSuchFieldException {
        java.lang.reflect.Field field = Minecraft.class.getDeclaredField("missTime");
        field.setAccessible(true);
        return field;
    }

    /** Ungrabs the mouse so physical mouse movement can no longer turn the camera. */
    private static void releaseMouse() {
        Minecraft client = Minecraft.getInstance();
        client.options.keyAttack.setDown(false);
        client.mouseHandler.releaseMouse();
    }

    private void hideHud() {
        Minecraft client = Minecraft.getInstance();

        if (!client.gui.hud.isHidden()) {
            client.gui.hud.toggle();
        }
    }

    private void createTestWorld() {
        Minecraft client = Minecraft.getInstance();
        String levelName = "sb-renderer-proof-" + System.currentTimeMillis();

        LevelSettings settings = new LevelSettings(levelName, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, true), true,
                WorldDataConfiguration.DEFAULT);

        Log.info("creating a flat singleplayer world '" + levelName + "'");

        client.createWorldOpenFlows().createFreshLevel(levelName, settings,
                new WorldOptions(0L, false, false),
                provider -> provider.lookupOrThrow(Registries.WORLD_PRESET)
                        .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                client.gui.screen());
    }

    private Shot shot(String name) {
        Shot shot = new Shot(name);
        script.step("screenshot " + name, ticks(30), t -> {
            if (t == 0) {
                assertCameraUnmoved();
            }

            return shot.poll();
        });
        return shot;
    }

    /**
     * Every screenshot is compared to another screenshot, so the camera has to be in exactly the
     * same place for all of them. Minecraft grabs the mouse when a world is entered, and a grabbed
     * mouse turns the view on any stray movement of the physical mouse on the developer's desk -
     * which produced a completely different picture in one run and would otherwise have been
     * reported as "the renderer draws something that does not go away again". The mouse is released
     * for the screenshot phases (see {@link #releaseMouse()}), and this check makes any remaining
     * camera movement a loud setup failure instead of a renderer verdict.
     */
    private static void assertCameraUnmoved() {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            throw new AssertionError("no client player");
        }

        boolean moved = Math.abs(client.player.getYRot()) > 0.01f
                || Math.abs(client.player.getXRot()) > 0.01f
                || Math.abs(client.player.getX() - 10.5) > 0.01
                || Math.abs(client.player.getY()) > 0.01
                || Math.abs(client.player.getZ() - 16.5) > 0.01;

        if (moved) {
            throw new AssertionError("The camera moved away from the fixed test position, so the screenshots "
                    + "are no longer comparable: " + describeAim());
        }
    }

    /**
     * Runs commands on the integrated server. A command whose name starts with {@code ?} is allowed
     * to fail (brigadier reports "no entity was found" as an error, which {@code kill}/{@code clear}
     * legitimately hit); every other failure aborts the run, so a typo can never be mistaken for a
     * renderer that does not draw.
     */
    private void commands(String label, String... commands) {
        Job[] job = new Job[1];

        script.step("commands: " + label, ticks(60), t -> {
            if (job[0] == null) {
                job[0] = submit(commands);
                return false;
            }

            if (job[0].error != null) {
                throw new AssertionError("Command batch '" + label + "' failed", job[0].error);
            }

            return job[0].done;
        });

        script.idle("let '" + label + "' reach the client", 10);
    }

    private static final class Job {
        volatile boolean done;
        volatile Throwable error;
    }

    private static Job submit(String[] commands) {
        Minecraft client = Minecraft.getInstance();
        MinecraftServer server = client.getSingleplayerServer();

        if (server == null) {
            throw new AssertionError("no integrated server - the test world was not opened");
        }

        Job job = new Job();

        server.execute(() -> {
            try {
                CommandSourceStack source = server.createCommandSourceStack();

                for (String raw : commands) {
                    boolean optional = raw.startsWith("?");
                    String command = optional ? raw.substring(1) : raw;

                    try {
                        server.getCommands().getDispatcher().execute(command, source);
                    } catch (CommandSyntaxException e) {
                        if (!optional) {
                            throw new AssertionError("Command failed: /" + command + " -> " + e.getMessage(), e);
                        }

                        Log.info("optional command reported nothing to do: /" + command + " (" + e.getMessage() + ")");
                    }
                }
            } catch (Throwable t) {
                job.error = t;
            } finally {
                job.done = true;
            }
        });

        return job;
    }

    private static int ticks(int seconds) {
        return seconds * 20;
    }

    // ------------------------------------------------------------------ result

    private void succeed() {
        finished = true;
        StringBuilder report = new StringBuilder("RESULT: PASS - all three in-world renderers draw on NeoForge 26.2\n");

        for (String line : proven) {
            report.append("  proven: ").append(line).append('\n');
        }

        report(report.toString(), 0, null);
    }

    private void fail(Throwable t) {
        finished = true;
        report("RESULT: FAIL - " + t + "\n  failing step: " + script.currentStepName()
                + " (" + script.currentStepIndex() + "/" + script.stepCount() + ", after "
                + script.currentStepTicks() + " ticks)\n", 1, t);
    }

    /**
     * Writes the result and kills the JVM. Deliberately no {@code disconnect} and no world close:
     * {@code Minecraft.disconnect} pumps client ticks itself in order to draw its progress screen,
     * so calling it from inside a client tick never returns.
     */
    private void report(String summary, int exitCode, Throwable failure) {
        try {
            if (failure != null) {
                Log.error(summary, failure);
            } else {
                Log.info(summary);
            }

            Log.info("screenshots: " + Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots"));

            Path file = Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots").resolve("RESULT.txt");
            Files.createDirectories(file.getParent());
            Files.writeString(file, summary);
        } catch (Throwable t) {
            System.out.println(Log.PREFIX + " could not write the result file: " + t);
        }

        System.out.flush();
        System.err.flush();
        Runtime.getRuntime().halt(exitCode);
    }
}
