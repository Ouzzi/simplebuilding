package com.simplebuilding.clientgametest;

import java.nio.file.Path;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.ClientState;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.guiDrawHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Proves that {@code BlockHighlightRenderer} still puts geometry on screen after the 26.2 rewrite
 * onto {@code SubmitNodeCollector.submitCustomGeometry} / {@code LevelRenderEvents.COLLECT_SUBMITS},
 * and that the two config options the renderer reads really steer what it draws.
 *
 * <p>Both of the renderer's two independent passes are covered:
 * <ul>
 *   <li><b>Sledgehammer highlight</b> - needs a {@link SledgehammerItem} in the <em>main</em> hand
 *       (offhand is not checked) and a block hit result. With no Radius/Break Through enchantment
 *       the pattern is the 3x3 plane around the target block on the hit side; the target block
 *       itself is skipped, so exactly the eight neighbours are outlined and filled.</li>
 *   <li><b>Octant highlight</b> - needs an {@link OctantItem} in the main or offhand carrying at
 *       least {@code Pos1} or {@code Pos2} in its custom data. The corner outlines are drawn
 *       unconditionally; the area fill additionally needs {@code showFill}, see below.</li>
 * </ul>
 *
 * <p>Between the two, the item is removed again and the screen has to return to the baseline. That
 * control rules out the alternative explanation "the picture drifts anyway" for the differences.
 *
 * <h2>The order of the steps is the argument, and it must not be shuffled</h2>
 *
 * <p>Every measurement in this class follows the same four beats, and a pixel difference proves
 * nothing without all four of them in this order:
 * <ol>
 *   <li><b>A noise floor first.</b> Two screenshots of the very same, untouched scene a few ticks
 *       apart, asserted to be identical ({@code assertUnchanged}). If the scene is not actually
 *       static - a drifting vignette, an animated texture, a mob that was not killed - then every
 *       "the renderer drew something" below it would be satisfied by that drift alone. The measured
 *       number is then reused as the bar the real signal has to clear tenfold.</li>
 *   <li><b>Every trigger condition of the branch under test, asserted before the shot.</b> The
 *       renderer legitimately draws nothing when the item is not in the main hand, when the eight
 *       neighbours are not solid, when the octant carries no positions, when
 *       {@code ClientState.showHighlights} is off, or when the config does not say what the test
 *       thinks it says. Without these assertions a setup that quietly failed is indistinguishable
 *       from a renderer that stopped working - and it would be reported as the latter.</li>
 *   <li><b>The measuring shot</b>, differing from the baseline only in the one input under
 *       test.</li>
 *   <li><b>A control shot back at the baseline state.</b> Taking the trigger away again has to
 *       restore the baseline picture. This is what turns "these two pictures differ" into "the
 *       trigger caused the difference".</li>
 * </ol>
 *
 * <p><b>The two config driven branches</b> (both live inside the renderer behind
 * {@code Minecraft.getInstance()}, which is why they can only be reached from a client test):
 * <ul>
 *   <li>{@code showFill = invertOctantSneak ^ hasConstructorsTouch}. The test walks all four
 *       combinations of the two inputs on one and the same octant, so it states the XOR and not
 *       merely "the fill can appear": either input alone has to produce the area fill, neither and
 *       both have to leave the outlines alone.</li>
 *   <li>{@code buildingHighlightOpacity} is read as a percentage and clamped:
 *       {@code baseAlpha = max(0, min(100, opacity)) / 100}, and only the <em>fill</em> alpha is
 *       scaled by it ({@code fillAlpha = 0.3 * baseAlpha}) while the outline alpha stays at a fixed
 *       0.3. The test therefore expects outlines at opacity 0 and clearly more painted pixels at
 *       opacity 100, and it pins both ends of the clamp: 500 has to look exactly like 100 and -50
 *       exactly like 0.</li>
 * </ul>
 *
 * <p><b>Why the opacity part builds a second, white scene.</b> The sledgehammer fill is mid grey
 * (0.5/0.5/0.5) at up to 30% alpha. On the stone wall of the default scene that lands within about
 * two levels of the wall's own colour - far below {@link ScreenshotDiff}'s 12 level threshold - so
 * on stone the fill is literally unmeasurable and only the black outlines show. Against a quartz
 * wall (~229 levels) the same fill moves a pixel by ~30 levels and the black outlines by ~69, so
 * both effects are visible and can be told apart by their pixel count.
 *
 * <p><b>Why the whole frame is compared and not just its right half.</b>
 * {@link ScreenshotDiff#compare(String, Path, Path, boolean)} offers a right-half-only mode for the
 * octant, because the mod also draws a rangefinder info panel in the upper left while an octant is
 * held. That panel is a HUD layer ({@code RangefinderHudOverlay}, attached through the loader's HUD
 * element registration), and {@link TestScene} hides the HUD for every shot in this class - so the
 * panel is not on screen at all here and cannot be counted as "the in-world renderer drew
 * something". Should a future version draw that panel outside the HUD, the octant measurement below
 * would start to be satisfiable by the panel alone, and the right-half mode is the fix.
 *
 * <p><b>Known defect (not pinned by any assertion here).</b> The config tooltip
 * {@code text.autoconfig.simplebuilding.option.tools.buildingHighlightOpacity.@Tooltip} reads
 * "Opacity of the Building Wand block highlight (0.0 - 1.0)", but the code reads the value as whole
 * percent 0..100 (default 40), and the building wand ghost preview does not read the option at all -
 * {@code BuildingWandPreviewRenderer} uses its own constant {@code GHOST_ALPHA = 180}. The option
 * only affects the sledgehammer and octant fills. Writing the wrong range into a test would cement
 * the mistake, so it is only recorded here.
 *
 * <p><b>Not covered.</b> The exact alpha value of a highlight pixel. A screenshot proves that the
 * fill scales with the option and is clamped at both ends, but the absolute blend factor cannot be
 * read back from the framebuffer without knowing the wall texture's per pixel colour, and pinning
 * it would break on any texture pack or lighting change. Also not covered: the offhand sledgehammer
 * (the renderer deliberately ignores it, and no shot here would tell an ignored offhand apart from
 * a renderer that draws nothing), the Radius/Break Through enchantments that change the pattern,
 * and {@code ClientState.showHighlights} being switched <em>off</em> - it is asserted to be on
 * before the filled shots, but no shot proves that turning it off removes the highlight.
 *
 * <h2>What the port to the shared step form changed, and what it did not</h2>
 *
 * <p>The measurements, their order, their thresholds, the sixteen screenshot names and every
 * assertion message are the ones the Fabric-only version used. Four things are mechanically
 * different, and each is a consequence of the step list rather than a decision about the test:
 * <ul>
 *   <li><b>Screenshot paths travel in a {@link Later}.</b> A step list registers every step before
 *       any of them runs, so a path cannot live in a local variable the way it does in straight
 *       line code - and it cannot be rebuilt from the screenshot name either, because Fabric writes
 *       {@code 0004_name.png} with a per-run counter while NeoForge writes {@code name.png}. Every
 *       comparison below therefore reads a path that {@link Script#shot} produced in an
 *       <em>earlier</em> step. An earlier attempt at this port dropped the path, compared nothing,
 *       and was formally complete while measuring exactly nothing; {@link Later#get} throwing on an
 *       unfilled value is what makes that failure mode loud instead of green.</li>
 *   <li><b>The comparisons run in {@link Script#verify}, not in {@link Script#act}.</b> They need
 *       no game state, and on Fabric a harness call inside a client task is forbidden outright.</li>
 *   <li><b>The trigger condition checks throw directly</b> instead of returning a problem string to
 *       a caller that throws. An {@code act} step already runs on the client thread and an
 *       exception in it fails the test naming the step, so the string relay had nothing left to
 *       do. The messages are unchanged.</li>
 *   <li><b>The cleanup is a pair of ordinary last steps, not a {@code finally} block.</b> A step
 *       list has no place to hang one. <b>They therefore do not run when a step above them
 *       fails</b>, and the two things they restore outlive the test: the mod config values
 *       ({@code buildingHighlightOpacity}, {@code invertOctantSneak}) and the hidden HUD. The
 *       hidden HUD is harmless - {@link TestScene#build} hides it again for the next test anyway -
 *       but the config is not reset by anything, so a client test that runs after a failure of this
 *       one may find opacity 100 and inverted sneak on. Any test that cares about those values has
 *       to set them itself, which is what this one does before every filled shot.</li>
 * </ul>
 */
public final class BlockHighlightClientTest {

    /**
     * The z plane one block in front of the wall - free air.
     *
     * <p>Derived from the wall instead of written out, so it cannot drift away from
     * {@link TestScene}. Same value the Fabric-only scene called {@code FRONT_Z}.
     */
    private static final int FRONT_Z = TestScene.WALL_Z - 1;

    /** Free air positions, 2.5 blocks in front of the camera - a fully visible box outline each. */
    private static final BlockPos OCTANT_POS_1 = new BlockPos(9, 1, FRONT_Z);
    private static final BlockPos OCTANT_POS_2 = new BlockPos(11, 1, FRONT_Z);

    /** The command that puts the sledgehammer in the main hand; used by both halves of the test. */
    private static final String GIVE_SLEDGEHAMMER =
            "item replace entity @a weapon.mainhand with simplebuilding:netherite_sledgehammer";

    /**
     * Restricts the octant comparisons to the right half of the frame.
     *
     * <p>A held octant draws its rangefinder panel in the top left corner. Without this the panel
     * counts as "the world renderer drew something", and the octant proof rests on a HUD element
     * instead of on the highlight. The NeoForge suite always measured this way; the Fabric one did
     * not, so sharing the body inherited the weaker half - and the first shared run on NeoForge
     * failed on exactly that, with 512 panel pixels against an allowance of 200. The stronger
     * form is the one that survives.
     */
    private static final boolean RIGHT_HALF_ONLY = true;

    private BlockHighlightClientTest() {
    }

    /**
     * The whole test, as steps.
     *
     * <p>Reads the two config values it is going to change before it changes them, so the last two
     * steps can put them back. On a fresh client those are the defaults (40 and false), but the
     * suite shares one world and one client across all tests, so "the default" is not a safe
     * assumption to restore to.
     */
    public static void inWorld(Script script) {
        TestScene.build(script, "minecraft:stone", "creative");

        Later<Integer> originalOpacity = new Later<>("the buildingHighlightOpacity found on entry");
        Later<Boolean> originalInvert = new Later<>("the invertOctantSneak found on entry");

        script.act("remember the highlight config so the last steps can put it back", client -> {
            originalOpacity.set(Simplebuilding.getConfig().tools.buildingHighlightOpacity);
            originalInvert.set(Simplebuilding.getConfig().tools.invertOctantSneak);
        });

        Later<Path> baseline = script.shot("highlight-a-empty-hand");
        // Ten ticks, so the noise floor spans real screen time rather than two frames of the same
        // render pass; anything that animates in the scene has a chance to move within it.
        script.idle("let ten ticks pass between the two baseline shots", 10);
        Later<Path> baselineAgain = script.shot("highlight-b-empty-hand-again");

        Later<ScreenshotDiff.Diff> noiseFloor = new Later<>("the noise floor of the stone scene");

        script.verify("measure the noise floor of the stone scene", () -> {
            ScreenshotDiff.Diff diff = ScreenshotDiff.compare(
                    "noise floor (empty hand vs. empty hand)", baseline.get(), baselineAgain.get());
            ScreenshotDiff.assertUnchanged(diff);
            noiseFloor.set(diff);
        });

        sledgehammerHighlight(script, baseline, noiseFloor);
        itemRemovalReturnsToBaseline(script, baseline, noiseFloor);
        Later<Path> octantOutlines = octantHighlight(script, baseline, noiseFloor);
        octantFillFollowsShowFill(script, octantOutlines);
        sledgehammerFillFollowsOpacity(script);

        restoreHighlightConfig(script, originalOpacity, originalInvert);
        TestScene.showHudAgain(script);
    }

    /**
     * The sledgehammer pass draws at all.
     *
     * <p>Fifteen ticks between the command and the shot: a changed main hand item starts the equip
     * animation, and the shot has to land after it, not inside it.
     */
    private static void sledgehammerHighlight(Script script, Later<Path> baseline,
                                              Later<ScreenshotDiff.Diff> noiseFloor) {
        script.command(GIVE_SLEDGEHAMMER);
        script.awaitPackets();
        script.idle("let the sledgehammer arrive and its equip animation finish", 15);

        assertSledgehammerTriggerConditions(script);

        Later<Path> withSledgehammer = script.shot("highlight-c-sledgehammer");

        script.verify("the sledgehammer highlight reached the screen", () -> {
            ScreenshotDiff.Diff signal = ScreenshotDiff.compare(
                    "sledgehammer highlight", baseline.get(), withSledgehammer.get());
            ScreenshotDiff.assertDrew("BlockHighlightRenderer (sledgehammer)", noiseFloor.get(), signal);
        });
    }

    /**
     * The control for the sledgehammer shot: with the item gone the picture has to be the baseline
     * again. Without it, "the picture drifts anyway" would explain the difference just as well.
     *
     * <p>The {@code clear} command is run in the strict form on purpose. It has something to clear
     * here - the sledgehammer the step before put in the hand - so on a loader whose harness
     * reports command failures, a clear that matched nothing fails the test instead of quietly
     * leaving the item in the hand.
     */
    private static void itemRemovalReturnsToBaseline(Script script, Later<Path> baseline,
                                                     Later<ScreenshotDiff.Diff> noiseFloor) {
        script.command("clear @a");
        script.awaitPackets();
        script.idle("let the cleared inventory reach the client", 15);

        script.act("control: the main hand really is empty again", client -> {
            if (client.player == null || !client.player.getMainHandItem().isEmpty()) {
                throw new AssertionError("Control step failed: main hand was not cleared. "
                        + TestScene.describeAim(client));
            }
        });

        Later<Path> cleared = script.shot("highlight-d-cleared-again");

        script.verify("removing the sledgehammer restores the baseline picture", () -> {
            ScreenshotDiff.Diff residual = ScreenshotDiff.compare(
                    "control (highlight removed again)", baseline.get(), cleared.get());
            ScreenshotDiff.assertLooksIdentical(noiseFloor.get(), residual,
                    "Removing the sledgehammer has to restore the baseline image");
        });
    }

    /**
     * The octant pass draws at all.
     *
     * @return the "octant in hand, outlines only" picture - the baseline of the area fill test
     */
    private static Later<Path> octantHighlight(Script script, Later<Path> baseline,
                                               Later<ScreenshotDiff.Diff> noiseFloor) {
        script.command("item replace entity @a weapon.mainhand with simplebuilding:octant["
                + "minecraft:custom_data={"
                + "Pos1:[I;" + OCTANT_POS_1.getX() + "," + OCTANT_POS_1.getY() + "," + OCTANT_POS_1.getZ() + "],"
                + "Pos2:[I;" + OCTANT_POS_2.getX() + "," + OCTANT_POS_2.getY() + "," + OCTANT_POS_2.getZ() + "]"
                + "}]");
        script.awaitPackets();
        script.idle("let the octant arrive and its equip animation finish", 15);

        assertOctantTriggerConditions(script);

        Later<Path> withOctant = script.shot("highlight-e-octant");

        script.verify("the octant highlight reached the screen", () -> {
            ScreenshotDiff.Diff signal =
                    ScreenshotDiff.compare("octant highlight", baseline.get(), withOctant.get(), RIGHT_HALF_ONLY);
            ScreenshotDiff.assertDrew("BlockHighlightRenderer (octant)", noiseFloor.get(), signal);
        });

        return withOctant;
    }

    /**
     * The octant area fill: {@code showFill = invertOctantSneak ^ hasConstructorsTouch}. All four
     * combinations of the two inputs are walked through, so the test states the XOR rather than
     * just "the fill can appear":
     * <pre>
     *   plain octant,    invertOctantSneak off  -&gt;  outlines only   (the baseline, shot e/f)
     *   plain octant,    invertOctantSneak on   -&gt;  area fill       (shot g)
     *   plain octant,    invertOctantSneak off  -&gt;  outlines only   (shot h, control)
     *   enchanted,       invertOctantSneak off  -&gt;  area fill       (shot i)
     *   enchanted,       invertOctantSneak on   -&gt;  outlines only   (shot j, both inputs cancel)
     * </pre>
     * The two "outlines only" pictures have to match the baseline within its own noise floor and
     * the two filled ones have to differ from it far above that noise floor.
     *
     * <p>This section measures its own noise floor rather than reusing the empty-handed one: its
     * baseline is a picture <em>with</em> the octant outlines in it, and the outlines are what the
     * later shots are compared against. A noise floor taken from a different picture would not be
     * the noise of these comparisons.
     *
     * <p>Constructor's Touch is applied with the vanilla {@code /enchant} command (the octant is in
     * {@code #simplebuilding:constructors_touch_enchantable} through
     * {@code #simplebuilding:octants_enchantable}) and its arrival on the client is read back from
     * the item's enchantment component before the shot, so a command that silently failed cannot
     * pass as "the renderer ignores the enchantment".
     *
     * <p>Opacity is pinned at 100 for the filled shots: at the default of 40 the orange fill lands
     * about 15 levels away from the stone behind it, which is only just above the 12 level
     * threshold and would make the measurement depend on the wall texture. It stays at 100 for the
     * unfilled shots too, so the only thing that ever changes between two pictures is one input of
     * the XOR.
     *
     * <p><b>What breaks this test:</b> dropping the {@code ClientState.showHighlights} check or
     * either half of the XOR (one of the two filled shots stops drawing), turning the XOR into an
     * OR (shot j starts drawing a fill), drawing the area fill unconditionally (shots h and j
     * differ from the baseline), or losing the {@code pos1 != null && pos2 != null} guard that
     * gates the whole block.
     */
    private static void octantFillFollowsShowFill(Script script, Later<Path> octantOutlines) {
        script.idle("let ten ticks pass before the second octant shot", 10);
        Later<Path> outlinesAgain = script.shot("highlight-f-octant-again");

        Later<ScreenshotDiff.Diff> octantNoise = new Later<>("the noise floor with the octant in hand");

        script.verify("measure the noise floor with the octant in hand", () -> {
            ScreenshotDiff.Diff diff = ScreenshotDiff.compare(
                    "noise floor (octant outlines twice)", octantOutlines.get(), outlinesAgain.get(), RIGHT_HALF_ONLY);
            ScreenshotDiff.assertUnchanged(diff);
            octantNoise.set(diff);
        });

        setHighlightConfig(script, 100, true);
        script.idle("let the changed config reach the next frame", 10);
        assertOctantFillTriggerConditions(script, true, false);

        Later<Path> filled = script.shot("highlight-g-octant-filled");

        script.verify("inverted sneak alone produces the area fill", () -> {
            ScreenshotDiff.Diff signal = ScreenshotDiff.compare(
                    "octant area fill (inverted sneak on)", octantOutlines.get(), filled.get(), RIGHT_HALF_ONLY);
            ScreenshotDiff.assertDrew("BlockHighlightRenderer (octant area fill)",
                    octantNoise.get(), signal);
        });

        setHighlightConfig(script, 100, false);
        script.idle("let the changed config reach the next frame", 10);
        assertOctantFillTriggerConditions(script, false, false);

        Later<Path> unfilled = script.shot("highlight-h-octant-unfilled-again");

        script.verify("switching inverted sneak off again removes the area fill", () -> {
            ScreenshotDiff.Diff residual = ScreenshotDiff.compare(
                    "control (inverted sneak off again)", octantOutlines.get(), unfilled.get(), RIGHT_HALF_ONLY);
            ScreenshotDiff.assertLooksIdentical(octantNoise.get(), residual,
                    "Switching invertOctantSneak off again has to remove the area fill");
        });

        script.command("enchant @a simplebuilding:constructors_touch 1");
        script.awaitPackets();
        script.idle("let the enchanted item reach the client", 10);
        assertOctantFillTriggerConditions(script, false, true);

        Later<Path> touched = script.shot("highlight-i-octant-touch-filled");

        script.verify("Constructor's Touch alone produces the area fill", () -> {
            ScreenshotDiff.Diff signal = ScreenshotDiff.compare(
                    "octant area fill (Constructor's Touch)", octantOutlines.get(), touched.get(), RIGHT_HALF_ONLY);
            ScreenshotDiff.assertDrew("BlockHighlightRenderer (octant area fill via the enchantment)",
                    octantNoise.get(), signal);
        });

        setHighlightConfig(script, 100, true);
        script.idle("let the changed config reach the next frame", 10);
        assertOctantFillTriggerConditions(script, true, true);

        Later<Path> bothInputs = script.shot("highlight-j-octant-both-unfilled");

        script.verify("the two inputs of the XOR cancel each other out", () -> {
            ScreenshotDiff.Diff cancelled = ScreenshotDiff.compare(
                    "control (enchantment and inverted sneak cancel out)",
                    octantOutlines.get(), bothInputs.get(), RIGHT_HALF_ONLY);
            ScreenshotDiff.assertLooksIdentical(octantNoise.get(), cancelled,
                    "invertOctantSneak together with Constructor's Touch has to cancel out and show no fill");
        });
    }

    /**
     * The sledgehammer fill and the {@code buildingHighlightOpacity} clamp, measured against a
     * quartz wall (see the class javadoc for why the default stone scene cannot show the fill).
     *
     * <p>The scene is rebuilt here, which wipes the octant out of the inventory and puts the player
     * back on the fixed spot with the aim asserted again. It does <em>not</em> touch the mod config:
     * this section inherits opacity 100 and inverted sneak on from the octant section. That is
     * harmless because no shot below is taken before the opacity has been set explicitly and read
     * back, and inverted sneak steers only the octant pass, which has no octant to run on any more.
     *
     * <p><b>What breaks this test:</b> reading the option as a 0.0-1.0 factor instead of whole
     * percent, dropping either side of {@code max(0, min(100, opacity))}, scaling the outline alpha
     * by the option as well (then opacity 0 would draw nothing), or not scaling the fill at all
     * (then opacity 0 and 100 would be the same picture).
     */
    private static void sledgehammerFillFollowsOpacity(Script script) {
        TestScene.build(script, "minecraft:quartz_block", "creative");

        Later<Path> baseline = script.shot("opacity-a-empty-hand");
        script.idle("let ten ticks pass between the two baseline shots", 10);
        Later<Path> baselineAgain = script.shot("opacity-b-empty-hand-again");

        Later<ScreenshotDiff.Diff> noiseFloor = new Later<>("the noise floor of the quartz scene");

        script.verify("measure the noise floor of the quartz scene", () -> {
            ScreenshotDiff.Diff diff = ScreenshotDiff.compare(
                    "noise floor (quartz wall, empty hand)", baseline.get(), baselineAgain.get());
            ScreenshotDiff.assertUnchanged(diff);
            noiseFloor.set(diff);
        });

        script.command(GIVE_SLEDGEHAMMER);
        script.awaitPackets();
        setHighlightOpacity(script, 0);
        script.idle("let the sledgehammer arrive and its equip animation finish", 15);

        assertSledgehammerTriggerConditions(script);
        assertOpacity(script, 0);

        Later<Path> fillOff = script.shot("opacity-c-fill-off");
        Later<ScreenshotDiff.Diff> outlinesOnly = new Later<>("the difference at opacity 0");

        script.verify("at opacity 0 the outlines are still drawn", () -> {
            ScreenshotDiff.Diff diff = ScreenshotDiff.compare(
                    "sledgehammer outlines at opacity 0", baseline.get(), fillOff.get());
            ScreenshotDiff.assertDrew("BlockHighlightRenderer (outlines at opacity 0)",
                    noiseFloor.get(), diff);
            outlinesOnly.set(diff);
        });

        setHighlightOpacity(script, 100);
        script.idle("let the changed config reach the next frame", 10);
        assertOpacity(script, 100);

        Later<Path> fillFull = script.shot("opacity-d-fill-full");
        Later<ScreenshotDiff.Diff> withFill = new Later<>("the difference at opacity 100");

        script.verify("at opacity 100 outlines and fill are drawn", () -> {
            ScreenshotDiff.Diff diff = ScreenshotDiff.compare(
                    "sledgehammer outlines and fill at opacity 100", baseline.get(), fillFull.get());
            ScreenshotDiff.assertDrew("BlockHighlightRenderer (fill at opacity 100)",
                    noiseFloor.get(), diff);
            withFill.set(diff);
        });

        script.verify("the fill grows with the option, not just the outlines", () -> {
            // The eight filled block faces cover roughly a 3x3 block square on screen, the outlines
            // only the four pixel wide grid between them - about ten times fewer pixels. Requiring a
            // factor of three leaves room for line width and resolution differences while still
            // failing loudly if the fill stops reacting to the option.
            if (withFill.get().changedPixels() < outlinesOnly.get().changedPixels() * 3) {
                throw new AssertionError("buildingHighlightOpacity does not scale the sledgehammer fill: "
                        + "opacity 100 changed " + withFill.get().changedPixels() + " pixels, opacity 0 changed "
                        + outlinesOnly.get().changedPixels() + " - expected at least three times as many, because "
                        + "at opacity 100 the eight neighbour faces are filled and at opacity 0 only their "
                        + "outlines are drawn.");
            }
        });

        setHighlightOpacity(script, 500);
        script.idle("let the changed config reach the next frame", 10);
        assertOpacity(script, 500);

        Later<Path> aboveRange = script.shot("opacity-e-above-range");

        script.verify("an opacity above the range is clamped to 100", () -> {
            ScreenshotDiff.Diff clampedHigh = ScreenshotDiff.compare(
                    "clamp above range (500 against 100)", fillFull.get(), aboveRange.get());
            ScreenshotDiff.assertLooksIdentical(noiseFloor.get(), clampedHigh,
                    "buildingHighlightOpacity 500 has to be clamped to 100 and look exactly like it");
        });

        setHighlightOpacity(script, -50);
        script.idle("let the changed config reach the next frame", 10);
        assertOpacity(script, -50);

        Later<Path> belowRange = script.shot("opacity-f-below-range");

        script.verify("an opacity below the range is clamped to 0", () -> {
            ScreenshotDiff.Diff clampedLow = ScreenshotDiff.compare(
                    "clamp below range (-50 against 0)", fillOff.get(), belowRange.get());
            ScreenshotDiff.assertLooksIdentical(noiseFloor.get(), clampedLow,
                    "buildingHighlightOpacity -50 has to be clamped to 0 and look exactly like it");
        });
    }

    /**
     * Verifies everything {@code drawSledgehammerHighlights} needs before the screenshot is taken.
     * Without this a missed setup would look exactly like a broken renderer.
     */
    private static void assertSledgehammerTriggerConditions(Script script) {
        TestScene.assertAimedAt(script, TestScene.TARGET, TestScene.TARGET_FACE);

        script.act("the sledgehammer highlight has everything it needs", client -> {
            String problem = sledgehammerProblem(client);

            if (problem != null) {
                throw new AssertionError("Sledgehammer highlight trigger conditions not met: " + problem);
            }
        });
    }

    /** @return what is missing, or null when the renderer has everything it needs */
    private static String sledgehammerProblem(net.minecraft.client.Minecraft client) {
        if (client.player == null || client.level == null) {
            return "no client player or level";
        }

        if (!(client.player.getMainHandItem().getItem() instanceof SledgehammerItem)) {
            return "main hand does not hold a SledgehammerItem but " + client.player.getMainHandItem();
        }

        // The eight highlighted positions are the neighbours of the target block in the wall
        // plane. All of them must be solid, otherwise the renderer legitimately draws nothing.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }

                BlockPos neighbour = TestScene.TARGET.offset(dx, dy, 0);

                if (client.level.getBlockState(neighbour).isAir()) {
                    return "neighbour " + neighbour + " of the target block is air, "
                            + "so there is nothing for the highlight to outline";
                }
            }
        }

        return null;
    }

    /** Verifies everything {@code drawOctantHighlights} needs before the screenshot is taken. */
    private static void assertOctantTriggerConditions(Script script) {
        script.act("the octant highlight has everything it needs", client -> {
            if (client.player == null) {
                throw new AssertionError("Octant highlight trigger conditions not met: no client player");
            }

            if (!(client.player.getMainHandItem().getItem() instanceof OctantItem)) {
                throw new AssertionError("Octant highlight trigger conditions not met: main hand does "
                        + "not hold an OctantItem but " + client.player.getMainHandItem());
            }
        });
    }

    /**
     * Verifies every input of {@code showFill = invertOctantSneak ^ hasConstructorsTouch} plus the
     * two positions and {@code ClientState.showHighlights}, which gate the area fill as well.
     * The Constructor's Touch check is read straight from the item's enchantment component instead
     * of through the mod's own helper, so a broken helper cannot make this precondition pass.
     */
    private static void assertOctantFillTriggerConditions(Script script, boolean expectedInvert,
                                                          boolean expectedTouch) {
        script.act("the octant area fill has the inputs this row of the truth table needs", client -> {
            String problem = octantFillProblem(client, expectedInvert, expectedTouch);

            if (problem != null) {
                throw new AssertionError("Octant area fill trigger conditions not met: " + problem);
            }
        });
    }

    /** @return what is missing, or null when this row of the truth table is really set up */
    private static String octantFillProblem(net.minecraft.client.Minecraft client, boolean expectedInvert,
                                            boolean expectedTouch) {
        if (client.player == null) {
            return "no client player";
        }

        ItemStack stack = client.player.getMainHandItem();

        if (!(stack.getItem() instanceof OctantItem)) {
            return "main hand does not hold an OctantItem but " + stack;
        }

        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = data.copyTag();

        if (guiDrawHelper.getPos(nbt, "Pos1") == null || guiDrawHelper.getPos(nbt, "Pos2") == null) {
            return "the octant does not carry both Pos1 and Pos2, so the area fill is never reached";
        }

        if (hasConstructorsTouch(stack) != expectedTouch) {
            return "the octant " + (expectedTouch ? "does not carry" : "carries")
                    + " Constructor's Touch, but this step of the truth table needs it "
                    + (expectedTouch ? "enchanted" : "plain") + " (stack: " + stack + ")";
        }

        if (!ClientState.showHighlights) {
            return "ClientState.showHighlights is off, which suppresses the area fill";
        }

        if (Simplebuilding.getConfig().tools.invertOctantSneak != expectedInvert) {
            return "invertOctantSneak is " + Simplebuilding.getConfig().tools.invertOctantSneak
                    + " but the test set it to " + expectedInvert;
        }

        return null;
    }

    /** Reads the enchantment component directly - no mod code involved. */
    private static boolean hasConstructorsTouch(ItemStack stack) {
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);

        if (enchantments == null) {
            return false;
        }

        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (holder.is(ModEnchantments.CONSTRUCTORS_TOUCH)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Reads the option back before the shot that depends on it.
     *
     * <p>Cheap, and it is the difference between "the renderer ignores the option" and "the test
     * never actually set it" - two failures that look identical in a screenshot.
     */
    private static void assertOpacity(Script script, int expected) {
        script.act("buildingHighlightOpacity really is " + expected, client -> {
            int actual = Simplebuilding.getConfig().tools.buildingHighlightOpacity;

            if (actual != expected) {
                throw new AssertionError("Trigger condition not met: buildingHighlightOpacity is " + actual
                        + " but the test set it to " + expected);
            }
        });
    }

    private static void setHighlightOpacity(Script script, int opacity) {
        script.act("set buildingHighlightOpacity to " + opacity, client ->
                Simplebuilding.getConfig().tools.buildingHighlightOpacity = opacity);
    }

    private static void setHighlightConfig(Script script, int opacity, boolean invertOctantSneak) {
        script.act("set buildingHighlightOpacity to " + opacity + " and invertOctantSneak to "
                + invertOctantSneak, client -> {
            Simplebuilding.getConfig().tools.buildingHighlightOpacity = opacity;
            Simplebuilding.getConfig().tools.invertOctantSneak = invertOctantSneak;
        });
    }

    /**
     * Puts the two config values back the way they were found.
     *
     * <p>This is the former {@code finally} block, and it is now an ordinary step: <b>it does not
     * run when a step above it failed.</b> See the class javadoc for what that leaves behind.
     */
    private static void restoreHighlightConfig(Script script, Later<Integer> originalOpacity,
                                               Later<Boolean> originalInvert) {
        script.act("put the highlight config back the way it was found", client -> {
            Simplebuilding.getConfig().tools.buildingHighlightOpacity = originalOpacity.get();
            Simplebuilding.getConfig().tools.invertOctantSneak = originalInvert.get();
        });
    }
}
