package com.simplebuilding.clientgametest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.client.ClientState;
import com.simplebuilding.client.gui.BuildingWandScreen;
import com.simplebuilding.client.gui.NetheriteHopperScreen;
import com.simplebuilding.client.gui.OctantScreen;
import com.simplebuilding.client.gui.TrimReferenceScreen;
import com.simplebuilding.client.gui.widget.CyclingTrimButton;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItemGroups;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.OctantItem;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;

/**
 * Opens the mod's screens in a running client and checks that they appear, survive twenty ticks and
 * close again. A screen that throws while initialising or rendering crashes the client, which fails
 * the whole client test run - so "still on screen after 20 ticks" is a real statement about init and
 * render, not a tautology.
 *
 * <p>Every screen that has a real trigger path is opened through it, because the trigger is client
 * only code that no server test can reach:
 * <ul>
 *   <li>the octant screen via the mod's settings key binding with an octant in the main hand,</li>
 *   <li>the building wand screen via the same key binding, which the mod gates on Constructor's
 *       Touch - both the refused and the accepted case are covered,</li>
 *   <li>the netherite hopper menu via an actual right click on a placed netherite hopper, which
 *       also exercises the server side menu opening and the menu screen registration,</li>
 *   <li>the trim reference screen via a real mouse click on the button that
 *       {@code SmithingScreenMixin} injects into the vanilla smithing screen,</li>
 *   <li>the creative inventory via the inventory key, which is also the only place the mod's
 *       creative tab is ever populated.</li>
 * </ul>
 * The building wand screen is additionally opened directly once, as a plain construction smoke test
 * that does not depend on the key binding or the enchantment being present. The config screen is
 * built from the registered config holder; see below for what the shared form had to give up there.
 *
 * <h2>Why this class takes screenshots but compares none</h2>
 *
 * <p>The renderer tests in this suite ({@link BlockHighlightClientTest},
 * {@link BuildingWandPreviewClientTest}) are difference tests, and their order is their argument: a
 * noise floor from two shots of the untouched scene first, then every trigger condition of the
 * branch under test, then the measuring shot, then a control shot back at the baseline. Without all
 * four beats a pixel difference proves nothing.
 *
 * <p>This class makes no pixel claim at all, so it has none of those four beats and must not
 * pretend to. Its assertions are made on the client's own state - which screen object is open, what
 * the widget arithmetic produced, what the creative tab offers - and every one of them would hold
 * on a completely black frame. The nine screenshots are checkpoints for the test runner (it counts
 * a fresh file per name and thereby knows how far the run got) and evidence for a human reading the
 * artefacts afterwards, nothing more.
 *
 * <p>A pixel comparison would also be wrong here rather than merely absent: {@code CyclingTrimButton}
 * picks its icon from {@code System.currentTimeMillis() / 1000 % items.size()}, so which trim
 * template is on the smithing screen depends on wall clock time, and no two runs draw the same
 * frame. What is kept from that methodology is its second beat, and that one is kept everywhere:
 * every trigger condition is asserted before the thing it triggers is looked at, so a setup that
 * quietly failed can never be reported as a broken screen.
 *
 * <h2>How the smithing button position is pinned</h2>
 *
 * <p>The mixin places its 20x20 button at {@code guiLeft - 25} / {@code guiTop + 5}. The test
 * computes the same two numbers from the screen size and clicks the resulting point with the real
 * cursor, so a button that moved would simply not be hit and the reference screen would not open.
 * {@code guiLeft} and {@code guiTop} are vanilla's centring of a 176x166 container GUI
 * ({@code AbstractContainerScreen} defaults, unchanged by {@code SmithingScreen}); that part of the
 * claim is carried by vanilla, not by the mod.
 *
 * <h2>Known defect (deliberately not pinned)</h2>
 *
 * <p>{@code TrimReferenceScreen} lists hard coded base values that disagree with the server side
 * numbers for at least Enderite, Netherite and Rib (for example Netherite "Amplifier" 5.0 in the
 * screen). This test only proves the screen opens and renders; asserting the numbers it currently
 * shows would cement the disagreement.
 *
 * <h2>Known defect (deliberately not pinned)</h2>
 *
 * <p>The Fabric ModMenu config button throws on this Minecraft line: {@code ModMenuIntegration}
 * looks {@code getConfigScreen} up reflectively on {@code AutoConfig}
 * ({@code AutoConfig.class.getMethod("getConfigScreen", Class.class, Screen.class)}), and Cloth
 * Config 26.2.155 has moved that method onto {@code AutoConfigClient}. The lookup therefore throws
 * {@code NoSuchMethodException}, the factory turns it into {@code IllegalStateException: Failed to
 * open Simplebuilding config screen}, and pressing the config button in ModMenu does not open the
 * settings. Only that one lookup is affected - the mod's other Cloth Config calls are
 * {@code AutoConfig.register} and {@code AutoConfig.getConfigHolder}, and both still sit on
 * {@code AutoConfig} in 26.2.155. The 1.21.11 line is not affected either (Cloth Config 21.11.153
 * still carries {@code getConfigScreen} on {@code AutoConfig}), which is why nothing but a 26.2
 * client run could have noticed it.
 *
 * <h2>What the port to the shared step form changed, and what it did not</h2>
 *
 * <p>The nine screenshot names, every assertion and every assertion message of the Fabric-only
 * version are the ones below. Five things are different, and each one is named here rather than
 * left to be discovered:
 * <ul>
 *   <li><b>The mod's own config screen entry points are no longer exercised.</b> Both of them are
 *       loader specific and neither exists in shared code: on Fabric it is
 *       {@code ModMenuIntegration#getModConfigScreenFactory} (ModMenu is not on the NeoForge
 *       classpath at all), on NeoForge the {@code IConfigScreenFactory} extension point plus
 *       {@code SimplebuildingNeoForgeClient#buildConfigScreen}. What is shared is the layer under
 *       both of them, and that is what {@link #modConfigScreenBuildsAndRenders} now uses:
 *       {@code AutoConfigClient.getConfigScreen(SimplebuildingConfig.class, null)}, which is in
 *       both cloth-config-fabric and cloth-config-neoforge 26.2.155. The claim that survives is
 *       "the config holder is registered and Cloth Config can turn it into a screen that
 *       initialises and renders". The claim that is lost is "the button a player presses reaches
 *       that screen" - including the reflective lookup defect above, which the Fabric-only class
 *       (kept, not deleted) still covers until a loader hook hands the entry point to shared code.
 *       Checkpoint {@code screen-h-mod-config} is unchanged, so the runner's count does not move;
 *       what it stands for is weaker, and this paragraph is where that is written down.</li>
 *   <li><b>Key presses are raw GLFW codes.</b> The Fabric-only version handed over the key
 *       <em>binding</em> and let the framework resolve it; the shared {@link Harness} takes a code,
 *       because that is the only thing both loaders can serve (NeoForge drives input through
 *       {@code KeyMapping.set/click} on {@code InputConstants.Type.KEYSYM}). A code silently
 *       assumes the binding still sits on that key, so {@link #assertSettingsKeyIsBound} and
 *       {@link #assertInventoryKeyIsBound} are new and not optional: without them a rebound key
 *       would leave every screen unopened and the mod would be reported as broken.</li>
 *   <li><b>Every case sets up what it needs.</b> The Fabric-only version built the scene once and
 *       let the cases inherit whatever the previous one had left in the hand, in the world and in
 *       front of the crosshair. All client tests share one world, so that is a defect even where it
 *       used to work: each case below clears the inventory itself, places its own block, aims the
 *       player itself and asserts the aim before it clicks. The clears in the setup are the
 *       tolerant form on purpose - a setup clear that fails because the case before it happened to
 *       leave an empty inventory would be exactly the order dependency this removes.</li>
 *   <li><b>The controls throw directly</b> instead of returning a problem string to a caller that
 *       throws. An {@code act} step already runs on the client thread and an exception in it fails
 *       the test naming the step, so the string relay had nothing left to do. The messages are
 *       unchanged. Three controls are new beyond the two key bindings, all of the same kind - they
 *       turn a failed setup into a setup failure instead of a screen failure: the octant really is
 *       in the main hand, the netherite hopper and the smithing table really are in the world, and
 *       the player really is in creative before the inventory key is pressed.</li>
 *   <li><b>The cleanup is a list of ordinary last steps.</b> A step list has no place to hang a
 *       {@code finally} block, so {@link #putTheWorldBack} runs as the final steps and <b>does not
 *       run when a step above it fails</b>. What it puts back: the block this class placed at
 *       {@link #INTERACTION_POS}, the inventory, the player's position and view angles, and the
 *       hidden HUD. All four are also rebuilt by the next {@link TestScene#build}, so a failure
 *       leaves nothing behind that the next test would notice - which is why this cleanup is a
 *       courtesy, unlike the config restore in {@link BlockHighlightClientTest}, where nothing else
 *       ever resets the values.</li>
 * </ul>
 *
 * <h2>Not covered</h2>
 *
 * <p>The cycling icon of the smithing button: it is picked from the wall clock (see above), so no
 * screenshot of it can be compared against anything. That the button cycles through
 * {@code #minecraft:trim_templates} is therefore only covered up to "the tag is read and the button
 * exists at the position the mixin computes".
 *
 * <p>The two loader specific config screen entry points, as described above.
 *
 * <p>That the octant screen and the wand screen <em>do</em> something - both are opened, rendered
 * and closed; none of their widgets is operated. Operating them needs mouse events delivered into a
 * screen, which is the same missing harness capability the smithing button click needs.
 *
 * <h2>What the NeoForge harness still owes this test</h2>
 *
 * <p>{@link #smithingTrimReferenceButton} is the first shared test that moves the cursor and clicks
 * a widget. Today {@code Harness.setCursorPos} throws {@code UnsupportedOperationException} on
 * NeoForge (vanilla's {@code MouseHandler.onMove} is private and the driver has no accessor mixin
 * for it yet), and even with the cursor moved, that driver's clicks go through
 * {@code KeyMapping.click}, which feeds the <em>binding</em> layer - screens read events, not
 * bindings, so a bound click never reaches {@code Screen.mouseClicked}. Both holes are in the
 * driver, not in this body, and both fail loudly rather than silently: that is why the steps are
 * written here as they are instead of being left out. The Fabric driver serves both today.
 */
