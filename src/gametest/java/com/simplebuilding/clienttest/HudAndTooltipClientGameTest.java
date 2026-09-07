package com.simplebuilding.clienttest;

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

import javax.imageio.ImageIO;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.client.gui.NetheriteHopperScreen;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.util.BundleTooltipAccessor;
import com.simplebuilding.util.HopperFilterMode;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
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
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;

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
 * that was never set up. Where a pixel difference would be too small or not attributable, the test
 * observes state instead and says so at the method. Two measurements deviate from the plain recipe
 * and carry their reason in the code: the filter button's two active modes differ by a single text
 * glyph and get a bar of their own instead of {@code assertDrew}'s world-renderer minimum, and the
 * survival inventory's player model is animated by vanilla for good and is painted out of all four
 * of that test's screenshots.
 *
 * <p><b>Why reflection.</b> {@code AbstractContainerScreen} keeps {@code leftPos}, {@code topPos}
 * and {@code hoveredSlot} protected and offers no getter. Putting the mouse on a specific slot -
 * the trigger of both the bundle submenu and the hopper click intercept - is impossible without
 * them. The three fields are read reflectively in {@link #screenField(String)}; if Minecraft ever
 * renames one, the test fails immediately with a message that says it is the test that needs
 * updating, not the mod.
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
 * {@link #testBundleTooltipBarUsesCapacityScale} therefore measures the netherite bundle, where
 * intended and actual behaviour agree, and does not pin the truncation down anywhere.
 *
 * <p><b>Known defect (ReinforcedBundleTooltipSubmenuHandler.onStopHovering).</b> Leaving the slot
 * only sends {@code ReinforcedBundleSelectionPayload(slot.index, -1)}; the client stack keeps the
 * old selection. Its own {@code onMouseScrolled} does both halves ("Client-Item sofort updaten"),
 * and so does the vanilla handler it stands in for: {@code BundleMouseActions.onStopHovering} goes
 * through {@code toggleSelectedBundleItem}, which calls {@code BundleItem.toggleSelectedItem} on the
 * client stack <em>and</em> sends the packet. The server cannot repair the difference, because
 * {@code BundleContents.selectedItem} is in neither {@code BundleContents.equals} nor
 * {@code BundleContents.STREAM_CODEC} - a container sync compares and transmits only the item list,
 * so a selection index never travels from server to client. Effect: after the mouse leaves the slot
 * the tooltip still highlights the old entry, and the next scroll starts from the stale index while
 * the server starts from -1. {@link #testBundleWheelSelectsAndLeavingClears} therefore measures the
 * clearing on the server stack, which is the half that works, and asserts nothing about the client
 * stack in either direction.
 *
 * <p><b>Known defect (ReinforcedBundleTooltipSubmenuHandler:39).</b> When nothing is selected the
 * handler substitutes index 0 for "no selection" and then still adds the wheel delta, so the very
 * first notch downwards lands on the <em>second</em> entry and the first entry cannot be reached by
 * scrolling down at all. {@link #testBundleWheelSelectsAndLeavingClears} only asserts that a notch
 * moves the selection by exactly one step, which holds before and after a fix.
 *
 * <p><b>Not covered</b>
 * <ul>
 *   <li><b>The trim button's icon and tooltip text.</b> {@code AbstractWidget} keeps its tooltip in
 *       a private {@code WidgetTooltipHolder} with no getter, and the ward smithing template icon
 *       is drawn straight into the render state without any identity that survives extraction.
 *       Proving the string "Toggle Resonance Stats" or the icon texture would need a pixel
 *       comparison against a stored reference image, which this repo deliberately does not keep.
 *       What is covered: the button exists, sits where the mixin puts it, is 20x20, and toggles a
 *       panel that really appears and disappears.</li>
 *   <li><b>The individual numbers in the L / S / C panel.</b> They are drawn as text into the
 *       render state; only their presence as a block of pixels is observable from outside.</li>
 *   <li><b>Green versus orange in the hopper filter icon.</b> The screenshot difference proves that
 *       a different glyph is drawn per mode, but the colour of a glyph cannot be read back without
 *       a reference image.</li>
 *   <li><b>The three-line HUD layout (which line holds which number).</b> Only the fact that the
 *       overlay grows by the position and volume lines is measurable in pixels.</li>
 * </ul>
 */
public final class HudAndTooltipClientGameTest implements FabricClientGameTest {

    /** Free air one block in front of the wall - the container blocks are placed here. */
    private static final BlockPos CONTAINER_POS = new BlockPos(10, 1, RendererTestScene.FRONT_Z);

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

    /** The vignette setting as it was before {@link #silenceHudDrift} switched it off. */
    private boolean vignetteBefore = true;

    private static final Field LEFT_POS = screenField("leftPos");
    private static final Field TOP_POS = screenField("topPos");
    private static final Field HOVERED_SLOT = screenField("hoveredSlot");

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            RendererTestScene.build(context, singleplayer, "minecraft:stone", "creative");

            try {
                testRangefinderHudShowsPositionsAndVolume(context, singleplayer);

                // Everything below lives in a container screen. The HUD goes back into hiding: it
                // renders behind the screen and would only add pixels that have nothing to do with
                // the screen under test. Survival is mandatory, not cosmetic - InventoryScreen.init
                // replaces itself with the creative screen when the player has infinite materials,
                // and the mod's button lives in InventoryScreen.
                RendererTestScene.makeRenderingDeterministic(context);
                singleplayer.getServer().runCommand("gamemode survival @a");
                singleplayer.getServer().runCommand("clear @a");
                singleplayer.getConnection().waitForClientboundPackets();
                context.waitTicks(20);

                testBundleTooltipBarUsesCapacityScale(context, singleplayer);
                testBundleWheelSelectsAndLeavingClears(context, singleplayer);
                testBundleSubmenuReachesVanillaContainerScreens(context, singleplayer);
                testHopperFilterButtonAndGhostSlots(context, singleplayer);
                testInventoryTrimStatsButtonToggles(context, singleplayer);
            } finally {
                RendererTestScene.showHudAgain(context);
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // RangefinderHudOverlay
    // ------------------------------------------------------------------------------------------

    /**
     * Proves that {@code RangefinderHudOverlay} still reaches the screen through
     * {@code HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ...)} and that it
     * really renders the measurement, not just its frame.
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
    private void testRangefinderHudShowsPositionsAndVolume(ClientGameTestContext context,
                                                           TestSingleplayerContext singleplayer) {
        // The HUD is what is under test here, so it has to be visible.
        RendererTestScene.showHudAgain(context);
        silenceHudDrift(context);

        try {
            giveOctant(context, singleplayer, false);
            assertRangefinderPreconditions(context, false);

            // The noise floor spans the same 60 ticks a trigger step below spans, not a shorter
            // window. A drift that only shows over a minute of screen time would otherwise fall
            // outside the noise floor and inside every signal - which is exactly how the vignette
            // slipped past a ten tick noise floor before silenceHudDrift existed.
            Path baseline = context.takeScreenshot("hud-a-octant-blank");
            context.waitTicks(60);
            Path baselineAgain = context.takeScreenshot("hud-b-octant-blank-again");

            ScreenshotDiff.Diff noiseFloor =
                    ScreenshotDiff.compare("noise floor (octant without positions, 60 ticks apart)",
                            baseline, baselineAgain);
            ScreenshotDiff.assertUnchanged(noiseFloor);

            giveOctant(context, singleplayer, true);
            assertRangefinderPreconditions(context, true);

            Path withPositions = context.takeScreenshot("hud-c-octant-positions");
            ScreenshotDiff.Diff signal =
                    ScreenshotDiff.compare("rangefinder HUD with both positions", baseline, withPositions);
            ScreenshotDiff.assertDrew("RangefinderHudOverlay", noiseFloor, signal);

            // Control: the same octant without positions has to restore the baseline picture. Without
            // this step "the picture drifts anyway" would explain the difference just as well.
            giveOctant(context, singleplayer, false);
            assertRangefinderPreconditions(context, false);

            Path clearedAgain = context.takeScreenshot("hud-d-octant-blank-control");
            ScreenshotDiff.Diff residual =
                    ScreenshotDiff.compare("control (positions removed again)", baseline, clearedAgain);
            assertBackToBaseline(residual, noiseFloor, "removing Pos1 and Pos2 from the octant");
        } finally {
            restoreHudDrift(context);
        }
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
     * <p><b>The chat.</b> {@code RendererTestScene.build} sets the game mode and the server answers
     * with "Your game mode has been updated" - a 660x14 block of pixels that fades out by itself two
     * hundred ticks later, in the middle of this method's screenshots. The filter drops every
     * message before it is queued, {@code clearMessages} removes the ones already queued, and the
     * chat stays empty for the rest of the method.
     *
     * <p>Neither is asserted afterwards: {@code ChatComponent} offers no way to read back its
     * message list or its filter. The vignette option is readable, and
     * {@link #assertRangefinderPreconditions} checks it before every screenshot.
     */
    private void silenceHudDrift(ClientGameTestContext context) {
        vignetteBefore = context.computeOnClient(client -> {
            boolean before = client.options.vignette().get();
            client.options.vignette().set(false);
            client.gui.hud.getChat().setVisibleMessageFilter(message -> false);
            client.gui.hud.getChat().clearMessages(true);
            return before;
        });

        context.waitTicks(10);
    }

    /**
     * Puts both back. Client options and the chat filter outlive a single client game test, so the
     * next test class in the run would otherwise measure a screen this one had reconfigured.
     */
    private void restoreHudDrift(ClientGameTestContext context) {
        boolean before = vignetteBefore;

        context.runOnClient(client -> {
            client.options.vignette().set(before);
            client.gui.hud.getChat().setVisibleMessageFilter(message -> true);
        });
    }

    private void giveOctant(ClientGameTestContext context, TestSingleplayerContext singleplayer, boolean withPositions) {
        String positions = withPositions
                ? "[minecraft:custom_data={"
                        + "Pos1:[I;" + OCTANT_POS_1.getX() + "," + OCTANT_POS_1.getY() + "," + OCTANT_POS_1.getZ() + "],"
                        + "Pos2:[I;" + OCTANT_POS_2.getX() + "," + OCTANT_POS_2.getY() + "," + OCTANT_POS_2.getZ() + "]"
                        + "}]"
                : "";

        singleplayer.getServer().runCommand(
                "item replace entity @a weapon.mainhand with simplebuilding:octant" + positions);
        singleplayer.getConnection().waitForClientboundPackets();
        // Equip animation plus the 40 tick item name popup that a changed main hand item triggers.
        context.waitTicks(60);
    }

    /** Everything {@code RangefinderHudOverlay.render} checks before it draws anything. */
    private void assertRangefinderPreconditions(ClientGameTestContext context, boolean expectPositions) {
        String problem = context.computeOnClient(client -> {
            if (client.player == null) {
                return "no client player";
            }

            if (client.gui.hud.isHidden()) {
                return "the HUD is hidden, so no HUD element is extracted at all";
            }

            if (client.options.vignette().get()) {
                return "the vignette is switched on again; its brightness creeps towards the light "
                        + "level by one percent per frame and never arrives, so the screen edges "
                        + "would drift between two screenshots (see silenceHudDrift)";
            }

            if (client.gui.screen() != null) {
                return "a screen is open (" + client.gui.screen().getClass().getName()
                        + "); the overlay returns early on OctantScreen and screens cover the HUD";
            }

            ItemStack held = client.player.getMainHandItem();

            if (!(held.getItem() instanceof OctantItem)) {
                return "main hand does not hold an OctantItem but " + held;
            }

            CompoundTag nbt = held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            boolean hasPos1 = nbt.getIntArray("Pos1").orElse(new int[0]).length == 3;
            boolean hasPos2 = nbt.getIntArray("Pos2").orElse(new int[0]).length == 3;

            if (expectPositions && !(hasPos1 && hasPos2)) {
                return "the octant should carry Pos1 and Pos2 but its custom data is " + nbt;
            }

            if (!expectPositions && (hasPos1 || hasPos2)) {
                return "the octant should carry no positions but its custom data is " + nbt;
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Rangefinder HUD trigger conditions not met: " + problem);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Reinforced bundle tooltip bar
    // ------------------------------------------------------------------------------------------

    /**
     * Proves that the mod's tooltip path really rescales the bundle progress bar: that
     * {@code ReinforcedBundleItem.getTooltipImage} hands out a {@link ReinforcedBundleTooltipData}
     * with the enlarged capacity, that the {@code ClientTooltipComponentCallback} turns it into a
     * {@link ClientBundleTooltip} with the capacity scale set, and that
     * {@code BundleTooltipComponentMixin} divides the occupancy by it.
     *
     * <p>This one observes render state instead of pixels, for the same reason
     * {@code MultiBlockBreakingClientGameTest} does: the only way to change the scale of a tooltip
     * that is already on screen is to change the item, and then the icon, the name and the item
     * grid change with it, so a screenshot difference would not be attributable to the bar. The
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
     * <p>What breaks this test: {@code getTooltipImage} losing the enlarged capacity, the tooltip
     * component callback not being registered, the mixin no longer applying to
     * {@code ClientBundleTooltip} (the {@link BundleTooltipAccessor} cast fails), or its
     * {@code ModifyArg} no longer hitting {@code extractBundleWithItemsTooltip}.
     */
    private void testBundleTooltipBarUsesCapacityScale(ClientGameTestContext context,
                                                       TestSingleplayerContext singleplayer) {
        String problem = context.computeOnClient(client -> {
            BundleContents contents = new BundleContents(List.of(
                    ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.STONE, 64))));

            ItemStack bundle = new ItemStack(ModItems.NETHERITE_BUNDLE);
            bundle.set(DataComponents.BUNDLE_CONTENTS, contents);

            TooltipComponent data = bundle.getTooltipImage().orElse(null);

            if (!(data instanceof ReinforcedBundleTooltipData reinforced)) {
                return "getTooltipImage did not return a ReinforcedBundleTooltipData but " + data;
            }

            // 2 tiers * 1.5 stacks * 64 items. The mod's own comment names the same number.
            if (reinforced.maxCapacity() != 192) {
                return "the netherite bundle reports a capacity of " + reinforced.maxCapacity()
                        + " items instead of 192, so the tooltip would be scaled by the wrong factor";
            }

            ClientTooltipComponent modComponent = ClientTooltipComponent.create(data);

            if (!(modComponent instanceof ClientBundleTooltip)) {
                return "the ClientTooltipComponentCallback did not produce a ClientBundleTooltip but "
                        + modComponent.getClass().getName();
            }

            if (!(modComponent instanceof BundleTooltipAccessor accessor)) {
                return "BundleTooltipComponentMixin is not applied - ClientBundleTooltip does not "
                        + "implement BundleTooltipAccessor";
            }

            TooltipShape scaled = extractShape(client, modComponent);
            TooltipShape scaledAgain = extractShape(client, modComponent);

            if (!scaled.equals(scaledAgain)) {
                return "extracting the same tooltip component twice gave different geometry ("
                        + scaled + " vs. " + scaledAgain + "); no comparison below this is meaningful";
            }

            TooltipShape unscaled = extractShape(client, new ClientBundleTooltip(contents));

            if (scaled.equals(unscaled)) {
                return "the mod's tooltip component draws exactly like an unscaled vanilla one ("
                        + unscaled + "), so the capacity scale never reached the progress bar";
            }

            if (scaled.textElements() >= unscaled.textElements()) {
                return "the unscaled bar should carry the \"Full\" label at weight 1 and the scaled "
                        + "one should not, but the scaled component has " + scaled.textElements()
                        + " text elements and the unscaled one " + unscaled.textElements();
            }

            accessor.simplebuilding$setCapacityScale(1.0f);
            TooltipShape control = extractShape(client, modComponent);

            if (!control.equals(unscaled)) {
                return "control failed: with the capacity scale set back to 1 the mod's component "
                        + "still does not draw like the vanilla one (" + control + " vs. " + unscaled
                        + "), so the measured difference cannot be attributed to the scale";
            }

            System.out.println("[simplebuilding-test] bundle tooltip bar: unscaled " + unscaled
                    + ", scaled by 3 " + scaled);
            return null;
        });

        if (problem != null) {
            throw new AssertionError("Reinforced bundle tooltip bar: " + problem);
        }

        // A picture of the real thing for the record - the assertions above are the proof.
        placeBundleInInventory(singleplayer, List.of(new ItemStack(Items.STONE, 64)));
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);

        openInventoryScreen(context);
        hoverSlot(context, findBundleSlot(context));
        context.waitTicks(10);
        context.takeScreenshot("tooltip-a-netherite-bundle-bar");
        closeScreen(context);
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
    private void testBundleWheelSelectsAndLeavingClears(ClientGameTestContext context,
                                                        TestSingleplayerContext singleplayer) {
        placeBundleInInventory(singleplayer, List.of(
                new ItemStack(Items.STONE, 16),
                new ItemStack(Items.DIRT, 16),
                new ItemStack(Items.OAK_PLANKS, 16)));
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);

        openInventoryScreen(context);

        int slotIndex = findBundleSlot(context);
        hoverSlot(context, slotIndex);
        assertBundleWheelPreconditions(context, slotIndex, 3);

        int start = clientSelectedIndex(context, slotIndex);

        if (start != BundleContents.NO_SELECTED_ITEM_INDEX) {
            throw new AssertionError("The bundle already had entry " + start
                    + " selected before the first wheel notch; the steps below would not be attributable.");
        }

        int first = scrollAndReadSelection(context, slotIndex, -1.0, start);

        if (first < 0 || first > 2) {
            throw new AssertionError("A wheel notch over the bundle slot did not select any entry: "
                    + "the selected index is " + first + ", expected one of 0..2.");
        }

        int second = scrollAndReadSelection(context, slotIndex, -1.0, first);

        if (second != first + 1) {
            throw new AssertionError("A wheel notch downwards moved the selection from " + first
                    + " to " + second + " instead of " + (first + 1) + ".");
        }

        int back = scrollAndReadSelection(context, slotIndex, 1.0, second);

        if (back != first) {
            throw new AssertionError("A wheel notch upwards moved the selection from " + second
                    + " to " + back + " instead of back to " + first + ".");
        }

        // The packet half: the server has to carry the same selection, otherwise the tooltip is the
        // only thing that ever knew about it and placing from the bundle would use the wrong entry.
        int onServer = serverSelectedIndex(singleplayer, slotIndex);

        if (onServer != back) {
            throw new AssertionError("The server stack has entry " + onServer + " selected while the "
                    + "client shows " + back + "; ReinforcedBundleSelectionPayload did not arrive.");
        }

        context.takeScreenshot("bundle-a-wheel-selection");

        // Leaving the slot. Read on the server, see the javadoc: no packet carries a selection
        // index back to the client, so the client stack is not an observable of this step.
        moveCursorToGui(context, PARK_X, PARK_Y);
        waitForServerSelection(context, singleplayer, slotIndex, BundleContents.NO_SELECTED_ITEM_INDEX,
                "leaving the bundle slot did not clear the selection on the server");

        closeScreen(context);
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
     * <p>What breaks this test: the mixin's injection point moving off {@code init}, or the handler
     * being registered from a mod screen instead of from the shared base class.
     */
    private void testBundleSubmenuReachesVanillaContainerScreens(ClientGameTestContext context,
                                                                 TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runCommand(
                "setblock " + CONTAINER_POS.getX() + " " + CONTAINER_POS.getY() + " " + CONTAINER_POS.getZ()
                        + " minecraft:chest");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(10);

        // Empty main hand, otherwise the right click would use the item instead of opening the chest.
        RendererTestScene.aimAt(context, singleplayer, "0.0", "0.0", CONTAINER_POS, Direction.NORTH);
        context.getInput().pressMouse(1);
        context.waitForScreen(ContainerScreen.class);
        context.waitTicks(20);

        int slotIndex = findBundleSlot(context);
        hoverSlot(context, slotIndex);
        assertBundleWheelPreconditions(context, slotIndex, 3);

        // The client stack still carries whatever the previous test left selected - the known
        // defect in onStopHovering means leaving a slot never clears the client side. A notch
        // downwards can only move while there is an entry below the current one, so rule that out
        // here: otherwise a clamped selection would look exactly like a missing handler.
        int start = clientSelectedIndex(context, slotIndex);

        if (start >= 2) {
            throw new AssertionError("The bundle already has its last of three entries selected ("
                    + start + "), so a wheel notch downwards has nowhere to go and this test could "
                    + "not tell a clamped selection from a handler that never ran.");
        }

        int after = scrollAndReadSelection(context, slotIndex, -1.0, start);

        if (after == start) {
            throw new AssertionError("A wheel notch over the bundle slot did nothing in a vanilla "
                    + "chest screen (selection stayed at " + start + "); the submenu handler is not "
                    + "registered on AbstractContainerScreen subclasses outside the mod.");
        }

        context.takeScreenshot("bundle-b-chest-wheel");

        moveCursorToGui(context, PARK_X, PARK_Y);
        context.waitTicks(10);
        closeScreen(context);

        singleplayer.getServer().runCommand(
                "setblock " + CONTAINER_POS.getX() + " " + CONTAINER_POS.getY() + " " + CONTAINER_POS.getZ()
                        + " minecraft:air");
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(10);
    }

    /** Everything {@code AbstractContainerScreen.mouseScrolled} and the mod's handler check. */
    private void assertBundleWheelPreconditions(ClientGameTestContext context, int slotIndex, int expectedEntries) {
        String problem = context.computeOnClient(client -> {
            AbstractContainerScreen<?> screen = containerScreen(client);
            Slot slot = screen.getMenu().slots.get(slotIndex);

            if (!slot.hasItem()) {
                return "slot " + slotIndex + " is empty, so mouseScrolled never reaches any handler";
            }

            ItemStack stack = slot.getItem();

            if (!(stack.getItem() instanceof ReinforcedBundleItem)) {
                return "slot " + slotIndex + " does not hold a ReinforcedBundleItem but " + stack;
            }

            if (stack.is(net.minecraft.tags.ItemTags.BUNDLES)) {
                return "the mod's bundle is in the minecraft:bundles tag, so vanilla's "
                        + "BundleMouseActions matches first and swallows every wheel notch - the "
                        + "measurement below would prove nothing about the mod";
            }

            BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);

            if (contents == null || contents.size() != expectedEntries) {
                return "the bundle should hold " + expectedEntries + " entries but holds "
                        + (contents == null ? "no bundle contents at all" : contents.size());
            }

            Slot hovered = hoveredSlot(screen);

            if (hovered != slot) {
                return "the mouse is not on the bundle slot (hovered: "
                        + (hovered == null ? "nothing" : "menu slot " + hovered.index) + ")";
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Bundle wheel trigger conditions not met: " + problem);
        }
    }

    private int scrollAndReadSelection(ClientGameTestContext context, int slotIndex, double amount, int previous) {
        context.getInput().scroll(0.0, amount);
        context.waitTicks(10);

        int now = clientSelectedIndex(context, slotIndex);

        if (now == previous) {
            throw new AssertionError("A wheel notch of " + amount + " over the bundle slot left the "
                    + "selection at " + previous + ".");
        }

        return now;
    }

    /**
     * Waits for the server stack of a menu slot to reach a selection index. Ticking the client also
     * ticks the integrated server, so this is where a packet the client just sent becomes visible.
     */
    private void waitForServerSelection(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                        int slotIndex, int expected, String message) {
        for (int tick = 0; tick < 120; tick++) {
            if (serverSelectedIndex(singleplayer, slotIndex) == expected) {
                return;
            }

            context.waitTick();
        }

        throw new AssertionError(message + ": the server stack still shows entry "
                + serverSelectedIndex(singleplayer, slotIndex) + " instead of " + expected + ".");
    }

    private int clientSelectedIndex(ClientGameTestContext context, int slotIndex) {
        return context.computeOnClient(client ->
                selectedIndexOf(containerScreen(client).getMenu().slots.get(slotIndex).getItem()));
    }

    private int serverSelectedIndex(TestSingleplayerContext singleplayer, int slotIndex) {
        return singleplayer.getServer().computeOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            return selectedIndexOf(player.containerMenu.slots.get(slotIndex).getItem());
        });
    }

    private static int selectedIndexOf(ItemStack stack) {
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        return contents == null ? Integer.MIN_VALUE : contents.getSelectedItemIndex();
    }

    private void placeBundleInInventory(TestSingleplayerContext singleplayer, List<ItemStack> entries) {
        singleplayer.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            ItemStack bundle = new ItemStack(ModItems.NETHERITE_BUNDLE);
            bundle.set(DataComponents.BUNDLE_CONTENTS,
                    new BundleContents(entries.stream().map(ItemStackTemplate::fromNonEmptyStack).toList()));
            player.getInventory().setItem(SAFE_INVENTORY_SLOT, bundle);
        });
    }

    private int findBundleSlot(ClientGameTestContext context) {
        int index = context.computeOnClient(client -> {
            AbstractContainerScreen<?> screen = containerScreen(client);
            int found = -1;

            for (int i = 0; i < screen.getMenu().slots.size(); i++) {
                if (screen.getMenu().slots.get(i).getItem().getItem() instanceof ReinforcedBundleItem) {
                    if (found != -1) {
                        return -2;
                    }

                    found = i;
                }
            }

            return found;
        });

        if (index == -1) {
            throw new AssertionError("No reinforced bundle in the open container screen.");
        }

        if (index == -2) {
            throw new AssertionError("More than one reinforced bundle in the open container screen; "
                    + "the test would not know which slot it is measuring.");
        }

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
    private void testHopperFilterButtonAndGhostSlots(ClientGameTestContext context,
                                                     TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runCommand(
                "setblock " + CONTAINER_POS.getX() + " " + CONTAINER_POS.getY() + " " + CONTAINER_POS.getZ()
                        + " simplebuilding:netherite_hopper");
        singleplayer.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            player.getInventory().setItem(SAFE_INVENTORY_SLOT, new ItemStack(Items.DIAMOND, 1));
        });
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(15);

        // Slightly upwards so the ray enters the hopper's top plate head on, as in ModScreensClientGameTest.
        RendererTestScene.aimAt(context, singleplayer, "0.0", "-4.0", CONTAINER_POS, Direction.NORTH);
        context.getInput().pressMouse(1);
        context.waitForScreen(NetheriteHopperScreen.class);
        context.waitTicks(20);

        moveCursorToGui(context, PARK_X, PARK_Y);
        clearWidgetFocus(context);
        assertFilterMode(context, HopperFilterMode.NONE);

        Path noFilter = context.takeScreenshot("hopper-a-filter-none");
        context.waitTicks(10);
        Path noFilterAgain = context.takeScreenshot("hopper-b-filter-none-again");

        ScreenshotDiff.Diff noiseFloor =
                ScreenshotDiff.compare("noise floor (hopper screen, filter off, twice)", noFilter, noFilterAgain);
        ScreenshotDiff.assertUnchanged(noiseFloor);

        clickFilterButton(context);
        waitForFilterMode(context, HopperFilterMode.WHITELIST);
        Path whitelist = context.takeScreenshot("hopper-c-filter-whitelist");
        ScreenshotDiff.assertDrew("NetheriteHopperScreen (whitelist icon)", noiseFloor,
                ScreenshotDiff.compare("filter off vs. whitelist", noFilter, whitelist));

        clickFilterButton(context);
        waitForFilterMode(context, HopperFilterMode.TYPE);
        Path type = context.takeScreenshot("hopper-d-filter-type");
        ScreenshotDiff.assertDrew("NetheriteHopperScreen (type icon)", noiseFloor,
                ScreenshotDiff.compare("filter off vs. type", noFilter, type));

        // The two active modes differ in one text glyph and nothing else, so this step gets its own
        // bar rather than assertDrew's: that method's absolute minimum of 400 pixels is calibrated
        // for block outlines in the world and is larger than a whole glyph. One glyph at GUI scale 2
        // occupies a 14x16 block of window pixels, and a check mark against a T changes 128 of them
        // here - so the bar is ten times the measured noise floor and at least 40 pixels, which is
        // a third of the measured signal and, at a noise floor of zero, unreachable by noise. If the
        // button ever drew the same thing for both modes the difference would be 0 and this fails.
        ScreenshotDiff.Diff glyphSwap = ScreenshotDiff.compare("whitelist vs. type", whitelist, type);
        int requiredForGlyph = Math.max(40, noiseFloor.changedPixels() * 10 + 40);

        if (glyphSwap.changedPixels() < requiredForGlyph) {
            throw new AssertionError("The filter button draws the same thing for whitelist and type: "
                    + glyphSwap + ", at least " + requiredForGlyph + " changed pixels were required. "
                    + "Both modes were asserted through getSyncedFilterMode before the screenshots.");
        }

        testHopperScreenSwallowsSlotClicks(context, singleplayer);
        clearWidgetFocus(context);

        Path ghost = context.takeScreenshot("hopper-e-ghost-slot");
        ScreenshotDiff.assertDrew("NetheriteHopperScreen (ghost item overlay)", noiseFloor,
                ScreenshotDiff.compare("filtered slot without vs. with a ghost item", type, ghost));

        closeScreen(context);
        singleplayer.getServer().runCommand(
                "setblock " + CONTAINER_POS.getX() + " " + CONTAINER_POS.getY() + " " + CONTAINER_POS.getZ()
                        + " minecraft:air");
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(10);
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
    private void testHopperScreenSwallowsSlotClicks(ClientGameTestContext context,
                                                    TestSingleplayerContext singleplayer) {
        int diamondSlot = findSlotWithItem(context, Items.DIAMOND);

        clickSlot(context, diamondSlot);
        assertCarriedIs(context, Items.DIAMOND, "picking the diamond up from the player inventory");

        clickSlot(context, 0);
        context.waitTicks(15);

        assertCarriedIs(context, Items.DIAMOND, "clicking hopper slot 0 with a filter active");

        String problem = context.computeOnClient(client -> {
            NetheriteHopperScreen screen = (NetheriteHopperScreen) client.gui.screen();
            NetheriteHopperScreenHandler menu = screen.getMenu();

            if (!menu.slots.get(0).getItem().isEmpty()) {
                return "hopper slot 0 now holds " + menu.slots.get(0).getItem()
                        + " - the click was not swallowed and the item was really placed";
            }

            ModHopperBlockEntity blockEntity = menu.getBlockEntity();

            if (blockEntity == null) {
                return "the client side menu has no block entity, so the screen can never draw a ghost item";
            }

            ItemStack ghost = blockEntity.getGhostItem(0);

            if (!ghost.is(Items.DIAMOND) || ghost.getCount() != 1) {
                return "the client block entity holds " + ghost + " as ghost item 0 instead of one diamond";
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Hopper slot click intercept: " + problem);
        }

        String serverProblem = singleplayer.getServer().computeOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();

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

            return null;
        });

        if (serverProblem != null) {
            throw new AssertionError("Hopper ghost item did not reach the server: " + serverProblem);
        }

        // Put the diamond back so the ghost overlay is the only difference in the next screenshot.
        clickSlot(context, diamondSlot);
        context.waitTicks(10);
        moveCursorToGui(context, PARK_X, PARK_Y);
        context.waitTicks(10);
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
    private void clearWidgetFocus(ClientGameTestContext context) {
        context.runOnClient(client -> containerScreen(client).setFocused(null));
        context.waitTick();

        String stillFocused = context.computeOnClient(client -> {
            GuiEventListener focused = containerScreen(client).getFocused();
            return focused == null ? null : focused.getClass().getName();
        });

        if (stillFocused != null) {
            throw new AssertionError("The screen still reports " + stillFocused + " as focused after "
                    + "setFocused(null); a focused widget draws its highlighted frame and would put "
                    + "its own pixels into the next difference.");
        }
    }

    private void clickFilterButton(ClientGameTestContext context) {
        int[] centre = context.computeOnClient(client -> {
            Screen screen = client.gui.screen();
            Button found = null;

            for (GuiEventListener child : screen.children()) {
                if (child instanceof Button button) {
                    if (found != null) {
                        return new int[0];
                    }

                    found = button;
                }
            }

            return found == null
                    ? null
                    : new int[] {found.getX() + found.getWidth() / 2, found.getY() + found.getHeight() / 2};
        });

        if (centre == null) {
            throw new AssertionError("The netherite hopper screen has no button; the filter toggle is gone.");
        }

        if (centre.length == 0) {
            throw new AssertionError("The netherite hopper screen has more than one button; the test "
                    + "would not know which one is the filter toggle.");
        }

        moveCursorToGui(context, centre[0], centre[1]);
        context.getInput().pressMouse(0);
        context.waitTicks(5);
        moveCursorToGui(context, PARK_X, PARK_Y);
        clearWidgetFocus(context);
        context.waitTicks(10);
    }

    private void waitForFilterMode(ClientGameTestContext context, HopperFilterMode expected) {
        for (int tick = 0; tick < 120; tick++) {
            if (currentFilterMode(context) == expected) {
                context.waitTicks(10);
                assertFilterMode(context, expected);
                return;
            }

            context.waitTick();
        }

        throw new AssertionError("The hopper filter never reached " + expected + "; it is still "
                + currentFilterMode(context) + ". The toggle packet or the container data sync is broken.");
    }

    private void assertFilterMode(ClientGameTestContext context, HopperFilterMode expected) {
        HopperFilterMode actual = currentFilterMode(context);

        if (actual != expected) {
            throw new AssertionError("The hopper screen reports filter mode " + actual + " instead of "
                    + expected + "; the screenshot below would not show what the test claims.");
        }
    }

    private HopperFilterMode currentFilterMode(ClientGameTestContext context) {
        return context.computeOnClient(client ->
                ((NetheriteHopperScreen) client.gui.screen()).getMenu().getSyncedFilterMode());
    }

    private void assertCarriedIs(ClientGameTestContext context, net.minecraft.world.item.Item expected, String step) {
        String carried = context.computeOnClient(client -> {
            ItemStack stack = containerScreen(client).getMenu().getCarried();
            return stack.is(expected) && stack.getCount() == 1 ? null : String.valueOf(stack);
        });

        if (carried != null) {
            throw new AssertionError("After " + step + " the cursor carries " + carried
                    + " instead of one " + expected + ".");
        }
    }

    private int findSlotWithItem(ClientGameTestContext context, net.minecraft.world.item.Item item) {
        int index = context.computeOnClient(client -> {
            AbstractContainerScreen<?> screen = containerScreen(client);

            for (int i = 0; i < screen.getMenu().slots.size(); i++) {
                if (screen.getMenu().slots.get(i).getItem().is(item)) {
                    return i;
                }
            }

            return -1;
        });

        if (index < 0) {
            throw new AssertionError("No slot in the open screen holds " + item + ".");
        }

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
     * mixin only sits on the survival screen. The caller switches the game mode before this runs.
     *
     * <p>What breaks this test: the mixin no longer injecting into {@code init} (no button at all),
     * the button's position or size changing, the click handler no longer flipping the flag, or
     * {@code renderTrimStats} no longer drawing the panel.
     */
    private void testInventoryTrimStatsButtonToggles(ClientGameTestContext context,
                                                     TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runCommand("clear @a");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(10);

        openInventoryScreen(context);
        moveCursorToGui(context, PARK_X, PARK_Y);
        context.waitTicks(10);
        clearWidgetFocus(context);

        int[] button = assertTrimButtonGeometry(context);
        int[] modelBox = playerModelBox(context);

        // The two baseline shots are twenty ticks apart, the same settling time clickAndPark gives
        // the screen after a click, so the noise floor covers as much screen time as every signal
        // measured against it.
        Path hidden = context.takeScreenshot("inventory-a-stats-hidden");
        context.waitTicks(20);
        Path hiddenAgain = context.takeScreenshot("inventory-b-stats-hidden-again");

        Path hiddenBase = withoutPlayerModel(hidden, modelBox);
        ScreenshotDiff.Diff noiseFloor = ScreenshotDiff.compare(
                "noise floor (inventory screen, stats hidden, twice)",
                hiddenBase, withoutPlayerModel(hiddenAgain, modelBox));
        ScreenshotDiff.assertUnchanged(noiseFloor);

        clickAndPark(context, button[0], button[1]);
        Path shown = context.takeScreenshot("inventory-c-stats-shown");
        ScreenshotDiff.assertDrew("InventoryScreenMixin (resonance stats panel)", noiseFloor,
                ScreenshotDiff.compare("resonance stats panel", hiddenBase,
                        withoutPlayerModel(shown, modelBox)));

        clickAndPark(context, button[0], button[1]);
        Path hiddenControl = context.takeScreenshot("inventory-d-stats-hidden-control");
        ScreenshotDiff.Diff residual = ScreenshotDiff.compare("control (stats toggled off again)",
                hiddenBase, withoutPlayerModel(hiddenControl, modelBox));
        assertBackToBaseline(residual, noiseFloor, "clicking the resonance stats button a second time");

        closeScreen(context);
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
    private int[] playerModelBox(ClientGameTestContext context) {
        int[] box = context.computeOnClient(client -> {
            AbstractContainerScreen<?> screen = containerScreen(client);
            double scaleX = client.getWindow().getScreenWidth() / (double) client.getWindow().getGuiScaledWidth();
            double scaleY = client.getWindow().getScreenHeight() / (double) client.getWindow().getGuiScaledHeight();

            return new int[] {
                    (int) Math.floor((leftPos(screen) + 26) * scaleX) - 2,
                    (int) Math.floor((topPos(screen) + 8) * scaleY) - 2,
                    (int) Math.ceil((leftPos(screen) + 75) * scaleX) + 2,
                    (int) Math.ceil((topPos(screen) + 78) * scaleY) + 2,
            };
        });

        int[] panel = context.computeOnClient(client -> {
            AbstractContainerScreen<?> screen = containerScreen(client);
            double scaleX = client.getWindow().getScreenWidth() / (double) client.getWindow().getGuiScaledWidth();
            double scaleY = client.getWindow().getScreenHeight() / (double) client.getWindow().getGuiScaledHeight();

            return new int[] {
                    (int) Math.floor((leftPos(screen) - 84 - 30) * scaleX),
                    (int) Math.floor((topPos(screen) + 10) * scaleY),
                    (int) Math.ceil((leftPos(screen) - 30) * scaleX),
                    (int) Math.ceil((topPos(screen) + 10 + 64) * scaleY),
            };
        });

        boolean overlaps = box[0] < panel[2] && panel[0] < box[2] && box[1] < panel[3] && panel[1] < box[3];

        if (overlaps) {
            throw new AssertionError("The player model mask " + describeBox(box) + " overlaps the "
                    + "resonance stats panel " + describeBox(panel) + ". Masking it away would hide "
                    + "part of the signal and the comparison below could pass without a panel.");
        }

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
     * Returns the GUI coordinates of its centre.
     *
     * <p>The only other button on the survival inventory screen is vanilla's recipe book toggle,
     * which is 20x18 - so "the single 20x20 button" identifies the mod's without any guessing.
     */
    private int[] assertTrimButtonGeometry(ClientGameTestContext context) {
        String problem = context.computeOnClient(client -> {
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
                return "the inventory screen has no 20x20 button - InventoryScreenMixin.init did not run";
            }

            if (candidates > 1) {
                return "the inventory screen has " + candidates + " buttons of 20x20; the test cannot "
                        + "tell which one belongs to the mod";
            }

            int expectedX = leftPos(screen) - 24;
            int expectedY = topPos(screen) + 10;

            if (found.getX() != expectedX || found.getY() != expectedY) {
                return "the trim button sits at " + found.getX() + "/" + found.getY() + " instead of "
                        + expectedX + "/" + expectedY + " (24 left of the inventory, 10 below its top edge)";
            }

            return null;
        });

        if (problem != null) {
            throw new AssertionError("Armor trim stats button: " + problem);
        }

        return context.computeOnClient(client -> {
            for (GuiEventListener child : containerScreen(client).children()) {
                if (child instanceof Button button && button.getWidth() == 20 && button.getHeight() == 20) {
                    return new int[] {button.getX() + 10, button.getY() + 10};
                }
            }

            return new int[] {0, 0};
        });
    }

    private void clickAndPark(ClientGameTestContext context, int guiX, int guiY) {
        moveCursorToGui(context, guiX, guiY);
        context.getInput().pressMouse(0);
        context.waitTicks(5);
        moveCursorToGui(context, PARK_X, PARK_Y);
        clearWidgetFocus(context);
        context.waitTicks(20);
    }

    // ------------------------------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------------------------------

    private void openInventoryScreen(ClientGameTestContext context) {
        context.getInput().pressKey(options -> options.keyInventory);
        context.waitForScreen(InventoryScreen.class);
        context.waitTicks(20);
    }

    private void closeScreen(ClientGameTestContext context) {
        // Through the player, not through setScreen(null): this is the path that also tells the
        // server the container is closed, so the next screen does not open on top of a stale menu.
        context.runOnClient(client -> client.player.closeContainer());
        context.waitForScreen(null);
        context.waitTicks(10);
    }

    private void clickSlot(ClientGameTestContext context, int slotIndex) {
        hoverSlot(context, slotIndex);
        context.getInput().pressMouse(0);
        context.waitTicks(10);
    }

    /**
     * Puts the mouse on the centre of a menu slot and waits until the screen has actually taken the
     * hover over. {@code hoveredSlot} is only updated while the screen extracts its render state,
     * so a slot is not hovered before the next frame - and everything the tests do afterwards
     * (wheel, click) reads exactly that field.
     */
    private void hoverSlot(ClientGameTestContext context, int slotIndex) {
        int[] centre = context.computeOnClient(client -> {
            AbstractContainerScreen<?> screen = containerScreen(client);
            Slot slot = screen.getMenu().slots.get(slotIndex);
            return new int[] {leftPos(screen) + slot.x + 8, topPos(screen) + slot.y + 8};
        });

        moveCursorToGui(context, centre[0], centre[1]);

        for (int tick = 0; tick < 60; tick++) {
            boolean onSlot = context.computeOnClient(client -> {
                AbstractContainerScreen<?> screen = containerScreen(client);
                return hoveredSlot(screen) == screen.getMenu().slots.get(slotIndex);
            });

            if (onSlot) {
                return;
            }

            context.waitTick();
        }

        throw new AssertionError("The mouse never landed on menu slot " + slotIndex
                + " at GUI position " + centre[0] + "/" + centre[1] + ".");
    }

    /**
     * Moves the cursor to a position in GUI coordinates. The input API takes raw window pixels,
     * while everything a screen sees is divided by the GUI scale, so the conversion has to happen
     * here - at GUI scale 2 a slot centre of 60 is window pixel 120.
     */
    private void moveCursorToGui(ClientGameTestContext context, double guiX, double guiY) {
        double[] raw = context.computeOnClient(client -> new double[] {
                guiX * client.getWindow().getScreenWidth() / (double) client.getWindow().getGuiScaledWidth(),
                guiY * client.getWindow().getScreenHeight() / (double) client.getWindow().getGuiScaledHeight()
        });

        context.getInput().setCursorPos(raw[0], raw[1]);
        context.waitTicks(5);
    }

    /**
     * Shared tail of every control step: the picture has to be back at the baseline. The allowance
     * is the same one the other renderer tests use - four times the measured noise floor plus a
     * small absolute margin scaled to the window.
     */
    private void assertBackToBaseline(ScreenshotDiff.Diff residual, ScreenshotDiff.Diff noiseFloor, String step) {
        int allowed = Math.max(noiseFloor.changedPixels() * 4 + 200, residual.totalPixels() / 20000);

        if (residual.changedPixels() > allowed) {
            throw new AssertionError("Control step failed: " + step + " did not restore the baseline "
                    + "image (" + residual + ", allowed " + allowed + " pixels). The measured "
                    + "difference cannot be attributed to the trigger.");
        }
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
}
