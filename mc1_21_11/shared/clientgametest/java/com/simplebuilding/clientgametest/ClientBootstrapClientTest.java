package com.simplebuilding.clientgametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.ClientState;
import com.simplebuilding.client.DoubleJumpController;
import com.simplebuilding.client.gui.BuildingWandScreen;
import com.simplebuilding.client.gui.OctantScreen;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.util.EnchantmentHelper;
import com.simplebuilding.util.ISpaceKeyTracker;
import com.simplebuilding.util.TrimBenefitUser;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.lwjgl.glfw.GLFW;

/**
 * Covers the client side bootstrap of the mod: the key mappings both loaders register, the client
 * tick loops hanging off them, the join listener that reports a config value to the server, the
 * {@code MouseMixin} that turns "modifier + wheel" into an octant scroll packet, and the
 * {@code MinecraftClientMixin} behind the pick block key.
 *
 * <p>None of this is reachable from a headless server test: key mappings, screens,
 * {@code MouseHandler.onScroll} and {@code pickBlockOrEntity} only exist on a client, and the
 * server side receivers they talk to are already covered by {@code NetworkHandlerTests}. What is
 * missing there - and what this class adds - is the proof that the client ever sends anything.
 *
 * <p><b>How the claims are made falsifiable.</b> This class takes no pixel measurements; there is
 * nothing to see on screen when a packet leaves. Instead every claim is anchored on a value that
 * is stored somewhere observable and that has a <em>different</em> value when the wiring is
 * missing:
 * <ul>
 *   <li>screens: {@code client.screen} - and every "no screen opened" step is bracketed by a
 *       step where the same key press does open a screen, so a key press that never arrived shows
 *       up as a red positive step instead of a green negative one;</li>
 *   <li>octant scroll: the {@code Pos1} / {@code Pos2} / {@code Shape} nbt of the server side item
 *       stack, plus the selected hotbar slot, which vanilla only moves when the mixin did
 *       <em>not</em> cancel the scroll event;</li>
 *   <li>the three payloads: the server side state they write
 *       ({@code TrimBenefitUser}, {@code ISpaceKeyTracker}, boot damage from
 *       {@code ModMessageHandlers.handleDoubleJump}), each of which starts out at a value the
 *       payload has to change.</li>
 * </ul>
 * Every precondition is asserted before the measurement for the same reason as in the screenshot
 * tests: an unchanged value must not be explainable by "the test never set the scene up
 * correctly". That order - control, then every trigger condition, then the measurement - is the
 * methodology the whole client suite is built on, and it is what makes a negative result mean
 * anything at all.
 *
 * <p><b>Why there is no {@link ScreenshotDiff} in this file.</b> Every claim here is a state
 * change behind the wire, and none of it is visible as pixels. The seven screenshots are
 * checkpoints for the runner and material for a human to look at; nothing is asserted on them, so
 * the {@link Later} handle {@link Script#shot} returns is deliberately dropped. Where a shared
 * test <em>does</em> compare pixels, the path has to come out of {@code shot(...)} and be read in
 * a later step, because Fabric writes {@code 0004_name.png} and NeoForge {@code name.png}; this
 * test simply has no such comparison.
 *
 * <p><b>The HUD stays visible for this whole test.</b> {@link TestScene#build} hides it, because
 * pixel tests need the first person hand gone; here the opposite is wanted - the two screen
 * screenshots read better with a hotbar under them, and no measurement in this file looks at
 * pixels. {@link TestScene#showHudAgain} is therefore called right after the scene is built, and
 * again as the last step.
 *
 * <p><b>What the shared form changed, and why it is not cosmetic.</b>
 * <ul>
 *   <li><b>Input is a raw GLFW code now.</b> The Fabric-only version handed the key
 *       <em>binding</em> to the framework ({@code pressKey(options -> options.keyJump)}); the
 *       shared {@link Harness} takes a code, because that is the only thing NeoForge can serve
 *       ({@code KeyMapping.set/click} on {@code InputConstants.Type.KEYSYM}). A raw code silently
 *       assumes the binding still sits on that key, so every key this test presses gets a
 *       {@link #assertBindingOnKey} control first. Without it a rebound or unbound key would turn
 *       a measurement into a no-op and the mod would be blamed for it.</li>
 *   <li><b>Server state is read through {@code Minecraft.getSingleplayerServer()}.</b> The shared
 *       harness has no {@code computeOnServer}: Fabric's is a framework service and NeoForge has
 *       nothing like it. The reads below therefore happen on the client thread, from the
 *       integrated server object. On Fabric that is exact - its framework runs the two tick loops
 *       against a barrier - while on NeoForge the server thread runs concurrently and a read can
 *       be one tick stale or, for a compound value, torn. Every such read sits inside an
 *       {@code await} that is polled once per tick, so a stale read costs a tick and nothing else.
 *       It is written down here rather than papered over.</li>
 *   <li><b>Server <em>writes</em> go through {@code server.execute}.</b> The three places that
 *       built an item in code rather than through command component syntax still do, because the
 *       component names are the part most likely to move between versions - but they now hand the
 *       work to the server thread and the following step waits for the observable outcome. A
 *       failure inside such a task is swallowed by the server's task queue, so the wait that
 *       follows it is not optional: it is what turns a silently skipped setup into a named
 *       timeout.</li>
 * </ul>
 *
 * <p><b>Known defect</b> (reported, not written into an assertion):
 * <ul>
 *   <li>{@code key.simplebuilding.toggle_octant_figure} and {@code key.simplebuilding.toggle_highlight}
 *       flip the very same field, {@code ClientState.showHighlights}, and that field gates only the
 *       octant area fill in {@code BlockHighlightRenderer}. So the key named "toggle highlight"
 *       does not touch the sledgehammer highlight it is named after, and the octant figure key is
 *       a second, unbound copy of it. Only the bound key's documented effect is asserted below;
 *       the duplication is deliberately left unpinned.</li>
 *   <li>{@code ClientToggleKeys} exists to keep both loaders on one implementation and NeoForge
 *       calls it, but Fabric never does - it keeps an inline copy in
 *       {@code SimplebuildingClient.onInitializeClient}. The copies already differ: the shared
 *       class returns early when {@code client.player == null}, the Fabric copy flips
 *       {@code showHighlights} anyway and only skips the actionbar message. The toggle assertion
 *       below runs with a player present, so it passes on both and does not pin the difference.</li>
 * </ul>
 *
 * <p><b>Not covered</b>, with the reason:
 * <ul>
 *   <li><b>The three HUD layers</b> attached during client initialisation. Neither loader offers a
 *       lookup for attached HUD elements, and each overlay draws nothing while its feature is
 *       idle, so "the layer is attached" cannot be told apart from "the layer drew nothing" from
 *       the outside. The overlays' own output belongs to the HUD test class.</li>
 *   <li><b>Pressing {@code key.simplebuilding.toggle_octant_figure}.</b> It ships bound to
 *       {@code InputConstants.UNKNOWN}, and an unbound mapping has no key code to press. Only its
 *       registration and its unbound state are asserted, as the tripwire for this note.</li>
 *   <li><b>"{@code SpaceKeyPayload} is sent only when the key state changes."</b> The server keeps
 *       just the last value, so a redundant send is invisible from the outside; proving the
 *       bandwidth guard would need a counter inside the mod.</li>
 *   <li><b>A join that reports the config default.</b> {@link #beforeWorld} has to send a value
 *       that differs from the server side default; a client that sends nothing and a client that
 *       sends the default are indistinguishable on the server.</li>
 *   <li><b>The actionbar text of the toggle keys</b> ("Highlights: ON"). It is an overlay message
 *       with a two second lifetime; asserting it belongs to a HUD pixel test, not here.</li>
 *   <li><b>That the same key press is what opens the screen on both loaders' own code path.</b>
 *       Fabric drains {@code settingsKey.consumeClick()} inline in its own tick listener, NeoForge
 *       in {@code SimplebuildingNeoForgeClient.onClientTick}. Both are exercised by running this
 *       body on both targets, which is the whole point of the shared form - but a target that is
 *       never run proves nothing, and that is a property of the run list, not of this file.</li>
 * </ul>
 */
public final class ClientBootstrapClientTest {

    // ------------------------------------------------------------------------------------------
    // Slots, positions and counts
    // ------------------------------------------------------------------------------------------

    /** First non hotbar inventory slot - where the reinforced bundle goes. */
    private static final int BUNDLE_SLOT = 9;

    /** Hotbar slot the tests keep the item under test in. */
    private static final int HOTBAR_SLOT = 0;

    /** Stone put into the bundle for the Master Builder pick. */
    private static final int BUNDLED_STONE_COUNT = 4;

    /**
     * The z plane one block in front of the wall.
     *
     * <p>Derived from {@link TestScene#WALL_Z} rather than written out, so a scene that ever moves
     * its wall takes the octant corners with it instead of leaving them embedded in stone.
     */
    private static final int FRONT_Z = TestScene.WALL_Z - 1;

    /** Octant corners. Only their nbt is read here, but free air keeps them out of the wall. */
    private static final BlockPos OCTANT_POS_1 = new BlockPos(9, 1, FRONT_Z);
    private static final BlockPos OCTANT_POS_2 = new BlockPos(11, 1, FRONT_Z);

    /**
     * Where the octant lives during the scroll test, and where its nbt is read from.
     *
     * <p>Reading it out of the main hand instead would be wrong for the two steps that expect the
     * scroll <em>not</em> to be cancelled: vanilla then moves the selection off the octant, the
     * main hand goes empty and "the octant has no Pos1" would be reported as the mod's fault. The
     * slot is fixed, the octant never leaves it, so this reads the same stack in every step.
     */
    private static final int OCTANT_SLOT = HOTBAR_SLOT;

    // ------------------------------------------------------------------------------------------
    // The keys this test presses
    // ------------------------------------------------------------------------------------------