public final class ModScreensClientTest {

    /**
     * The z plane one block in front of the wall - free air.
     *
     * <p>Derived from {@link TestScene#WALL_Z} instead of written out, so it cannot drift away from
     * the scene. Same value the Fabric-only scene called {@code FRONT_Z}.
     */
    private static final int FRONT_Z = TestScene.WALL_Z - 1;

    /**
     * Where both interaction cases put their block.
     *
     * <p>One position for the hopper and the smithing table, exactly as the Fabric-only version had
     * it (it kept two constants with the same value). It stands in front of the wall, so the block
     * is free standing and the crosshair reaches its north face from the fixed player spot.
     */
    private static final BlockPos INTERACTION_POS = new BlockPos(10, 1, FRONT_Z);

    /**
     * The player spot {@link TestScene#build} teleports to.
     *
     * <p>Repeated here because the two aiming cases have to teleport the player themselves and must
     * land on the very same spot; a different spot would move the crosshair and the aim assertion
     * would fail for a reason that has nothing to do with the screens.
     */
    private static final String PLAYER_SPOT = "10.5 0.0 16.5";

    /** Vanilla container GUI size, see {@code AbstractContainerScreen}'s two argument constructor. */
    private static final int CONTAINER_WIDTH = 176;
    private static final int CONTAINER_HEIGHT = 166;

