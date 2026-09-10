package com.simplebuilding.clientgametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.client.gui.NetheriteHopperScreen;
import com.simplebuilding.client.gui.RangefinderHudOverlay;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.util.BundleTooltipAccessor;
import com.simplebuilding.util.HopperFilterMode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Covers the client-only display code: the rangefinder HUD overlay, the reinforced bundle tooltip
 * (bar scaling and mouse wheel submenu), the netherite hopper screen and the armor trim button the
 * mod injects into the survival inventory. None of these have a server side that a headless game
 * test could reach.
 *
 * <p><b>Method.</b> Same as the other renderer tests in this package: measure a noise floor from
 * two screenshots of an unchanged screen, then flip exactly one trigger, then take the trigger away
 * again and require the picture to come back. Every condition the code under test checks is
 * asserted <em>before</em> the screenshot, so an unchanged picture can never be blamed on a scene
 * that was never set up. Without that order a pixel difference proves nothing: a picture that
 * changed could have changed for any reason, a picture that did not could have had no trigger.
 * Where a pixel difference would be too small or not attributable, the test observes state instead
 * and says so at the method. Two measurements deviate from the plain recipe and carry their reason
 * in the code: the filter button's two active modes differ by a single text glyph and get a bar of
 * their own instead of {@code assertDrew}'s world renderer minimum, and the survival inventory's
 * player model is animated by vanilla for good and is painted out of all four of that test's
 * screenshots.
 *
 * <p><b>Why reflection.</b> {@code AbstractContainerScreen} keeps {@code leftPos}, {@code topPos}
 * and {@code hoveredSlot} protected and offers no getter. Putting the mouse on a specific slot -
 * the trigger of both the bundle submenu and the hopper click intercept - is impossible without
 * them. The three fields are read reflectively in {@link #screenField(String)}; if Minecraft ever
 * renames one, the test fails immediately with a message that says it is the test that needs
 * updating, not the mod. Both targets of the shared tree run on official Mojang mappings, so the
 * three names are the runtime names on Fabric and on NeoForge alike.
 *
 * <p><b>Known defect (NetheriteHopperScreen.mouseClicked, common/.../client/gui/NetheriteHopperScreen.java).</b>
 * The click intercept selects its slots with {@code hoveredSlot.getContainerSlot() < 5}.
 * {@code getContainerSlot()} is the index inside the <em>backing container</em>, and
 * {@code HopperMenu} adds the player inventory through {@code addStandardInventorySlots}, which
 * gives the hotbar the container indices 0..8. Hotbar slots 1 to 5 therefore also satisfy the
 * condition: with a filter active, clicking them is swallowed and writes a hopper ghost item
 * instead of picking the stack up. The menu side ({@code ModHopperScreenHandler.clicked}) uses the
 * menu index and is correct, so only the client screen is affected. This test deliberately uses
 * hopper slot 0 (menu index 0, container index 0), which behaves the same before and after a fix,
 * and it parks its own working item in inventory slot 9 so it never runs into the defect.
 *
 * <p><b>Known defect (BundleTooltipComponentMixin:41).</b> The occupancy is divided by
 * {@code (int) capacityScale}. The scale is {@code maxCapacity / 64}, and the base capacity of a
 * reinforced bundle is 1.5 stacks per tier, so the scale is 1.5 for the reinforced bundle and 4.5
 * for the enderite bundle: both are truncated (to 1 and 4) and their tooltip bars fill too early -
 * the reinforced bundle is not scaled at all. Only the netherite bundle has an integral scale (3).
 * {@link #bundleTooltipBarUsesCapacityScale} therefore measures the netherite bundle, where
 * intended and actual behaviour agree, and does not pin the truncation down anywhere.
 *
 * <p><b>Known defect (ReinforcedBundleTooltipSubmenuHandler.onStopHovering).</b> Leaving the slot
 * only sends {@code ReinforcedBundleSelectionPayload(slot.index, -1)}; the client stack keeps the
 * old selection. Its own {@code onMouseScrolled} does both halves, and so does the vanilla handler
 * it stands in for: {@code BundleMouseActions.onStopHovering} goes through
 * {@code toggleSelectedBundleItem}, which calls {@code BundleItem.toggleSelectedItem} on the client
 * stack <em>and</em> sends the packet. The server cannot repair the difference, because
 * {@code BundleContents.selectedItem} is in neither {@code BundleContents.equals} nor
 * {@code BundleContents.STREAM_CODEC} - a container sync compares and transmits only the item list,
 * so a selection index never travels from server to client. Effect: after the mouse leaves the slot
 * the tooltip still highlights the old entry, and the next scroll starts from the stale index while
 * the server starts from -1. {@link #bundleWheelSelectsAndLeavingClears} therefore measures the
 * clearing on the server stack, which is the half that works, and asserts nothing about the client
 * stack in either direction.
 *
 * <p><b>Known defect (ReinforcedBundleTooltipSubmenuHandler:39).</b> When nothing is selected the
 * handler substitutes index 0 for "no selection" and then still adds the wheel delta, so the very
 * first notch downwards lands on the <em>second</em> entry and the first entry cannot be reached by
 * scrolling down at all. {@link #bundleWheelSelectsAndLeavingClears} only asserts that a notch
 * moves the selection by exactly one step, which holds before and after a fix.
 *
 * <p><b>Not covered</b>
 * <ul>
 *   <li><b>The colour of the trim button's tooltip.</b> The tooltip text itself is read from the
 *       render state now (see {@code assertTrimButtonIconAndTooltip}), and so is the icon's
 *       sprite; what the flattened text loses is the AQUA + BOLD style of the first line.</li>
 *   <li><b>The individual numbers in the L / S / C panel.</b> They are drawn as text into the
 *       render state; only their presence as a block of pixels is observable from outside.</li>
 *   <li><b>Green versus orange in the hopper filter icon.</b> The screenshot difference proves that
 *       a different glyph is drawn per mode, but the colour of a glyph cannot be read back without
 *       a reference image.</li>
 *   <li><b>The three line HUD layout (which line holds which number).</b> Only the fact that the
 *       overlay grows by the position and volume lines is measurable in pixels.</li>
 * </ul>
 *
 * <h2>What the port to the shared step form changed, and what it did not</h2>
 *
 * <p>The measurements, their order inside each case, their thresholds, the sixteen screenshot names
 * and every assertion message are the ones the Fabric-only version used. What is mechanically
 * different is a consequence of the step list or of the second loader, never a decision about what
 * the test claims:
 * <ul>
 *   <li><b>Screenshot paths travel in a {@link Later}.</b> A step list registers every step before
 *       any of them runs, so a path cannot live in a local variable, and it cannot be rebuilt from
 *       the screenshot name either: Fabric writes {@code 0004_name.png} with a per run counter
 *       while NeoForge writes {@code name.png}. Every comparison below reads a path that
 *       {@link Script#shot} produced in an <em>earlier</em> step.</li>
 *   <li><b>The comparisons and the image masking run in {@link Script#verify}.</b> They need no
 *       game state, and on Fabric a harness call inside a client task is forbidden outright.</li>
 *   <li><b>The trigger condition checks throw directly</b> instead of returning a problem string to
 *       a caller that throws. An {@code act} step already runs on the client thread and an
 *       exception in it fails the test naming the step. The messages are unchanged.</li>
 *   <li><b>Every case builds its own scene.</b> The straight line version set the game mode once
 *       for all four screen cases and let the chest case inherit the bundle the wheel case had put
 *       in the inventory. All client tests share one world, so an inherited scene is an order
 *       dependency: the chest case would pass or fail depending on what ran before it. Each case
 *       now starts with {@link TestScene#build}, which fills the working volume with air, rebuilds
 *       the wall, clears the inventory, sets the game mode and teleports the player back - so
 *       leftover container blocks, leftover items and a leftover game mode are gone before anything
 *       is measured.</li>
 *   <li><b>The inventory key is a raw GLFW code.</b> The shared {@link Harness} presses key codes,
 *       where Fabric's own input took the key <em>binding</em> and resolved it. A binding that
 *       moved would leave every screen case pressing a key that opens nothing, so
 *       {@link #assertInventoryKeyIsBound} is the control that closes that hole.</li>
 *   <li><b>Server side reads and writes go through {@link ServerProbe}.</b> Fabric's context had
 *       {@code runOnServer} / {@code computeOnServer}; the shared {@link Harness} has neither, and
 *       reading the server's player from the client thread would be a data race on NeoForge, where
 *       the integrated server really does run on its own thread. The work is handed to the server
 *       thread with {@code MinecraftServer.execute} and polled from the client tick instead - the
 *       same shape the NeoForge driver already uses for commands.</li>
 *   <li><b>The cleanup is a pair of ordinary last steps, not a {@code finally} block.</b> A step
 *       list has no place to hang one. <b>They therefore do not run when a step above them
 *       fails</b>, and what they restore outlives the test: the {@code vignette} option and the
 *       chat's visible message filter. After a failure inside the HUD case a later test finds the
 *       vignette off and the chat silenced. Neither makes another test wrong - both only remove
 *       moving pixels - but the mod config, the world and the game mode are put right by the next
 *       {@link TestScene#build} while these two are not.</li>
 * </ul>
 */
public final class HudAndTooltipClientTest {

    /**
     * The z plane one block in front of the wall - free air.
     *
     * <p>Derived from the wall instead of written out, so it cannot drift away from
     * {@link TestScene}. Same value the Fabric-only scene called {@code FRONT_Z}.
     */
    private static final int FRONT_Z = TestScene.WALL_Z - 1;

    /** Free air one block in front of the wall - the container blocks are placed here. */
    private static final BlockPos CONTAINER_POS = new BlockPos(10, 1, FRONT_Z);

    /** Octant corners chosen so that all three of dx, dy and dz differ - that is the volume branch. */
    private static final BlockPos OCTANT_POS_1 = new BlockPos(10, 1, 20);
    private static final BlockPos OCTANT_POS_2 = new BlockPos(13, 4, 24);

    /**
     * Where the cursor is parked for every screenshot: top left, over no slot and no widget, so no
     * hover highlight and no tooltip can creep into a difference measurement.
     */
    private static final int PARK_X = 4;
    private static final int PARK_Y = 4;

    /** Player inventory slot for the test's working items - container index 9, see the class javadoc. */
    private static final int SAFE_INVENTORY_SLOT = 9;

    /** How many diamonds the hopper click test carries on the cursor; see the ghost item checks. */
    private static final int GHOST_CURSOR_COUNT = 3;

    /**
     * The GLFW key code that opens the inventory.
     *
     * <p>A raw code is the only thing both loaders can serve, and it silently assumes the binding
     * still sits on this key - which {@link #assertInventoryKeyIsBound} asserts before every use.
     */
    private static final int INVENTORY_KEY = InputConstants.KEY_E;

    private static final Field LEFT_POS = screenField("leftPos");
    private static final Field TOP_POS = screenField("topPos");
    /** The overlay NetheriteHopperScreen paints over a slot that carries a filter. */
    private static final int GHOST_SLOT_OVERLAY = 0x60FFAA00;

    private static final Field HOVERED_SLOT = screenField("hoveredSlot");

    private HudAndTooltipClientTest() {
    }

    /**
     * The whole test, as steps.
     *
     * <p>Five independent cases. Each one builds the scene it needs from scratch rather than
     * inheriting it, so the run is the same whatever ran before it - see the port notes in the
     * class javadoc.
     *
     * <p>The HUD case runs in creative and the five screen cases in survival, and neither choice is
     * cosmetic. The screen cases must be in survival: {@code InventoryScreen.init} replaces itself
     * with {@code CreativeModeInventoryScreen} as soon as the player has infinite materials, and
     * the mod's button lives in {@code InventoryScreen}. The HUD case keeps the creative mode the
     * straight line version measured in, because its subject is a visible HUD and a survival HUD
     * puts the health, hunger and armor bars on it - three more things that can repaint themselves
     * inside a difference measurement, none of which this case has anything to say about.
     */
    public static void inWorld(Script script) {
        rangefinderHudShowsPositionsAndVolume(script);
        bundleTooltipBarUsesCapacityScale(script);
        bundleWheelSelectsAndLeavingClears(script);
        bundleSubmenuReachesVanillaContainerScreens(script);
        hopperFilterButtonAndGhostSlots(script);
        inventoryTrimStatsButtonToggles(script);

        // What a finally block used to do. As steps these run only when everything above them
        // passed; see the port notes in the class javadoc for what a failure leaves behind.
        TestScene.showHudAgain(script);
    }

    // ------------------------------------------------------------------------------------------
    // RangefinderHudOverlay
    // ------------------------------------------------------------------------------------------

    /**
     * Proves that {@code RangefinderHudOverlay} still reaches the screen through the loader's HUD
     * element registration and that it really renders the measurement, not just its frame.
     *
     * <p>The trigger is deliberately <em>not</em> "octant in hand": that would also swap the held
     * item model and the hotbar icon, and the difference could no longer be attributed to the
     * overlay. The octant is in the main hand for every single screenshot; only its custom data
     * changes, which nothing else on screen reads. Between the shots the test waits 60 ticks
     * because a changed main hand item starts the equip animation and the 40 tick item name popup
     * above the hotbar - both would otherwise land in the difference.
     *
     * <p>With Pos1 and Pos2 set and all three edge lengths different, the overlay takes its volume
     * branch and grows from two lines to five. Because the box is vertically centred on the box
     * height, every pixel of it moves, so the signal is the whole overlay, not just the two extra
     * lines.
     *
     * <p>What breaks this test: the HUD element registration disappearing or moving behind an
     * element that is not drawn, {@code render} returning early (screen check, main and offhand
     * octant lookup, the {@code Pos1}/{@code Pos2} int array reads) or the volume branch no longer
     * producing any text.
     */
    private static void rangefinderHudShowsPositionsAndVolume(Script script) {
        // Creative, and the HUD back on: this is the one case whose subject is the HUD.
        TestScene.build(script, "minecraft:stone", "creative");
        TestScene.showHudAgain(script);

        Later<Boolean> vignetteBefore = silenceHudDrift(script);

        giveOctant(script, false);
        assertRangefinderPreconditions(script, false);

        // The noise floor spans the same 60 ticks a trigger step below spans, not a shorter
        // window. A drift that only shows over a minute of screen time would otherwise fall
        // outside the noise floor and inside every signal - which is exactly how the vignette
        // slipped past a ten tick noise floor before silenceHudDrift existed.
        Later<Path> baseline = script.shot("hud-a-octant-blank");
        script.idle("let sixty ticks pass between the two baseline shots", 60);
        Later<Path> baselineAgain = script.shot("hud-b-octant-blank-again");

        Later<ScreenshotDiff.Diff> noiseFloor = new Later<>("the noise floor of the HUD scene");

        script.verify("measure the noise floor of the HUD scene", () -> {
            ScreenshotDiff.Diff diff = ScreenshotDiff.compare(
                    "noise floor (octant without positions, 60 ticks apart)",
                    baseline.get(), baselineAgain.get());
            ScreenshotDiff.assertUnchanged(diff);
            noiseFloor.set(diff);
        });

        giveOctant(script, true);
        assertRangefinderPreconditions(script, true);

        Later<Path> withPositions = script.shot("hud-c-octant-positions");

        script.verify("the rangefinder overlay reached the screen", () -> {
            ScreenshotDiff.Diff signal = ScreenshotDiff.compare(
                    "rangefinder HUD with both positions", baseline.get(), withPositions.get());
            ScreenshotDiff.assertDrew("RangefinderHudOverlay", noiseFloor.get(), signal);
        });

        assertTheRangefinderReadsWhatItMeasured(script);
        assertAnOffhandOctantAlsoShowsTheHud(script);

        // Control: the same octant without positions has to restore the baseline picture. Without
        // this step "the picture drifts anyway" would explain the difference just as well.
        giveOctant(script, false);
        assertRangefinderPreconditions(script, false);

        Later<Path> clearedAgain = script.shot("hud-d-octant-blank-control");

        script.verify("removing the positions restores the baseline picture", () -> {
            ScreenshotDiff.Diff residual = ScreenshotDiff.compare(
                    "control (positions removed again)", baseline.get(), clearedAgain.get());
            ScreenshotDiff.assertBackToBaseline("removing Pos1 and Pos2 from the octant",
                    noiseFloor.get(), residual);
        });

        restoreHudDrift(script, vignetteBefore);
    }

    /**
     * Switches off the two things that repaint a visible HUD while nobody touches the mod. Both were
     * found by this test failing, not guessed.
     *
     * <p><b>The vignette.</b> {@code Hud.extractCameraOverlays} darkens the screen edges with
     * {@code vignetteBrightness}, and {@code Hud.updateVignetteBrightness} moves that value one
     * percent per <em>frame</em> towards {@code 1 - Lightmap.getBrightness(light level at the eye)}.
     * It starts at 1.0 and approaches the target asymptotically, so it is never finished: measured
     * in this scene it was still brightening the corner pixels from 87 to 100 of 255 between the
     * baseline and the control shot - 22451 changed pixels on a screen where nothing had happened,
     * a hundred times the budget of the control step. An asymptote cannot be waited out, so the
     * option goes off. The vignette is vanilla's; no renderer under test draws into it.
     *
     * <p><b>The chat.</b> {@link TestScene#build} sets the game mode and the server answers with
     * "Your game mode has been updated" - a 660x14 block of pixels that fades out by itself two
     * hundred ticks later, in the middle of this case's screenshots. The filter drops every message
     * before it is queued, {@code clearMessages} removes the ones already queued, and the chat stays
     * empty for the rest of the case.
     *
     * <p>Neither is asserted afterwards: {@code ChatComponent} offers no way to read back its
     * message list or its filter. The vignette option is readable, and
     * {@link #assertRangefinderPreconditions} checks it before every screenshot.
     *
     * @return the vignette setting as it was on entry, for {@link #restoreHudDrift}
     */
    private static Later<Boolean> silenceHudDrift(Script script) {
        Later<Boolean> vignetteBefore = new Later<>("the vignette option found on entry");

        script.act("silence the vignette and the chat", client -> {
            vignetteBefore.set(client.options.vignette().get());
            client.options.vignette().set(false);
            client.gui.hud.getChat().setVisibleMessageFilter(message -> false);
            client.gui.hud.getChat().clearMessages(true);
        });

        script.idle("let the silenced chat leave the screen", 10);
        return vignetteBefore;
    }

    /**
     * Puts both back. Client options and the chat filter outlive a single client test, so the next
     * test in the run would otherwise measure a screen this one had reconfigured.
     *
     * <p>An ordinary step, so it does not run when something above it failed - see the port notes
     * in the class javadoc.
     */
    private static void restoreHudDrift(Script script, Later<Boolean> vignetteBefore) {
        script.act("put the vignette and the chat filter back", client -> {
            client.options.vignette().set(vignetteBefore.get());
            client.gui.hud.getChat().setVisibleMessageFilter(message -> true);
        });
    }


    // ------------------------------------------------------------------------------------------
    // What the rangefinder actually says, and whose hand it looks in
    // ------------------------------------------------------------------------------------------

    /**
     * The HUD reports the measurement the two positions describe, line by line.
     *
     * <p>The screenshot above only says that five lines appeared where two were. It cannot read
     * them, and the panel does not even change width when the numbers do: its width comes from
     * {@code Math.max(actualTextWidth, width("Pos 2: -8888, 888, -8888"))}, so the longest sample
     * line decides. Dropping the {@code + 1} from all three edge lengths turns a 4x4x5 volume of
     * 80 into a 3x3x4 volume of 36 - a measuring tool that measures wrong - and every pixel check
     * in this file stays green.
     *
     * <p>Read by calling the overlay with a recorder instead of a real graphics context, the same
     * way {@code AirJumpClientTest} reads its cooldown bar. That is a CPU only pass into a
     * throwaway render state; nothing reaches the GPU and nothing on screen changes.
     *
     * <p>The expected numbers are computed here from the two positions rather than copied, so the
     * step still says something if the positions are ever moved: both ends are inclusive, which is
     * what the {@code + 1} is for.
     */
    private static void assertTheRangefinderReadsWhatItMeasured(Script script) {
        script.act("the rangefinder HUD reports the volume it measured", client -> {
            List<String> lines = rangefinderLines(client);

            int dx = Math.abs(OCTANT_POS_1.getX() - OCTANT_POS_2.getX()) + 1;
            int dy = Math.abs(OCTANT_POS_1.getY() - OCTANT_POS_2.getY()) + 1;
            int dz = Math.abs(OCTANT_POS_1.getZ() - OCTANT_POS_2.getZ()) + 1;

            List<String> expected = List.of(
                    "Pos 1: " + OCTANT_POS_1.getX() + ", " + OCTANT_POS_1.getY() + ", " + OCTANT_POS_1.getZ(),
                    "Pos 2: " + OCTANT_POS_2.getX() + ", " + OCTANT_POS_2.getY() + ", " + OCTANT_POS_2.getZ(),
                    "Volume: " + (dx * dy * dz) + " blocks³",
                    "(" + dx + " x " + dy + " x " + dz + ")");

            for (String line : expected) {
                if (!lines.contains(line)) {
                    throw new AssertionError("The rangefinder HUD does not say [" + line + "]. It says "
                            + lines + ". Both ends of the selection count, so a "
                            + OCTANT_POS_1 + " to " + OCTANT_POS_2 + " selection is "
                            + dx + " by " + dy + " by " + dz + " blocks. The screenshot check above "
                            + "cannot see this: the panel keeps its width whatever the numbers are, "
                            + "because the longest sample line decides it.");
                }
            }
        });
    }

    /**
     * An octant in the off hand shows the HUD too.
     *
     * <p>The overlay looks in the main hand first and falls back to the off hand, and the class
     * javadoc of the ancestor claimed that fallback was covered. It was not: every shot in this
     * case puts the octant in the MAIN hand with {@code item replace ... weapon.mainhand}, so
     * deleting the fallback changes nothing here.
     *
     * <p>Measured by reading the overlay rather than by a screenshot, because the two hands draw
     * the same panel - a picture could not tell which hand produced it, and the main hand has to
     * be empty for the statement to mean anything.
     */
    private static void assertAnOffhandOctantAlsoShowsTheHud(Script script) {
        script.command("item replace entity @a weapon.mainhand with minecraft:air", true);
        script.command("item replace entity @a weapon.offhand with simplebuilding:octant["
                + "minecraft:custom_data={"
                + "Pos1:[I;" + OCTANT_POS_1.getX() + "," + OCTANT_POS_1.getY() + "," + OCTANT_POS_1.getZ() + "],"
                + "Pos2:[I;" + OCTANT_POS_2.getX() + "," + OCTANT_POS_2.getY() + "," + OCTANT_POS_2.getZ() + "]"
                + "}]");
        script.awaitPackets();
        script.idle("let the hands swap over on the client", 30);

        script.act("setup: the octant really is in the off hand and nowhere else", client -> {
            if (client.player == null) {
                throw new AssertionError("Setup failed: no client player.");
            }

            if (!(client.player.getOffhandItem().getItem() instanceof OctantItem)) {
                throw new AssertionError("Setup failed: the off hand holds "
                        + client.player.getOffhandItem() + " instead of an octant.");
            }

            if (client.player.getMainHandItem().getItem() instanceof OctantItem) {
                throw new AssertionError("Setup failed: the main hand still holds an octant, so the "
                        + "check below would pass through the main hand branch.");
            }
        });

        script.act("an octant in the off hand still draws the rangefinder HUD", client -> {
            List<String> lines = rangefinderLines(client);

            if (lines.isEmpty()) {
                throw new AssertionError("With the octant in the OFF hand the rangefinder HUD draws "
                        + "nothing. The overlay looks in the main hand first and falls back to the "
                        + "off hand; that fallback is gone. Every screenshot in this case holds the "
                        + "octant in the main hand, so none of them can see it.");
            }
        });

        script.command("item replace entity @a weapon.offhand with minecraft:air");
        script.awaitPackets();
        script.idle("let the emptied off hand reach the client", 20);
    }

    /**
     * The lines the rangefinder overlay would draw right now, as plain strings.
     *
     * <p>Calls {@code RangefinderHudOverlay.render} with a recorder: a real
     * {@code GuiGraphicsExtractor} over a throwaway {@code GuiRenderState}, so every guard inside
     * the overlay runs for real and nothing is submitted to the GPU. An empty list therefore means
     * "the overlay decided to draw nothing", which is a statement in itself.
     */
    private static List<String> rangefinderLines(Minecraft client) {
        GuiRenderState state = new GuiRenderState();
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(client, state,
                client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        RangefinderHudOverlay.render(graphics);

        List<String> lines = new ArrayList<>();
        for (DrawnText text : drawnTexts(state)) {
            lines.add(text.text());
        }
        return lines;
    }

    /** Puts an octant in the main hand, with or without the two positions in its custom data. */
    private static void giveOctant(Script script, boolean withPositions) {
        String positions = withPositions
                ? "[minecraft:custom_data={"
                        + "Pos1:[I;" + OCTANT_POS_1.getX() + "," + OCTANT_POS_1.getY() + "," + OCTANT_POS_1.getZ() + "],"
                        + "Pos2:[I;" + OCTANT_POS_2.getX() + "," + OCTANT_POS_2.getY() + "," + OCTANT_POS_2.getZ() + "]"
                        + "}]"
                : "";

        script.command("item replace entity @a weapon.mainhand with simplebuilding:octant" + positions);
        script.awaitPackets();
        // Equip animation plus the 40 tick item name popup that a changed main hand item triggers.
        script.idle("let the octant arrive and its equip animation and name popup finish", 60);
    }

    /** Everything {@code RangefinderHudOverlay.render} checks before it draws anything. */
    private static void assertRangefinderPreconditions(Script script, boolean expectPositions) {
        script.act("the rangefinder trigger conditions hold (positions: " + expectPositions + ")", client -> {
            if (client.player == null) {
                throw new AssertionError("Rangefinder HUD trigger conditions not met: no client player");
            }

            if (client.gui.hud.isHidden()) {
                throw new AssertionError("Rangefinder HUD trigger conditions not met: the HUD is "
                        + "hidden, so no HUD element is extracted at all");
            }

            if (client.options.vignette().get()) {
                throw new AssertionError("Rangefinder HUD trigger conditions not met: the vignette is "
                        + "switched on again; its brightness creeps towards the light level by one "
                        + "percent per frame and never arrives, so the screen edges would drift "
                        + "between two screenshots (see silenceHudDrift)");
            }

            if (client.gui.screen() != null) {
                throw new AssertionError("Rangefinder HUD trigger conditions not met: a screen is open ("
                        + client.gui.screen().getClass().getName() + "); the overlay returns early on "
                        + "OctantScreen and screens cover the HUD");
            }

            ItemStack held = client.player.getMainHandItem();

            if (!(held.getItem() instanceof OctantItem)) {
                throw new AssertionError("Rangefinder HUD trigger conditions not met: main hand does "
                        + "not hold an OctantItem but " + held);
            }

            CompoundTag nbt = held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            boolean hasPos1 = nbt.getIntArray("Pos1").orElse(new int[0]).length == 3;
            boolean hasPos2 = nbt.getIntArray("Pos2").orElse(new int[0]).length == 3;

            if (expectPositions && !(hasPos1 && hasPos2)) {
                throw new AssertionError("Rangefinder HUD trigger conditions not met: the octant should "
                        + "carry Pos1 and Pos2 but its custom data is " + nbt);
            }

            if (!expectPositions && (hasPos1 || hasPos2)) {
                throw new AssertionError("Rangefinder HUD trigger conditions not met: the octant should "
                        + "carry no positions but its custom data is " + nbt);
            }
        });
    }

    // ------------------------------------------------------------------------------------------
    // Reinforced bundle tooltip bar
    // ------------------------------------------------------------------------------------------

    /**
     * Proves that the mod's tooltip path really rescales the bundle progress bar: that
     * {@code ReinforcedBundleItem.getTooltipImage} hands out a {@link ReinforcedBundleTooltipData}
     * with the enlarged capacity, that the tooltip component callback turns it into a
     * {@link ClientBundleTooltip} with the capacity scale set, and that
     * {@code BundleTooltipComponentMixin} divides the occupancy by it.
     *
     * <p>This one observes render state instead of pixels, for the same reason
     * {@link MultiBlockBreakingClientTest} does: the only way to change the scale of a tooltip that
     * is already on screen is to change the item, and then the icon, the name and the item grid
     * change with it, so a screenshot difference would not be attributable to the bar. The
     * component is therefore extracted twice into a fresh {@code GuiRenderState} - a CPU only pass,
     * exactly the one the real tooltip runs every frame - and the resulting element geometry is
     * compared.
     *
     * <p>Structure, the same three steps as the pixel tests:
     * <ul>
     *   <li><b>Noise floor</b> - the same component extracted twice has to give the identical
     *       geometry, otherwise nothing below it means anything.</li>
     *   <li><b>Signal</b> - a plain {@code new ClientBundleTooltip(contents)} (capacity scale 1, no
     *       mod involved) against the component the mod's callback produced. Same class, same
     *       contents, same font, same size: the capacity scale is the only difference. With 64
     *       stone in a netherite bundle the unscaled weight is exactly 1, so vanilla paints the
     *       full bar and its "Full" label, while a third of that paints a short bar and no label -
     *       hence the extra assertion on the number of text elements, which is the readable half of
     *       the geometry difference.</li>
     *   <li><b>Control</b> - setting the scale back to 1 on the very same component has to
     *       reproduce the unscaled geometry down to the pixel. That rules out everything except the
     *       capacity scale as the cause of the difference.</li>
     * </ul>
     *
     * <p>The netherite bundle is used because its capacity factor is 3, the only integral one - see
     * the known defect in the class javadoc.
     *
     * <p>The whole proof is one {@code act} step: it needs the client thread (font metrics, render
     * state) and nothing else, and an exception inside it fails the test naming the step.
     *
     * <p>What breaks this test: {@code getTooltipImage} losing the enlarged capacity, the tooltip
     * component callback not being registered, the mixin no longer applying to
     * {@code ClientBundleTooltip} (the {@link BundleTooltipAccessor} cast fails), or its
     * {@code ModifyArg} no longer hitting {@code extractBundleWithItemsTooltip}.
     */
    private static void bundleTooltipBarUsesCapacityScale(Script script) {
        TestScene.build(script, "minecraft:stone", "survival");

        script.act("the reinforced bundle tooltip bar is scaled by the enlarged capacity", client -> {
            BundleContents contents = new BundleContents(List.of(
                    ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.STONE, 64))));

            ItemStack bundle = new ItemStack(ModItems.NETHERITE_BUNDLE);
            bundle.set(DataComponents.BUNDLE_CONTENTS, contents);

            TooltipComponent data = bundle.getTooltipImage().orElse(null);

            if (!(data instanceof ReinforcedBundleTooltipData reinforced)) {
                throw new AssertionError("Reinforced bundle tooltip bar: getTooltipImage did not return "
                        + "a ReinforcedBundleTooltipData but " + data);
            }

            // 2 tiers * 1.5 stacks * 64 items. The mod's own comment names the same number.
            if (reinforced.maxCapacity() != 192) {
                throw new AssertionError("Reinforced bundle tooltip bar: the netherite bundle reports a "
                        + "capacity of " + reinforced.maxCapacity() + " items instead of 192, so the "
                        + "tooltip would be scaled by the wrong factor");
            }

            ClientTooltipComponent modComponent = ClientTooltipComponent.create(data);

            if (!(modComponent instanceof ClientBundleTooltip)) {
                throw new AssertionError("Reinforced bundle tooltip bar: the ClientTooltipComponentCallback "
                        + "did not produce a ClientBundleTooltip but " + modComponent.getClass().getName());
            }

            if (!(modComponent instanceof BundleTooltipAccessor accessor)) {
                throw new AssertionError("Reinforced bundle tooltip bar: BundleTooltipComponentMixin is "
                        + "not applied - ClientBundleTooltip does not implement BundleTooltipAccessor");
            }

            TooltipShape scaled = extractShape(client, modComponent);
            TooltipShape scaledAgain = extractShape(client, modComponent);

            if (!scaled.equals(scaledAgain)) {
                throw new AssertionError("Reinforced bundle tooltip bar: extracting the same tooltip "
                        + "component twice gave different geometry (" + scaled + " vs. " + scaledAgain
                        + "); no comparison below this is meaningful");
            }

            TooltipShape unscaled = extractShape(client, new ClientBundleTooltip(contents));

            if (scaled.equals(unscaled)) {
                throw new AssertionError("Reinforced bundle tooltip bar: the mod's tooltip component draws "
                        + "exactly like an unscaled vanilla one (" + unscaled + "), so the capacity scale "
                        + "never reached the progress bar");
            }

            if (scaled.textElements() >= unscaled.textElements()) {
                throw new AssertionError("Reinforced bundle tooltip bar: the unscaled bar should carry the "
                        + "\"Full\" label at weight 1 and the scaled one should not, but the scaled "
                        + "component has " + scaled.textElements() + " text elements and the unscaled one "
                        + unscaled.textElements());
            }

            // The VALUE of the scale, not just that there is one. Everything above proves the mod's
            // component draws differently from an unscaled one; it does not say by how much, and
            // "maxCapacity / 32" instead of "/ 64" would pass every line of it - the bar would
            // simply fill half as fast as the bundle does. The mixin has no getter, so the value is
            // read the way a player would read it: a vanilla component set to the expected scale
            // has to draw the same geometry, and one set to the doubled scale has to draw a
            // different one. That second half is what makes the first half a statement.
            float expectedScale = reinforced.maxCapacity() / 64.0f;
            ClientBundleTooltip reference = new ClientBundleTooltip(contents);
            ((BundleTooltipAccessor) reference).simplebuilding$setCapacityScale(expectedScale);
            TooltipShape atExpectedScale = extractShape(client, reference);

            if (!atExpectedScale.equals(scaled)) {
                throw new AssertionError("Reinforced bundle tooltip bar: the mod's component does not draw "
                        + "like a component scaled by maxCapacity / 64 = " + expectedScale + " (mod: "
                        + scaled + ", reference: " + atExpectedScale + "). The scale the callback sets is "
                        + "not the capacity in stacks.");
            }

            ((BundleTooltipAccessor) reference).simplebuilding$setCapacityScale(expectedScale * 2);
            TooltipShape atDoubledScale = extractShape(client, reference);

            if (atDoubledScale.equals(scaled)) {
                throw new AssertionError("Reinforced bundle tooltip bar: a component scaled by "
                        + (expectedScale * 2) + " draws exactly like the mod's (" + scaled + "), so the "
                        + "geometry cannot tell the expected scale from a doubled one and the check "
                        + "above proves nothing about the value.");
            }

            accessor.simplebuilding$setCapacityScale(1.0f);
            TooltipShape control = extractShape(client, modComponent);

            if (!control.equals(unscaled)) {
                throw new AssertionError("Reinforced bundle tooltip bar: control failed - with the capacity "
                        + "scale set back to 1 the mod's component still does not draw like the vanilla one ("
                        + control + " vs. " + unscaled + "), so the measured difference cannot be attributed "
                        + "to the scale");
            }

            TestLog.info("bundle tooltip bar: unscaled " + unscaled + ", scaled by " + expectedScale + " "
                    + scaled);
        });

        // A picture of the real thing for the record - the assertions above are the proof.
        placeBundleInInventory(script, () -> List.of(new ItemStack(Items.STONE, 64)));
        script.awaitPackets();
        script.idle("let the bundle reach the client", 15);

        openInventoryScreen(script);
        Later<Integer> bundleSlot = findBundleSlot(script);
        hoverSlot(script, bundleSlot, "the bundle slot");
        script.idle("let the tooltip appear", 10);
        script.shot("tooltip-a-netherite-bundle-bar");
        closeScreen(script);
    }

    /** Element geometry and text element count of one tooltip component extraction. */
    private record TooltipShape(int textElements, String geometry) {
        @Override
        public String toString() {
            return textElements + " text elements, " + geometry.length() + " chars of geometry";
        }
    }

    /**
     * Runs the component's extraction pass into a throwaway render state and boils the result down
     * to something comparable: the sorted bounds of every GUI element plus the number of text
     * elements. Everything that changes the progress bar changes the bounds.
     */
    private static TooltipShape extractShape(Minecraft client, ClientTooltipComponent component) {
        GuiRenderState state = new GuiRenderState();
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(client, state, 0, 0);

        int width = component.getWidth(client.font);
        int height = component.getHeight(client.font);
        component.extractImage(client.font, 0, 0, width, height, graphics);

        List<String> rectangles = new ArrayList<>();
        state.forEachElement(element -> {
            var bounds = element.bounds();
            rectangles.add(bounds == null
                    ? "?"
                    : bounds.left() + "/" + bounds.top() + "/" + bounds.width() + "/" + bounds.height());
        }, GuiRenderState.TraverseRange.ALL);
        Collections.sort(rectangles);

        int[] texts = new int[1];
        state.forEachText(text -> texts[0]++);

        return new TooltipShape(texts[0], String.join(" ", rectangles));
    }

    // ------------------------------------------------------------------------------------------
    // Reinforced bundle mouse wheel submenu
    // ------------------------------------------------------------------------------------------

    /**
     * Proves that {@code ReinforcedBundleTooltipSubmenuHandler} is wired into the container screen,
     * that a wheel notch over the bundle slot moves the selection by exactly one entry in the
     * natural direction on both the client and the server, and that leaving the slot clears the
     * selection again.
     *
     * <p>The measurement is attributable because the mod's bundles are <em>not</em> in
     * {@code minecraft:bundles}: vanilla's own {@code BundleMouseActions} matches on that tag, it
     * runs first in {@code itemSlotMouseActions} and it returns true unconditionally, so if the mod
     * ever added its bundles to that tag every wheel notch below would be vanilla's work and this
     * test would silently stop testing the mod. The precondition check therefore also asserts that
     * the stack is not in the tag.
     *
     * <p>The step size is asserted, not the absolute index: see the known defect about the first
     * notch in the class javadoc.
     *
     * <p>Leaving the slot is measured on the <em>server</em> stack, and that is not a shortcut: as
     * described under the known defect in the class javadoc, a selection index cannot travel from
     * the server to the client at all, and the mod's {@code onStopHovering} does not write the
     * client stack itself. The server index is therefore the only observable this step has - and a
     * complete one, because {@code onStopHovering} is the only place in the mod that ever sends -1.
     *
     * <p>What breaks this test: {@code HandledScreenMixin} no longer registering the handler,
     * {@code matches} no longer recognising a {@link ReinforcedBundleItem}, the wheel direction
     * flipping, the selection payload not being registered or handled, or {@code onStopHovering}
     * losing its packet.
     */
    private static void bundleWheelSelectsAndLeavingClears(Script script) {
        TestScene.build(script, "minecraft:stone", "survival");

        placeBundleInInventory(script, () -> List.of(
                new ItemStack(Items.STONE, 16),
                new ItemStack(Items.DIRT, 16),
                new ItemStack(Items.OAK_PLANKS, 16)));
        script.awaitPackets();
        script.idle("let the bundle reach the client", 15);

        openInventoryScreen(script);

        Later<Integer> slotIndex = findBundleSlot(script);
        hoverSlot(script, slotIndex, "the bundle slot");
        assertBundleWheelPreconditions(script, slotIndex, 3);

        Later<Integer> start = readClientSelection(script, slotIndex, "before the first wheel notch");

        script.verify("nothing is selected in the bundle yet", () -> {
            if (start.get() != BundleContents.NO_SELECTED_ITEM_INDEX) {
                throw new AssertionError("The bundle already had entry " + start.get()
                        + " selected before the first wheel notch; the steps below would not be attributable.");
            }
        });

        Later<Integer> first = scrollAndReadSelection(script, slotIndex, -1.0, start, null);

        script.verify("the first wheel notch selected an entry", () -> {
            if (first.get() < 0 || first.get() > 2) {
                throw new AssertionError("A wheel notch over the bundle slot did not select any entry: "
                        + "the selected index is " + first.get() + ", expected one of 0..2.");
            }
        });

        Later<Integer> second = scrollAndReadSelection(script, slotIndex, -1.0, first, null);

        script.verify("a second notch downwards moved the selection by exactly one", () -> {
            if (second.get() != first.get() + 1) {
                throw new AssertionError("A wheel notch downwards moved the selection from " + first.get()
                        + " to " + second.get() + " instead of " + (first.get() + 1) + ".");
            }
        });

        Later<Integer> back = scrollAndReadSelection(script, slotIndex, 1.0, second, null);

        // intValue() on both sides on purpose: these are Later<Integer>, and "!=" on two boxed
        // Integers compares references. It happens to agree with the values here only because
        // small Integers are cached, which is not something a test may rest on.
        script.verify("a notch upwards moved the selection back by exactly one", () -> {
            if (back.get().intValue() != first.get().intValue()) {
                throw new AssertionError("A wheel notch upwards moved the selection from " + second.get()
                        + " to " + back.get() + " instead of back to " + first.get() + ".");
            }
        });

        // The packet half: the server has to carry the same selection, otherwise the tooltip is the
        // only thing that ever knew about it and placing from the bundle would use the wrong entry.
        Later<Integer> onServer = readServerSelection(script, slotIndex);

        script.verify("the selection reached the server", () -> {
            if (onServer.get().intValue() != back.get().intValue()) {
                throw new AssertionError("The server stack has entry " + onServer.get() + " selected while "
                        + "the client shows " + back.get() + "; ReinforcedBundleSelectionPayload did not arrive.");
            }
        });

        script.shot("bundle-a-wheel-selection");

        // Leaving the slot. Read on the server, see the javadoc: no packet carries a selection
        // index back to the client, so the client stack is not an observable of this step.
        parkCursor(script);
        awaitServerSelection(script, slotIndex, BundleContents.NO_SELECTED_ITEM_INDEX,
                "leaving the bundle slot did not clear the selection on the server");

        closeScreen(script);
    }

    /**
     * Proves that the handler {@code HandledScreenMixin} injects reaches container screens the mod
     * knows nothing about, by doing the same wheel notch in a plain vanilla chest.
     *
     * <p>A vanilla {@code ContainerScreen} is the point of the test: the mixin sits on
     * {@code AbstractContainerScreen.init}, so if it were ever narrowed to the mod's own screens
     * this would be the case that notices. The bundle sits in a player inventory slot, which is
     * part of the chest menu, so no chest content is needed.
     *
     * <p><b>The bundle is placed here, not inherited.</b> The straight line version relied on the
     * wheel case above having put one in the inventory and on the selection it had left on the
     * client stack. All client tests share one world, so that was an order dependency: run this
     * case alone and it fails on a missing bundle. It now builds its own scene and places its own
     * bundle, which also makes the selection start at "nothing selected" again.
     *
     * <p>The guard below stays even though a fresh bundle can only start unselected: the client
     * stack is never cleared by {@code onStopHovering} (see the known defect in the class javadoc),
     * so a bundle that somehow survived into this case with its last entry selected would make a
     * notch downwards a no-op - and a clamped selection would look exactly like a missing handler.
     *
     * <p>What breaks this test: the mixin's injection point moving off {@code init}, or the handler
     * being registered from a mod screen instead of from the shared base class.
     */
    private static void bundleSubmenuReachesVanillaContainerScreens(Script script) {
        TestScene.build(script, "minecraft:stone", "survival");

        placeBundleInInventory(script, () -> List.of(
                new ItemStack(Items.STONE, 16),
                new ItemStack(Items.DIRT, 16),
                new ItemStack(Items.OAK_PLANKS, 16)));
        script.command("setblock " + CONTAINER_POS.getX() + " " + CONTAINER_POS.getY() + " "
                + CONTAINER_POS.getZ() + " minecraft:chest");
        script.awaitPackets();
        script.idle("let the chest and the bundle reach the client", 15);

        aimAt(script, "0.0", "0.0", CONTAINER_POS, Direction.NORTH);

        // Empty main hand, otherwise the right click would use the item instead of opening the
        // chest. TestScene.build cleared the inventory and the bundle went into slot 9, so this is
        // an assertion rather than a step - but an unasserted empty hand is how a right click ends
        // up placing a block in front of the chest instead of opening it.
        script.act("the main hand is empty so the right click opens the chest", client -> {
            if (client.player == null || !client.player.getMainHandItem().isEmpty()) {
                throw new AssertionError("The main hand is not empty ("
                        + (client.player == null ? "no player" : client.player.getMainHandItem())
                        + "), so the right click below would use the item instead of opening the chest.");
            }
        });

        script.harness("right click the chest", harness -> harness.pressMouse(1));
        awaitScreen(script, ContainerScreen.class, "the vanilla chest screen");
        script.idle("let the chest screen settle", 20);

        Later<Integer> slotIndex = findBundleSlot(script);
        hoverSlot(script, slotIndex, "the bundle slot");
        assertBundleWheelPreconditions(script, slotIndex, 3);

        Later<Integer> start = readClientSelection(script, slotIndex, "before the wheel notch in the chest");

        script.verify("a notch downwards still has somewhere to go", () -> {
            if (start.get() >= 2) {
                throw new AssertionError("The bundle already has its last of three entries selected ("
                        + start.get() + "), so a wheel notch downwards has nowhere to go and this test could "
                        + "not tell a clamped selection from a handler that never ran.");
            }
        });

        scrollAndReadSelection(script, slotIndex, -1.0, start,
                "A wheel notch over the bundle slot did nothing in a vanilla chest screen; the submenu "
                        + "handler is not registered on AbstractContainerScreen subclasses outside the mod.");

        script.shot("bundle-b-chest-wheel");

        parkCursor(script);
        script.idle("let the cursor leave the slot", 10);
        closeScreen(script);

        script.command("setblock " + CONTAINER_POS.getX() + " " + CONTAINER_POS.getY() + " "
                + CONTAINER_POS.getZ() + " minecraft:air");
        script.command("clear @a");
        script.awaitPackets();
        script.idle("let the removed chest reach the client", 10);
    }

    /** Everything {@code AbstractContainerScreen.mouseScrolled} and the mod's handler check. */
    private static void assertBundleWheelPreconditions(Script script, Later<Integer> slotIndex,
                                                       int expectedEntries) {
        script.act("the bundle wheel trigger conditions hold", client -> {
            AbstractContainerScreen<?> screen = containerScreen(client);
            Slot slot = screen.getMenu().slots.get(slotIndex.get());

            if (!slot.hasItem()) {
                throw new AssertionError("Bundle wheel trigger conditions not met: slot " + slotIndex.get()
                        + " is empty, so mouseScrolled never reaches any handler");
            }

            ItemStack stack = slot.getItem();

            if (!(stack.getItem() instanceof ReinforcedBundleItem)) {
                throw new AssertionError("Bundle wheel trigger conditions not met: slot " + slotIndex.get()
                        + " does not hold a ReinforcedBundleItem but " + stack);
            }

            if (stack.is(ItemTags.BUNDLES)) {
                throw new AssertionError("Bundle wheel trigger conditions not met: the mod's bundle is in "
                        + "the minecraft:bundles tag, so vanilla's BundleMouseActions matches first and "
                        + "swallows every wheel notch - the measurement below would prove nothing about "
                        + "the mod");
            }

            BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);

            if (contents == null || contents.size() != expectedEntries) {
                throw new AssertionError("Bundle wheel trigger conditions not met: the bundle should hold "
                        + expectedEntries + " entries but holds "
                        + (contents == null ? "no bundle contents at all" : contents.size()));
            }

            Slot hovered = hoveredSlot(screen);

            if (hovered != slot) {
                throw new AssertionError("Bundle wheel trigger conditions not met: the mouse is not on the "
                        + "bundle slot (hovered: "
                        + (hovered == null ? "nothing" : "menu slot " + hovered.index) + ")");
            }
        });
    }

    /**
     * One wheel notch over the hovered slot, then the new selection index.
     *
     * <p>The wheel is a harness step and the read is a client step, so the two cannot be one call
     * the way they were in straight line code. The previous index arrives as a {@link Later}
     * because it too was only known at run time.
     *
     * @param whenUnchanged what to say if the notch moved nothing; null uses the generic message
     */
    private static Later<Integer> scrollAndReadSelection(Script script, Later<Integer> slotIndex,
                                                         double amount, Later<Integer> previous,
                                                         String whenUnchanged) {
        Later<Integer> now = new Later<>("the bundle selection after a wheel notch of " + amount);

        script.harness("turn the wheel by " + amount + " over the bundle slot",
                harness -> harness.scroll(amount));
        script.idle("let the wheel notch reach the handler", 10);

        script.act("read the bundle selection after a wheel notch of " + amount, client -> {
            int selected = selectedIndexOf(
                    containerScreen(client).getMenu().slots.get(slotIndex.get()).getItem());

            if (selected == previous.get()) {
                throw new AssertionError(whenUnchanged != null
                        ? whenUnchanged + " (selection stayed at " + previous.get() + ")"
                        : "A wheel notch of " + amount + " over the bundle slot left the selection at "
                                + previous.get() + ".");
            }

            now.set(selected);
        });

        return now;
    }

    /** The selection index the client's copy of the slot's stack carries right now. */
    private static Later<Integer> readClientSelection(Script script, Later<Integer> slotIndex, String when) {
        Later<Integer> selected = new Later<>("the client's bundle selection " + when);

        script.act("read the client's bundle selection " + when, client -> selected.set(selectedIndexOf(
                containerScreen(client).getMenu().slots.get(slotIndex.get()).getItem())));

        return selected;
    }

    /** The selection index the server's copy of the same menu slot carries right now. */
    private static Later<Integer> readServerSelection(Script script, Later<Integer> slotIndex) {
        return onServer(script, "the server's bundle selection", server -> selectedIndexOf(
                firstPlayer(server).containerMenu.slots.get(slotIndex.get()).getItem()));
    }

    /**
     * Waits for the server stack of a menu slot to reach a selection index.
     *
     * <p>Every poll is a fresh job on the server thread, so the value is read where it lives rather
     * than across threads; ticking the client also ticks the integrated server, which is where a
     * packet the client just sent becomes visible.
     */
    private static void awaitServerSelection(Script script, Later<Integer> slotIndex, int expected,
                                             String message) {
        int[] lastSeen = {Integer.MIN_VALUE};
        ServerProbe<Integer> probe = new ServerProbe<>("reading the server's bundle selection",
                server -> selectedIndexOf(firstPlayer(server).containerMenu.slots.get(slotIndex.get()).getItem()));

        script.await("the server's bundle selection reaches " + expected, 120, client -> {
            if (!probe.poll()) {
                return false;
            }

            lastSeen[0] = probe.value();

            if (lastSeen[0] == expected) {
                return true;
            }

            probe.askAgain();
            return false;
        }, client -> message + ": the server stack still shows entry " + lastSeen[0] + " instead of "
                + expected + ".");
    }

    private static int selectedIndexOf(ItemStack stack) {
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        return contents == null ? Integer.MIN_VALUE : contents.getSelectedItemIndex();
    }

    /**
     * Puts a netherite bundle with the given entries into {@link #SAFE_INVENTORY_SLOT}.
     *
     * <p>The entries are built by a supplier rather than handed over as stacks, because the steps
     * of a script are registered long before they run and an {@link ItemStack} built at
     * registration time would be one shared, mutable object.
     */
    private static void placeBundleInInventory(Script script, Supplier<List<ItemStack>> entries) {
        runOnServer(script, "put a netherite bundle in the player inventory", server -> {
            ItemStack bundle = new ItemStack(ModItems.NETHERITE_BUNDLE);
            bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(
                    entries.get().stream().map(ItemStackTemplate::fromNonEmptyStack).toList()));
            firstPlayer(server).getInventory().setItem(SAFE_INVENTORY_SLOT, bundle);
        });
    }

    /** The one menu slot holding a reinforced bundle; more than one would make a measurement ambiguous. */
    private static Later<Integer> findBundleSlot(Script script) {
        Later<Integer> index = new Later<>("the menu slot holding the reinforced bundle");

        script.act("find the reinforced bundle in the open screen", client -> {
            AbstractContainerScreen<?> screen = containerScreen(client);
            int found = -1;

            for (int i = 0; i < screen.getMenu().slots.size(); i++) {
                if (screen.getMenu().slots.get(i).getItem().getItem() instanceof ReinforcedBundleItem) {
                    if (found != -1) {
                        throw new AssertionError("More than one reinforced bundle in the open container "
                                + "screen; the test would not know which slot it is measuring.");
                    }

                    found = i;
                }
            }

            if (found == -1) {
                throw new AssertionError("No reinforced bundle in the open container screen.");
            }

            index.set(found);
        });

        return index;
    }

    // ------------------------------------------------------------------------------------------
    // NetheriteHopperScreen
    // ------------------------------------------------------------------------------------------

    /**
     * Proves the three things {@code NetheriteHopperScreen} draws on top of the vanilla hopper
     * background: a different filter button icon per mode, the orange overlay plus ghost item on a
     * filtered slot, and - by state, not by pixels - that a click on a hopper slot is swallowed and
     * turned into a ghost item instead of a real item move.
     *
     * <p>The cursor is parked in the top left corner for every screenshot. That matters: the filter
     * button and every slot show a tooltip while hovered, and the button also draws a hover
     * highlight, so a cursor left anywhere near the GUI would put its own pixels into each
     * difference. Parking is not enough on its own - a clicked button stays focused and keeps
     * drawing its highlighted frame - so {@link #clearWidgetFocus} runs before every screenshot.
     *
     * <p>Both filter modes are measured against the same "filter off" baseline rather than against
     * each other. Off shows a full 16x16 barrier item icon while the other two show a single glyph,
     * so those two differences are large. Whitelist against type is only a glyph swap - a few
     * hundred pixels - which is far above the measured noise floor but below what
     * {@code ScreenshotDiff.assertDrew} demands of a world renderer, so that one step uses its own
     * documented threshold.
     *
     * <p>What breaks this test: the filter button disappearing or no longer sending
     * {@code ToggleHopperFilterPayload}, the mode no longer being synced through the container data
     * (the icon would stop changing), {@code extractRenderState} losing the icon or the orange slot
     * overlay, {@code mouseClicked} no longer swallowing the click (the item would land in the
     * hopper slot), or {@code SetHopperGhostItemPayload} not reaching the block entity.
     */
    private static void hopperFilterButtonAndGhostSlots(Script script) {
        TestScene.build(script, "minecraft:stone", "survival");

        script.command("setblock " + CONTAINER_POS.getX() + " " + CONTAINER_POS.getY() + " "
                + CONTAINER_POS.getZ() + " simplebuilding:netherite_hopper");
        // THREE diamonds, and that is the whole point of the number. Both ghost item setters copy
        // the stack and force the count to one; with a single diamond on the cursor the copy and
        // the original agree, so deleting either clamp changes nothing anywhere in this test. With
        // three, a missing clamp shows a stack of three sitting in the filter slot - which is what
        // a player would see, and what the ghost item is explicitly not supposed to be.
        runOnServer(script, "put three diamonds in the player inventory",
                server -> firstPlayer(server).getInventory()
                        .setItem(SAFE_INVENTORY_SLOT, new ItemStack(Items.DIAMOND, GHOST_CURSOR_COUNT)));
        script.awaitPackets();
        script.idle("let the hopper and the diamond reach the client", 15);

        // Slightly upwards so the ray enters the hopper's top plate head on.
        aimAt(script, "0.0", "-4.0", CONTAINER_POS, Direction.NORTH);

        script.harness("right click the hopper", harness -> harness.pressMouse(1));
        awaitScreen(script, NetheriteHopperScreen.class, "the netherite hopper screen");
        script.idle("let the hopper screen settle", 20);

        parkCursor(script);
        clearWidgetFocus(script);
        assertFilterMode(script, HopperFilterMode.NONE);
        assertFilterButtonGeometry(script);

        Later<Path> noFilter = script.shot("hopper-a-filter-none");
        script.idle("let ten ticks pass between the two baseline shots", 10);
        Later<Path> noFilterAgain = script.shot("hopper-b-filter-none-again");

        Later<ScreenshotDiff.Diff> noiseFloor = new Later<>("the noise floor of the hopper screen");

        script.verify("measure the noise floor of the hopper screen", () -> {
            ScreenshotDiff.Diff diff = ScreenshotDiff.compare(
                    "noise floor (hopper screen, filter off, twice)", noFilter.get(), noFilterAgain.get());
            ScreenshotDiff.assertUnchanged(diff);
            noiseFloor.set(diff);
        });

        clickFilterButton(script);
        waitForFilterMode(script, HopperFilterMode.WHITELIST);
        assertFilterGlyph(script, HopperFilterMode.WHITELIST, "✔", 0xFF55FF55);
        Later<Path> whitelist = script.shot("hopper-c-filter-whitelist");

        script.verify("the whitelist icon reached the screen", () -> ScreenshotDiff.assertDrew(
                "NetheriteHopperScreen (whitelist icon)", noiseFloor.get(),
                ScreenshotDiff.compare("filter off vs. whitelist", noFilter.get(), whitelist.get())));

        clickFilterButton(script);
        waitForFilterMode(script, HopperFilterMode.TYPE);
        assertFilterGlyph(script, HopperFilterMode.TYPE, "T", 0xFFFFAA00);
        Later<Path> type = script.shot("hopper-d-filter-type");

        script.verify("the type icon reached the screen", () -> ScreenshotDiff.assertDrew(
                "NetheriteHopperScreen (type icon)", noiseFloor.get(),
                ScreenshotDiff.compare("filter off vs. type", noFilter.get(), type.get())));

        // The two active modes differ in one text glyph and nothing else, so this step gets its own
        // bar rather than assertDrew's: that method's absolute minimum of 400 pixels is calibrated
        // for block outlines in the world and is larger than a whole glyph. One glyph at GUI scale 2
        // occupies a 14x16 block of window pixels, and a check mark against a T changes 128 of them
        // here - so the bar is ten times the measured noise floor and at least 40 pixels, which is
        // a third of the measured signal and, at a noise floor of zero, unreachable by noise. If the
        // button ever drew the same thing for both modes the difference would be 0 and this fails.
        script.verify("the two active filter modes draw different glyphs", () -> {
            ScreenshotDiff.Diff glyphSwap =
                    ScreenshotDiff.compare("whitelist vs. type", whitelist.get(), type.get());
            int requiredForGlyph = Math.max(40, noiseFloor.get().changedPixels() * 10 + 40);

            if (glyphSwap.changedPixels() < requiredForGlyph) {
                throw new AssertionError("The filter button draws the same thing for whitelist and type: "
                        + glyphSwap + ", at least " + requiredForGlyph + " changed pixels were required. "
                        + "Both modes were asserted through getSyncedFilterMode before the screenshots.");
            }
        });

        hopperScreenSwallowsSlotClicks(script);
        clearWidgetFocus(script);

        Later<Path> ghost = script.shot("hopper-e-ghost-slot");

        script.verify("the ghost item overlay reached the screen", () -> ScreenshotDiff.assertDrew(
                "NetheriteHopperScreen (ghost item overlay)", noiseFloor.get(),
                ScreenshotDiff.compare("filtered slot without vs. with a ghost item",
                        type.get(), ghost.get())));

        assertGhostSlotOverlay(script);
        assertTheGhostIconStaysOutOfAnOccupiedSlot(script);

        closeScreen(script);
        script.command("setblock " + CONTAINER_POS.getX() + " " + CONTAINER_POS.getY() + " "
                + CONTAINER_POS.getZ() + " minecraft:air");
        script.command("clear @a");
        script.awaitPackets();
        script.idle("let the removed hopper reach the client", 10);
    }


    /**
     * The screen writes the ghost item into the client block entity itself, without waiting.
     *
     * <p>Read immediately after the click and before any waiting step, which is what makes it a
     * statement at all. The check further down runs fifteen ticks later, and by then the whole
     * detour - packet to the server, ghost item stored there, broadcast back, receiver writes it
     * into the client block entity - has long finished. Deleting the two lines in
     * {@code NetheriteHopperScreen.mouseClicked} that do it locally therefore changes nothing
     * fifteen ticks later, while in the game it costs a visible round trip before the filter icon
     * appears.
     *
     * <p>"Before the server could have answered" is not a guess: the click is handed to the
     * screen inside the same client step that reads the block entity, on the client thread, and
     * the answer needs at least one server tick and one packet in each direction. A click through
     * the window with the check in the next step was not that - see the comment in the method.
     *
     * <p>The count is asserted here too. {@code setGhostItemClient} copies the stack and forces
     * the count to one; the cursor deliberately carries {@link #GHOST_CURSOR_COUNT}, so a missing
     * clamp shows up as a stack of three in the filter slot.
     */
    private static void assertTheGhostItemIsThereBeforeTheServerCouldHaveAnswered(Script script) {
        // The click is handed to the screen here, on the client thread, and the block entity is
        // read in the same call - which is the only place "before the server could have
        // answered" is literally true. A click through the window and a check in the next step
        // sat a tick or more apart, and the server's own SyncHopperGhostItemPayload was back by
        // then: with the local write deleted the check stayed green (mutation round of
        // 2026-09-10, three times over). The event is the one the window would build.
        script.act("click hopper slot 0 and read the client block entity in the same call", client -> {
            NetheriteHopperScreen screen = (NetheriteHopperScreen) client.gui.screen();
            NetheriteHopperScreenHandler menu = screen.getMenu();
            ModHopperBlockEntity blockEntity = menu.getBlockEntity();

            if (blockEntity == null) {
                throw new AssertionError("The client side menu has no block entity, so the screen "
                        + "can never draw a ghost item.");
            }

            Slot slot = menu.slots.get(0);
            double x = leftPos(screen) + slot.x + 8;
            double y = topPos(screen) + slot.y + 8;
            screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false);

            ItemStack ghost = blockEntity.getGhostItem(0);

            if (ghost.isEmpty()) {
                throw new AssertionError("The click was swallowed but the client block entity still "
                        + "has no ghost item in slot 0. The screen is waiting for the server to tell "
                        + "it what it already knows, so the filter icon appears a round trip late.");
            }

            if (!ghost.is(Items.DIAMOND) || ghost.getCount() != 1) {
                throw new AssertionError("The client block entity holds " + ghost + " as ghost item 0 "
                        + "instead of exactly one diamond. The cursor carried " + GHOST_CURSOR_COUNT
                        + ", and a ghost item is a marker, not a stack - the count has to be clamped "
                        + "to one.");
            }
        });
    }

    /**
     * The click intercept, measured by state: pick a diamond up from the player inventory, click
     * hopper slot 0 with it and require that the diamond is still on the cursor, that the hopper
     * slot is still empty and that both the client and the server block entity now hold the ghost
     * item. Then the diamond goes back where it came from, so the screenshot that follows differs
     * from the previous one only in the ghost overlay.
     *
     * <p>Hopper slot 0 is menu index 0 and container index 0; it is the only slot whose treatment
     * is the same before and after the container index defect described in the class javadoc.
     */
    private static void hopperScreenSwallowsSlotClicks(Script script) {
        Later<Integer> diamondSlot = findSlotWithItem(script, Items.DIAMOND);

        clickSlot(script, diamondSlot, "the diamond slot");
        assertCarriedIs(script, Items.DIAMOND, GHOST_CURSOR_COUNT,
                "picking the diamonds up from the player inventory");

        hoverSlot(script, fixed("hopper slot 0", 0), "hopper slot 0");
        assertTheGhostItemIsThereBeforeTheServerCouldHaveAnswered(script);
        script.idle("let the swallowed click reach the block entity", 15);

        assertCarriedIs(script, Items.DIAMOND, GHOST_CURSOR_COUNT,
                "clicking hopper slot 0 with a filter active");

        script.act("the client turned the click into a ghost item", client -> {
            NetheriteHopperScreen screen = (NetheriteHopperScreen) client.gui.screen();
            NetheriteHopperScreenHandler menu = screen.getMenu();

            if (!menu.slots.get(0).getItem().isEmpty()) {
                throw new AssertionError("Hopper slot click intercept: hopper slot 0 now holds "
                        + menu.slots.get(0).getItem()
                        + " - the click was not swallowed and the item was really placed");
            }

            ModHopperBlockEntity blockEntity = menu.getBlockEntity();

            if (blockEntity == null) {
                throw new AssertionError("Hopper slot click intercept: the client side menu has no block "
                        + "entity, so the screen can never draw a ghost item");
            }

            ItemStack ghost = blockEntity.getGhostItem(0);

            if (!ghost.is(Items.DIAMOND) || ghost.getCount() != 1) {
                throw new AssertionError("Hopper slot click intercept: the client block entity holds "
                        + ghost + " as ghost item 0 instead of one diamond");
            }
        });

        Later<String> serverProblem = onServer(script, "the server's view of the ghost item", server -> {
            ServerPlayer player = firstPlayer(server);

            if (!(player.containerMenu instanceof NetheriteHopperScreenHandler menu)) {
                return "the server has " + player.containerMenu + " open instead of the hopper menu";
            }

            ModHopperBlockEntity blockEntity = menu.getBlockEntity();

            if (blockEntity == null) {
                return "the server side menu has no block entity";
            }

            ItemStack ghost = blockEntity.getGhostItem(0);

            if (!ghost.is(Items.DIAMOND) || ghost.getCount() != 1) {
                return "the server block entity holds " + ghost + " as ghost item 0 instead of one "
                        + "diamond - SetHopperGhostItemPayload did not arrive";
            }

            // Empty means "nothing to report". A null answer would be indistinguishable from a
            // value the server never produced, which Later.get() would then blame on the wrong step.
            return "";
        });

        script.verify("the ghost item reached the server", () -> {
            if (!serverProblem.get().isEmpty()) {
                throw new AssertionError("Hopper ghost item did not reach the server: " + serverProblem.get());
            }
        });

        // Put the diamonds back so the ghost overlay is the only difference in the next screenshot.
        clickSlot(script, diamondSlot, "the diamond slot");
        script.idle("let the diamonds land back in the inventory", 10);
        parkCursor(script);
        script.idle("let the parked cursor settle", 10);
    }

    /**
     * Takes the keyboard focus off every widget of the open screen and checks that it is gone.
     *
     * <p>Why this is needed at all: a mouse click on a button leaves that button focused, and
     * {@code AbstractWidget} draws the focused state with the highlighted sprite - a lighter frame
     * around the whole widget. That frame is 36 window pixels wide on each edge of a hopper filter
     * button, so it walked into every "before the click versus after the click" difference and made
     * a measurement labelled "the icon changed" partly a measurement of "a button was clicked". It
     * also survives parking the cursor, which is why the resonance stats control step found the
     * screen 304 pixels away from a baseline whose panel was long gone.
     *
     * <p>Clearing it here rather than living with it keeps every difference in this class down to
     * the one thing its label claims. Focus is vanilla widget state; nothing under test draws it.
     */
    private static void clearWidgetFocus(Script script) {
        script.act("take the focus off every widget", client -> containerScreen(client).setFocused(null));
        script.idle("let the cleared focus reach the next frame", 1);

        script.act("no widget is focused any more", client -> {
            GuiEventListener focused = containerScreen(client).getFocused();

            if (focused != null) {
                throw new AssertionError("The screen still reports " + focused.getClass().getName()
                        + " as focused after setFocused(null); a focused widget draws its highlighted "
                        + "frame and would put its own pixels into the next difference.");
            }
        });
    }


    // ------------------------------------------------------------------------------------------
    // Reading what a screen actually draws
    // ------------------------------------------------------------------------------------------

    /**
     * Runs a screen's own extraction pass into a throwaway render state and hands it over.
     *
     * <p>A screenshot difference can only say "something changed". It cannot say <em>what</em>,
     * and that gap is where several claims about this screen quietly lived: that the filter button
     * sits where it says it does, that the check mark belongs to whitelist and the T to type
     * match, that the overlay on a filtered slot is that particular orange, and that the ghost
     * item is only drawn into an empty slot. Every one of those survives a mutation that a pixel
     * difference still calls "drew something".
     *
     * <p>A CPU-only pass: {@code GuiRenderState} collects the elements and nothing is submitted to
     * the GPU, so this costs a frame's worth of arithmetic and changes nothing on screen.
     */
    private static GuiRenderState extractScreenState(Minecraft client) {
        GuiRenderState state = new GuiRenderState();
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(client, state,
                client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        Screen screen = client.gui.screen();

        if (screen == null) {
            throw new AssertionError("No screen is open, so there is nothing to extract.");
        }

        screen.extractRenderState(graphics, -1, -1, 0.0f);
        return state;
    }

    /** Every filled rectangle of one extraction pass, in the order the screen produced them. */
    private static List<ColoredRectangleRenderState> filledRectangles(GuiRenderState state) {
        List<ColoredRectangleRenderState> rectangles = new ArrayList<>();
        state.forEachElement(element -> {
            if (element instanceof ColoredRectangleRenderState rectangle) {
                rectangles.add(rectangle);
            }
        }, GuiRenderState.TraverseRange.ALL);
        return rectangles;
    }

    /** Every text of one extraction pass as (string, colour, x, y). */
    private static List<DrawnText> drawnTexts(GuiRenderState state) {
        List<DrawnText> texts = new ArrayList<>();
        state.forEachText(text -> texts.add(DrawnText.of(text)));
        return texts;
    }

    /**
     * One text the screen drew, flattened to what a reader would see.
     *
     * <p>Read out of {@link net.minecraft.client.renderer.state.gui.GuiTextRenderState} by
     * reflection: the class keeps its string, colour and position private and offers only
     * {@code bounds()}. Bounds alone cannot tell a check mark from a T, which is exactly the
     * distinction one of the claims here rests on. The reflection is guarded and says what it was
     * after when it fails, so a rename turns into a clear message instead of a silent pass.
     */
    private record DrawnText(String text, int color, int x, int y) {

        static DrawnText of(Object state) {
            return new DrawnText(readSequence(state), readInt(state, "color"),
                    readInt(state, "x"), readInt(state, "y"));
        }

        private static String readSequence(Object state) {
            Object raw = read(state, "text");

            if (!(raw instanceof FormattedCharSequence sequence)) {
                throw new AssertionError("GuiTextRenderState.text is no longer a FormattedCharSequence "
                        + "but " + raw + "; the filter glyph cannot be read.");
            }

            StringBuilder text = new StringBuilder();
            sequence.accept((index, style, codePoint) -> {
                text.appendCodePoint(codePoint);
                return true;
            });
            return text.toString();
        }

        private static int readInt(Object state, String name) {
            Object value = read(state, name);

            if (!(value instanceof Integer number)) {
                throw new AssertionError("GuiTextRenderState." + name + " is no longer an int but "
                        + value + ".");
            }

            return number;
        }

        private static Object read(Object state, String name) {
            try {
                Field field = state.getClass().getDeclaredField(name);
                field.setAccessible(true);
                return field.get(state);
            } catch (ReflectiveOperationException | RuntimeException e) {
                throw new AssertionError("Cannot read GuiTextRenderState." + name + " - the drawn text "
                        + "of a screen is unreadable, so the filter glyph cannot be told apart from "
                        + "any other glyph.", e);
            }
        }
    }


    // ------------------------------------------------------------------------------------------
    // What the hopper screen draws, pinned by value rather than by "something changed"
    // ------------------------------------------------------------------------------------------

    /**
     * The filter button is 18x18 and sits right of the five slots, one row down.
     *
     * <p>Nothing above this pins it. {@link #clickFilterButton} finds "the only button on the
     * screen" and clicks the centre the button reports itself, so a button moved off the screen
     * edge or blown up to 30x30 is still found, still clicked, still toggles the mode - and the
     * screenshots still differ, because the icon moved with it. The numbers here are the ones
     * {@code NetheriteHopperScreen.init} computes, written out a second time so that changing one
     * of them is a decision and not an accident.
     *
     * <p>Relative to the screen origin rather than absolute, because that depends on the window.
     */
    private static void assertFilterButtonGeometry(Script script) {
        script.act("the filter button is 18x18 right of the five slots", client -> {
            AbstractContainerScreen<?> screen = containerScreen(client);
            Button button = onlyButton(screen);

            int expectedX = leftPos(screen) + 44 + 5 * 18 + 4;
            int expectedY = topPos(screen) + 19;

            if (button.getX() != expectedX || button.getY() != expectedY
                    || button.getWidth() != 18 || button.getHeight() != 18) {
                throw new AssertionError("The hopper filter button is " + button.getWidth() + "x"
                        + button.getHeight() + " at " + button.getX() + "/" + button.getY()
                        + ", expected 18x18 at " + expectedX + "/" + expectedY
                        + " (right of the five hopper slots, one row below the top). A button that "
                        + "has moved or changed size is still found and still clicked by this test, "
                        + "so nothing else here would notice.");
            }
        });
    }

    /**
     * The active filter modes draw their own glyph in their own colour.
     *
     * <p>The screenshot pair proves that whitelist and type look <em>different</em>. It cannot
     * prove <em>which</em> is which: swapping the check mark with the T, or the green with the
     * orange, leaves both differences exactly as large. This reads the text the screen actually
     * submitted and compares it with what a player is told to expect.
     *
     * <p>Restricted to the text inside the button, because the screen also draws the word
     * "Filter:" beside it and the inventory label below.
     */
    private static void assertFilterGlyph(Script script, HopperFilterMode mode,
                                          String expectedGlyph, int expectedColor) {
        script.act("the " + mode + " filter button draws its own glyph", client -> {
            Button button = onlyButton(containerScreen(client));
            List<DrawnText> inside = new ArrayList<>();

            for (DrawnText text : drawnTexts(extractScreenState(client))) {
                if (text.x() >= button.getX() && text.x() < button.getX() + button.getWidth()
                        && text.y() >= button.getY() && text.y() < button.getY() + button.getHeight()) {
                    inside.add(text);
                }
            }

            if (inside.size() != 1) {
                throw new AssertionError("The " + mode + " filter button draws " + inside.size()
                        + " texts instead of exactly one: " + inside + ". Everything drawn inside the "
                        + "button is the mode icon, so more than one means the icon cannot be named.");
            }

            DrawnText glyph = inside.get(0);

            if (!glyph.text().equals(expectedGlyph) || glyph.color() != expectedColor) {
                throw new AssertionError("The " + mode + " filter button draws [" + glyph.text()
                        + "] in " + String.format("0x%08X", glyph.color()) + ", expected ["
                        + expectedGlyph + "] in " + String.format("0x%08X", expectedColor)
                        + ". Two modes that swapped their glyphs or their colours produce screenshot "
                        + "differences of exactly the same size, so only this can tell them apart.");
            }
        });
    }

    /**
     * A filtered slot gets the orange overlay, over the whole slot and in that colour.
     *
     * <p>{@code assertDrew} measures "with a ghost item versus without", so ANY visible colour
     * passes it - magenta, or an opaque black that hides the icon the overlay is supposed to sit
     * behind. The colour and the rectangle are pinned here, at the slot the ghost item was just
     * written to.
     */
    private static void assertGhostSlotOverlay(Script script) {
        script.act("the filtered slot carries the orange overlay", client -> {
            AbstractContainerScreen<?> screen = containerScreen(client);
            Slot slot = screen.getMenu().slots.get(0);
            int slotX = leftPos(screen) + slot.x;
            int slotY = topPos(screen) + slot.y;

            List<String> near = new ArrayList<>();

            for (ColoredRectangleRenderState rectangle : filledRectangles(extractScreenState(client))) {
                // The corners come back the other way round. GuiGraphicsExtractor.fill swaps them
                // so that x0 holds the LARGER value, which is the opposite of what it is called
                // with - so the comparison is between the two spans, not between named corners.
                int left = Math.min(rectangle.x0(), rectangle.x1());
                int top = Math.min(rectangle.y0(), rectangle.y1());
                int right = Math.max(rectangle.x0(), rectangle.x1());
                int bottom = Math.max(rectangle.y0(), rectangle.y1());

                if (left != slotX || top != slotY) {
                    if (Math.abs(left - slotX) <= 20 && Math.abs(top - slotY) <= 20) {
                        near.add(String.format("%d/%d..%d/%d in 0x%08X", left, top, right, bottom,
                                rectangle.col1()));
                    }
                    continue;
                }

                near.add(String.format("%d/%d..%d/%d in 0x%08X", left, top, right, bottom,
                        rectangle.col1()));

                if (right == slotX + 16 && bottom == slotY + 16
                        && rectangle.col1() == GHOST_SLOT_OVERLAY
                        && rectangle.col2() == GHOST_SLOT_OVERLAY) {
                    return;
                }
            }

            throw new AssertionError("The filtered hopper slot has no 16x16 overlay in "
                    + String.format("0x%08X", GHOST_SLOT_OVERLAY) + " at " + slotX + "/" + slotY
                    + ". Rectangles drawn near that corner: " + near + ". The screenshot check "
                    + "only asks whether anything changed there, so any other colour - including "
                    + "one opaque enough to hide the ghost icon - passes it.");
        });
    }

    /**
     * The ghost icon stays out of a slot that already holds something.
     *
     * <p>The measurement above runs with hopper slot 0 empty from beginning to end, so removing
     * the "slot is empty" guard produces a bit-identical screenshot. Here the slot is filled
     * first: the overlay has to stay - it marks the slot as filtered either way - while the ghost
     * icon has to disappear, because in the game it would be painted over the real item and make
     * the slot unreadable.
     *
     * <p>Counted by item render states rather than by pixels: the icon and a real item are both
     * items, and at the same position, so a picture cannot separate them.
     */
    private static void assertTheGhostIconStaysOutOfAnOccupiedSlot(Script script) {
        Later<Integer> withEmptySlot = new Later<>("the items drawn in the empty filtered slot");

        script.act("count what is drawn in the filtered slot while it is empty",
                client -> withEmptySlot.set(itemsDrawnInHopperSlotZero(client)));

        script.verify("setup: the empty filtered slot draws exactly one item, the ghost icon", () -> {
            if (withEmptySlot.get() != 1) {
                throw new AssertionError("Setup failed: the empty filtered slot draws "
                        + withEmptySlot.get() + " items instead of the one ghost icon, so the check "
                        + "below could not tell whether the icon disappeared.");
            }
        });

        runOnServer(script, "put a stone block into hopper slot 0", server -> {
            BlockEntity entity = firstPlayer(server).level().getBlockEntity(CONTAINER_POS);

            if (!(entity instanceof Container container)) {
                throw new AssertionError("Setup failed: " + CONTAINER_POS + " holds " + entity
                        + " instead of the netherite hopper.");
            }

            container.setItem(0, new ItemStack(Items.STONE, 1));
        });

        script.awaitPackets();
        script.await("the filled hopper slot reaches the client", 120,
                client -> !containerScreen(client).getMenu().slots.get(0).getItem().isEmpty(),
                client -> "Hopper slot 0 still reads as empty on the client, so the check below "
                        + "would measure the empty case again.");
        script.idle("let the filled slot settle", 10);

        script.act("the ghost icon is gone while the slot holds an item", client -> {
            int drawn = itemsDrawnInHopperSlotZero(client);

            // Exactly one: the stone block vanilla draws. Two would be the stone block AND the
            // ghost icon on top of it; zero would mean the slot is not being read at all.
            if (drawn != 1) {
                throw new AssertionError("Hopper slot 0 holds a stone block and the screen draws "
                        + drawn + " items at that position instead of one (the block itself). "
                        + (drawn > 1
                                ? "The ghost icon is being painted over a real item, which makes the "
                                        + "slot content unreadable. "
                                : "Not even the real item is found there, so this reader is broken. ")
                        + "This is invisible to every screenshot in this test, because all of them "
                        + "are taken while the slot is empty.");
            }
        });

        script.act("the overlay is still there, so the slot is still marked as filtered", client -> {
            AbstractContainerScreen<?> screen = containerScreen(client);
            Slot slot = screen.getMenu().slots.get(0);
            int slotX = leftPos(screen) + slot.x;
            int slotY = topPos(screen) + slot.y;

            for (ColoredRectangleRenderState rectangle : filledRectangles(extractScreenState(client))) {
                // Same swapped corners as above.
                if (Math.min(rectangle.x0(), rectangle.x1()) == slotX
                        && Math.min(rectangle.y0(), rectangle.y1()) == slotY
                        && rectangle.col1() == GHOST_SLOT_OVERLAY) {
                    return;
                }
            }

            throw new AssertionError("A filled slot with a filter lost its orange overlay as well. "
                    + "Only the icon may go; the mark that the slot is filtered has to stay.");
        });

        runOnServer(script, "empty hopper slot 0 again", server -> {
            if (firstPlayer(server).level().getBlockEntity(CONTAINER_POS) instanceof Container container) {
                container.setItem(0, ItemStack.EMPTY);
            }
        });
        script.awaitPackets();
        script.idle("let the emptied slot reach the client", 10);
    }

    /**
     * How many item icons the screen draws at hopper slot 0 - the ghost, the real item, or both.
     *
     * <p>Through the pose, not the raw coordinates, and the first run is the reason: the real
     * item in a slot is drawn by {@code AbstractContainerScreen} at the slot's OWN x/y under a
     * pose translated to the screen origin, while the mod draws the ghost after that translation
     * is gone, at absolute coordinates. Comparing raw x/y therefore found the ghost and never the
     * real item - and reported "0 items" for a slot that visibly held a stone block. Both land on
     * the same screen pixel once the pose is applied, which is the only comparison that means
     * "drawn in that slot".
     */
    private static int itemsDrawnInHopperSlotZero(Minecraft client) {
        AbstractContainerScreen<?> screen = containerScreen(client);
        Slot slot = screen.getMenu().slots.get(0);
        int slotX = leftPos(screen) + slot.x;
        int slotY = topPos(screen) + slot.y;
        int[] count = {0};

        extractScreenState(client).forEachItem(item -> {
            org.joml.Vector2f onScreen = item.pose().transformPosition(item.x(), item.y(), new org.joml.Vector2f());

            if (Math.round(onScreen.x) == slotX && Math.round(onScreen.y) == slotY) {
                count[0]++;
            }
        });

        return count[0];
    }

    /** The screen's only button, with the assertion that there is exactly one. */
    private static Button onlyButton(AbstractContainerScreen<?> screen) {
        Button found = null;

        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button button) {
                if (found != null) {
                    throw new AssertionError("The netherite hopper screen has more than one button; "
                            + "the test would not know which one is the filter toggle.");
                }

                found = button;
            }
        }

        if (found == null) {
            throw new AssertionError("The netherite hopper screen has no button; the filter toggle "
                    + "is gone.");
        }

        return found;
    }

    /**
     * Clicks the hopper screen's single button and leaves the screen ready for a screenshot.
     *
     * <p>The button is identified as "the only one on the screen", which is why the step asserts
     * that there is exactly one: the mod adds the filter toggle and vanilla's hopper screen adds
     * nothing, so two buttons would mean the test is clicking something it cannot name.
     */
    private static void clickFilterButton(Script script) {
        moveCursorToGui(script, "the filter button", client -> {
            Button found = onlyButton(containerScreen(client));
            return new int[] {found.getX() + found.getWidth() / 2, found.getY() + found.getHeight() / 2};
        });

        script.harness("click the filter button", harness -> harness.pressMouse(0));
        script.idle("let the click reach the button", 5);
        parkCursor(script);
        clearWidgetFocus(script);
        script.idle("let the new filter icon settle", 10);
    }

    /** Waits for the synced filter mode, then settles and asserts it again before any screenshot. */
    private static void waitForFilterMode(Script script, HopperFilterMode expected) {
        script.await("the hopper filter reaches " + expected, 120,
                client -> currentFilterMode(client) == expected,
                client -> "The hopper filter never reached " + expected + "; it is still "
                        + currentFilterMode(client) + ". The toggle packet or the container data sync "
                        + "is broken.");

        script.idle("let the new filter mode settle", 10);
        assertFilterMode(script, expected);
    }

    private static void assertFilterMode(Script script, HopperFilterMode expected) {
        script.act("the hopper screen reports filter mode " + expected, client -> {
            HopperFilterMode actual = currentFilterMode(client);

            if (actual != expected) {
                throw new AssertionError("The hopper screen reports filter mode " + actual + " instead of "
                        + expected + "; the screenshot below would not show what the test claims.");
            }
        });
    }

    private static HopperFilterMode currentFilterMode(Minecraft client) {
        return ((NetheriteHopperScreen) client.gui.screen()).getMenu().getSyncedFilterMode();
    }

    private static void assertCarriedIs(Script script, Item expected, int count, String step) {
        script.act("the cursor carries " + count + " " + expected + " after " + step, client -> {
            ItemStack stack = containerScreen(client).getMenu().getCarried();

            if (!stack.is(expected) || stack.getCount() != count) {
                throw new AssertionError("After " + step + " the cursor carries " + stack
                        + " instead of " + count + " " + expected + ".");
            }
        });
    }

    private static Later<Integer> findSlotWithItem(Script script, Item item) {
        Later<Integer> index = new Later<>("the menu slot holding " + item);

        script.act("find the slot holding " + item, client -> {
            AbstractContainerScreen<?> screen = containerScreen(client);

            for (int i = 0; i < screen.getMenu().slots.size(); i++) {
                if (screen.getMenu().slots.get(i).getItem().is(item)) {
                    index.set(i);
                    return;
                }
            }

            throw new AssertionError("No slot in the open screen holds " + item + ".");
        });

        return index;
    }

    // ------------------------------------------------------------------------------------------
    // InventoryScreenMixin - the armor trim stats button
    // ------------------------------------------------------------------------------------------

    /**
     * Proves that {@code InventoryScreenMixin} adds its 20x20 button 24 pixels to the left of the
     * inventory background and that clicking it really toggles the resonance stats panel on and off.
     *
     * <p>The panel is measured in pixels because its numbers only exist as render state; the
     * mixin's {@code isStatsVisible} flag is unique and private, so the picture is the only
     * observable. That is why the control step matters here more than anywhere else: the panel has
     * to disappear again on the second click, which rules out any drift as the explanation.
     *
     * <p>The cursor is moved onto the button for the click and straight back to the parking spot
     * afterwards. {@code InventoryScreen} renders the player model looking at the mouse, so a
     * cursor left in a different place between two screenshots would turn the model's head and add
     * pixels of its own.
     *
     * <p><b>Why the player model is painted out.</b> A parked cursor is not enough: this screen is
     * the one place in the mod's UI that is animated on purpose. {@code HumanoidModel.setupAnim}
     * adds {@code cos(ageInTicks * 0.09) * 0.05} to the arms' {@code zRot} and
     * {@code sin(ageInTicks * 0.067) * 0.05} to their {@code xRot}, so the arms sway forever, with
     * periods of about 70 and 94 ticks. Measured here that was 123 changed pixels between two
     * screenshots taken while nothing happened - twice what {@code assertUnchanged} allows and,
     * worse, a random sample of the sway rather than a constant, so every threshold derived from it
     * would wander from run to run. All four screenshots are therefore compared with the model's
     * viewport painted over in both images, so the sway cannot enter any measurement. Everything
     * outside that one rectangle is still compared in full, the panel included, by the shared
     * {@link ScreenshotDiff}. The excluded rectangle is {@code InventoryScreen}'s own entity
     * viewport, and {@link #playerModelBox} refuses to hand it out if it touches the rectangle the
     * mixin draws the panel into.
     *
     * <p>Survival is required: {@code InventoryScreen.init} hands over to
     * {@code CreativeModeInventoryScreen} as soon as the player has infinite materials, and the
     * mixin only sits on the survival screen. This case builds its scene in survival itself.
     *
     * <p>What breaks this test: the mixin no longer injecting into {@code init} (no button at all),
     * the button's position or size changing, the click handler no longer flipping the flag, or
     * {@code renderTrimStats} no longer drawing the panel.
     */
    private static void inventoryTrimStatsButtonToggles(Script script) {
        TestScene.build(script, "minecraft:stone", "survival");

        openInventoryScreen(script);
        parkCursor(script);
        script.idle("let the parked cursor settle", 10);
        clearWidgetFocus(script);

        Later<int[]> button = assertTrimButtonGeometry(script);
        assertTrimButtonIconAndTooltip(script, button);
        Later<int[]> modelBox = playerModelBox(script);

        // The two baseline shots are twenty ticks apart, the same settling time clickAndPark gives
        // the screen after a click, so the noise floor covers as much screen time as every signal
        // measured against it.
        Later<Path> hidden = script.shot("inventory-a-stats-hidden");
        script.idle("let twenty ticks pass between the two baseline shots", 20);
        Later<Path> hiddenAgain = script.shot("inventory-b-stats-hidden-again");

        Later<Path> hiddenBase = new Later<>("the masked baseline of the inventory screen");
        Later<ScreenshotDiff.Diff> noiseFloor = new Later<>("the noise floor of the inventory screen");

        script.verify("measure the noise floor of the inventory screen", () -> {
            hiddenBase.set(withoutPlayerModel(hidden.get(), modelBox.get()));

            ScreenshotDiff.Diff diff = ScreenshotDiff.compare(
                    "noise floor (inventory screen, stats hidden, twice)",
                    hiddenBase.get(), withoutPlayerModel(hiddenAgain.get(), modelBox.get()));
            ScreenshotDiff.assertUnchanged(diff);
            noiseFloor.set(diff);
        });

        clickAndPark(script, button);
        Later<Path> shown = script.shot("inventory-c-stats-shown");

        script.verify("the resonance stats panel reached the screen", () -> ScreenshotDiff.assertDrew(
                "InventoryScreenMixin (resonance stats panel)", noiseFloor.get(),
                ScreenshotDiff.compare("resonance stats panel", hiddenBase.get(),
                        withoutPlayerModel(shown.get(), modelBox.get()))));

        clickAndPark(script, button);
        Later<Path> hiddenControl = script.shot("inventory-d-stats-hidden-control");

        script.verify("clicking again restores the baseline picture", () -> {
            ScreenshotDiff.Diff residual = ScreenshotDiff.compare("control (stats toggled off again)",
                    hiddenBase.get(), withoutPlayerModel(hiddenControl.get(), modelBox.get()));
            ScreenshotDiff.assertBackToBaseline("clicking the resonance stats button a second time",
                    noiseFloor.get(), residual);
        });

        closeScreen(script);
    }

    /**
     * The window pixel rectangle the inventory screen animates its player model in, padded by two
     * pixels. The GUI coordinates are {@code InventoryScreen.extractBackground}'s own arguments to
     * {@code extractEntityInInventoryFollowsMouse}: {@code leftPos + 26 / topPos + 8} to
     * {@code leftPos + 75 / topPos + 78}.
     *
     * <p>Before handing the rectangle out it is checked against the one the mixin draws its panel
     * into - {@code leftPos - 114 / topPos + 10}, 84 by 64 GUI pixels, straight out of
     * {@code InventoryScreenMixin.renderTrimStats}. If the two ever overlap the mask would swallow
     * part of the signal and the pixel test would go quietly green on a panel that is not there,
     * which is the one failure mode a masked comparison can hide. Either side moving is enough to
     * fail it: the mixin's panel offsets, or the entity viewport this method mirrors.
     */
    private static Later<int[]> playerModelBox(Script script) {
        Later<int[]> box = new Later<>("the window pixel box of the animated player model");

        script.act("work out the player model mask and prove it misses the panel", client -> {
            AbstractContainerScreen<?> screen = containerScreen(client);
            double scaleX = client.getWindow().getScreenWidth() / (double) client.getWindow().getGuiScaledWidth();
            double scaleY = client.getWindow().getScreenHeight() / (double) client.getWindow().getGuiScaledHeight();

            int[] model = {
                    (int) Math.floor((leftPos(screen) + 26) * scaleX) - 2,
                    (int) Math.floor((topPos(screen) + 8) * scaleY) - 2,
                    (int) Math.ceil((leftPos(screen) + 75) * scaleX) + 2,
                    (int) Math.ceil((topPos(screen) + 78) * scaleY) + 2,
            };

            int[] panel = {
                    (int) Math.floor((leftPos(screen) - 84 - 30) * scaleX),
                    (int) Math.floor((topPos(screen) + 10) * scaleY),
                    (int) Math.ceil((leftPos(screen) - 30) * scaleX),
                    (int) Math.ceil((topPos(screen) + 10 + 64) * scaleY),
            };

            boolean overlaps = model[0] < panel[2] && panel[0] < model[2]
                    && model[1] < panel[3] && panel[1] < model[3];

            if (overlaps) {
                throw new AssertionError("The player model mask " + describeBox(model) + " overlaps the "
                        + "resonance stats panel " + describeBox(panel) + ". Masking it away would hide "
                        + "part of the signal and the comparison below could pass without a panel.");
            }

            box.set(model);
        });

        return box;
    }

    private static String describeBox(int[] box) {
        return box[0] + "/" + box[1] + " to " + box[2] + "/" + box[3];
    }

    /**
     * Writes a copy of a screenshot with one rectangle painted over in a flat colour and returns
     * the copy. Both sides of every comparison go through this with the same rectangle, so the
     * masked area contributes exactly zero changed pixels while everything outside it is compared
     * by the shared {@link ScreenshotDiff} as usual - tolerance and thresholds included, nothing is
     * reimplemented here.
     *
     * <p>The copies go to a temporary file rather than next to the screenshots on purpose: the test
     * runner globs the screenshot directory and reports every file there that no source promised.
     * For the same reason the name prefix carries no hyphen - the runner reads hyphenated lowercase
     * literals as promised screenshot names.
     */
    private static Path withoutPlayerModel(Path screenshot, int[] box) {
        try {
            BufferedImage image = ImageIO.read(screenshot.toFile());

            if (image == null) {
                throw new AssertionError("Could not decode screenshot " + screenshot);
            }

            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.BLACK);
            graphics.fillRect(box[0], box[1], box[2] - box[0], box[3] - box[1]);
            graphics.dispose();

            Path masked = Files.createTempFile("sbmask", ".png");
            masked.toFile().deleteOnExit();
            ImageIO.write(image, "png", masked.toFile());
            return masked;
        } catch (IOException e) {
            throw new AssertionError("Could not mask the player model out of " + screenshot, e);
        }
    }

    /**
     * Finds the mod's button among the screen's children and checks the geometry the mixin promises.
     * Publishes the GUI coordinates of its centre.
     *
     * <p>The only other button on the survival inventory screen is vanilla's recipe book toggle,
     * which is 20x18 - so "the single 20x20 button" identifies the mod's without any guessing.
     */
    private static Later<int[]> assertTrimButtonGeometry(Script script) {
        Later<int[]> centre = new Later<>("the centre of the armor trim stats button");

        script.act("the armor trim stats button exists where the mixin puts it", client -> {
            AbstractContainerScreen<?> screen = containerScreen(client);
            Button found = null;
            int candidates = 0;

            for (GuiEventListener child : screen.children()) {
                if (child instanceof Button button && button.getWidth() == 20 && button.getHeight() == 20) {
                    found = button;
                    candidates++;
                }
            }

            if (candidates == 0) {
                throw new AssertionError("Armor trim stats button: the inventory screen has no 20x20 "
                        + "button - InventoryScreenMixin.init did not run");
            }

            if (candidates > 1) {
                throw new AssertionError("Armor trim stats button: the inventory screen has " + candidates
                        + " buttons of 20x20; the test cannot tell which one belongs to the mod");
            }

            int expectedX = leftPos(screen) - 24;
            int expectedY = topPos(screen) + 10;

            if (found.getX() != expectedX || found.getY() != expectedY) {
                throw new AssertionError("Armor trim stats button: the trim button sits at " + found.getX()
                        + "/" + found.getY() + " instead of " + expectedX + "/" + expectedY
                        + " (24 left of the inventory, 10 below its top edge)");
            }

            centre.set(new int[] {found.getX() + 10, found.getY() + 10});
        });

        return centre;
    }


    /**
     * The button carries the ward smithing template as its icon and says "Toggle Resonance
     * Stats" when hovered.
     *
     * <p>Both were listed as not covered above, for two reasons that have since gone: the tooltip
     * sits in a private holder without a getter, and the icon was drawn "without any identity that
     * survives extraction". The render state is read now, and both DO survive it - the tooltip
     * because the widget hands it to the deferred pass of the same extraction, the icon because
     * an item's render state still knows its particle sprite, which for an item model is the
     * item's own texture. No reference image is needed for either.
     *
     * <p>The tooltip is read with the real cursor on the button: the holder only shows the
     * tooltip once the mouse has been there for longer than its delay, measured from the first
     * frame that saw the hover, and the frames between the steps are real frames.
     */
    private static void assertTrimButtonIconAndTooltip(Script script, Later<int[]> button) {
        script.act("the trim button draws the ward smithing template as its icon", client -> {
            int iconX = button.get()[0] - 10 + 2;
            int iconY = button.get()[1] - 10 + 2;
            List<String> sprites = new ArrayList<>();
            List<String> elsewhere = new ArrayList<>();

            extractScreenState(client).forEachItem(item -> {
                org.joml.Vector2f onScreen = item.pose().transformPosition(item.x(), item.y(), new org.joml.Vector2f());
                String sprite = String.valueOf(item.itemStackRenderState()
                        .pickParticleMaterial(RandomSource.create()).sprite().contents().name());

                if (Math.round(onScreen.x) == iconX && Math.round(onScreen.y) == iconY) {
                    sprites.add(sprite);
                } else {
                    elsewhere.add(sprite + "@" + Math.round(onScreen.x) + "/" + Math.round(onScreen.y));
                }
            });

            if (!sprites.contains("minecraft:item/ward_armor_trim_smithing_template")) {
                throw new AssertionError("The trim button at " + (iconX - 2) + "/" + (iconY - 2)
                        + " does not carry the ward smithing template: items drawn two pixels inside it "
                        + sprites + ", items drawn elsewhere on the screen " + elsewhere);
            }
        });

        // The real cursor, not only the extraction's mouse argument: every real frame in between
        // would otherwise see the widget unhovered and reset the holder's display timer, and the
        // second pass would find a tooltip that had "just" started showing - which is what the
        // first run reported, a screen that drew nothing but "Crafting".
        moveCursorToGui(script, "the armor trim stats button", client -> button.get());
        script.idle("let the tooltip delay pass with the cursor on the button", 10);

        script.act("the trim button's tooltip reads Toggle Resonance Stats", client -> {
            List<String> texts = new ArrayList<>();

            for (DrawnText text : drawnTexts(extractScreenStateWithTooltip(client, button.get()[0], button.get()[1]))) {
                texts.add(text.text());
            }

            if (!texts.contains("Toggle Resonance Stats")
                    || !texts.contains("Click to show/hide trim multipliers.")) {
                throw new AssertionError("Hovering the trim button did not put its tooltip into the render "
                        + "state: expected the lines 'Toggle Resonance Stats' and 'Click to show/hide trim "
                        + "multipliers.', the screen drew " + texts);
            }
        });

        parkCursor(script);
        clearWidgetFocus(script);
        script.idle("let the parked cursor settle before the baseline shots", 10);
    }

    /**
     * {@link #extractScreenState} with the mouse at a GUI position and the deferred elements -
     * the tooltips - included, which is what the real frame does after the widgets.
     */
    private static GuiRenderState extractScreenStateWithTooltip(Minecraft client, int mouseX, int mouseY) {
        GuiRenderState state = new GuiRenderState();
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(client, state,
                client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        Screen screen = client.gui.screen();

        if (screen == null) {
            throw new AssertionError("No screen is open, so there is nothing to extract.");
        }

        screen.extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, 0.0f);
        return state;
    }

    /** Clicks a GUI position, parks the cursor again and lets the screen settle for the next shot. */
    private static void clickAndPark(Script script, Later<int[]> guiPoint) {
        moveCursorToGui(script, "the armor trim stats button", client -> guiPoint.get());
        script.harness("click the armor trim stats button", harness -> harness.pressMouse(0));
        script.idle("let the click reach the button", 5);
        parkCursor(script);
        clearWidgetFocus(script);
        script.idle("let the toggled panel settle", 20);
    }

    // ------------------------------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------------------------------

    /**
     * Aims the player at a block from the scene's fixed standing position.
     *
     * <p>The assertion afterwards is the same one {@link TestScene#build} ends with, and it is what
     * keeps a right click from being blamed on a screen that never opened: a teleport that has not
     * arrived on the client yet looks exactly like a container that refuses to open.
     */
    private static void aimAt(Script script, String yaw, String pitch, BlockPos expected,
                              Direction expectedFace) {
        script.command("tp @a 10.5 0.0 16.5 " + yaw + " " + pitch);
        script.awaitPackets();
        script.idle("let the new view angles reach the client", 10);
        TestScene.assertAimedAt(script, expected, expectedFace);
    }

    /**
     * Presses the inventory key and waits for the survival inventory.
     *
     * <p>The key binding control runs first: the harness presses a raw GLFW code, so a rebound
     * inventory key would leave the screen closed and every step after it would fail on a missing
     * container screen rather than on the key.
     */
    private static void openInventoryScreen(Script script) {
        assertInventoryKeyIsBound(script);
        script.harness("press the inventory key", harness -> harness.pressKey(INVENTORY_KEY));
        awaitScreen(script, InventoryScreen.class, "the survival inventory screen");
        script.idle("let the inventory screen settle", 20);
    }

    /**
     * The inventory binding really is the key {@link #INVENTORY_KEY} presses.
     *
     * <p>New in the shared form and not optional, for the same reason the sneak key check is in
     * {@link MultiBlockBreakingClientTest}: Fabric's own input took the binding and resolved it,
     * the shared harness takes a code.
     */
    private static void assertInventoryKeyIsBound(Script script) {
        script.act("the inventory binding sits on the key the harness presses", client -> {
            if (!client.options.keyInventory.matches(InputConstants.Type.KEYSYM.getOrCreate(INVENTORY_KEY))) {
                throw new AssertionError("The inventory binding is not on GLFW key " + INVENTORY_KEY
                        + " any more (it says \"" + client.options.keyInventory.saveString() + "\"), so "
                        + "pressing that key would open nothing and every screen measurement below would "
                        + "fail for a reason that has nothing to do with the mod.");
            }
        });
    }

    /** Waits until the expected screen is open, and says what is open instead if it never is. */
    private static void awaitScreen(Script script, Class<? extends Screen> expected, String what) {
        script.await("wait for " + what, 120,
                client -> expected.isInstance(client.gui.screen()),
                client -> what + " never opened; the screen is "
                        + (client.gui.screen() == null ? "none" : client.gui.screen().getClass().getName())
                        + ". " + TestScene.describeAim(client));
    }

    private static void closeScreen(Script script) {
        // Through the player, not through setScreen(null): this is the path that also tells the
        // server the container is closed, so the next screen does not open on top of a stale menu.
        script.act("close the open screen", client -> client.player.closeContainer());
        script.await("wait for the screen to be gone", 120, client -> client.gui.screen() == null,
                client -> "the screen "
                        + (client.gui.screen() == null ? "none" : client.gui.screen().getClass().getName())
                        + " is still open after closeContainer()");
        script.idle("let the closed container settle", 10);
    }

    private static void clickSlot(Script script, Later<Integer> slotIndex, String what) {
        hoverSlot(script, slotIndex, what);
        script.harness("click " + what, harness -> harness.pressMouse(0));
        script.idle("let the click on " + what + " be handled", 10);
    }

    /**
     * Puts the mouse on the centre of a menu slot and waits until the screen has actually taken the
     * hover over. {@code hoveredSlot} is only updated while the screen extracts its render state,
     * so a slot is not hovered before the next frame - and everything the tests do afterwards
     * (wheel, click) reads exactly that field.
     */
    private static void hoverSlot(Script script, Later<Integer> slotIndex, String what) {
        moveCursorToGui(script, what, client -> {
            AbstractContainerScreen<?> screen = containerScreen(client);
            Slot slot = screen.getMenu().slots.get(slotIndex.get());
            return new int[] {leftPos(screen) + slot.x + 8, topPos(screen) + slot.y + 8};
        });

        script.await("the screen reports " + what + " as hovered", 60, client -> {
            AbstractContainerScreen<?> screen = containerScreen(client);
            return hoveredSlot(screen) == screen.getMenu().slots.get(slotIndex.get());
        }, client -> {
            Slot hovered = hoveredSlot(containerScreen(client));
            return "The mouse never landed on menu slot " + slotIndex.get() + " (" + what + "); the screen "
                    + "reports " + (hovered == null ? "no slot" : "menu slot " + hovered.index)
                    + " as hovered.";
        });
    }

    /** Moves the cursor back to the corner where it can neither hover a slot nor a widget. */
    private static void parkCursor(Script script) {
        moveCursorToGui(script, "the parking spot", client -> new int[] {PARK_X, PARK_Y});
    }

    /**
     * A position on the screen, worked out on the client thread when the step runs.
     *
     * <p>Its own type rather than a plain value, because everything worth pointing at here -
     * a slot, a button, the GUI origin - only exists once the screen is open, which is later than
     * the moment the steps are registered.
     */
    @FunctionalInterface
    private interface GuiPoint {
        int[] at(Minecraft client);
    }

    /**
     * Moves the cursor to a position in GUI coordinates.
     *
     * <p>Two steps, and they cannot be one: the position has to be computed on the client thread,
     * while the harness call has to happen outside a client task (Fabric throws otherwise). The
     * conversion belongs to the first half - the input API takes raw window pixels, while
     * everything a screen sees is divided by the GUI scale, so at GUI scale 2 a slot centre of 60
     * is window pixel 120.
     */
    private static void moveCursorToGui(Script script, String what, GuiPoint point) {
        Later<double[]> raw = new Later<>("the window pixel position of " + what);

        script.act("work out where " + what + " is", client -> {
            int[] gui = point.at(client);
            raw.set(new double[] {
                    gui[0] * client.getWindow().getScreenWidth()
                            / (double) client.getWindow().getGuiScaledWidth(),
                    gui[1] * client.getWindow().getScreenHeight()
                            / (double) client.getWindow().getGuiScaledHeight()});
        });

        script.harness("move the cursor to " + what, harness -> {
            double[] position = raw.get();
            harness.setCursorPos(position[0], position[1]);
        });

        script.idle("let the cursor move take effect", 5);
    }

    /** A value that is already known while the steps are being registered. */
    private static Later<Integer> fixed(String what, int value) {
        Later<Integer> later = new Later<>(what);
        later.set(value);
        return later;
    }

    private static AbstractContainerScreen<?> containerScreen(Minecraft client) {
        Screen screen = client.gui.screen();

        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            throw new AssertionError("Expected a container screen to be open but found "
                    + (screen == null ? "none" : screen.getClass().getName()));
        }

        return containerScreen;
    }

    private static int leftPos(AbstractContainerScreen<?> screen) {
        try {
            return LEFT_POS.getInt(screen);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Could not read AbstractContainerScreen.leftPos", e);
        }
    }

    private static int topPos(AbstractContainerScreen<?> screen) {
        try {
            return TOP_POS.getInt(screen);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Could not read AbstractContainerScreen.topPos", e);
        }
    }

    private static Slot hoveredSlot(AbstractContainerScreen<?> screen) {
        try {
            return (Slot) HOVERED_SLOT.get(screen);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Could not read AbstractContainerScreen.hoveredSlot", e);
        }
    }

    private static Field screenField(String name) {
        try {
            Field field = AbstractContainerScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("AbstractContainerScreen has no field '" + name + "' any more. "
                    + "The container tests read the GUI origin and the hovered slot through it because "
                    + "Minecraft offers no getter - this test needs updating, the mod does not.", e);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Talking to the integrated server
    // ------------------------------------------------------------------------------------------

    /**
     * Work that has to happen on the integrated server thread.
     *
     * <p>Four things in this file are only observable there: the player's real inventory, the
     * server's copy of a menu slot, the server side block entity of the hopper, and which menu the
     * server thinks is open. All four are what a packet has to reach, so reading the client's copy
     * instead would test nothing.
     */
    @FunctionalInterface
    private interface ServerWork<T> {
        T run(MinecraftServer server) throws Exception;
    }

    /** The same, without an answer. */
    @FunctionalInterface
    private interface ServerAction {
        void run(MinecraftServer server) throws Exception;
    }

    /**
     * One question or one change handed to the integrated server thread, polled from the client tick.
     *
     * <p><b>Why not simply read the server's player from the client thread.</b> Fabric's own client
     * test context had {@code runOnServer} and {@code computeOnServer}; the shared {@link Harness}
     * has neither, because the two loaders cannot express the same thing - and a straight read
     * would be a data race on NeoForge, where the integrated server really is a second thread
     * ticking while the client ticks. The work therefore runs where the state lives:
     * {@code MinecraftServer.execute} enqueues it, and only the finished answer crosses back,
     * through volatile fields. That is the same shape the NeoForge driver already uses for commands.
     *
     * <p><b>Why polling and not waiting.</b> A step never blocks - blocking the client tick would
     * stop the very loop that lets the server run the task. So the first poll submits, later polls
     * report; the answer arrives one client tick later at the earliest.
     *
     * <p>An exception thrown on the server thread is kept and rethrown into the polling step, so a
     * failure there fails the test naming the step rather than disappearing into a server log.
     */
    private static final class ServerProbe<T> {

        private final String what;
        private final ServerWork<T> work;
        private volatile boolean submitted;
        private volatile boolean done;
        private volatile T value;
        private volatile Throwable failure;

        ServerProbe(String what, ServerWork<T> work) {
            this.what = what;
            this.work = work;
        }

        /** Submits on the first call; true once the answer is in. */
        boolean poll() {
            Throwable problem = failure;

            if (problem != null) {
                throw new AssertionError("The work on the integrated server failed while " + what
                        + ": " + problem, problem);
            }

            if (done) {
                return true;
            }

            if (!submitted) {
                submitted = true;
                submit();
            }

            return false;
        }

        private void submit() {
            MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();

            if (server == null) {
                throw new AssertionError("There is no integrated server, so " + what + " cannot be done. "
                        + "This step needs a joined single player world - move it into "
                        + "ClientTests.inWorld() if it is not there already.");
            }

            server.execute(() -> {
                try {
                    // value first, done second: done is volatile, so a poller that sees done also
                    // sees the value. The other order would hand out a null answer once in a while.
                    value = work.run(server);
                    done = true;
                } catch (Throwable t) {
                    failure = t;
                }
            });
        }

        T value() {
            return value;
        }

        /** Throws the answer away so the next poll asks the server again. */
        void askAgain() {
            done = false;
            submitted = false;
        }
    }

    /**
     * Asks the integrated server one question, as a step, and publishes the answer for later steps.
     *
     * <p>The answer must not be null: {@link Later#get} treats null as "the step that fills this
     * has not run" and would blame the wrong step. Where "nothing to report" is a possible answer,
     * the callers return an empty string instead.
     */
    private static <T> Later<T> onServer(Script script, String name, ServerWork<T> work) {
        Later<T> answer = new Later<>(name);
        ServerProbe<T> probe = new ServerProbe<>("reading " + name, work);

        script.await("ask the integrated server for " + name, 200, client -> {
            if (!probe.poll()) {
                return false;
            }

            answer.set(probe.value());
            return true;
        }, client -> "the integrated server never answered for " + name);

        return answer;
    }

    /** Changes something on the integrated server, as a step, and waits until it has happened. */
    private static void runOnServer(Script script, String name, ServerAction action) {
        ServerProbe<Boolean> probe = new ServerProbe<>(name, server -> {
            action.run(server);
            return Boolean.TRUE;
        });

        script.await(name, 200, client -> probe.poll(),
                client -> "the integrated server never got round to " + name);
    }

    /**
     * The one player of the single player world.
     *
     * <p>Called on the server thread only, from inside a {@link ServerProbe}. The whole suite runs
     * one client against one integrated server, so "the first player" is "the player".
     */
    private static ServerPlayer firstPlayer(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();

        if (players.isEmpty()) {
            throw new AssertionError("The integrated server has no player, so nothing about the "
                    + "server side of this measurement can be read.");
        }

        return players.getFirst();
    }
}