    /**
     * The GLFW codes behind the bindings this test drives.
     *
     * <p>Each of them is the binding's shipped default, and each is checked against the live
     * binding by {@link #assertBindingOnKey} before it is pressed - see the class javadoc for why
     * a raw code needs that control at all.
     */
    private static final int SETTINGS_KEY = GLFW.GLFW_KEY_G;
    private static final int HIGHLIGHT_TOGGLE_KEY = GLFW.GLFW_KEY_H;
    private static final int JUMP_KEY = GLFW.GLFW_KEY_SPACE;

    /**
     * The three modifiers {@code MouseMixin} reads, on the same keys the mixin asks GLFW about.
     *
     * <p>Shift is read by the mixin through {@code client.options.keyShift.isDown()} - the binding
     * layer - while Control and Alt go through {@code InputConstants.isKeyDown(window, ...)},
     * which asks GLFW about the real window. That difference is the single biggest portability
     * hazard in this file and is why {@link #assertModifierIsVisibleToTheMixin} exists.
     */
    private static final int CONTROL_KEY = GLFW.GLFW_KEY_LEFT_CONTROL;
    private static final int SHIFT_KEY = GLFW.GLFW_KEY_LEFT_SHIFT;
    private static final int ALT_KEY = GLFW.GLFW_KEY_LEFT_ALT;

    /** Vanilla ships pick block on the middle mouse button, not on a keyboard key. */
    private static final int PICK_MOUSE_BUTTON = 2;

    /**
     * The config value {@link #beforeWorld} switched off, so {@link #inWorld} can put it back.
     *
     * <p>A static field because the two halves are two different {@link Script} objects: the
     * config has to be changed before the world - and with it the join event - exists, and the
     * measurement can only happen inside it. Reading it before {@link #beforeWorld} has run throws
     * by name instead of quietly restoring {@code null}.
     */
    private static final Later<Boolean> ORIGINAL_TRIM_BENEFITS =
            new Later<>("the enableArmorTrimBenefits value from before the world was created");

    private ClientBootstrapClientTest() {
    }

    // ------------------------------------------------------------------------------------------
    // The two halves
    // ------------------------------------------------------------------------------------------

    /**
     * Switches {@code enableArmorTrimBenefits} off before the world exists.
     *
     * <p><b>This has to run before the world is created, and it is the caller's job to make sure
     * of it</b> - register it in {@code ClientTests.beforeWorld()}, not in the in-world list. The
     * reason is the measurement itself: the server side flag from {@code PlayerEntityMixin}
     * defaults to {@code true}, so a client that sends {@code true} and a client that sends
     * nothing at all look identical on the server. Only a client that reports {@code false} proves
     * that a {@code TrimBenefitPayload} was really sent and handled - and the join listener reads
     * the config once, when the connection comes up.
     *
     * <p>The value is put back as soon as the measurement is done (see {@link #inWorld}), so the
     * rest of the run sees the config it started with. What cannot be put back is the server side
     * flag: there is no second join, and re-sending the payload by hand would prove nothing. It
     * stays {@code false} for the remainder of the run, which is a deliberate, documented cost of
     * covering the join listener at all.
     *
     * <p>The mod reads this through {@code AutoConfig.getConfigHolder(...).getConfig()}, which
     * hands out the very instance {@code Simplebuilding.getConfig()} returns - both are the
     * holder's single config object. If that ever stops being true the measurement below goes red
     * rather than quietly passing.
     */
    public static void beforeWorld(Script script) {
        script.act("remember and switch off enableArmorTrimBenefits before the world exists", client -> {
            ORIGINAL_TRIM_BENEFITS.set(Simplebuilding.getConfig().enableArmorTrimBenefits);
            Simplebuilding.getConfig().enableArmorTrimBenefits = false;
        });
    }

    /** Everything that needs a joined world, in the order the old straight-line test ran it. */
    public static void inWorld(Script script) {
        trimBenefitConfigReachedTheServerOnJoin(script);

        // Survival for the whole class: the pick block mixin bails out in creative, and the air
        // jump has to damage the boots, which creative would skip.
        TestScene.build(script, "minecraft:stone", "survival");
        // No pixel measurement happens here, so the HUD goes back on - the screenshots of the
        // screens read better with a hotbar under them.
        TestScene.showHudAgain(script);

        toggleKeysReachTheClientTickLoop(script);
        settingsKeyOnlyReactsToTheRightItem(script);
        settingsKeyNeedsConstructorsTouchOnAWand(script);
        octantScrollNeedsAModifierAndNoScreen(script);
        masterBuilderPickPullsFromTheBundle(script);
        spaceKeyStateReachesTheServer(script);
        airJumpRunsOnEveryClientTick(script);

        cleanUp(script);
    }

    /**
     * What the old {@code finally} blocks did, as ordinary last steps.
     *
     * <p><b>These do not run when a step above fails.</b> A step list has no {@code finally}: a
     * throwing step ends the script, and everything registered after it is skipped. That is
     * acceptable here because a failed client test takes the whole run down on both loaders - the
     * NeoForge driver halts the JVM outright - so there is no later test that could inherit a held
     * key. What these steps do buy is the <em>green</em> path: the next entry in the run list must
     * not start with Control still held or the jump key still down.
     *
     * <p>The config itself is not restored here; it is put back right after its measurement, which
     * is strictly earlier and therefore strictly better. See {@link #beforeWorld}.
     */
    private static void cleanUp(Script script) {
        script.harness("release every key and button this test held", harness -> {
            harness.releaseKey(CONTROL_KEY);
            harness.releaseKey(SHIFT_KEY);
            harness.releaseKey(ALT_KEY);
            harness.releaseKey(JUMP_KEY);
            harness.releaseMouse(0);
            harness.releaseMouse(1);
            harness.releaseMouse(PICK_MOUSE_BUTTON);
            // Not the same as releasing button 0: on NeoForge this also tells the game mode to
            // stop destroying and lets the mouse go. Nothing here mines, so it is a belt and
            // braces reset of the one input the facade owns.
            harness.setAttacking(false);
        });
        script.idle("let the released input settle", 5);

        // The next entry rebuilds the scene and hides the HUD again, so this costs nothing either
        // way - it is here so the world this test leaves behind is the world it wanted to see.
        TestScene.showHudAgain(script);
    }

    // ------------------------------------------------------------------------------------------
    // The cases
    // ------------------------------------------------------------------------------------------

    /**
     * The client reported its {@code enableArmorTrimBenefits} setting to the server when it joined.
     *
     * <p>The measurement works because the two sides start out disagreeing: the server side flag
     * lives in {@code PlayerEntityMixin} and defaults to {@code true}, while
     * {@link #beforeWorld} set the config to {@code false} before the world was created. So the
     * server can only end up at {@code false} if a {@code TrimBenefitPayload} really was sent and
     * handled.
     *
     * <p>The first step is not decoration. If {@link #beforeWorld} was never registered, the
     * config still holds its default, the client sent the same value the server already had, and
     * the assertion below would pass without proving anything. That is exactly the false green
     * this suite exists to prevent, so the missing registration is reported as a setup error
     * naming the fix.
     *
     * <p><b>What breaks this test:</b> dropping or misplacing the join listener, sending the
     * literal instead of the config value, or the payload losing its receiver - the server player
     * then keeps the default {@code true} and this goes red.
     */
    private static void trimBenefitConfigReachedTheServerOnJoin(Script script) {
        script.act("the join measurement was set up before the world", client -> {
            if (!ORIGINAL_TRIM_BENEFITS.isSet()) {
                throw new AssertionError("Setup failed: ClientBootstrapClientTest.beforeWorld never ran, so "
                        + "enableArmorTrimBenefits still held its default when the world was created. The value "
                        + "the client reported on join is then the same as the server side default and the step "
                        + "below could not fail. Register beforeWorld in ClientTests.beforeWorld().");
            }

            if (Simplebuilding.getConfig().enableArmorTrimBenefits) {
                throw new AssertionError("Setup failed: enableArmorTrimBenefits was supposed to be off before "
                        + "the world was created, otherwise the value the client sends is the same as the "
                        + "server side default and this test could not fail. It reads "
                        + Simplebuilding.getConfig().enableArmorTrimBenefits + " now, so something switched it "
                        + "back on between beforeWorld and here.");
            }
        });

        script.awaitPackets();
        script.idle("let the join payload reach the server", 10);

        script.await("the server learned that armour trim benefits are switched off", 100,
                client -> !((TrimBenefitUser) serverPlayer(client)).simplebuilding$areTrimBenefitsEnabled(),
                client -> "The client never reported enableArmorTrimBenefits=false to the server on join. The "
                        + "server side flag still holds its default, so no TrimBenefitPayload arrived. The "
                        + "client side config reads "
                        + Simplebuilding.getConfig().enableArmorTrimBenefits + " right now.");

        // Put back immediately rather than in the last steps: the join listener has already read
        // it and will not read it again, so every tick the run spends with a foreign config value
        // is a tick of avoidable risk for the tests that come after this one.
        script.act("restore enableArmorTrimBenefits", client ->
                Simplebuilding.getConfig().enableArmorTrimBenefits = ORIGINAL_TRIM_BENEFITS.get());
    }