    /** Offsets {@code SmithingScreenMixin} uses for its button, relative to the GUI's top left. */
    private static final int BUTTON_OFFSET_X = -25;
    private static final int BUTTON_OFFSET_Y = 5;
    private static final int BUTTON_SIZE = 20;

    /**
     * The GLFW key code the settings key steps press.
     *
     * <p>Both loaders register {@code key.simplebuilding.simple_settings} on G
     * ({@code SimplebuildingClient} on Fabric, {@code SimplebuildingNeoForgeClient} on NeoForge).
     * {@link #assertSettingsKeyIsBound} is what keeps that assumption honest at run time.
     */
    private static final int SETTINGS_KEY = InputConstants.KEY_G;

    /** Vanilla's default inventory key; checked against the binding by {@link #assertInventoryKeyIsBound}. */
    private static final int INVENTORY_KEY = InputConstants.KEY_E;

    /**
     * How long a screen may take to appear.
     *
     * <p>Generous on purpose: the hopper menu is opened by the <em>server</em> (right click, menu
     * provider, open packet back to the client), so this budget has to cover a round trip and not
     * just a frame. A screen that has not appeared within five seconds is not slow, it is missing,
     * and the diagnosis printed on timeout says which screen was open instead.
     */
    private static final int SCREEN_TIMEOUT_TICKS = 100;

    /**
     * How long every opened screen has to stay on screen before it is photographed.
     *
     * <p>Twenty ticks is the whole point of the class: init runs once, render runs every frame, and
     * a screen that throws in either takes the client down with it. One tick would only prove that
     * the object was constructed.
     */
    private static final int RENDER_TICKS = 20;

    /** Every chisel a creative player has to be able to take out of the mod's tab. */
    private static final List<Item> CHISELS = List.of(
            ModItems.STONE_CHISEL,
            ModItems.COPPER_CHISEL,
            ModItems.IRON_CHISEL,
            ModItems.GOLD_CHISEL,
            ModItems.DIAMOND_CHISEL,
            ModItems.NETHERITE_CHISEL,
            ModItems.ENDERITE_CHISEL);

    /** The legacy spatulas: still registered for old worlds, deliberately kept out of the tab. */
    private static final List<Item> SPATULAS = List.of(
            ModItems.STONE_SPATULA,
            ModItems.COPPER_SPATULA,
            ModItems.IRON_SPATULA,
            ModItems.GOLD_SPATULA,
            ModItems.DIAMOND_SPATULA,
            ModItems.NETHERITE_SPATULA);

    private ModScreensClientTest() {
    }

    /**
     * The whole test, as steps.
     *
     * <p>The scene is built once here and then kept intact by the cases themselves - each of them
     * restores what it changed, and each of them establishes what it needs rather than inheriting
     * it. The case order below is the Fabric-only one (which is why the checkpoint letters are not
     * in alphabetical order); nothing depends on it any more, and that is the point.
     */
    public static void inWorld(Script script) {
        TestScene.build(script, "minecraft:stone", "creative");
        // Screens are unaffected by the hidden HUD, but the screenshots read better with it.
        TestScene.showHudAgain(script);

        octantScreenViaSettingsKey(script);
        buildingWandScreenConstructsAndRenders(script);
        buildingWandScreenNeedsConstructorsTouch(script);
        netheriteHopperMenu(script);
        smithingTrimReferenceButton(script);
        modConfigScreenBuildsAndRenders(script);
        creativeInventoryShowsTheModTab(script);

        putTheWorldBack(script);
    }

    /**
     * The octant screen, opened the way a player opens it: the settings key with an octant in the
     * main hand.
     *
     * <p>The item is given here rather than inherited, and that it really arrived on the client is
     * asserted before the key is pressed - the mod's key handler reads the main hand item, so an
     * item that never arrived and a key handler that stopped looking at the main hand would produce
     * exactly the same "no screen opened".
     *
     * <p><b>What breaks this test:</b> the settings binding being dropped or moved, the octant
     * branch of the key handler being lost, and {@code OctantScreen} throwing in its constructor,
     * in {@code init} or in {@code render}.
     */
    private static void octantScreenViaSettingsKey(Script script) {
        clearTheHand(script);
        script.command("item replace entity @a weapon.mainhand with simplebuilding:octant");
        script.awaitPackets();
        script.idle("let the octant arrive and its equip animation finish", 10);

        script.act("the main hand really holds an octant", client -> {
            if (client.player == null
                    || !(client.player.getMainHandItem().getItem() instanceof OctantItem)) {
                throw new AssertionError("Octant setup failed: the main hand does not hold an "
                        + "OctantItem but "
                        + (client.player == null ? "there is no player" : client.player.getMainHandItem())
                        + ", so the settings key would have nothing to open a screen for.");
            }
        });

        pressSettingsKey(script, "with an octant in the main hand");
        awaitScreen(script, OctantScreen.class, "octant screen");
        script.idle("let the octant screen render " + RENDER_TICKS + " frames", RENDER_TICKS);

        assertStillOpen(script, OctantScreen.class, "octant screen");
        script.shot("screen-a-octant");
        closeScreen(script, "octant screen");
    }

