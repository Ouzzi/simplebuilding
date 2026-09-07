package com.simplebuilding.clienttest;

import java.nio.file.Path;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.ClientState;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.guiDrawHelper;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
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
 * two levels of the wall's own colour - far below {@code ScreenshotDiff}'s 12 level threshold - so
 * on stone the fill is literally unmeasurable and only the black outlines show. Against a quartz
 * wall (~229 levels) the same fill moves a pixel by ~30 levels and the black outlines by ~69, so
 * both effects are visible and can be told apart by their pixel count.
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
 * it would break on any texture pack or lighting change.
 */
public final class BlockHighlightClientGameTest implements FabricClientGameTest {

    /** Free air positions, 2.5 blocks in front of the camera - a fully visible box outline each. */
    private static final BlockPos OCTANT_POS_1 = new BlockPos(9, 1, RendererTestScene.FRONT_Z);
    private static final BlockPos OCTANT_POS_2 = new BlockPos(11, 1, RendererTestScene.FRONT_Z);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            RendererTestScene.build(context, singleplayer, "minecraft:stone", "creative");

            int originalOpacity = context.computeOnClient(client ->
                    Simplebuilding.getConfig().tools.buildingHighlightOpacity);
            boolean originalInvert = context.computeOnClient(client ->
                    Simplebuilding.getConfig().tools.invertOctantSneak);