    /**
     * The highlight toggle key is registered, bound and drained by the client tick loop.
     *
     * <p>Pressing it has to flip {@code ClientState.showHighlights}, and pressing it again has to
     * flip it back - a single press could still be explained by some other code touching the flag,
     * two presses in opposite directions cannot.
     *
     * <p>The octant figure key is only checked for being registered and unbound. That is the
     * tripwire for the "Not covered" note in the class javadoc: an unbound mapping has no key code
     * for the harness to press, so if someone ever gives it a default key, this assertion goes red
     * and the note has to be revisited. Its <em>effect</em> is deliberately not asserted, see
     * "Known defect".
     *
     * <p><b>What breaks this test:</b> losing the key mapping registration, dropping the
     * {@code consumeClick} loop out of the client tick listener, or moving the toggle to a
     * different flag than {@code ClientState.showHighlights}.
     */
    private static void toggleKeysReachTheClientTickLoop(Script script) {
        script.act("the toggle key mappings are registered and in their shipped state", client -> {
            String problem = null;

            if (ClientState.highlightToggleKey == null) {
                problem = "the highlight toggle key was never registered";
            } else if (ClientState.highlightToggleKey.isUnbound()) {
                problem = "the highlight toggle key is unbound, so it cannot be pressed";
            } else if (ClientState.octantFigureToggleKey == null) {
                problem = "the octant figure toggle key was never registered";
            } else if (!ClientState.octantFigureToggleKey.isUnbound()) {
                problem = "the octant figure toggle key is bound now. It used to ship unbound, which is why "
                        + "the class javadoc lists it as not coverable - that note needs revisiting";
            }

            if (problem != null) {
                throw new AssertionError("Toggle key preconditions not met: " + problem);
            }
        });

        assertBindingOnKey(script, "the highlight toggle", client -> ClientState.highlightToggleKey,
                HIGHLIGHT_TOGGLE_KEY);

        boolean[] before = new boolean[1];
        boolean[] afterFirst = new boolean[1];

        script.act("read ClientState.showHighlights before the first press", client ->
                before[0] = ClientState.showHighlights);

        script.harness("press the highlight toggle key", harness -> harness.pressKey(HIGHLIGHT_TOGGLE_KEY));
        script.idle("let the client tick loop drain the key", 5);

        script.act("the first press flipped ClientState.showHighlights", client -> {
            afterFirst[0] = ClientState.showHighlights;

            if (afterFirst[0] == before[0]) {
                throw new AssertionError("The highlight toggle key did not flip ClientState.showHighlights "
                        + "(still " + before[0] + "). Either the key mapping is not registered with the client "
                        + "or the client tick loop that drains it is gone.");
            }
        });

        script.harness("press the highlight toggle key again", harness -> harness.pressKey(HIGHLIGHT_TOGGLE_KEY));
        script.idle("let the client tick loop drain the second press", 5);

        script.act("the second press restored ClientState.showHighlights", client -> {
            boolean afterSecond = ClientState.showHighlights;

            if (afterSecond != before[0]) {
                throw new AssertionError("The second press of the highlight toggle key did not restore "
                        + "ClientState.showHighlights (" + before[0] + " -> " + afterFirst[0] + " -> "
                        + afterSecond + "). The flag is not driven by the key alone.");
            }
        });
    }

    /**
     * The settings key opens the octant manager for <em>any</em> octant - no enchantment involved -
     * and opens nothing at all for an item that is neither an octant nor a building wand.
     *
     * <p>Order matters: the negative step runs first and the positive step right after it, in the
     * same scene and with the same key. That is what makes the negative step worth anything - a key
     * press that never reached the client would take the positive step down with it.
     *
     * <p><b>What breaks this test:</b> dropping the {@code instanceof OctantItem} guard (the stone
     * step then opens a screen), adding an enchantment requirement to the octant branch (the octant
     * step then opens nothing), or breaking the screen itself, which crashes the client.
     */
    private static void settingsKeyOnlyReactsToTheRightItem(Script script) {
        assertBindingOnKey(script, "the settings", client -> ClientState.settingsKey, SETTINGS_KEY);

        giveMainHand(script, "minecraft:stone");

        script.harness("press the settings key with stone in hand", harness -> harness.pressKey(SETTINGS_KEY));
        script.idle("give a screen the chance to open", 10);
        assertNoScreen(script, "a plain stone block in the main hand");
        script.shot("bootstrap-a-settings-key-plain-item");

        giveMainHand(script, "simplebuilding:octant");

        script.act("the octant in hand carries no enchantment", client -> {
            String problem = null;

            if (client.player == null) {
                problem = "no client player";
            } else {
                ItemStack stack = client.player.getMainHandItem();

                if (!(stack.getItem() instanceof OctantItem)) {
                    problem = "the main hand does not hold an OctantItem but " + stack;
                } else if (!stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()) {
                    // "for every octant, without enchantment" is the claim - so prove the octant
                    // really carries none, otherwise the step below would also pass with a hidden
                    // requirement.
                    problem = "the octant carries enchantments (" + stack.getEnchantments() + "), so this step "
                            + "would not prove that the manager opens without one";
                }
            }

            if (problem != null) {
                throw new AssertionError("Octant settings key preconditions not met: " + problem);
            }
        });

        script.harness("press the settings key with an octant in hand", harness -> harness.pressKey(SETTINGS_KEY));
        awaitScreen(script, OctantScreen.class, "octant manager");
        script.idle("let the octant manager settle", 20);
        assertScreenIs(script, OctantScreen.class, "octant manager");
        script.shot("bootstrap-b-octant-manager");
        closeScreen(script);

        // "Any octant" includes the sixteen coloured ones, and until this step no client test had
        // ever held one: every step above and in every other class hands out the plain
        // simplebuilding:octant. Narrowing the key's guard from "instanceof OctantItem" to "is the
        // plain octant" would switch the manager off for all sixteen colours and leave every
        // client test green. One colour is enough to catch that; the server side walks all
        // sixteen in allOctantColoursShareDurabilityAndTheirPaint.
        giveMainHand(script, "simplebuilding:octant_lime");

        script.act("the coloured octant is an OctantItem with a colour", client -> {
            ItemStack stack = client.player == null ? ItemStack.EMPTY : client.player.getMainHandItem();

            if (!(stack.getItem() instanceof OctantItem octant) || octant.getColor() == null) {
                throw new AssertionError("Setup failed: the main hand holds " + stack
                        + " instead of a coloured octant, so the step below would not prove anything "
                        + "about the sixteen colours.");
            }
        });

        script.harness("press the settings key with a coloured octant in hand",
                harness -> harness.pressKey(SETTINGS_KEY));
        awaitScreen(script, OctantScreen.class, "octant manager for a coloured octant");
        script.idle("let the octant manager settle", 10);
        assertScreenIs(script, OctantScreen.class, "octant manager for a coloured octant");
        script.shot("bootstrap-b2-coloured-octant-manager");
        closeScreen(script);
    }

    /**
     * The settings key opens the building wand screen only for a wand that carries Constructor's
     * Touch.
     *
     * <p>{@code ModScreensClientGameTest} constructs {@link BuildingWandScreen} directly and says
     * so; this is the missing half - the real key path including the enchantment gate. Both steps
     * assert the enchantment level the mod reads (through the same
     * {@code EnchantmentHelper.getEnchantmentLevel} with the client level) before pressing the key,
     * so "no screen" can never mean "the enchantment never arrived on the client".
     *
     * <p><b>What breaks this test:</b> dropping the enchantment check (the plain wand then opens
     * the screen), the enchantment component not surviving the trip to the client, or the wand
     * branch losing its {@code instanceof BuildingWandItem} guard.
     */
    private static void settingsKeyNeedsConstructorsTouchOnAWand(Script script) {
        giveMainHand(script, "simplebuilding:netherite_building_wand");
        assertWandInHandWithTouchLevel(script, 0);

        script.harness("press the settings key with a plain wand in hand",
                harness -> harness.pressKey(SETTINGS_KEY));
        script.idle("give a screen the chance to open", 10);
        assertNoScreen(script, "a building wand without Constructor's Touch");
        script.shot("bootstrap-c-settings-key-plain-wand");

        selectHotbarSlot(script, HOTBAR_SLOT);
        script.command("clear @a", true);

        // Built in code and handed to the server thread, not written as command component syntax:
        // the component spelling is the part most likely to move between versions, and the Fabric
        // command path swallows a brigadier parse error, so a renamed component would arrive as a
        // plain wand and the step below would read as "the enchantment gate is broken".
        onServer(script, "put an enchanted building wand into the hotbar", server -> {
            ServerPlayer player = serverPlayer(server);
            ItemStack wand = new ItemStack(ModItems.NETHERITE_BUILDING_WAND);
            wand.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(ModEnchantments.CONSTRUCTORS_TOUCH), 1);
            player.getInventory().setItem(HOTBAR_SLOT, wand);
            player.inventoryMenu.broadcastChanges();
        });

        script.awaitPackets();
        script.idle("let the enchanted wand reach the client", 15);

        assertWandInHandWithTouchLevel(script, 1);

        script.harness("press the settings key with an enchanted wand in hand",
                harness -> harness.pressKey(SETTINGS_KEY));
        awaitScreen(script, BuildingWandScreen.class, "building wand settings");
        script.idle("let the building wand settings settle", 20);
        assertScreenIs(script, BuildingWandScreen.class, "building wand settings");
        script.shot("bootstrap-d-building-wand-settings");
        closeScreen(script);

        // --- Only the MAIN hand counts. The key reads getMainHandItem() and nothing else --------
        // Moved, not re-created: the very stack that just opened the screen goes to the off hand
        // and the main hand is emptied, so the only thing that changed is the hand. Every step
        // above keeps the off hand empty, which is why "main hand only" was a claim without a
        // witness - a key that fell back to the off hand would have opened the same screen here.
        onServer(script, "move the enchanted wand to the off hand", server -> {
            ServerPlayer player = serverPlayer(server);
            ItemStack wand = player.getInventory().getItem(HOTBAR_SLOT);
            player.getInventory().setItem(HOTBAR_SLOT, ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.OFFHAND, wand);
            player.inventoryMenu.broadcastChanges();
        });
        script.awaitPackets();
        script.idle("let the moved wand reach the client", 15);

        script.act("the enchanted wand is in the off hand and the main hand is empty", client -> {
            if (client.player == null || client.level == null) {
                throw new AssertionError("Setup failed: no client player or level.");
            }

            ItemStack off = client.player.getOffhandItem();
            int level = EnchantmentHelper.getEnchantmentLevel(off, client.level, ModEnchantments.CONSTRUCTORS_TOUCH);

            if (!(off.getItem() instanceof BuildingWandItem) || level < 1) {
                throw new AssertionError("Setup failed: the off hand holds " + off + " with Constructor's "
                        + "Touch level " + level + "; the step below needs the enchanted wand there.");
            }

            if (!client.player.getMainHandItem().isEmpty()) {
                throw new AssertionError("Setup failed: the main hand still holds "
                        + client.player.getMainHandItem() + ", so the key could open the screen through it.");
            }
        });