    /**
     * The wand screen built directly, without the key binding and without the enchantment.
     *
     * <p>This is the plain construction smoke test: it separates "the screen class is broken" from
     * "the path to the screen is broken", which the two cases below cannot do on their own. The
     * stack is built on the client thread and handed to the screen the same way the mod does it.
     */
    private static void buildingWandScreenConstructsAndRenders(Script script) {
        script.act("open the building wand screen directly", client ->
                client.gui.setScreen(new BuildingWandScreen(new ItemStack(ModItems.NETHERITE_BUILDING_WAND))));

        awaitScreen(script, BuildingWandScreen.class, "building wand screen");
        script.idle("let the building wand screen render " + RENDER_TICKS + " frames", RENDER_TICKS);

        assertStillOpen(script, BuildingWandScreen.class, "building wand screen");
        script.shot("screen-b-building-wand");
        closeScreen(script, "building wand screen");
    }

    /**
     * The settings key opens the wand screen only for a wand that carries Constructor's Touch. Both
     * halves of that condition are checked in one go: first the plain wand, which must leave the key
     * press without effect, then the same wand after {@code /enchant}, which must open the screen.
     *
     * <p>The enchantment is applied with the vanilla {@code /enchant} command rather than an item
     * component literal on purpose - the component syntax for enchantments has changed repeatedly
     * between Minecraft versions, while the command has not, and the test verifies on the client
     * that the enchantment really arrived before it presses the key.
     *
     * <p>The negative half is the one that needs the twenty idle ticks: "no screen opened" is only
     * a statement if the screen had time to open. It is also the half that decides what the
     * positive half proves - without it, a key handler that opens the wand screen unconditionally
     * would pass just as well.
     *
     * <p><b>What breaks this test:</b> dropping the Constructor's Touch check in the settings key
     * handler (the first half fails), losing the wand branch of the handler or the screen's
     * constructor throwing (the second half fails), and any rebinding of the key that leaves
     * {@code ClientState.settingsKey} unregistered.
     */
    private static void buildingWandScreenNeedsConstructorsTouch(Script script) {
        clearTheHand(script);
        script.command(
                "item replace entity @a weapon.mainhand with simplebuilding:netherite_building_wand");
        script.awaitPackets();
        script.idle("let the wand arrive and its equip animation finish", 10);

        assertMainHandWand(script, false);

        pressSettingsKey(script, "with a wand that has no Constructor's Touch");
        script.idle("give a screen that must not open " + RENDER_TICKS + " ticks to appear", RENDER_TICKS);

        script.act("the settings key opened nothing for a wand without Constructor's Touch", client -> {
            String opened = describeScreen(client);

            if (!opened.equals("none")) {
                throw new AssertionError("The settings key opened " + opened + " for a building wand without "
                        + "Constructor's Touch. The mod only allows the wand screen for an enchanted wand.");
            }
        });

        script.shot("screen-d-wand-without-touch");

        script.command("enchant @a simplebuilding:constructors_touch 1");
        script.awaitPackets();
        script.idle("let the enchanted wand reach the client", 10);

        assertMainHandWand(script, true);

        pressSettingsKey(script, "with a wand that carries Constructor's Touch");
        awaitScreen(script, BuildingWandScreen.class, "building wand screen via the settings key");
        script.idle("let the building wand screen render " + RENDER_TICKS + " frames", RENDER_TICKS);

        assertStillOpen(script, BuildingWandScreen.class, "building wand screen via the settings key");
        script.shot("screen-e-wand-with-touch");
        closeScreen(script, "building wand screen via the settings key");
    }

    /**
     * The netherite hopper menu, opened by right clicking the placed block.
     *
     * <p>This is the only case that crosses to the server and back: the click reaches
     * {@code use} on the block, the server opens the menu and sends it, and the client turns the
     * menu id into {@code NetheriteHopperScreen} through the loader's screen registration
     * (Fabric's {@code MenuScreens} registration, NeoForge's {@code RegisterMenuScreensEvent}). A
     * missing registration is one of the few mod defects that only shows up as a client crash, so
     * the round trip is the point and a directly constructed screen would not replace it.
     *
     * <p>The hand is emptied first: a held item would let vanilla's use-item path win over the
     * block interaction.
     */
    private static void netheriteHopperMenu(Script script) {
        clearTheHand(script);
        script.command("setblock " + INTERACTION_POS.getX() + " " + INTERACTION_POS.getY() + " "
                + INTERACTION_POS.getZ() + " simplebuilding:netherite_hopper");
        script.awaitPackets();
        script.idle("let the placed hopper reach the client", 10);

        script.act("the netherite hopper really stands in the world", client -> {
            if (client.level == null
                    || !client.level.getBlockState(INTERACTION_POS).is(ModBlocks.NETHERITE_HOPPER)) {
                throw new AssertionError("Netherite hopper setup failed: block " + INTERACTION_POS + " is "
                        + (client.level == null ? "in no level" : client.level.getBlockState(INTERACTION_POS))
                        + ", so the right click below would open nothing.");
            }
        });

        // Slightly upwards so the ray enters the hopper's top plate (y 0.625..1.0 of the block)
        // head on instead of grazing the funnel below it.
        aimAt(script, "0.0", "-4.0", INTERACTION_POS, Direction.NORTH);

        script.harness("right click the netherite hopper", harness -> harness.pressMouse(1));
        awaitScreen(script, NetheriteHopperScreen.class, "netherite hopper menu");
        script.idle("let the netherite hopper menu render " + RENDER_TICKS + " frames", RENDER_TICKS);

        assertStillOpen(script, NetheriteHopperScreen.class, "netherite hopper menu");
        script.shot("screen-c-netherite-hopper");
        closeScreen(script, "netherite hopper menu");

        clearTheInteractionSpot(script);
    }