            try {
                Path baseline = context.takeScreenshot("highlight-a-empty-hand");
                context.waitTicks(10);
                Path baselineAgain = context.takeScreenshot("highlight-b-empty-hand-again");

                ScreenshotDiff.Diff noiseFloor =
                        ScreenshotDiff.compare("noise floor (empty hand vs. empty hand)", baseline, baselineAgain);
                ScreenshotDiff.assertUnchanged(noiseFloor);

                testSledgehammerHighlight(context, singleplayer, baseline, noiseFloor);
                testItemRemovalReturnsToBaseline(context, singleplayer, baseline, noiseFloor);
                Path octantOutlines = testOctantHighlight(context, singleplayer, baseline, noiseFloor);
                testOctantFillFollowsShowFill(context, singleplayer, octantOutlines);
                testSledgehammerFillFollowsOpacity(context, singleplayer);
            } finally {
                setHighlightConfig(context, originalOpacity, originalInvert);
                RendererTestScene.showHudAgain(context);
            }
        }
    }

    private void testSledgehammerHighlight(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                           Path baseline, ScreenshotDiff.Diff noiseFloor) {
        singleplayer.getServer().runCommand(
                "item replace entity @a weapon.mainhand with simplebuilding:netherite_sledgehammer");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);

        assertSledgehammerTriggerConditions(context);

        Path withSledgehammer = context.takeScreenshot("highlight-c-sledgehammer");
        ScreenshotDiff.Diff signal =
                ScreenshotDiff.compare("sledgehammer highlight", baseline, withSledgehammer);
        ScreenshotDiff.assertDrew("BlockHighlightRenderer (sledgehammer)", noiseFloor, signal);
    }

    private void testItemRemovalReturnsToBaseline(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                                  Path baseline, ScreenshotDiff.Diff noiseFloor) {
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);

        boolean handIsEmpty = context.computeOnClient(client ->
                client.player != null && client.player.getMainHandItem().isEmpty());

        if (!handIsEmpty) {
            throw new AssertionError("Control step failed: main hand was not cleared. "
                    + RendererTestScene.describeAim(context));
        }

        Path cleared = context.takeScreenshot("highlight-d-cleared-again");
        ScreenshotDiff.Diff residual =
                ScreenshotDiff.compare("control (highlight removed again)", baseline, cleared);

        assertLooksIdentical(noiseFloor, residual,
                "Removing the sledgehammer has to restore the baseline image");
    }

    /** Returns the "octant in hand, outlines only" picture - the baseline of the area fill test. */
    private Path testOctantHighlight(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                     Path baseline, ScreenshotDiff.Diff noiseFloor) {
        singleplayer.getServer().runCommand(
                "item replace entity @a weapon.mainhand with simplebuilding:octant["
                        + "minecraft:custom_data={"
                        + "Pos1:[I;" + OCTANT_POS_1.getX() + "," + OCTANT_POS_1.getY() + "," + OCTANT_POS_1.getZ() + "],"
                        + "Pos2:[I;" + OCTANT_POS_2.getX() + "," + OCTANT_POS_2.getY() + "," + OCTANT_POS_2.getZ() + "]"
                        + "}]");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);

        assertOctantTriggerConditions(context);

        Path withOctant = context.takeScreenshot("highlight-e-octant");
        ScreenshotDiff.Diff signal = ScreenshotDiff.compare("octant highlight", baseline, withOctant);
        ScreenshotDiff.assertDrew("BlockHighlightRenderer (octant)", noiseFloor, signal);
        return withOctant;
    }

    /**
     * The octant area fill: {@code showFill = invertOctantSneak ^ hasConstructorsTouch}. All four
     * combinations of the two inputs are walked through, so the test states the XOR rather than
     * just "the fill can appear":
     * <pre>
     *   plain octant,    invertOctantSneak off  ->  outlines only   (the baseline, shot e/f)
     *   plain octant,    invertOctantSneak on   ->  area fill       (shot g)
     *   plain octant,    invertOctantSneak off  ->  outlines only   (shot h, control)
     *   enchanted,       invertOctantSneak off  ->  area fill       (shot i)
     *   enchanted,       invertOctantSneak on   ->  outlines only   (shot j, both inputs cancel)
     * </pre>
     * The two "outlines only" pictures have to match the baseline within its own noise floor and
     * the two filled ones have to differ from it far above that noise floor.
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
    private void testOctantFillFollowsShowFill(ClientGameTestContext context,
                                               TestSingleplayerContext singleplayer, Path octantOutlines) {
        context.waitTicks(10);
        Path outlinesAgain = context.takeScreenshot("highlight-f-octant-again");
        ScreenshotDiff.Diff octantNoise =
                ScreenshotDiff.compare("noise floor (octant outlines twice)", octantOutlines, outlinesAgain);
        ScreenshotDiff.assertUnchanged(octantNoise);

        setHighlightConfig(context, 100, true);
        context.waitTicks(10);
        assertOctantFillTriggerConditions(context, true, false);

        Path filled = context.takeScreenshot("highlight-g-octant-filled");
        ScreenshotDiff.Diff signal =
                ScreenshotDiff.compare("octant area fill (inverted sneak on)", octantOutlines, filled);
        ScreenshotDiff.assertDrew("BlockHighlightRenderer (octant area fill)", octantNoise, signal);

        setHighlightConfig(context, 100, false);
        context.waitTicks(10);
        assertOctantFillTriggerConditions(context, false, false);

        Path unfilled = context.takeScreenshot("highlight-h-octant-unfilled-again");
        ScreenshotDiff.Diff residual =
                ScreenshotDiff.compare("control (inverted sneak off again)", octantOutlines, unfilled);
        assertLooksIdentical(octantNoise, residual,
                "Switching invertOctantSneak off again has to remove the area fill");

        singleplayer.getServer().runCommand("enchant @a simplebuilding:constructors_touch 1");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(10);
        assertOctantFillTriggerConditions(context, false, true);

        Path touched = context.takeScreenshot("highlight-i-octant-touch-filled");
        ScreenshotDiff.Diff touchedSignal =
                ScreenshotDiff.compare("octant area fill (Constructor's Touch)", octantOutlines, touched);
        ScreenshotDiff.assertDrew("BlockHighlightRenderer (octant area fill via the enchantment)",
                octantNoise, touchedSignal);

        setHighlightConfig(context, 100, true);
        context.waitTicks(10);
        assertOctantFillTriggerConditions(context, true, true);

        Path bothInputs = context.takeScreenshot("highlight-j-octant-both-unfilled");
        ScreenshotDiff.Diff cancelled =
                ScreenshotDiff.compare("control (enchantment and inverted sneak cancel out)",
                        octantOutlines, bothInputs);
        assertLooksIdentical(octantNoise, cancelled,
                "invertOctantSneak together with Constructor's Touch has to cancel out and show no fill");
    }

    /**
     * The sledgehammer fill and the {@code buildingHighlightOpacity} clamp, measured against a
     * quartz wall (see the class javadoc for why the default stone scene cannot show the fill).
     *
     * <p><b>What breaks this test:</b> reading the option as a 0.0-1.0 factor instead of whole
     * percent, dropping either side of {@code max(0, min(100, opacity))}, scaling the outline alpha
     * by the option as well (then opacity 0 would draw nothing), or not scaling the fill at all
     * (then opacity 0 and 100 would be the same picture).
     */
    private void testSledgehammerFillFollowsOpacity(ClientGameTestContext context,
                                                    TestSingleplayerContext singleplayer) {
        RendererTestScene.build(context, singleplayer, "minecraft:quartz_block", "creative");

        Path baseline = context.takeScreenshot("opacity-a-empty-hand");
        context.waitTicks(10);
        Path baselineAgain = context.takeScreenshot("opacity-b-empty-hand-again");
        ScreenshotDiff.Diff noiseFloor =
                ScreenshotDiff.compare("noise floor (quartz wall, empty hand)", baseline, baselineAgain);
        ScreenshotDiff.assertUnchanged(noiseFloor);

        singleplayer.getServer().runCommand(
                "item replace entity @a weapon.mainhand with simplebuilding:netherite_sledgehammer");
        singleplayer.getConnection().waitForClientboundPackets();
        setHighlightOpacity(context, 0);
        context.waitTicks(15);

        assertSledgehammerTriggerConditions(context);
        assertOpacity(context, 0);

        Path fillOff = context.takeScreenshot("opacity-c-fill-off");
        ScreenshotDiff.Diff outlinesOnly =
                ScreenshotDiff.compare("sledgehammer outlines at opacity 0", baseline, fillOff);
        ScreenshotDiff.assertDrew("BlockHighlightRenderer (outlines at opacity 0)", noiseFloor, outlinesOnly);

        setHighlightOpacity(context, 100);
        context.waitTicks(10);
        assertOpacity(context, 100);

        Path fillFull = context.takeScreenshot("opacity-d-fill-full");
        ScreenshotDiff.Diff withFill =
                ScreenshotDiff.compare("sledgehammer outlines and fill at opacity 100", baseline, fillFull);
        ScreenshotDiff.assertDrew("BlockHighlightRenderer (fill at opacity 100)", noiseFloor, withFill);

        // The eight filled block faces cover roughly a 3x3 block square on screen, the outlines only
        // the four pixel wide grid between them - about ten times fewer pixels. Requiring a factor of
        // three leaves room for line width and resolution differences while still failing loudly if
        // the fill stops reacting to the option.
        if (withFill.changedPixels() < outlinesOnly.changedPixels() * 3) {
            throw new AssertionError("buildingHighlightOpacity does not scale the sledgehammer fill: "
                    + "opacity 100 changed " + withFill.changedPixels() + " pixels, opacity 0 changed "
                    + outlinesOnly.changedPixels() + " - expected at least three times as many, because "
                    + "at opacity 100 the eight neighbour faces are filled and at opacity 0 only their "
                    + "outlines are drawn.");
        }

        setHighlightOpacity(context, 500);
        context.waitTicks(10);
        assertOpacity(context, 500);

        Path aboveRange = context.takeScreenshot("opacity-e-above-range");
        ScreenshotDiff.Diff clampedHigh =
                ScreenshotDiff.compare("clamp above range (500 against 100)", fillFull, aboveRange);
        assertLooksIdentical(noiseFloor, clampedHigh,
                "buildingHighlightOpacity 500 has to be clamped to 100 and look exactly like it");

        setHighlightOpacity(context, -50);
        context.waitTicks(10);
        assertOpacity(context, -50);

        Path belowRange = context.takeScreenshot("opacity-f-below-range");
        ScreenshotDiff.Diff clampedLow =
                ScreenshotDiff.compare("clamp below range (-50 against 0)", fillOff, belowRange);
        assertLooksIdentical(noiseFloor, clampedLow,
                "buildingHighlightOpacity -50 has to be clamped to 0 and look exactly like it");
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

    /** Verifies everything {@code drawOctantHighlights} needs before the screenshot is taken. */
    private void assertOctantTriggerConditions(ClientGameTestContext context) {
        String problem = context.computeOnClient(client -> {
            if (client.player == null) {
                return "no client player";
            }

            if (!(client.player.getMainHandItem().getItem() instanceof OctantItem)) {
                return "main hand does not hold an OctantItem but " + client.player.getMainHandItem();
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Octant highlight trigger conditions not met: " + problem);
        }
    }

    /**
     * Verifies every input of {@code showFill = invertOctantSneak ^ hasConstructorsTouch} plus the
     * two positions and {@code ClientState.showHighlights}, which gate the area fill as well.
     * The Constructor's Touch check is read straight from the item's enchantment component instead
     * of through the mod's own helper, so a broken helper cannot make this precondition pass.
     */
    private void assertOctantFillTriggerConditions(ClientGameTestContext context, boolean expectedInvert,
                                                   boolean expectedTouch) {
        String problem = context.computeOnClient(client -> {
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
        });

        if (problem != null) {
            throw new AssertionError("Octant area fill trigger conditions not met: " + problem);
        }
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

    private void assertOpacity(ClientGameTestContext context, int expected) {
        int actual = context.computeOnClient(client ->
                Simplebuilding.getConfig().tools.buildingHighlightOpacity);

        if (actual != expected) {
            throw new AssertionError("Trigger condition not met: buildingHighlightOpacity is " + actual
                    + " but the test set it to " + expected);
        }
    }

    private void setHighlightOpacity(ClientGameTestContext context, int opacity) {
        context.runOnClient(client -> Simplebuilding.getConfig().tools.buildingHighlightOpacity = opacity);
    }

    private void setHighlightConfig(ClientGameTestContext context, int opacity, boolean invertOctantSneak) {
        context.runOnClient(client -> {
            Simplebuilding.getConfig().tools.buildingHighlightOpacity = opacity;
            Simplebuilding.getConfig().tools.invertOctantSneak = invertOctantSneak;
        });
    }

    /**
     * Two screenshots have to show the same thing. The tolerance is the measured noise floor with
     * headroom, not zero: the comparison is only ever used where the claim is "nothing changed".
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