        script.harness("press the settings key with the enchanted wand in the OFF hand",
                harness -> harness.pressKey(SETTINGS_KEY));
        script.idle("give a screen the chance to open", 10);
        assertNoScreen(script, "an enchanted building wand in the off hand");
        script.shot("bootstrap-d2-offhand-wand-no-screen");

        script.command("item replace entity @a weapon.offhand with minecraft:air");
        script.awaitPackets();
        script.idle("let the emptied off hand reach the client", 10);

        // --- Every wand tier, not only the netherite one -------------------------------------
        // The guard is "instanceof BuildingWandItem"; narrowing it to the netherite wand would
        // switch the screen off for the five other tiers, and the positive step above - the only
        // one in the suite - holds exactly the netherite wand. The copper one is the far end.
        onServer(script, "put an enchanted COPPER wand into the hotbar", server -> {
            ServerPlayer player = serverPlayer(server);
            ItemStack wand = new ItemStack(ModItems.COPPER_BUILDING_WAND);
            wand.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(ModEnchantments.CONSTRUCTORS_TOUCH), 1);
            player.getInventory().setItem(HOTBAR_SLOT, wand);
            player.inventoryMenu.broadcastChanges();
        });
        script.awaitPackets();
        script.idle("let the copper wand reach the client", 15);
        selectHotbarSlot(script, HOTBAR_SLOT);
        assertWandInHandWithTouchLevel(script, 1);

        script.act("it really is the copper wand, not the netherite one again", client -> {
            if (client.player == null || !client.player.getMainHandItem().is(ModItems.COPPER_BUILDING_WAND)) {
                throw new AssertionError("Setup failed: the main hand holds "
                        + (client.player == null ? "nothing" : String.valueOf(client.player.getMainHandItem()))
                        + " instead of the copper wand, so this step would repeat the netherite one.");
            }
        });

        script.harness("press the settings key with an enchanted copper wand in hand",
                harness -> harness.pressKey(SETTINGS_KEY));
        awaitScreen(script, BuildingWandScreen.class, "building wand settings for the copper wand");
        script.idle("let the building wand settings settle", 10);
        assertScreenIs(script, BuildingWandScreen.class, "building wand settings for the copper wand");
        script.shot("bootstrap-d3-copper-wand-settings");
        closeScreen(script);
    }

    /**
     * {@code MouseMixin} turns the mouse wheel into an {@code OctantScrollPayload} only while an
     * octant is in the main hand, no screen is open and Shift, Control or Alt is held - and it
     * cancels the vanilla event exactly in that case.
     *
     * <p>Two independent observations are used, because either one alone would be ambiguous:
     * <ul>
     *   <li>the server side {@code Pos1} / {@code Pos2} / {@code Shape} nbt says the payload
     *       arrived, and by how much - the amount is {@code signum(vertical)}, so a wheel event of
     *       four still has to move exactly one block. It is read out of {@link #OCTANT_SLOT}, not
     *       out of the main hand: the two steps that expect no cancellation let vanilla move the
     *       selection off the octant;</li>
     *   <li>the selected hotbar slot says whether the event was cancelled: vanilla moves it on
     *       every uncancelled scroll, so an unchanged slot proves the mixin swallowed the event and
     *       a changed slot proves it did not.</li>
     * </ul>
     *
     * <p><b>Shift and the other two modifiers do not travel the same road, and that is what makes
     * this the hardest case to share.</b> The mixin reads Shift through
     * {@code client.options.keyShift.isDown()} - the binding layer, which both harnesses drive -
     * but Control and Alt through {@code InputConstants.isKeyDown(client.getWindow(), ...)}, which
     * asks GLFW about the real window. Fabric's client gametest framework replaces that lookup
     * with its own set of held keys, so its synthetic presses are visible to the mixin. NeoForge's
     * driver has no such replacement, and a held Control there would be invisible to
     * {@code isKeyDown} - the payload would never be sent, and every Control and Alt assertion
     * below would blame the mod for a gap in the harness. {@link #assertModifierIsVisibleToTheMixin}
     * is what turns that into a named setup failure instead. It is asserted separately for Control
     * and for Alt rather than once, because a harness could plausibly fake one and not the other.
     *
     * <p><b>This case also needs a working {@code Harness.scroll}.</b> The wheel has no binding, so
     * neither {@code KeyMapping.set} nor {@code KeyMapping.click} can express it; it goes through
     * {@code MouseHandler.onScroll}, which is private. The NeoForge driver says so out loud rather
     * than doing nothing quietly, which is the correct failure: a silently ineffective scroll would
     * leave every assertion here green about a mixin that was never reached.
     *
     * <p><b>What breaks this test:</b> dropping the modifier check, the screen check or the
     * {@code Locked} check (the corresponding "unchanged" assertion goes red), removing
     * {@code ci.cancel()} (the hotbar slot moves during the modifier steps), losing the
     * {@code signum} normalisation (the four wheel step moves four blocks), or the payload not
     * reaching its server side handler at all.
     */
    private static void octantScrollNeedsAModifierAndNoScreen(Script script) {
        // Rebuilt rather than inherited: the case before this one moved the hotbar selection and
        // filled the hand, and the case after it teleports the player away. A case that needs the
        // player at the scene's fixed spot, facing south, says so by putting him there.
        restoreAimingScene(script);
        giveOctantWithCorners(script, false);

        script.act("the server player still faces the direction the scroll handler resolves against", client -> {
            // The server resolves the scroll direction from the player's facing, so pin it down
            // first: yaw 0 is SOUTH (+Z), and a pitch beyond +-60 would switch the handler to the
            // UP / DOWN branch and move Pos1 along y instead of z.
            ServerPlayer player = serverPlayer(client);
            String problem = null;

            if (player.getDirection() != Direction.SOUTH) {
                problem = "the server player faces " + player.getDirection() + " instead of SOUTH";
            } else if (Math.abs(player.getXRot()) > 60.0f) {
                problem = "the server player's pitch is " + player.getXRot()
                        + ", which switches the scroll handler to the UP / DOWN branch";
            }

            if (problem != null) {
                throw new AssertionError("Octant scroll preconditions not met: " + problem);
            }
        });

        assertOctantPos(script, "Pos1", OCTANT_POS_1, "before the first scroll");
        assertOctantPos(script, "Pos2", OCTANT_POS_2, "before the first scroll");

        int[] slotBefore = new int[1];
        script.act("read the selected hotbar slot before any scrolling", client ->
                slotBefore[0] = selectedSlot(client));

        // --- Control moves Pos1 along the facing direction, one block per wheel notch -----------
        script.harness("hold Control", harness -> harness.holdKey(CONTROL_KEY));
        script.idle("let the held Control reach the window state", 2);
        assertModifierIsVisibleToTheMixin(script, "Control", CONTROL_KEY);

        scrollAndSettle(script, 1.0);
        assertOctantPos(script, "Pos1", OCTANT_POS_1.offset(0, 0, 1),
                "after one wheel notch with Control");

        // signum: a wheel event of four is still exactly one block.
        scrollAndSettle(script, 4.0);
        assertOctantPos(script, "Pos1", OCTANT_POS_1.offset(0, 0, 2),
                "after a wheel event of four with Control - the mixin normalises the amount with signum");

        scrollAndSettle(script, -1.0);
        assertOctantPos(script, "Pos1", OCTANT_POS_1.offset(0, 0, 1),
                "after scrolling back down with Control");

        script.harness("release Control", harness -> harness.releaseKey(CONTROL_KEY));
        script.idle("let the released Control settle", 2);

        assertSelectedSlot(script, () -> slotBefore[0],
                "the mixin did not cancel the vanilla scroll while Control was held");
        script.shot("bootstrap-e-octant-scroll");

        // --- Shift moves Pos2. Read from options.keyShift, not from InputConstants --------------
        assertBindingOnKey(script, "the sneak", client -> client.options.keyShift, SHIFT_KEY);
        script.harness("hold Shift", harness -> harness.holdKey(SHIFT_KEY));
        script.idle("let the held Shift reach the binding layer", 2);

        script.act("the sneak binding reports itself as down", client -> {
            if (!client.options.keyShift.isDown()) {
                throw new AssertionError("Setup failed: the harness holds GLFW key " + SHIFT_KEY
                        + " but options.keyShift.isDown() is false, which is the exact value MouseMixin "
                        + "reads. The Shift branch below would record nothing for a reason that has nothing "
                        + "to do with the mod.");
            }
        });

        scrollAndSettle(script, 1.0);
        script.harness("release Shift", harness -> harness.releaseKey(SHIFT_KEY));
        script.idle("let the released Shift settle", 2);

        assertOctantPos(script, "Pos2", OCTANT_POS_2.offset(0, 0, 1), "after one wheel notch with Shift");
        assertOctantPos(script, "Pos1", OCTANT_POS_1.offset(0, 0, 1), "after the Shift scroll - Pos1 is not "
                + "supposed to move with Shift");
        assertSelectedSlot(script, () -> slotBefore[0],
                "the mixin did not cancel the vanilla scroll while Shift was held");

        // --- Alt cycles the shape. No shape has been stored yet, so CUBOID is the starting point -
        script.act("the octant carries no shape yet", client -> {
            String shape = readOctantShape(client);

            if (!shape.isEmpty()) {
                throw new AssertionError("Setup failed: the octant already carries a Shape (" + shape
                        + "), so the step below cannot show that Alt advanced it from the CUBOID default.");
            }
        });

        script.harness("hold Alt", harness -> harness.holdKey(ALT_KEY));
        script.idle("let the held Alt reach the window state", 2);
        assertModifierIsVisibleToTheMixin(script, "Alt", ALT_KEY);

        scrollAndSettle(script, 1.0);
        script.harness("release Alt", harness -> harness.releaseKey(ALT_KEY));
        script.idle("let the released Alt settle", 2);

        script.act("one wheel notch with Alt advanced the shape by exactly one", client -> {
            String shape = readOctantShape(client);
            String expectedShape = OctantItem.SelectionShape.CYLINDER.name();

            if (!expectedShape.equals(shape)) {
                throw new AssertionError("One wheel notch with Alt was supposed to advance the octant shape "
                        + "from " + OctantItem.SelectionShape.CUBOID.name() + " to " + expectedShape
                        + ", but the server side stack holds "
                        + (shape.isEmpty() ? "no shape at all" : shape) + ".");
            }
        });

        assertSelectedSlot(script, () -> slotBefore[0],
                "the mixin did not cancel the vanilla scroll while Alt was held");

        // --- An open screen switches the mixin off, even with a modifier held -------------------
        BlockPos posBeforeScreen = OCTANT_POS_1.offset(0, 0, 1);

        // setScreenAndShow rather than gui.setScreen: it is the call the mod's own tick loop uses
        // to open its screens on NeoForge, so it is the one proven to leave the client in the
        // state MouseMixin then reads through client.screen.
        script.act("open a chat screen", client -> client.setScreenAndShow(new ChatScreen("", false)));
        awaitScreen(script, ChatScreen.class, "chat screen");
        script.idle("let the chat screen settle", 5);

        script.harness("hold Control while a screen is open", harness -> harness.holdKey(CONTROL_KEY));
        script.idle("let the held Control reach the window state", 2);
        // Asserted again, even though the Control block above already proved it. This step expects
        // Pos1 NOT to move, and a Control the mixin cannot see would satisfy that expectation for
        // entirely the wrong reason - the screen check would be reported as working while never
        // being reached.
        assertModifierIsVisibleToTheMixin(script, "Control", CONTROL_KEY);
        scrollAndSettle(script, 1.0);
        script.harness("release Control again", harness -> harness.releaseKey(CONTROL_KEY));
        script.idle("let the released Control settle", 2);

        closeScreen(script);
        assertOctantPos(script, "Pos1", posBeforeScreen,
                "after scrolling with Control while a screen was open - the mixin is supposed to stay out of it");

        // --- Without a modifier the event stays vanilla, which moves the hotbar selection -------
        // Only the slot is asserted here: with all three modifier flags false the server side
        // handler would not move anything either, so an unchanged Pos1 could not fail and would
        // be a decorative assertion.
        scrollAndSettle(script, 1.0);

        script.act("a plain scroll still moves the hotbar selection", client -> {
            int slotAfterPlainScroll = selectedSlot(client);

            if (slotAfterPlainScroll == slotBefore[0]) {
                throw new AssertionError("A plain scroll without a modifier left the selected hotbar slot at "
                        + slotBefore[0] + ". The mixin is only allowed to cancel the event while a modifier is "
                        + "held, so vanilla's hotbar scrolling has to still work here.");
            }
        });

        selectHotbarSlot(script, () -> slotBefore[0]);

        // --- A modifier over a NON-octant is vanilla's business ----------------------------------
        // The mixin's first guard is "an OctantItem in the main hand". Every modifier step above
        // holds an octant, and the one step with another item holds no modifier - so replacing
        // that guard with "true" (the mixin then swallows every modified wheel tick for every
        // item, and vanilla's modifier hotbar switching is dead in the whole game) leaves all of
        // them green. This is the step that holds both: stone in hand, Control down, one notch -
        // and the hotbar selection has to move, because vanilla has to still be reached.
        giveMainHand(script, "minecraft:stone");

        int[] slotBeforeStone = new int[1];
        script.act("read the selected hotbar slot before the stone scroll", client ->
                slotBeforeStone[0] = selectedSlot(client));

        script.harness("hold Control with stone in hand", harness -> harness.holdKey(CONTROL_KEY));
        script.idle("let the held Control reach the window state", 2);
        assertModifierIsVisibleToTheMixin(script, "Control", CONTROL_KEY);
        scrollAndSettle(script, 1.0);
        script.harness("release Control after the stone scroll", harness -> harness.releaseKey(CONTROL_KEY));
        script.idle("let the released Control settle", 2);

        script.act("a modified scroll over a plain item still reaches vanilla", client -> {
            if (selectedSlot(client) == slotBeforeStone[0]) {
                throw new AssertionError("Scrolling with Control while holding STONE left the selected "
                        + "hotbar slot at " + slotBeforeStone[0] + ". The mixin is only allowed to cancel "
                        + "the event for an octant; with anything else in hand vanilla's modifier "
                        + "scrolling has to keep working.");
            }
        });

        selectHotbarSlot(script, () -> slotBeforeStone[0]);

        // --- A locked octant is left to vanilla as well -----------------------------------------
        giveOctantWithCorners(script, true);

        int[] slotBeforeLocked = new int[1];
        script.act("read the selected hotbar slot before the locked scroll", client ->
                slotBeforeLocked[0] = selectedSlot(client));

        script.harness("hold Control for the locked octant", harness -> harness.holdKey(CONTROL_KEY));
        script.idle("let the held Control reach the window state", 2);
        // Same reason as in the open screen block: this step expects Pos1 not to move, and an
        // invisible Control would deliver exactly that without the Locked check ever running.
        assertModifierIsVisibleToTheMixin(script, "Control", CONTROL_KEY);
        scrollAndSettle(script, 1.0);
        script.harness("release Control after the locked octant", harness -> harness.releaseKey(CONTROL_KEY));
        script.idle("let the released Control settle", 2);

        assertOctantPos(script, "Pos1", OCTANT_POS_1, "after scrolling with Control on a locked octant");

        script.act("the locked octant fell through to vanilla", client -> {
            if (selectedSlot(client) == slotBeforeLocked[0]) {
                throw new AssertionError("Scrolling with Control on a locked octant left the selected hotbar "
                        + "slot at " + slotBeforeLocked[0] + ". A locked octant is supposed to fall through to "
                        + "vanilla instead of cancelling the event.");
            }
        });

        selectHotbarSlot(script, () -> slotBeforeLocked[0]);
    }

    /**
     * The pick block key pulls a targeted block out of a Master Builder bundle when the player does
     * not own it yet and is not in creative.
     *
     * <p>The control step runs the very same press with the very same bundle, only without the
     * enchantment. Vanilla cannot satisfy that press - {@code tryPickItem} searches the inventory
     * and nothing else - so the hand has to stay empty there, and every difference between the two
     * steps is the mixin's.
     *
     * <p>Vanilla ships pick block on the middle mouse <em>button</em>, not on a keyboard key, so
     * this presses button {@link #PICK_MOUSE_BUTTON} and checks the live binding against it first.
     * The Fabric-only version handed the binding itself to the framework and never had to know.
     *
     * <p><b>What breaks this test:</b> dropping the Master Builder check (the control step fills
     * the hand), dropping the {@code findSlotMatchingItem} or the creative guard, the payload not
     * being sent, or {@code handleMasterBuilderPick} no longer taking the stack out of the bundle.
     */
    private static void masterBuilderPickPullsFromTheBundle(Script script) {
        // The case before this one scrolled the hotbar around and the octant is still in slot 0;
        // this one needs an empty hand and the crosshair back on the wall, so it rebuilds both
        // rather than inheriting them.
        restoreAimingScene(script);
        assertBindingOnKey(script, "the pick block", client -> client.options.keyPickItem,
                InputConstants.Type.MOUSE, PICK_MOUSE_BUTTON);

        giveBundleWithStone(script, false);

        script.act("nothing but the enchantment can explain a filled hand", client -> {
            String problem = null;

            if (client.player == null) {
                problem = "no client player";
            } else if (client.player.isCreative()) {
                problem = "the player is in creative, where the mixin returns immediately";
            } else if (!client.player.getMainHandItem().isEmpty()) {
                problem = "the main hand is not empty but holds " + client.player.getMainHandItem()
                        + ", so the handler would take its free slot branch";
            } else if (client.player.getInventory().findSlotMatchingItem(new ItemStack(Items.STONE)) != -1) {
                problem = "the inventory already holds stone, so the mixin returns before it ever looks at "
                        + "a bundle";
            }

            if (problem != null) {
                throw new AssertionError("Master Builder pick preconditions not met: " + problem);
            }
        });

        TestScene.assertAimedAt(script, TestScene.TARGET, TestScene.TARGET_FACE);

        script.act("the bundle really holds the stone the pick is supposed to find", client -> {
            int inBundle = countStoneInBundle(client);

            if (inBundle != BUNDLED_STONE_COUNT) {
                throw new AssertionError("Setup failed: the bundle does not hold the " + BUNDLED_STONE_COUNT
                        + " stone the pick is supposed to find, it holds " + inBundle + ".");
            }
        });

        script.harness("press pick block with an unenchanted bundle",
                harness -> harness.pressMouse(PICK_MOUSE_BUTTON));
        script.idle("give the pick the chance to fill the hand", 20);

        script.act("the unenchanted bundle handed out nothing", client -> {
            if (!serverPlayer(client).getMainHandItem().isEmpty()) {
                throw new AssertionError("Control step failed: picking the targeted stone block filled the "
                        + "hand with " + describeServerMainHand(client) + " although the bundle carries no "
                        + "Master Builder. Neither vanilla nor the mod is allowed to hand out a block the "
                        + "player does not own, so the signal step below would not be attributable to the "
                        + "enchantment.");
            }
        });

        giveBundleWithStone(script, true);
        TestScene.assertAimedAt(script, TestScene.TARGET, TestScene.TARGET_FACE);

        script.harness("press pick block with a Master Builder bundle",
                harness -> harness.pressMouse(PICK_MOUSE_BUTTON));

        script.await("the pick pulled the targeted stone out of the bundle", 100,
                client -> serverPlayer(client).getMainHandItem().is(Items.STONE),
                client -> "The pick block key never pulled the targeted stone out of the Master Builder "
                        + "bundle. The hand holds " + describeServerMainHand(client) + " and the bundle still "
                        + "holds " + countStoneInBundle(client) + " stone. " + TestScene.describeAim(client));

        script.shot("bootstrap-f-master-builder-pick");

        script.act("the stone in the hand came out of the bundle", client -> {
            int leftInBundle = countStoneInBundle(client);

            if (leftInBundle != 0) {
                throw new AssertionError("The stone in the hand did not come out of the bundle: the bundle "
                        + "still holds " + leftInBundle + " stone.");
            }
        });

        // --- In creative the mixin stays out of it ------------------------------------------------
        // Until here creative was only a PRECONDITION ("not in creative, or the mixin returns at
        // once"), so the guard itself never ran and deleting it changed nothing. The observation
        // that separates the two is the bundle, not the hand: vanilla's creative pick fills the
        // hand for free either way, but only the mixin would take the stone OUT of the bundle.
        script.command("gamemode creative @a");
        script.command("clear @a", true);
        script.awaitPackets();
        script.idle("let creative mode reach the client", 10);
        giveBundleWithStone(script, true);
        TestScene.assertAimedAt(script, TestScene.TARGET, TestScene.TARGET_FACE);

        script.act("setup: creative, an empty hand and a full Master Builder bundle", client -> {
            if (client.player == null || !client.player.isCreative()) {
                throw new AssertionError("Setup failed: the client player is not in creative.");
            }

            if (countStoneInBundle(client) != BUNDLED_STONE_COUNT) {
                throw new AssertionError("Setup failed: the bundle holds " + countStoneInBundle(client)
                        + " stone instead of " + BUNDLED_STONE_COUNT + ".");
            }
        });

        script.harness("press pick block in creative with a Master Builder bundle",
                harness -> harness.pressMouse(PICK_MOUSE_BUTTON));
        script.idle("give the pick the chance to act", 20);

        script.act("in creative the bundle keeps every one of its stone", client -> {
            int leftInBundle = countStoneInBundle(client);

            if (leftInBundle != BUNDLED_STONE_COUNT) {
                throw new AssertionError("In creative the pick took stone out of the Master Builder bundle "
                        + "(" + leftInBundle + " of " + BUNDLED_STONE_COUNT + " left). The mixin is supposed to "
                        + "return before it looks at any bundle when the player is in creative - vanilla "
                        + "hands the block out for free there, and a bundle that empties itself in creative "
                        + "is a bundle that loses items for nothing.");
            }
        });

        script.command("gamemode survival @a");
        script.command("clear @a", true);
        script.awaitPackets();
        script.idle("let survival reach the client again", 10);
    }

    /**
     * The client sends a {@code SpaceKeyPayload} whenever the jump key changes state.
     *
     * <p>Observed on the server side flag from {@code PlayerEntityMixin}, which starts at
     * {@code false}: holding jump has to turn it on, releasing it has to turn it off again. Both
     * directions are asserted, so a flag that is stuck at one value cannot pass.
     *
     * <p><b>What breaks this test:</b> dropping the tick listener, sending only one edge, or the
     * payload losing its server side receiver.
     */
    private static void spaceKeyStateReachesTheServer(Script script) {
        assertBindingOnKey(script, "the jump", client -> client.options.keyJump, JUMP_KEY);

        script.act("the server does not think the jump key is held yet", client -> {
            if (readSpacePressedOnServer(client)) {
                throw new AssertionError("Setup failed: the server already thinks the jump key is held before "
                        + "the test pressed it, so 'pressed' below would not prove anything.");
            }
        });

        int[] payloadsBefore = new int[1];
        script.act("count the space key payloads the server has handled so far",
                client -> payloadsBefore[0] = PayloadCounter.spaceKeyPayloads());

        script.harness("hold the jump key", harness -> harness.holdKey(JUMP_KEY));

        script.await("the rising edge of the jump key reached the server", 60,
                client -> readSpacePressedOnServer(client),
                client -> "Holding the jump key never reached the server: the SpaceKeyPayload for the rising "
                        + "edge was not sent or not handled. options.keyJump.isDown() reads "
                        + client.options.keyJump.isDown() + " on the client.");

        // Held for two seconds ON PURPOSE. The flag above reads "pressed" after one packet and
        // after forty exactly alike; the count below does not. "Only when the state changes" is
        // the bandwidth promise the tick handler makes in its own comment, and this is the only
        // step in the suite that could notice it being broken.
        script.idle("keep the jump key held for forty ticks", 40);

        script.act("holding the key for forty ticks sent exactly one payload", client -> {
            int sent = PayloadCounter.spaceKeyPayloads() - payloadsBefore[0];

            if (sent != 1) {
                throw new AssertionError("Holding the jump key for forty ticks made the server handle "
                        + sent + " SpaceKeyPayloads instead of one. The client is supposed to send the "
                        + "key state only when it changes, not on every tick it is held.");
            }
        });

        script.harness("release the jump key", harness -> harness.releaseKey(JUMP_KEY));

        script.await("the falling edge of the jump key reached the server", 60,
                client -> !readSpacePressedOnServer(client),
                client -> "Releasing the jump key never reached the server: the SpaceKeyPayload for the "
                        + "falling edge was not sent or not handled. options.keyJump.isDown() reads "
                        + client.options.keyJump.isDown() + " on the client.");

        script.idle("give a chatty client the chance to keep sending", 20);

        script.act("the release sent exactly one more payload", client -> {
            int sent = PayloadCounter.spaceKeyPayloads() - payloadsBefore[0];

            if (sent != 2) {
                throw new AssertionError("One press and one release made the server handle " + sent
                        + " SpaceKeyPayloads instead of two - one per edge.");
            }
        });

        waitForGround(script);
    }

    /**
     * {@code DoubleJumpController.tick} runs on every client tick, and the air jump it performs
     * reaches the server.
     *
     * <p>The player is teleported into the air instead of jumping off the floor: the controller
     * needs a tick in which the player is airborne <em>and</em> the jump key is up before it will
     * accept the next press, and a teleport buys a long, calm fall for that instead of the eleven
     * odd ticks a ground jump leaves.
     *
     * <p>Two observations, one per side of the wire: {@code getCooldownRemaining()} turning
     * positive is the client tick loop, and the boots taking one point of damage is
     * {@code handleDoubleJump} on the server. The damage is only accepted while the player is
     * still airborne, because landing hard damages armour too and would otherwise make this pass
     * for the wrong reason - that is what the {@code -1} below is for: once the player is on the
     * ground the poll reads a value its predicate can never accept, so the window closes itself.
     *
     * <p><b>What breaks this test:</b> unregistering {@code DoubleJumpController::tick} from the
     * client tick event, the air jump branch losing one of its conditions, the enchantment level no
     * longer being read from the equipped boots, or {@code DoubleJumpPayload} not being sent or not
     * being handled.
     */
    private static void airJumpRunsOnEveryClientTick(Script script) {
        script.command("clear @a", true);

        // In code and on the server thread, for the same reason as the enchanted wand above: the
        // enchantment component spelling is what moves between versions, and a swallowed parse
        // error would arrive as plain boots and read as "the controller no longer sees the level".
        onServer(script, "equip enchanted diamond boots", server -> {
            ServerPlayer player = serverPlayer(server);
            ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
            boots.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(ModEnchantments.DOUBLE_JUMP), 1);
            player.setItemSlot(EquipmentSlot.FEET, boots);
            player.inventoryMenu.broadcastChanges();
        });

        script.awaitPackets();
        script.idle("let the enchanted boots reach the client", 15);

        script.act("every condition the air jump branch needs is met", client -> {
            String problem = null;

            if (client.player == null) {
                problem = "no client player";
            } else if (!Simplebuilding.getConfig().enableDoubleJump) {
                problem = "enableDoubleJump is off in the config, so the controller returns immediately";
            } else if (Simplebuilding.getConfig().airJumpCooldownTicks <= 0) {
                problem = "airJumpCooldownTicks is " + Simplebuilding.getConfig().airJumpCooldownTicks
                        + ", so a successful air jump would leave no cooldown to observe";
            } else if (DoubleJumpController.getDoubleJumpLevel(client.player) != 1) {
                problem = "the client sees Double Jump level "
                        + DoubleJumpController.getDoubleJumpLevel(client.player)
                        + " on the player instead of 1 - the enchanted boots did not arrive";
            } else if (client.player.getAbilities().flying) {
                problem = "the player is flying, which the air jump branch skips";
            }

            if (problem != null) {
                throw new AssertionError("Air jump preconditions not met: " + problem);
            }
        });

        script.act("the boots are undamaged before the jump", client -> {
            int bootDamageBefore = serverPlayer(client).getItemBySlot(EquipmentSlot.FEET).getDamageValue();

            if (bootDamageBefore != 0) {
                throw new AssertionError("Setup failed: the boots are already damaged (" + bootDamageBefore
                        + "), so the damage caused by the air jump could not be told apart from it.");
            }
        });

        // High enough for a long fall, low enough to stay inside the cleared volume of the scene.
        script.command("tp @a 10.5 8.0 16.5 0.0 0.0");
        script.awaitPackets();

        // A few ticks of falling with the jump key up: this is what puts the controller into the
        // state the air jump needs (not on ground, key not pressed, was not on ground last tick).
        script.await("the player left the ground after the teleport", 60,
                client -> client.player != null && !client.player.onGround(),
                client -> "Scene setup failed: the player never left the ground after being teleported into "
                        + "the air. " + TestScene.describeAim(client));
        script.idle("let the fall settle", 3);

        script.act("the air jump is not already on cooldown", client -> {
            if (DoubleJumpController.getCooldownRemaining() != 0) {
                throw new AssertionError("Setup failed: the air jump is already on cooldown before the test "
                        + "pressed jump, so a positive cooldown afterwards would prove nothing.");
            }
        });

        assertBindingOnKey(script, "the jump", client -> client.options.keyJump, JUMP_KEY);
        script.harness("hold the jump key in mid air", harness -> harness.holdKey(JUMP_KEY));

        int[] cooldown = new int[1];

        script.await("the air jump fired on the client", 25,
                client -> {
                    cooldown[0] = DoubleJumpController.getCooldownRemaining();
                    return cooldown[0] > 0;
                },
                client -> "The air jump never fired: DoubleJumpController.getCooldownRemaining() stayed at "
                        + "zero while the player was falling with the jump key held. Either the controller is "
                        + "not called on every client tick any more or one of its conditions no longer "
                        + "matches. " + TestScene.describeAim(client));

        script.shot("bootstrap-g-air-jump");
        script.harness("release the jump key", harness -> harness.releaseKey(JUMP_KEY));

        script.verify("report the observed air jump cooldown", () ->
                TestLog.info("air jump cooldown after the jump: " + cooldown[0] + " ticks"));

        // -1 marks "already landed": from that point on armour damage could be fall damage instead
        // of the air jump's, so the poll has to see the damage before that happens.
        int[] damage = new int[1];

        script.await("the air jump reached the server and damaged the boots", 40,
                client -> {
                    ServerPlayer player = serverPlayer(client);
                    damage[0] = player.onGround()
                            ? -1
                            : player.getItemBySlot(EquipmentSlot.FEET).getDamageValue();
                    return damage[0] >= 1;
                },
                client -> "The air jump never reached the server: the boots took no damage while the player "
                        + "was still airborne, so no DoubleJumpPayload was sent or handled. The last value "
                        + "seen was " + damage[0] + " (-1 means the player had already landed, which closes "
                        + "the window on purpose).");

        script.act("the air jump cost the boots exactly one point of durability", client -> {
            if (damage[0] != 1) {
                throw new AssertionError("The air jump damaged the boots by " + damage[0]
                        + " points instead of the single point handleDoubleJump applies.");
            }
        });

        script.command("tp @a 10.5 0.0 16.5 0.0 0.0");
        script.awaitPackets();
        waitForGround(script);
    }

    // ------------------------------------------------------------------------------------------
    // Scene helpers
    // ------------------------------------------------------------------------------------------

    /**
     * Puts the player back where the scene says and makes sure the wall he aims at is whole.
     *
     * <p>New in the shared form, and not a nicety. The whole in-world list shares one world and the
     * cases in this file move the player (the air jump teleports him eight blocks up), move the
     * hotbar selection and fill the hand. A case that needs the crosshair on {@link
     * TestScene#TARGET} therefore puts it there itself rather than trusting the case before it -
     * an order dependency between cases is a defect even when it happens to hold today, because
     * the day it stops holding the failure lands on the renderer, not on the reordering.
     *
     * <p>The fill is tolerated: a wall that is already whole fills no block, and vanilla reports
     * that as a command failure.
     */
    private static void restoreAimingScene(Script script) {
        script.command("fill -12 -4 " + TestScene.WALL_Z + " 32 24 " + TestScene.WALL_Z
                + " minecraft:stone", true);
        script.command("tp @a 10.5 0.0 16.5 0.0 0.0");
        script.awaitPackets();
        script.idle("let the restored scene reach the client", 10);
        TestScene.assertAimedAt(script, TestScene.TARGET, TestScene.TARGET_FACE);
    }

    private static void giveMainHand(Script script, String itemArgument) {
        // Always the same slot, and the selection is pinned to it first: "weapon.mainhand" would
        // follow whatever slot happens to be selected, which earlier steps are allowed to move.
        selectHotbarSlot(script, HOTBAR_SLOT);
        script.command("clear @a", true);
        script.command("item replace entity @a hotbar." + HOTBAR_SLOT + " with " + itemArgument);
        script.awaitPackets();
        script.idle("let the item reach the client", 15);
    }

    /**
     * Puts an octant carrying both corner positions - and optionally the Locked flag - into
     * {@link #OCTANT_SLOT} and selects that slot.
     *
     * <p>Everything the scroll steps depend on is verified here rather than assumed. The Fabric
     * command path swallows a brigadier parse error, so a renamed component or a changed nbt
     * spelling would otherwise arrive as a plain octant - and "Pos1 did not move" would then be
     * satisfied by an octant that never had a Pos1 to move. The NeoForge driver does report the
     * error, which means this check is the one that keeps the two loaders' failure modes the same
     * shape instead of one silent and one loud.
     */
    private static void giveOctantWithCorners(Script script, boolean locked) {
        selectHotbarSlot(script, OCTANT_SLOT);
        script.command("clear @a", true);
        script.command("item replace entity @a hotbar." + OCTANT_SLOT + " with "
                + "simplebuilding:octant["
                + "minecraft:custom_data={"
                + "Pos1:[I;" + OCTANT_POS_1.getX() + "," + OCTANT_POS_1.getY() + "," + OCTANT_POS_1.getZ() + "],"
                + "Pos2:[I;" + OCTANT_POS_2.getX() + "," + OCTANT_POS_2.getY() + "," + OCTANT_POS_2.getZ() + "]"
                + (locked ? ",Locked:1b" : "")
                + "}]");
        script.awaitPackets();
        script.idle("let the octant reach the client", 15);

        script.act("the octant arrived on the server with both corners", client -> {
            ItemStack stack = serverPlayer(client).getInventory().getItem(OCTANT_SLOT);
            String problem = null;

            if (!(stack.getItem() instanceof OctantItem)) {
                problem = "hotbar slot " + OCTANT_SLOT + " holds " + stack + " instead of an octant";
            } else {
                CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

                if (nbt.getIntArray("Pos1").orElse(new int[0]).length != 3
                        || nbt.getIntArray("Pos2").orElse(new int[0]).length != 3) {
                    problem = "the octant did not arrive with both corner positions, its custom data is " + nbt;
                } else if (nbt.getBooleanOr("Locked", false) != locked) {
                    problem = "the octant's Locked flag is " + nbt.getBooleanOr("Locked", false)
                            + " instead of " + locked;
                }
            }

            if (problem != null) {
                throw new AssertionError("Octant setup failed on the server: " + problem);
            }
        });

        // The mixin reads the client's main hand, so the client has to see the octant too.
        script.act("the client sees the octant in its main hand", client -> {
            if (client.player == null
                    || !(client.player.getMainHandItem().getItem() instanceof OctantItem)) {
                throw new AssertionError("Octant setup failed on the client: the main hand does not hold an "
                        + "OctantItem. " + TestScene.describeAim(client));
            }
        });
    }

    /**
     * Puts a reinforced bundle holding stone into the first non hotbar slot and empties the hand.
     *
     * <p>The contents are built in code rather than through the command's component syntax: the
     * component names are the part most likely to move between versions, and a command that fails
     * to parse is silently swallowed by the Fabric command path. The write goes to the server
     * thread through {@link #onServer} and the step after it waits for the outcome - a failure
     * inside a server task is swallowed by the task queue, so without that wait a skipped setup
     * would look like a working one.
     */
    private static void giveBundleWithStone(Script script, boolean masterBuilder) {
        selectHotbarSlot(script, HOTBAR_SLOT);
        script.command("clear @a", true);

        onServer(script, "put a reinforced bundle with stone into the inventory", server -> {
            ServerPlayer player = serverPlayer(server);
            ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);

            if (masterBuilder) {
                bundle.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(ModEnchantments.MASTER_BUILDER), 1);
            }

            bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(
                    (new ItemStack(Items.STONE, BUNDLED_STONE_COUNT)))));

            player.getInventory().setItem(BUNDLE_SLOT, bundle);
            player.inventoryMenu.broadcastChanges();
        });

        script.await("the bundle arrived with its stone in it", 100,
                client -> countStoneInBundle(client) == BUNDLED_STONE_COUNT,
                client -> "The reinforced bundle never turned up in inventory slot " + BUNDLE_SLOT
                        + " with " + BUNDLED_STONE_COUNT + " stone in it (it holds "
                        + countStoneInBundle(client) + "). The server task that builds it either did not run "
                        + "or threw, and a throw inside a server task is swallowed by the task queue.");

        script.awaitPackets();
        script.idle("let the bundle reach the client", 15);
    }

    /** Scrolls the wheel and gives the packet a client tick out and a server tick in. */
    private static void scrollAndSettle(Script script, double amount) {
        script.harness("scroll the wheel by " + amount, harness -> harness.scroll(amount));
        script.idle("let the scroll payload travel and come back", 10);
    }

    private static void selectHotbarSlot(Script script, int slot) {
        selectHotbarSlot(script, () -> slot);
    }

    /**
     * Selects a hotbar slot on the client and waits until the server agrees.
     *
     * <p>Driven from the client rather than the server on purpose: {@code LocalPlayer} notices the
     * changed selection on its next tick and sends {@code ServerboundSetCarriedItemPacket}, so both
     * sides end up on the same slot. Setting it on the server alone would leave the client - which
     * is where the mixin reads the main hand - pointing somewhere else. The result is asserted
     * because a selection that silently stayed put would turn the steps below into no-ops.
     *
     * <p>The slot arrives as a supplier because several callers restore a slot an earlier step
     * read at run time, and in a step list a run time value cannot be baked into a step that is
     * registered before it exists.
     */
    private static void selectHotbarSlot(Script script, IntSupplier slot) {
        script.act("select a hotbar slot on the client", client -> {
            if (client.player != null) {
                client.player.getInventory().setSelectedSlot(slot.getAsInt());
            }
        });
        script.idle("let the selection reach the server", 10);

        script.await("the server agrees about the selected hotbar slot", 100,
                client -> serverPlayer(client).getInventory().getSelectedSlot() == slot.getAsInt(),
                client -> "Setup failed: the client selected hotbar slot " + slot.getAsInt()
                        + " but the server still has slot "
                        + serverPlayer(client).getInventory().getSelectedSlot()
                        + " selected, so the two sides would look at different stacks.");
    }

    private static void waitForGround(Script script) {
        script.await("the player is back on the ground", 100,
                client -> client.player != null && client.player.onGround(),
                client -> "Scene setup failed: the player never landed again. "
                        + TestScene.describeAim(client));
    }

    // ------------------------------------------------------------------------------------------
    // Reaching the integrated server
    // ------------------------------------------------------------------------------------------

    /**
     * Hands one piece of work to the integrated server's thread.
     *
     * <p>The shared harness has no {@code runOnServer}: Fabric's is a framework service and
     * NeoForge has nothing like it. {@code MinecraftServer} is a task queue, though, and both
     * loaders have the very same one behind {@code Minecraft.getSingleplayerServer()}, so the work
     * is queued from the client thread and runs on the server thread - which is where it belongs
     * and where the original {@code runOnServer} ran it.
     *
     * <p><b>Always follow this with a step that waits for the outcome.</b> A throw inside a queued
     * task never reaches the script; the server logs it and carries on. The wait is what turns
     * that into a named timeout instead of a setup that silently did nothing.
     */
    private static void onServer(Script script, String name, Consumer<MinecraftServer> work) {
        script.act(name, client -> {
            MinecraftServer server = requireServer(client);
            server.execute(() -> work.accept(server));
        });
    }

    private static MinecraftServer requireServer(Minecraft client) {
        MinecraftServer server = client.getSingleplayerServer();

        if (server == null) {
            throw new AssertionError("There is no integrated server. Every assertion in this class reads the "
                    + "server side player, so this test only works inside a single player world.");
        }

        return server;
    }

    /**
     * The one player on the test server, read from the client thread.
     *
     * <p>See the class javadoc for what that costs: on NeoForge the server thread runs
     * concurrently, so a value read here can be a tick stale. Every caller that cares sits inside
     * an {@code await} that asks again next tick.
     */
    private static ServerPlayer serverPlayer(Minecraft client) {
        return serverPlayer(requireServer(client));
    }

    private static ServerPlayer serverPlayer(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();

        if (players.size() != 1) {
            throw new AssertionError("Expected exactly one player on the test server, found " + players.size()
                    + ". Every assertion in this class reads that single player.");
        }

        return players.get(0);
    }

    // ------------------------------------------------------------------------------------------
    // Reading and asserting
    // ------------------------------------------------------------------------------------------

    private static boolean readSpacePressedOnServer(Minecraft client) {
        return ((ISpaceKeyTracker) serverPlayer(client)).simplebuilding$isSpacePressed();
    }

    private static int[] readOctantIntArray(Minecraft client, String key) {
        CompoundTag nbt = serverPlayer(client).getInventory().getItem(OCTANT_SLOT)
                .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return nbt.getIntArray(key).orElse(new int[0]);
    }

    private static String readOctantShape(Minecraft client) {
        CompoundTag nbt = serverPlayer(client).getInventory().getItem(OCTANT_SLOT)
                .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return nbt.getString("Shape").orElse("");
    }

    private static int countStoneInBundle(Minecraft client) {
        BundleContents contents = serverPlayer(client).getInventory().getItem(BUNDLE_SLOT)
                .get(DataComponents.BUNDLE_CONTENTS);

        if (contents == null) {
            return 0;
        }

        int count = 0;

        for (ItemStack stack : contents.items()) {

            if (stack.is(Items.STONE)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    private static String describeServerMainHand(Minecraft client) {
        return String.valueOf(serverPlayer(client).getMainHandItem());
    }

    private static int selectedSlot(Minecraft client) {
        return client.player == null ? -1 : client.player.getInventory().getSelectedSlot();
    }

    /**
     * The octant in {@link #OCTANT_SLOT} carries this corner at this position, right now.
     *
     * <p>An {@code act} rather than an {@code await}: the scroll step before it already spent ten
     * ticks letting the payload travel, so a value that is still wrong here is wrong, not late.
     * Waiting for it would turn "the mixin never sent anything" into a slow pass.
     */
    private static void assertOctantPos(Script script, String key, BlockPos expected, String when) {
        script.act("the octant's " + key + " is " + expected + " " + when, client -> {
            int[] actual = readOctantIntArray(client, key);

            if (actual.length != 3) {
                throw new AssertionError("The octant in hotbar slot " + OCTANT_SLOT + " has no usable " + key
                        + " " + when + " (stored value has " + actual.length + " entries instead of 3).");
            }

            BlockPos pos = new BlockPos(actual[0], actual[1], actual[2]);

            if (!pos.equals(expected)) {
                throw new AssertionError("The octant's " + key + " is " + pos + " " + when
                        + ", expected " + expected + ".");
            }
        });
    }

    private static void assertSelectedSlot(Script script, IntSupplier expected, String message) {
        script.act("the selected hotbar slot did not move", client -> {
            int actual = selectedSlot(client);

            if (actual != expected.getAsInt()) {
                throw new AssertionError(message + ": the selected hotbar slot moved from "
                        + expected.getAsInt() + " to " + actual + ", so vanilla's scroll handling ran.");
            }
        });
    }

    private static void assertWandInHandWithTouchLevel(Script script, int expectedLevel) {
        script.act("the wand in hand carries Constructor's Touch level " + expectedLevel, client -> {
            String problem = null;

            if (client.player == null || client.level == null) {
                problem = "no client player or level";
            } else {
                ItemStack stack = client.player.getMainHandItem();

                if (!(stack.getItem() instanceof BuildingWandItem)) {
                    problem = "the main hand does not hold a BuildingWandItem but " + stack;
                } else {
                    // Read exactly the way the client tick loop reads it, including the client
                    // level: an enchantment lookup needs a registry access, and the one the mod
                    // uses is the client's.
                    int level = EnchantmentHelper.getEnchantmentLevel(stack, client.level,
                            ModEnchantments.CONSTRUCTORS_TOUCH);

                    if (level != expectedLevel) {
                        problem = "the wand in hand carries Constructor's Touch level " + level
                                + " instead of " + expectedLevel;
                    }
                }
            }

            if (problem != null) {
                throw new AssertionError("Building wand preconditions not met: " + problem);
            }
        });
    }

    private static void assertNoScreen(Script script, String situation) {
        script.act("no screen opened for " + situation, client -> {
            Screen screen = client.screen;

            if (screen != null) {
                throw new AssertionError("The settings key opened " + screen.getClass().getName() + " for "
                        + situation + ", although nothing was supposed to open.");
            }
        });
    }

    /** Waits for a screen of this class to be the current one. */
    private static void awaitScreen(Script script, Class<? extends Screen> screenClass, String label) {
        script.await("the " + label + " opens", 100,
                client -> screenClass.isInstance(client.screen),
                client -> "The " + label + " never opened. The current screen is "
                        + (client.screen == null ? "none"
                                : client.screen.getClass().getName())
                        + ", the main hand holds "
                        + (client.player == null ? "nothing - there is no player"
                                : String.valueOf(client.player.getMainHandItem())) + ".");
    }

    private static void assertScreenIs(Script script, Class<? extends Screen> screenClass, String label) {
        script.act("the " + label + " is still open", client -> {
            String actual = client.screen == null ? "none"
                    : client.screen.getClass().getName();

            if (!actual.equals(screenClass.getName())) {
                throw new AssertionError("The " + label + " did not stay open: current screen is " + actual);
            }
        });
    }

    private static void closeScreen(Script script) {
        script.act("close the current screen", client -> client.setScreen(null));
        script.await("the screen is gone", 100, client -> client.screen == null,
                client -> "The screen would not close; it is still "
                        + (client.screen == null ? "none"
                                : client.screen.getClass().getName()) + ".");
        script.idle("let the closed screen settle", 5);
    }

    // ------------------------------------------------------------------------------------------
    // Controls for the raw input codes
    // ------------------------------------------------------------------------------------------

    private static void assertBindingOnKey(Script script, String label,
                                           Function<Minecraft, KeyMapping> mapping, int glfwKeyCode) {
        assertBindingOnKey(script, label, mapping, InputConstants.Type.KEYSYM, glfwKeyCode);
    }

    /**
     * The binding really is the key the harness is about to press.
     *
     * <p>New in the shared form and not optional. {@link Harness} takes a raw code, so a binding
     * that moved - because a default changed, because an options file was inherited from an
     * earlier run, or because the mapping was never registered - would leave the press landing
     * nowhere while the test believes it pressed the key. Every measurement after it would then
     * report the mod as broken. This turns that into a setup failure that names the binding and
     * what it says now.
     */
    private static void assertBindingOnKey(Script script, String label,
                                           Function<Minecraft, KeyMapping> mapping,
                                           InputConstants.Type type, int code) {
        script.act(label + " binding sits on the key the harness presses", client -> {
            KeyMapping keyMapping = mapping.apply(client);

            if (keyMapping == null) {
                throw new AssertionError("Setup failed: " + label + " key mapping does not exist, so the "
                        + "press below would land nowhere.");
            }

            if (!keyMapping.saveString().equals(type.getOrCreate(code).getName())) {
                throw new AssertionError("Setup failed: " + label + " binding is not on " + type + " " + code
                        + " any more (it says \"" + keyMapping.saveString() + "\"). The harness presses a raw "
                        + "code, so the measurement below would silently press nothing and the mod would be "
                        + "blamed for the empty result.");
            }
        });
    }

    /**
     * A held Control or Alt is visible where {@code MouseMixin} actually looks for it.
     *
     * <p>The mixin asks {@code InputConstants.isKeyDown(client.getWindow(), ...)}, which goes
     * straight to GLFW and knows nothing about a key a test framework pretends to hold. Fabric's
     * client gametest framework replaces that lookup with its own set of held keys, so its
     * synthetic presses arrive; a driver without that replacement leaves the mixin blind, the
     * payload is never sent, and every Control and Alt assertion in this file would go red about a
     * mod that is perfectly fine.
     *
     * <p>Asserted once per modifier, immediately after it is held and before the wheel is turned -
     * the same place the other trigger conditions are asserted, and for the same reason.
     */
    private static void assertModifierIsVisibleToTheMixin(Script script, String label, int glfwKeyCode) {
        script.act("the held " + label + " is visible to InputConstants.isKeyDown", client -> {
            if (!InputConstants.isKeyDown(client.getWindow(), glfwKeyCode)) {
                throw new AssertionError("Setup failed: the harness holds GLFW key " + glfwKeyCode + " ("
                        + label + ") but InputConstants.isKeyDown says it is up, and that is the exact call "
                        + "MouseMixin uses for Control and Alt. This driver's input does not reach the real "
                        + "window state, so the scroll below would never be turned into an OctantScrollPayload "
                        + "and the mod would be blamed for it. Teach the driver to answer isKeyDown for the "
                        + "keys it holds - Fabric's client gametest framework does exactly that.");
            }
        });
    }
}