    /**
     * The button {@code SmithingScreenMixin} adds to the vanilla smithing screen, clicked with the
     * real cursor at the position the mixin's own arithmetic produces.
     *
     * <p>The position is computed on the client from the current screen size and then asserted
     * against the widget the mixin really added, before anything is clicked. That order matters: a
     * click into empty space and a press handler that stopped opening the reference screen both end
     * as "no reference screen", and only the assertion tells them apart.
     *
     * <p>The cursor position is converted from GUI coordinates to window coordinates with vanilla's
     * own formula inverted ({@code MouseHandler} scales window to GUI as
     * {@code guiX = xpos * guiScaledWidth / screenWidth}), because the harness moves the cursor in
     * window coordinates. It is computed in an {@code act} step and read in the following
     * {@code harness} step: the value only exists at run time, and a step list registers every step
     * before any of them runs, so it travels in a {@link Later} rather than in a local variable.
     *
     * <p><b>On NeoForge these two input steps do not work yet</b> - see the class javadoc; they
     * fail with the driver's own message naming what is missing.
     *
     * <p><b>What breaks this test:</b> the mixin no longer applying (no button among the screen's
     * children), the button moving away from {@code guiLeft - 25 / guiTop + 5}, its press handler no
     * longer opening {@code TrimReferenceScreen}, and that screen throwing while it collects its
     * entries or renders them.
     */
    private static void smithingTrimReferenceButton(Script script) {
        clearTheHand(script);
        script.command("setblock " + INTERACTION_POS.getX() + " " + INTERACTION_POS.getY() + " "
                + INTERACTION_POS.getZ() + " minecraft:smithing_table");
        script.awaitPackets();
        script.idle("let the placed smithing table reach the client", 10);

        script.act("the smithing table really stands in the world", client -> {
            if (client.level == null
                    || !client.level.getBlockState(INTERACTION_POS).is(Blocks.SMITHING_TABLE)) {
                throw new AssertionError("Smithing table setup failed: block " + INTERACTION_POS + " is "
                        + (client.level == null ? "in no level" : client.level.getBlockState(INTERACTION_POS))
                        + ", so the right click below would open nothing.");
            }
        });

        aimAt(script, "0.0", "0.0", INTERACTION_POS, Direction.NORTH);

        script.harness("right click the smithing table", harness -> harness.pressMouse(1));
        awaitScreen(script, SmithingScreen.class, "vanilla smithing screen");
        script.idle("let the smithing screen render " + RENDER_TICKS + " frames", RENDER_TICKS);

        assertStillOpen(script, SmithingScreen.class, "vanilla smithing screen");
        script.shot("screen-f-smithing-table");

        script.act("the mixin added exactly one trim button where its arithmetic says", client -> {
            String problem = trimButtonProblem(client);

            if (problem != null) {
                throw new AssertionError("SmithingScreenMixin did not add its reference button: " + problem);
            }
        });

        Later<double[]> cursor = new Later<>("the window position of the trim button's centre");

        script.act("compute the window position of the trim button's centre", client -> {
            Screen screen = client.gui.screen();

            if (screen == null) {
                throw new AssertionError("The smithing screen closed between the button check and the "
                        + "cursor computation, so there is nothing to click.");
            }

            double guiX = (screen.width - CONTAINER_WIDTH) / 2.0 + BUTTON_OFFSET_X + BUTTON_SIZE / 2.0;
            double guiY = (screen.height - CONTAINER_HEIGHT) / 2.0 + BUTTON_OFFSET_Y + BUTTON_SIZE / 2.0;

            // MouseHandler scales window coordinates to GUI coordinates with
            // guiX = xpos * guiScaledWidth / screenWidth, so this is that formula inverted.
            cursor.set(new double[] {
                    guiX * client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth(),
                    guiY * client.getWindow().getScreenHeight() / client.getWindow().getGuiScaledHeight()
            });
        });

        script.harness("move the cursor onto the trim button", harness ->
                harness.setCursorPos(cursor.get()[0], cursor.get()[1]));
        script.idle("let the moved cursor be picked up by the screen", 2);
        script.harness("click the trim button", harness -> harness.pressMouse(0));
        script.idle("let the trim reference screen open", 10);

        assertStillOpen(script, TrimReferenceScreen.class, "trim reference screen");
        script.shot("screen-g-trim-reference");
        closeScreen(script, "trim reference screen");

        clearTheInteractionSpot(script);
    }

