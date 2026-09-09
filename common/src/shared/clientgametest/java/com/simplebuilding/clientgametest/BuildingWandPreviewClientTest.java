package com.simplebuilding.clientgametest;

import java.nio.file.Path;
import java.util.Map;

import com.simplebuilding.items.custom.BuildingWandItem;
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
 * <p><b>The order of the steps is the argument, not a style.</b> A pixel difference on its own
 * proves nothing, so this test always walks the same four stages, and every one of them is a
 * precondition for the next being readable at all:
 * <ol>
 *   <li><b>noise floor</b> - two screenshots of the very same, untouched scene. In the frozen
 *       scene of {@link TestScene} that difference has to be essentially zero, and it is asserted
 *       ({@link ScreenshotDiff#assertUnchanged}), so a scene that is not actually static fails
 *       here as a setup error instead of quietly producing "differences" further down. The
 *       measured value is then the baseline every later claim is scaled against;</li>
 *   <li><b>every trigger condition</b> - re-computed from the client's own state right before the
 *       measurement, including the preview map the renderer itself would build. Without this a
 *       missing difference could always be explained by "the test never armed the renderer";</li>
 *   <li><b>the measurement</b> - the screenshot with the trigger in place, against the baseline;</li>
 *   <li><b>the control</b> - take the trigger away again and require the baseline picture back.
 *       Without it, "the picture drifts anyway" would explain the signal just as well as the
 *       renderer would.</li>
 * </ol>
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
 * At the fixed camera of {@link TestScene} the 11x11 preview plane covers the whole viewport
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
 *
 * <p><b>Why the pixel work sits in {@code verify} steps and the file paths in {@link Later}.</b>
 * Every comparison below needs the PNG of an <em>earlier</em> screenshot. The path cannot be built
 * from the checkpoint name, because the two loaders write different file names for the same name
 * (Fabric numbers its files with a per-run counter, NeoForge does not), so it is taken from what
 * {@code shot(...)} hands back and read one or more steps later. The comparisons themselves need no
 * game state at all, which is why they are {@code verify} steps: on Fabric a harness call from
 * inside a client task is forbidden outright, and a comparison inside one would rule out the
 * screenshots around it.
 */
public final class BuildingWandPreviewClientTest {

    /**
     * Upper bound for the share of the viewport the shrunk ghost blocks may repaint. Half sized
     * cubes cover at most ~40% of their cell once perspective shows their side faces; full sized
     * ones would tile the plane and cover over 90%.
     */
    private static final double MAX_PAINTED_PERCENT = 65.0;

    /** The wand under test; its diameter of 11 is what makes the preview plane fill the view. */
    private static final String WAND = "simplebuilding:netherite_building_wand";

    private BuildingWandPreviewClientTest() {
    }

    /** Everything this test does; it needs the joined world the driver keeps open. */
    public static void inWorld(Script script) {
        TestScene.build(script, "minecraft:stone", "creative");

        // Material for the preview goes in before the baseline so it is not part of the diff:
        // the offhand stack is invisible anyway with the HUD and the first person hand hidden, but
        // handing it out later would change the inventory between the two compared frames.
        script.command("item replace entity @a weapon.offhand with minecraft:stone 64");
        script.awaitPackets();
        script.idle("let the offhand material arrive", 15);

        Later<Path> baseline = script.shot("wand-a-no-wand");
        script.idle("let a few frames pass between the two baseline shots", 10);
        Later<Path> baselineAgain = script.shot("wand-b-no-wand-again");

        Later<ScreenshotDiff.Diff> noiseFloor = new Later<>("the measured noise floor");
        script.verify("measure the noise floor of the unchanged scene", () -> {
            ScreenshotDiff.Diff diff = ScreenshotDiff.compare(
                    "noise floor (no wand vs. no wand)", baseline.get(), baselineAgain.get());
            ScreenshotDiff.assertUnchanged(diff);
            noiseFloor.set(diff);
        });

        script.command("item replace entity @a weapon.mainhand with " + WAND);
        script.awaitPackets();
        script.idle("let the wand arrive in the main hand", 15);

        Later<Integer> previewSize = assertPreviewTriggerConditions(script, "the positive case");
        script.verify("record how many preview positions the wand offers",
                () -> TestLog.info("building wand preview positions: " + previewSize.get()));

        Later<Path> withWand = script.shot("wand-c-preview");
        script.verify("the preview renderer drew ghost blocks", () -> {
            ScreenshotDiff.Diff signal = ScreenshotDiff.compare(
                    "building wand ghost preview", baseline.get(), withWand.get());
            ScreenshotDiff.assertDrew("BuildingWandPreviewRenderer", noiseFloor.get(), signal);
        });

        // Control: taking the wand away has to restore the baseline picture. Strict command on
        // purpose - at this point the player provably holds the wand and 64 stone, so a "clear"
        // that matches nothing would mean the setup is not what this test thinks it is.
        script.command("clear @a");
        script.awaitPackets();
        script.idle("let the empty inventory arrive", 15);

        Later<Path> withoutWand = script.shot("wand-d-removed-again");
        script.verify("removing the wand restores the baseline picture", () -> {
            ScreenshotDiff.Diff residual = ScreenshotDiff.compare(
                    "control (wand removed again)", baseline.get(), withoutWand.get());
            // assertBackToBaseline rather than assertLooksIdentical: same tolerance, but its
            // failure message names the claim that actually broke here - "the trigger was taken
            // away again", not "the trigger was never sufficient". It builds its own sentence
            // around this phrase, so the phrase is a subject, not a full claim.
            ScreenshotDiff.assertBackToBaseline("removing the wand", noiseFloor.get(), residual);
        });

        offhandWandDrawsNothing(script, baseline, noiseFloor);
        withoutMaterialDrawsNothing(script, baseline, noiseFloor);
        ghostBlocksLeaveGaps(script, baseline, noiseFloor);

        // What the old finally block did. These are the last steps rather than a cleanup hook,
        // because a step list has no hook: on a FAILING run they do not run at all, and the HUD
        // stays hidden for whatever runs next. That is acceptable only because every test starts
        // with TestScene.build, which hides the HUD again anyway - the restore is for the human
        // looking at the client after the run, not for the tests behind it.
        TestScene.showHudAgain(script);
    }

    /**
     * The renderer only ever looks at the main hand. Material goes into the main hand and the wand
     * into the offhand, so everything else the renderer needs is in place and only the hand is wrong.
     *
     * <p><b>What breaks this test:</b> scanning both hands for the wand, for example by copying the
     * offhand fallback that {@code BlockHighlightRenderer.findOctantStack} uses for the octant.
     */
    private static void offhandWandDrawsNothing(Script script, Later<Path> baseline,
                                                Later<ScreenshotDiff.Diff> noiseFloor) {
        // Tolerant: the control step above already emptied the inventory, and vanilla's clear
        // reports "no items were found" as a command failure. Marked here and nowhere else,
        // because a clear that silently matches nothing elsewhere would hide a broken setup.
        script.command("clear @a", true);
        script.command("item replace entity @a weapon.mainhand with minecraft:stone 64");
        script.command("item replace entity @a weapon.offhand with " + WAND);
        script.awaitPackets();
        script.idle("let the swapped hands arrive", 15);

        TestScene.assertAimedAt(script, TestScene.TARGET, TestScene.TARGET_FACE);

        script.act("the wand is in the offhand and the main hand holds the material", client -> {
            String problem = null;

            if (client.player == null) {
                problem = "no client player";
            } else if (client.player.getMainHandItem().getItem() instanceof BuildingWandItem) {
                problem = "the wand ended up in the main hand, which is the case the positive test covers";
            } else if (!(client.player.getOffhandItem().getItem() instanceof BuildingWandItem)) {
                problem = "the offhand does not hold a BuildingWandItem but " + client.player.getOffhandItem();
            }

            if (problem != null) {
                throw new AssertionError("Offhand wand setup failed: " + problem);
            }
        });

        Later<Path> offhandOnly = script.shot("wand-e-offhand-only");
        script.verify("an offhand wand drew nothing", () -> {
            ScreenshotDiff.Diff diff = ScreenshotDiff.compare(
                    "wand in the offhand only", baseline.get(), offhandOnly.get());
            ScreenshotDiff.assertLooksIdentical(noiseFloor.get(), diff,
                    "A building wand in the offhand must not draw a preview");
        });
    }

    /**
     * Without a block item in offhand or hotbar {@code getPreviewStates} returns an empty map and
     * the renderer bails out before it submits anything. The wand itself sits in hotbar slot 0 and
     * is not a block item, so clearing the inventory really does leave no material.
     *
     * <p>The empty preview map is asserted <em>before</em> the screenshot for the same reason the
     * positive case asserts a full one: an unchanged picture is the expected outcome here, and an
     * unchanged picture is also what a test that simply forgot to remove the material would see.
     *
     * <p><b>What breaks this test:</b> falling back to a default block when no material is found, or
     * moving the {@code previewMap.isEmpty()} check behind the geometry submission.
     */
    private static void withoutMaterialDrawsNothing(Script script, Later<Path> baseline,
                                                    Later<ScreenshotDiff.Diff> noiseFloor) {
        // Strict: the step before provably left stone in the main hand and the wand in the offhand.
        script.command("clear @a");
        script.command("item replace entity @a weapon.mainhand with " + WAND);
        script.awaitPackets();
        script.idle("let the empty inventory and the wand arrive", 15);

        TestScene.assertAimedAt(script, TestScene.TARGET, TestScene.TARGET_FACE);

        script.act("the wand has no material anywhere, so its preview map is empty", client -> {
            String problem = null;

            if (client.player == null || client.level == null) {
                problem = "no client player or level";
            } else if (!(client.player.getMainHandItem().getItem() instanceof BuildingWandItem wand)) {
                problem = "main hand does not hold a BuildingWandItem but " + client.player.getMainHandItem();
            } else if (!(client.hitResult instanceof BlockHitResult hit)
                    || hit.getType() != HitResult.Type.BLOCK) {
                problem = "no block hit result";
            } else {
                Map<BlockPos, BlockState> preview = BuildingWandItem.getPreviewStates(
                        client.level, client.player, client.player.getMainHandItem(),
                        hit.getBlockPos(), hit.getDirection(), wand.getWandSquareDiameter());

                if (!preview.isEmpty()) {
                    problem = "the preview map still holds " + preview.size() + " positions, so some block "
                            + "item is left in the inventory and this would not test the empty branch";
                }
            }

            if (problem != null) {
                throw new AssertionError("Missing material setup failed: " + problem);
            }
        });

        Later<Path> noMaterial = script.shot("wand-f-no-material");
        script.verify("a wand without material drew nothing", () -> {
            ScreenshotDiff.Diff diff = ScreenshotDiff.compare(
                    "wand without any material", baseline.get(), noMaterial.get());
            ScreenshotDiff.assertLooksIdentical(noiseFloor.get(), diff,
                    "A building wand without material must not draw a preview");
        });
    }

    /**
     * The ghost blocks are drawn at half size, so the wall stays visible between them. See the class
     * javadoc for the geometry behind the bound.
     *
     * <p>This case still runs the full argument: it is measured against the same noise floor and it
     * first has to pass {@link ScreenshotDiff#assertDrew}. An upper bound alone would be satisfied
     * by a renderer that draws nothing at all.
     *
     * <p><b>What breaks this test:</b> dropping the {@code GHOST_SCALE} translate/scale/translate
     * block around the model, or raising the scale towards 1.0 - the ghosts would then tile the whole
     * preview plane and repaint nearly the entire viewport.
     */
    private static void ghostBlocksLeaveGaps(Script script, Later<Path> baseline,
                                             Later<ScreenshotDiff.Diff> noiseFloor) {
        // Strict: the step before provably left the wand in the main hand.
        script.command("clear @a");
        script.command("item replace entity @a weapon.offhand with minecraft:redstone_block 64");
        script.command("item replace entity @a weapon.mainhand with " + WAND);
        script.awaitPackets();
        script.idle("let the red material and the wand arrive", 15);

        Later<Integer> renderable = assertPreviewTriggerConditions(script, "the ghost scale case");

        script.act("the whole 11x11 plane is renderable, which the area bound assumes", client -> {
            int count = renderable.get();

            if (count < 100) {
                throw new AssertionError("Ghost scale measurement needs the full 11x11 plane in front of "
                        + "the wall, but only " + count + " preview positions are replaceable. "
                        + TestScene.describeAim(client));
            }
        });

        Later<Path> ghosts = script.shot("wand-g-ghost-gaps");
        script.verify("the ghost blocks leave the wall visible between them", () -> {
            ScreenshotDiff.Diff painted = ScreenshotDiff.compare(
                    "ghost blocks against the wall", baseline.get(), ghosts.get());
            ScreenshotDiff.assertDrew("BuildingWandPreviewRenderer (scaled ghosts)",
                    noiseFloor.get(), painted);

            if (painted.percent() > MAX_PAINTED_PERCENT) {
                throw new AssertionError("The ghost preview repainted " + painted
                        + " of the viewport. The 11x11 plane covers the whole screen at this camera, so "
                        + "anything above " + MAX_PAINTED_PERCENT + "% means the ghost blocks are no longer "
                        + "shrunk to GHOST_SCALE = 0.5 and tile their cells without a gap.");
            }
        });
    }

    /**
     * Recomputes exactly what the renderer computes and asserts there is something to draw.
     *
     * <p>Hands back the number of preview positions that are actually renderable (replaceable
     * target blocks) as a {@link Later}, so a failure of the screenshot comparison behind it can
     * never be blamed on an empty preview map - and so the ghost scale case can additionally
     * demand the full plane.
     *
     * <p>All of it is one client step, deliberately: the aim, the held item, the hit result and the
     * preview map have to describe the <em>same</em> moment. Split across two client tasks the
     * player could have moved in between, and the count would then belong to a state nobody
     * checked.
     *
     * @param what names the caller in the log and in the failure message, because this runs twice
     */
    private static Later<Integer> assertPreviewTriggerConditions(Script script, String what) {
        TestScene.assertAimedAt(script, TestScene.TARGET, TestScene.TARGET_FACE);

        Later<Integer> renderable = new Later<>("the number of renderable preview positions for " + what);

        script.act("the wand preview trigger conditions hold for " + what, client -> {
            if (client.player == null || client.level == null) {
                throw new AssertionError("Building wand preview trigger conditions not met: "
                        + "no client player or level");
            }

            if (!(client.player.getMainHandItem().getItem() instanceof BuildingWandItem wand)) {
                throw new AssertionError("Building wand preview trigger conditions not met: "
                        + "main hand does not hold a BuildingWandItem but " + client.player.getMainHandItem());
            }

            if (!(client.hitResult instanceof BlockHitResult hit)
                    || hit.getType() != HitResult.Type.BLOCK) {
                throw new AssertionError("Building wand preview trigger conditions not met: "
                        + "no block hit result");
            }

            Map<BlockPos, BlockState> preview = BuildingWandItem.getPreviewStates(
                    client.level, client.player, client.player.getMainHandItem(),
                    hit.getBlockPos(), hit.getDirection(), wand.getWandSquareDiameter());

            int count = 0;

            for (BlockPos pos : preview.keySet()) {
                if (client.level.getBlockState(pos).canBeReplaced()) {
                    count++;
                }
            }

            if (count < 9) {
                throw new AssertionError("Building wand preview trigger conditions not met: only " + count
                        + " renderable preview positions, expected the 11x11 plane in front of the wall. "
                        + TestScene.describeAim(client));
            }

            renderable.set(count);
        });

        return renderable;
    }
}