    /**
     * The config screen Cloth Config builds for {@code SimplebuildingConfig}, shown and rendered.
     *
     * <p>What this proves: {@code AutoConfig.register} has run (otherwise there is no config holder
     * and no screen), every field of {@code SimplebuildingConfig} is one Cloth Config can build a
     * widget for, and the resulting screen initialises and renders without taking the client down.
     * That is a client only claim - the screen needs the whole GUI stack - and no server test can
     * make it.
     *
     * <p>What this no longer proves, and the class javadoc says it again because it is the one real
     * loss of this port: that the button a player presses reaches this screen. Both entry points
     * are loader specific and invisible from shared code.
     *
     * <p>The screen is created in one step and shown in the next, with the object identity - not
     * the class - asserted afterwards: {@code AutoConfigClient} hands back a plain
     * {@code ConfigScreen}, and comparing classes would also accept a different screen of the same
     * class that something else had opened in between.
     */
    private static void modConfigScreenBuildsAndRenders(Script script) {
        Later<Screen> configScreen = new Later<>("the config screen Cloth Config built");

        script.act("build the config screen from the registered config holder", client -> {
            Screen screen = AutoConfigClient.getConfigScreen(SimplebuildingConfig.class, null).get();

            if (screen == null) {
                throw new AssertionError("Cloth Config built no screen for SimplebuildingConfig, so "
                        + "the config holder is not registered and the config button could not work "
                        + "even with the reflective lookup repaired.");
            }

            configScreen.set(screen);
        });

        script.act("show the config screen", client -> client.gui.setScreen(configScreen.get()));

        script.await("wait for the config screen", SCREEN_TIMEOUT_TICKS,
                client -> client.gui.screen() == configScreen.get(),
                client -> "the config screen never became the open screen; the current screen is "
                        + describeScreen(client));

        script.idle("let the config screen render " + RENDER_TICKS + " frames", RENDER_TICKS);

        script.act("the config screen stayed open", client -> {
            if (client.gui.screen() != configScreen.get()) {
                throw new AssertionError("The config screen did not stay open: current screen is "
                        + describeScreen(client));
            }
        });

        script.shot("screen-h-mod-config");
        closeScreen(script, "config screen");
    }

    /**
     * The creative inventory, opened with the inventory key, and what the mod's own tab offers in
     * it.
     *
     * <p>This is not the same claim as the server side {@code DataIntegrityTests}, which calls
     * {@code ModItemGroupsContent#populate} directly. A registered tab and a populated tab are two
     * different things: {@code CreativeModeTab#getDisplayItems} stays empty until
     * {@code CreativeModeTabs#tryRebuildTabContents} has run, and the only place that happens is
     * {@code CreativeModeInventoryScreen}'s constructor - on the client. Opening the screen first
     * and reading the tab afterwards therefore covers the whole chain: the tab is registered, the
     * loader's builder wired {@code ModItemGroupsContent#populate} into it, and the screen it is
     * shown in initialises without throwing.
     *
     * <p>Two opposite statements are made about the contents, so neither can pass by accident: all
     * seven chisels have to be there, and none of the six legacy spatulas may be. The spatulas are
     * the deliberate half of the chisel rename - they stay in the registry so old worlds keep their
     * stacks, but a player starting today is meant to find only the chisels. The empty tab is
     * caught separately and reported as such, because an empty tab would satisfy the second
     * statement while proving nothing.
     *
     * <p>The game mode check before the key press is new and is there because the key is: in
     * survival the same key opens the survival inventory, and "the creative inventory did not stay
     * open" would be a true but thoroughly misleading message.
     *
     * <p>What breaks this test: the tab registration dropped or renamed, {@code populate} throwing
     * or returning early (which shows up as every chisel missing at once), a dropped
     * {@code entries.accept} line, or a spatula being added to the tab without the rename decision
     * being revisited.
     */
    private static void creativeInventoryShowsTheModTab(Script script) {
        clearTheHand(script);
        script.awaitPackets();
        script.idle("let the cleared inventory reach the client", 10);

        script.act("the player is in creative, which is what the inventory key needs", client -> {
            if (client.gameMode == null || !client.gameMode.getPlayerMode().isCreative()) {
                throw new AssertionError("Creative inventory setup failed: the player is in game mode "
                        + (client.gameMode == null ? "?" : client.gameMode.getPlayerMode())
                        + ", so the inventory key opens the survival inventory instead.");
            }
        });

        assertInventoryKeyIsBound(script);
        script.harness("press the inventory key", harness -> harness.pressKey(INVENTORY_KEY));

        awaitScreen(script, CreativeModeInventoryScreen.class, "creative inventory");
        script.idle("let the creative inventory render " + RENDER_TICKS + " frames", RENDER_TICKS);

        assertStillOpen(script, CreativeModeInventoryScreen.class, "creative inventory");
        script.shot("screen-i-creative-inventory");

        script.act("the mod's creative tab offers every chisel and no spatula", client -> {
            List<String> problems = creativeTabProblems();

            if (!problems.isEmpty()) {
                throw new AssertionError("The mod's creative tab shows the wrong items: " + problems);
            }
        });

        closeScreen(script, "creative inventory");
    }

    /** @return everything wrong with the mod's tab, empty when it offers exactly what it should */
    private static List<String> creativeTabProblems() {
        List<String> found = new ArrayList<>();
        Collection<ItemStack> offered = ModItemGroups.BUILDING_ITEMS_GROUP.getDisplayItems();

        if (offered.isEmpty()) {
            found.add("the mod's creative tab is empty after the creative inventory was opened, "
                    + "so neither of the two checks below could fail");
            return found;
        }

        Set<Item> inTheTab = new HashSet<>();

        for (ItemStack stack : offered) {
            inTheTab.add(stack.getItem());
        }

        for (Item chisel : CHISELS) {
            if (!inTheTab.contains(chisel)) {
                found.add(chisel.getDescriptionId() + " is missing from the tab, so a creative "
                        + "player cannot take it out of the inventory at all");
            }
        }

        for (Item spatula : SPATULAS) {
            if (inTheTab.contains(spatula)) {
                found.add(spatula.getDescriptionId() + " is offered in the tab; the six legacy "
                        + "spatulas are kept registered for old worlds but deliberately hidden "
                        + "from new players");
            }
        }

        return found;
    }

    /**
     * What is wrong with the button the mixin should have added, or null when it is exactly right.
     *
     * <p>Both halves are needed. The position check is what makes the click below meaningful; the
     * "exactly one" check is what keeps a mixin that applies twice - once per loader path, say -
     * from passing on the strength of its first button while a second one sits somewhere else.
     */
    private static String trimButtonProblem(Minecraft client) {
        Screen screen = client.gui.screen();

        if (screen == null) {
            return "no screen open";
        }

        int expectedX = (screen.width - CONTAINER_WIDTH) / 2 + BUTTON_OFFSET_X;
        int expectedY = (screen.height - CONTAINER_HEIGHT) / 2 + BUTTON_OFFSET_Y;
        int found = 0;
        StringBuilder positions = new StringBuilder();

        for (GuiEventListener child : screen.children()) {
            if (child instanceof CyclingTrimButton button) {
                found++;
                positions.append(" [").append(button.getX()).append(",").append(button.getY())
                        .append(" ").append(button.getWidth()).append("x").append(button.getHeight())
                        .append("]");

                if (button.getX() != expectedX || button.getY() != expectedY
                        || button.getWidth() != BUTTON_SIZE || button.getHeight() != BUTTON_SIZE) {
                    return "the trim button sits at " + button.getX() + "," + button.getY() + " sized "
                            + button.getWidth() + "x" + button.getHeight() + ", expected " + expectedX
                            + "," + expectedY + " sized " + BUTTON_SIZE + "x" + BUTTON_SIZE
                            + " (25 pixels left of a centred " + CONTAINER_WIDTH + "x" + CONTAINER_HEIGHT
                            + " GUI on a " + screen.width + "x" + screen.height + " screen)";
                }
            }
        }

        if (found != 1) {
            return "expected exactly one CyclingTrimButton among the smithing screen's children but "
                    + "found " + found + ":" + positions;
        }

        return null;
    }

    /**
     * Checks that the main hand holds a building wand and whether it carries Constructor's Touch.
     * The enchantment is read straight from the item's component, not through the mod's own helper,
     * so a broken helper cannot make this precondition pass.
     */
    private static void assertMainHandWand(Script script, boolean expectConstructorsTouch) {
        script.act("the main hand holds a building wand that is "
                + (expectConstructorsTouch ? "enchanted" : "plain"), client -> {
            String problem = mainHandWandProblem(client, expectConstructorsTouch);

            if (problem != null) {
                throw new AssertionError("Building wand setup failed: " + problem);
            }
        });
    }

    /** @return what is wrong with the wand in the main hand, or null when it is the wanted one */
    private static String mainHandWandProblem(Minecraft client, boolean expectConstructorsTouch) {
        if (client.player == null) {
            return "no client player";
        }

        ItemStack stack = client.player.getMainHandItem();

        if (!(stack.getItem() instanceof BuildingWandItem)) {
            return "main hand does not hold a BuildingWandItem but " + stack;
        }

        boolean touched = false;
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);

        if (enchantments != null) {
            for (Holder<Enchantment> holder : enchantments.keySet()) {
                if (holder.is(ModEnchantments.CONSTRUCTORS_TOUCH)) {
                    touched = true;
                }
            }
        }

        if (touched != expectConstructorsTouch) {
            return "the wand " + (touched ? "carries" : "does not carry") + " Constructor's Touch, "
                    + "but the test needs it " + (expectConstructorsTouch ? "enchanted" : "plain")
                    + " (stack: " + stack + ")";
        }

        return null;
    }

    /**
     * The mod's settings binding really is the key the harness presses.
     *
     * <p>New in the shared form and not optional: {@link Harness#pressKey} takes a raw GLFW code, so
     * a binding that moved would leave every screen below unopened and the mod's key handler would
     * be reported as broken. The null check in front of it covers the other half - a mod that never
     * registered the binding at all.
     */
    private static void assertSettingsKeyIsBound(Script script) {
        script.act("the settings binding sits on the key the harness presses", client -> {
            KeyMapping binding = ClientState.settingsKey;

            if (binding == null) {
                throw new AssertionError("ClientState.settingsKey is null, so the mod never registered "
                        + "its settings binding and no key press could ever open the octant or the "
                        + "building wand screen.");
            }

            if (!binding.matches(InputConstants.Type.KEYSYM.getOrCreate(SETTINGS_KEY))) {
                throw new AssertionError("The mod's settings binding is not on GLFW key " + SETTINGS_KEY
                        + " any more (it says \"" + binding.saveString() + "\"), so the key the harness "
                        + "presses would open nothing and the screens below would be reported as broken "
                        + "for a reason that has nothing to do with them.");
            }
        });
    }

    /** The same control for vanilla's inventory binding, for the same reason. */
    private static void assertInventoryKeyIsBound(Script script) {
        script.act("the inventory binding sits on the key the harness presses", client -> {
            if (!client.options.keyInventory.matches(InputConstants.Type.KEYSYM.getOrCreate(INVENTORY_KEY))) {
                throw new AssertionError("The inventory binding is not on GLFW key " + INVENTORY_KEY
                        + " any more (it says \"" + client.options.keyInventory.saveString() + "\"), so "
                        + "the key the harness presses would not open the creative inventory.");
            }
        });
    }

    /** Presses the mod's settings key, after checking that it is the key the mod listens to. */
    private static void pressSettingsKey(Script script, String why) {
        assertSettingsKeyIsBound(script);
        script.harness("press the settings key " + why, harness -> harness.pressKey(SETTINGS_KEY));
    }

    /**
     * Empties the inventory at the start of a case.
     *
     * <p>Tolerant on purpose. A strict {@code clear} fails when there is nothing to clear, and
     * whether there is something depends on what the case before this one left behind - which is
     * exactly the order dependency every case here is written to avoid. Where a clear is the thing
     * under test (as in {@link BlockHighlightClientTest}, which clears an item it has just given
     * and then photographs the result), the strict form is right; here it would only make the test
     * brittle.
     */
    private static void clearTheHand(Script script) {
        script.command("clear @a", true);
    }

    /** Takes the block this class placed back out of the world, so the next case starts in air. */
    private static void clearTheInteractionSpot(Script script) {
        script.command("setblock " + INTERACTION_POS.getX() + " " + INTERACTION_POS.getY() + " "
                + INTERACTION_POS.getZ() + " minecraft:air", true);
        script.awaitPackets();
        script.idle("let the cleared interaction spot reach the client", 10);
    }

    /**
     * Teleports the player onto the scene's fixed spot with the given view angles and waits until
     * the crosshair really reports the expected block and face.
     *
     * <p>The position is repeated rather than left alone, so a case that follows one which moved the
     * player still starts from the known spot. The assertion afterwards is what makes the click that
     * follows meaningful at all: an aim that silently missed produces the same "no screen opened" as
     * a broken menu.
     */
    private static void aimAt(Script script, String yaw, String pitch, BlockPos expected,
                             Direction expectedFace) {
        script.command("tp @a " + PLAYER_SPOT + " " + yaw + " " + pitch);
        script.awaitPackets();
        script.idle("let the new view angles reach the client", 10);
        TestScene.assertAimedAt(script, expected, expectedFace);
    }

    /** Waits until a screen of that class is the open one, and says what was open instead if not. */
    private static void awaitScreen(Script script, Class<? extends Screen> screenClass, String label) {
        script.await("wait for the " + label, SCREEN_TIMEOUT_TICKS,
                client -> screenClass.isInstance(client.gui.screen()),
                client -> "the " + label + " never opened; the current screen is " + describeScreen(client)
                        + ". " + TestScene.describeAim(client));
    }

    /**
     * Fails unless exactly that screen class is open right now.
     *
     * <p>Asserted on the exact class name rather than with {@code isInstance}, which is what the
     * Fabric-only version did and is the stronger statement: a subclass that replaced the screen
     * would satisfy an instance check while being a different screen than the one named.
     */
    private static void assertStillOpen(Script script, Class<? extends Screen> screenClass, String label) {
        script.act("the " + label + " is still open", client -> {
            String actual = describeScreen(client);

            if (!actual.equals(screenClass.getName())) {
                throw new AssertionError("The " + label + " did not stay open: current screen is " + actual);
            }
        });
    }

    /**
     * Closes whatever screen is open and waits until none is.
     *
     * <p>{@code setScreen(null)} is vanilla's own close path: it hands the screen its
     * {@code removed}, re-grabs the mouse and restores the toggle key states. That is why the cases
     * below can press keys and click blocks again afterwards without any further ceremony.
     */
    private static void closeScreen(Script script, String label) {
        script.act("close the " + label, client -> client.gui.setScreen(null));

        script.await("wait until no screen is open any more", SCREEN_TIMEOUT_TICKS,
                client -> client.gui.screen() == null,
                client -> "a screen is still open after closing the " + label + ": "
                        + describeScreen(client));

        script.idle("let the closed screen settle", 5);
    }

    /**
     * Puts back everything this class changed in the shared world and client.
     *
     * <p>This is the former cleanup, and it is now an ordinary list of steps: <b>it does not run
     * when a step above it failed.</b> Everything here is also redone by the next
     * {@link TestScene#build} - the volume is refilled, the inventory cleared, the player
     * teleported, the HUD hidden - so a failure leaves nothing behind that a later test would
     * notice. It is written out anyway, because "the next test rebuilds it" is a property of the
     * other tests and not of this one, and a test that relies on someone else tidying up is the
     * order dependency this class is otherwise free of.
     */
    private static void putTheWorldBack(Script script) {
        script.act("make sure no screen is left open", client -> client.gui.setScreen(null));
        clearTheInteractionSpot(script);
        script.command("clear @a", true);
        script.command("tp @a " + PLAYER_SPOT + " 0.0 0.0");
        script.awaitPackets();
        script.idle("let the restored world reach the client", 10);

        // Hides the HUD again and re-applies the frozen render options, so the client is left the
        // way TestScene.build leaves it rather than the way this class needed it.
        TestScene.makeRenderingDeterministic(script);
    }

    /** The open screen's class name, or "none" - the spelling every message here uses. */
    private static String describeScreen(Minecraft client) {
        return client.gui.screen() == null ? "none" : client.gui.screen().getClass().getName();
    }
}
