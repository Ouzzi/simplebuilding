package com.simplebuilding.clientgametest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.imageio.ImageIO;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.DoubleJumpController;
import com.simplebuilding.client.gui.DoubleJumpHudOverlay;
import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MusicToastDisplayState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.item.ItemStack;

/**
 * Covers the air jump (double jump) feature end to end on a real client: the key edge detection and
 * the ground edge detection in {@code DoubleJumpController#tick}, the cooldown state machine it
 * drives, and the HUD bar {@code DoubleJumpHudOverlay} draws from it.
 *
 * <p><b>Why this needs a client test at all.</b> Every branch under test sits behind
 * {@code Minecraft.getInstance()} and {@code client.options.keyJump}: the whole trigger condition,
 * the cooldown arithmetic and the overlay. The headless server tests can reach none of it - they
 * only see the {@code DoubleJumpPayload} that arrives afterwards.
 *
 * <p><b>How the behaviour is observed.</b> Not with screenshots. A double jump is a single tick of
 * physics; a pixel diff could neither time it nor tell it apart from the ordinary vanilla jump.
 * Instead the test records a tick-by-tick series of the state the controller produces - the same
 * {@code grounded} expression the controller evaluates, the same {@code keyJump.isDown()}, the y
 * velocity, the fall distance and both cooldown counters - and every assertion below is made
 * against that series. This is the same trade-off (and the same technique) as in
 * {@link MultiBlockBreakingClientTest}, which observes render state instead of pixels.
 *
 * <p><b>Where that series comes from in the shared form, and what changed.</b> The Fabric-only
 * ancestor of this test registered a second {@code ClientTickEvents.END_CLIENT_TICK} listener and
 * leaned on Fabric's registration order to make it run <em>after</em> the mod's own controller
 * listener in the same tick. That wiring is not expressible in shared code: NeoForge has no
 * ordered-by-registration event bus and would need an explicit priority, which is exactly why
 * {@link BreakingStateRecorder}'s listener lives in the two drivers rather than here. So the probe
 * is a plain script step instead - {@link #recordTicks} samples once per client tick from inside an
 * {@code await} condition, the same shape {@code MultiBlockBreakingClientTest} uses to read the
 * breaking render states. Two consequences are written into the code rather than hoped for:
 * <ul>
 *   <li>The sample is <b>deduplicated and gap-checked</b> against {@code player.tickCount}. Steps
 *       that finish immediately run back to back within one client tick, so without the dedup a
 *       key press between two recording steps would produce two samples of the same tick and
 *       "the cooldown counted down by exactly one per tick" would fail on a phantom. A skipped
 *       tick is a different failure and just as fatal to the analysis, so
 *       {@link Recording#snapshot} refuses a series with a hole in it.</li>
 *   <li>The sampling point is a <b>fixed point in the tick, but not provably after the mod's
 *       controller</b>: on NeoForge the script is driven from {@code ClientTickEvent.Post}, the
 *       same event the mod ticks on, and nothing orders the two. Everything the series is asked
 *       about except one claim is a relation between neighbouring samples and survives a uniform
 *       one-tick shift. The exception is the exact {@code 0.5} impulse, which is only readable
 *       before the next tick's physics runs; that assertion therefore names the sampling order as
 *       the first suspect in its failure message (see
 *       {@link #theAirJumpSetsUpwardVelocityAndClearsFallDistance}).</li>
 * </ul>
 *
 * <p><b>The HUD is covered twice, on purpose.</b>
 * <ul>
 *   <li>{@link #theHudBarReachesTheScreenWhileTheAirJumpRecharges} is a screenshot difference test
 *       in the frozen {@link TestScene}, with a measured noise floor, the bar as the only change
 *       and a control step after the cooldown has run out. It proves the overlay is really attached
 *       as a HUD element and really reaches the framebuffer - which a direct call never could - and
 *       it checks that essentially all changed pixels sit inside the screen band the overlay claims
 *       to draw into.</li>
 *   <li>{@link #theHudBarGeometryColoursAndLabelAreExactlyWhatTheOverlayDraws} calls
 *       {@code DoubleJumpHudOverlay.render} with a {@link GuiGraphicsExtractor} subclass that
 *       records the {@code fill} and {@code centeredText} calls instead of executing them. A
 *       screenshot can say "something appeared"; only this can say "82x7 border, 80x5 track, amber
 *       fill of exactly this width, centered, top edge 55 pixels above the bottom, label
 *       {@code "Air Jump"}". The controller state it renders is real, produced by a real air jump.
 *       It does not prove the overlay is registered anywhere - the screenshot test does that.</li>
 * </ul>
 *
 * <p><b>Vanilla-borne facts this test leans on</b> (not mod behaviour, so stated here rather than
 * asserted): holding the jump key on the ground makes vanilla jump and, while held, keep hopping;
 * {@code onGround()} is already false at the end of the tick in which vanilla jumps, because
 * {@code LivingEntity#aiStep} jumps and then moves within that same tick; ladders keep
 * {@code onClimbable()} true and let the player slide down at 0.15/tick; water keeps
 * {@code isInWater()} true and pushes the player up 0.04/tick while jump is held; and {@code /tp}
 * zeroes velocity and fall distance.
 *
 * <p><b>Harness traps this test has to dodge.</b>
 * <ul>
 *   <li>A press-and-release in one step is invisible to the controller: {@code keyJump.isDown()}
 *       would be false again by the time the client ticks. Every press below is therefore
 *       {@code holdKey} / record ticks / {@code releaseKey}, never {@code pressKey}.</li>
 *   <li>The shared {@link Harness} presses a <em>raw GLFW code</em>, where the Fabric-only ancestor
 *       handed over the key binding and let the framework resolve it.
 *       {@link #assertJumpKeyIsBound} is the control that closes that hole: a jump key that moved
 *       would leave the player standing while the test believed it pressed jump, and every
 *       negative result below would then be true for a reason that has nothing to do with the
 *       mod.</li>
 *   <li>In creative a second fresh jump press within 7 ticks of the first, while airborne, toggles
 *       vanilla creative flight, which would silently block the air jump. Presses are therefore at
 *       least 12 ticks apart, {@link #clearCreativeFlight} runs before every phase, and
 *       {@link #assertNeverFlew} additionally asserts that flight never turned itself on.</li>
 *   <li>The HUD phases need the HUD <em>visible</em>, so {@link TestScene}'s "hide everything"
 *       trick is not available and the moving parts of the vanilla HUD have to be switched off one
 *       by one - see {@link #silenceChatAndToasts}. A noise floor measured ten ticks apart does not
 *       catch them: a chat line only starts fading 200 ticks after it arrived.</li>
 * </ul>
 *
 * <p><b>Every case sets up its own scene.</b> The whole client suite shares one world, and inside
 * this file the cases used to share one setup as well - boots equipped once at the top, the ladder
 * and water shafts built once, a cooldown value left behind by the case before. That is an order
 * dependency and therefore a defect even where it happened to work: a case that is moved, removed
 * or fails leaves the next one measuring something else. So every case here calls
 * {@link #returnToRest}, equips the boots it needs at the level it needs, sets the config values it
 * needs, and the two cases that need the ladder and the water shaft rebuild them (tolerantly, so an
 * already correct scene costs nothing). What this test changes - config values, game mode, held
 * keys, creative flight, chat and toast options - it puts back itself in its last steps.
 *
 * <p><b>Known defect (reported, not written into any assertion).</b>
 * <ul>
 *   <li>{@code DoubleJumpHudOverlay.FILL_READY} (green) is dead code. The overlay returns early
 *       unless {@code DoubleJumpController.isOnCooldown()}, i.e. unless {@code remaining > 0}, and
 *       its fill fraction is {@code (max - remaining) / max}, which reaches 1.0 only at
 *       {@code remaining == 0}. The {@code charged >= 1.0f} branch can therefore never be
 *       taken.</li>
 *   <li>{@code DoubleJumpController#getDoubleJumpLevel} scans <em>all</em> {@code EquipmentSlot}
 *       values, so enchanted boots merely <em>held in a hand</em> let the client air-jump, while
 *       the server side {@code ModMessageHandlers#handleDoubleJump} only ever looks at
 *       {@code EquipmentSlot.FEET} and will not spend durability for it. {@link #equipBoots} has to
 *       work around this: it enchants the boots in the main hand, copies them onto the feet and
 *       then empties the hand again, because boots left in the hand would satisfy the client's
 *       level check for the wrong slot.</li>
 *   <li>With {@code airJumpCooldownTicks <= 1} the level 2 cooldown ({@code Math.max(1, base / 2)})
 *       is <em>longer</em> than the level 1 cooldown ({@code base}, clamped to 0). Level 2 is worse
 *       than level 1 at that setting. The assertion below states the code's documented behaviour;
 *       it does not endorse the inversion.</li>
 * </ul>
 *
 * <p><b>Not covered</b> (and why - all of these are gaps that survive a client test):
 * <ul>
 *   <li><b>That the green {@code FILL_READY} is never drawn.</b> Any assertion would be a
 *       tautology: {@code remaining > 0} makes {@code (max - remaining) / max < 1} true by
 *       arithmetic, so the check could not fail whatever the overlay does. What <em>is</em> pinned
 *       is the boundary that makes the branch dead - the recorder case asserts that at
 *       {@code remaining == 0} the overlay draws nothing at all, so widening the guard to
 *       {@code remaining >= 0} (the one realistic way to reach the green) turns it red.</li>
 *   <li><b>That an air jump sends {@code DoubleJumpPayload}.</b> The handler's only effect for a
 *       creative player is {@code player.fallDistance = 0} on the server, and the server zeroes
 *       fall distance by itself the moment the player moves upwards - the observation would not be
 *       attributable to the packet. The survival-mode effect (one point of boot durability) is
 *       covered by the case below, which is the only reason that case switches game modes.</li>
 *   <li><b>That Fabric and NeoForge call the same {@code tick} and register the same HUD layer.</b>
 *       That is loader wiring outside this client's control; it belongs to the client bootstrap
 *       gap list, not here.</li>
 *   <li><b>The exact drawn colour on screen.</b> The recorder sees the colour arguments the
 *       overlay passes; the framebuffer check only sees that pixels changed inside the expected
 *       band. Blending {@code 0xC0000000} over an arbitrary world background is not something a
 *       stable assertion can be built on at an unknown GUI scale.</li>
 *   <li><b>That the boot durability was spent on the <em>server</em>.</b> The Fabric-only ancestor
 *       waited on {@code ServerPlayer#getItemBySlot}; the shared {@link Harness} deliberately has
 *       no server handle, so the wait is on the client's copy of its own boots instead. That is one
 *       synchronisation step further away from the effect - what is proven is "the server damaged
 *       the boots and told the client", not "the server damaged the boots". The extra step cannot
 *       manufacture the damage, only lose it, so the assertion stays sound in the direction that
 *       matters.</li>
 * </ul>
 */
public final class AirJumpClientTest {

    /** Upward velocity {@code DoubleJumpController} assigns on an air jump. */
    private static final double AIR_JUMP_VELOCITY = 0.5;

    /** Cooldown used for the behaviour cases - short enough that recharging between them is cheap. */
    private static final int SHORT_COOLDOWN = 40;

    /** Cooldown used for the HUD cases - long enough to settle the scene and still see the bar. */
    private static final int LONG_COOLDOWN = 200;

    // The bar the overlay claims to draw. Deliberately a second, independent copy of the numbers in
    // DoubleJumpHudOverlay: if the overlay changes them, these assertions have to go red.
    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_BOTTOM_OFFSET = 55;
    private static final int LABEL_OFFSET = 10;
    private static final int BORDER_COLOR = 0xC0000000;
    private static final int TRACK_COLOR = 0xFF2B2B2B;
    private static final int FILL_CHARGING = 0xFFFFC83C;
    private static final int LABEL_COLOR = 0xFFFFFFFF;
    private static final String LABEL = "Air Jump";

    /** The one position every case returns to: standing still on the scene floor, facing the wall. */
    private static final double HOME_X = 10.5;
    private static final double HOME_Y = 0.0;
    private static final double HOME_Z = 16.5;

    /** The same three numbers as a {@code /tp} argument, so there is only one source for them. */
    private static final String HOME = HOME_X + " " + HOME_Y + " " + HOME_Z + " 0.0 0.0";

    /** Per channel difference at which a pixel counts as changed - same value as {@link ScreenshotDiff}. */
    private static final int CHANNEL_TOLERANCE = 12;

    /**
     * The GLFW key code every jump press uses.
     *
     * <p>Raw, because that is the only thing both loaders can serve - and therefore guarded by
     * {@link #assertJumpKeyIsBound}, which is what keeps a moved binding from turning this whole
     * file into a very thorough proof that a standing player does not air-jump.
     */
    private static final int JUMP_KEY = InputConstants.KEY_SPACE;

    /** The boots every case wears; netherite so that survival fall damage cannot destroy them. */
    private static final String BOOTS_ITEM = "minecraft:netherite_boots";

    private AirJumpClientTest() {
    }

    /**
     * The whole test, as steps.
     *
     * <p>Reads the four values it is going to change - both air jump config options, the chat
     * visibility and the music toast state - before it changes them, so the last steps can put them
     * back. On a fresh client those are the defaults, but the suite shares one world and one client
     * across all tests, so "the default" is not a safe assumption to restore to.
     */
    public static void inWorld(Script script) {
        TestScene.build(script, "minecraft:stone", "creative");

        Later<Boolean> originalEnabled = new Later<>("the enableDoubleJump found on entry");
        Later<Integer> originalCooldown = new Later<>("the airJumpCooldownTicks found on entry");
        Later<ChatVisiblity> originalChat = new Later<>("the chat visibility found on entry");
        Later<MusicToastDisplayState> originalMusicToast = new Later<>("the music toast state found on entry");

        script.act("remember the config and the HUD options so the last steps can put them back",
                client -> {
                    originalEnabled.set(Simplebuilding.getConfig().enableDoubleJump);
                    originalCooldown.set(Simplebuilding.getConfig().airJumpCooldownTicks);
                    originalChat.set(client.options.chatVisibility().get());
                    originalMusicToast.set(client.options.musicToast().get());
                });

        assertJumpKeyIsBound(script);

        theAirJumpNeedsTheJumpKeyToGoDownAgainWhileAlreadyAirborne(script);
        theAirJumpNeedsThePlayerToHaveBeenAirborneInThePreviousTickToo(script);
        laddersAndWaterCountAsGroundAndBlockTheAirJump(script);
        creativeFlightBlocksTheAirJump(script);
        theAirJumpSetsUpwardVelocityAndClearsFallDistance(script);
        theCooldownCountsDownEveryTickIncludingAfterLanding(script);
        theCooldownLengthComesFromTheConfigAndHalvesAtLevelTwo(script);
        aNonPositiveCooldownConfigLetsTheAirJumpRepeatFreely(script);
        disablingTheFeatureSkipsTheJumpAndWipesTheCooldown(script);
        theAirJumpTellsTheServerWhichSpendsBootDurability(script);

        // From here on the HUD has to be on screen, which is the one thing TestScene.build
        // deliberately switches off. Everything that moves on the vanilla HUD by itself is silenced
        // in the same breath - see silenceChatAndToasts for why a noise floor does not catch it.
        TestScene.showHudAgain(script);
        silenceChatAndToasts(script);
        theHudBarReachesTheScreenWhileTheAirJumpRecharges(script);
        theHudBarGeometryColoursAndLabelAreExactlyWhatTheOverlayDraws(script);

        // --- Cleanup ----------------------------------------------------------------------------
        // What a finally block used to do. As steps these run only when everything before them
        // passed: a failing run leaves the jump key held, the config on the case's value, chat
        // hidden and the HUD however the last case left it. The next test's TestScene.build puts
        // the world, the game mode and the rendering options right again, but it knows nothing
        // about this mod's config or about the chat option - so a failure here really does leak
        // into the tests that follow, and that is the price of not having a finally block.
        script.harness("release the jump key", harness -> harness.releaseKey(JUMP_KEY));
        clearCreativeFlight(script);
        restoreConfig(script, originalEnabled, originalCooldown);
        restoreChatAndToasts(script, originalChat, originalMusicToast);
        TestScene.showHudAgain(script);
    }

    // ------------------------------------------------------------------------------------------
    // Trigger conditions
    // ------------------------------------------------------------------------------------------

    /**
     * The air jump fires on a <em>new</em> jump key press only. Holding the key does not repeat it,
     * even while the player is airborne with the cooldown ready.
     *
     * <p>Part 1 holds the key for 60 ticks starting from the ground. Vanilla makes the player hop
     * over and over, so there are plenty of ticks where everything the controller wants is true
     * except the key edge - and the probe is required to have seen such a tick, otherwise this is
     * reported as a broken setup rather than as passing. Not one of them may arm the cooldown.
     *
     * <p>Part 2 is the control: the same scene, the same key, but released and pressed again while
     * falling. Now the cooldown has to be armed exactly once. Without it, "the mod is switched off"
     * or "the boots are not on" would explain part 1 just as well.
     *
     * <p>What breaks this test: dropping {@code !jumpKeyPressed} from the trigger, or forgetting to
     * write {@code jumpKeyPressed = jumping} at the end of the tick - part 1 would then arm the
     * cooldown on the first airborne tick of every hop.
     */
    private static void theAirJumpNeedsTheJumpKeyToGoDownAgainWhileAlreadyAirborne(Script script) {
        setConfig(script, true, SHORT_COOLDOWN);
        equipBoots(script, 1);
        returnToRest(script);

        Recording held = new Recording();
        Later<List<Tick>> heldTicks = pressJumpAndRecord(script, held, "the held key from the ground",
                2, 60, 2);

        script.verify("a held jump key never arms the air jump cooldown", () -> {
            List<Tick> ticks = heldTicks.get();
            int qualifying = firstTickBlockedOnlyByTheKeyEdge(ticks);

            if (qualifying < 0) {
                throw new AssertionError("Setup failed: holding the jump key never produced a tick where "
                        + "the player was airborne in this and the previous tick, with the key down and the "
                        + "cooldown ready. Nothing about the key edge can be concluded from this run. "
                        + describe(ticks));
            }

            List<Integer> armed = cooldownArmedTicks(ticks);

            if (!armed.isEmpty()) {
                throw new AssertionError("A held jump key armed the air jump cooldown at tick(s) " + armed
                        + ", but the trigger requires the key to go down again. Tick " + qualifying
                        + " was airborne with the key already held. " + describe(ticks));
            }

            assertNeverFlew(ticks, "while the jump key was held from the ground");
        });

        // Control: release, fall, press again - the very same scene now has to arm the cooldown.
        returnToRest(script);
        Later<List<Tick>> fresh = pressJumpWhileFalling(script, "the fresh press control", 12.0, 0.0, 3);

        script.verify("control: a fresh press in the same scene does arm the cooldown", () -> {
            List<Integer> freshArmed = cooldownArmedTicks(fresh.get());

            if (freshArmed.size() != 1) {
                throw new AssertionError("Control failed: a fresh jump press while falling has to arm the "
                        + "cooldown exactly once, but it was armed at tick(s) " + freshArmed
                        + ". The negative result above would not be attributable to the key edge. "
                        + describe(fresh.get()));
            }

            assertNeverFlew(fresh.get(), "during the fresh press control");
        });
    }

    /**
     * The air jump additionally requires the player to have been airborne <em>in the previous tick
     * as well</em> ({@code !wasOnGround}), which is what keeps it from firing on the same tick as
     * the ordinary vanilla jump.
     *
     * <p>A single tap from standing isolates that term exactly. On the tick the key goes down,
     * vanilla jumps and moves within the same tick, so the controller sees: airborne now, key down
     * now, key up in the previous tick, cooldown ready, not flying - every condition satisfied
     * except that the previous tick was still grounded. The test insists that such a tick exists
     * (otherwise the setup, not the mod, is at fault) and that the cooldown stays untouched.
     *
     * <p>What breaks this test: dropping {@code !wasOnGround}, or updating {@code wasOnGround}
     * before the trigger is evaluated instead of after. Every ordinary jump would then also burn
     * the air jump.
     */
    private static void theAirJumpNeedsThePlayerToHaveBeenAirborneInThePreviousTickToo(Script script) {
        setConfig(script, true, SHORT_COOLDOWN);
        equipBoots(script, 1);
        returnToRest(script);

        Recording tap = new Recording();
        Later<List<Tick>> ticks = pressJumpAndRecord(script, tap, "the single tap from standing", 3, 2, 3);

        script.verify("the ordinary vanilla jump does not arm the air jump cooldown", () -> {
            int takeOff = firstTickBlockedOnlyByThePreviousGroundTick(ticks.get());

            if (takeOff < 0) {
                throw new AssertionError("Setup failed: tapping jump from standing never produced the "
                        + "take-off tick (airborne now, grounded in the previous tick, key freshly down, "
                        + "cooldown ready). Without it nothing can be said about the previous-tick term. "
                        + describe(ticks.get()));
            }

            List<Integer> armed = cooldownArmedTicks(ticks.get());

            if (!armed.isEmpty()) {
                throw new AssertionError("The ordinary vanilla jump already armed the air jump cooldown at "
                        + "tick(s) " + armed + ". Take-off tick was " + takeOff + ", where the player had "
                        + "still been on the ground one tick earlier. " + describe(ticks.get()));
            }
        });
    }

    /**
     * {@code grounded} is {@code onGround() || onClimbable() || isInWater()}, so a ladder and water
     * both block the air jump exactly like solid ground does.
     *
     * <p>Both halves put the player where {@code onGround()} is false and only the second or third
     * term of that expression is true, wait until the client actually reports that state, then
     * press jump freshly. Neither may arm the cooldown. The control for both is every other case in
     * this class, which arms the cooldown from free air with the very same key press.
     *
     * <p>The two structures are built here rather than once at the top of the file, because
     * {@link TestScene#build} fills the whole working volume with air and this case has to be able
     * to run on its own. The fills are tolerated ("nothing was filled" is a command error) so a
     * scene that is already correct costs nothing.
     *
     * <p>What breaks this test: shortening {@code grounded} to plain {@code onGround()}. The player
     * could then air-jump off a ladder and out of water, and both halves go red.
     */
    private static void laddersAndWaterCountAsGroundAndBlockTheAirJump(Script script) {
        setConfig(script, true, SHORT_COOLDOWN);
        equipBoots(script, 1);
        buildClimbableAndWaterShafts(script);

        returnToRest(script);
        assertNoAirJumpFrom(script, "14.5 8.0 16.5 0.0 0.0", "on a ladder",
                client -> client.player != null && client.player.onClimbable()
                        && !client.player.onGround() && !client.player.isInWater(),
                tick -> tick.climbable() && !tick.onGround() && !tick.inWater());

        returnToRest(script);
        assertNoAirJumpFrom(script, "18.5 3.0 16.5 0.0 0.0", "inside water",
                client -> client.player != null && client.player.isInWater()
                        && !client.player.onGround() && !client.player.onClimbable(),
                tick -> tick.inWater() && !tick.onGround() && !tick.climbable());
    }

    /**
     * No air jump while creative flight is on ({@code abilities.flying}).
     *
     * <p>Flight is switched on directly on the client player - which is where the controller reads
     * it - and reported to the server with {@code onUpdateAbilities} so the integrated server does
     * not fight the movement. Holding jump then flies the player upwards: airborne, key freshly
     * down, cooldown ready. Only {@code !flying} stands in the way, and the probe is required to
     * have seen exactly that combination.
     *
     * <p>The control switches flight off again in the same scene and repeats the press, which now
     * has to arm the cooldown.
     *
     * <p>What breaks this test: dropping the {@code !player.getAbilities().flying} guard. Flying
     * creative players would burn air jumps every time they gain altitude.
     */
    private static void creativeFlightBlocksTheAirJump(Script script) {
        setConfig(script, true, SHORT_COOLDOWN);
        equipBoots(script, 1);
        returnToRest(script);

        script.command("tp @a 10.5 12.0 16.5 0.0 0.0");
        script.awaitPackets();
        script.act("switch creative flight on", client -> {
            if (client.player != null) {
                client.player.getAbilities().flying = true;
                client.player.onUpdateAbilities();
            }
        });
        script.await("the client player reports creative flight", 100,
                client -> client.player != null && client.player.getAbilities().flying
                        && !client.player.onGround(),
                AirJumpClientTest::describePlayer);

        Recording flying = new Recording();
        Later<List<Tick>> flyingTicks = pressJumpAndRecord(script, flying, "the press while flying", 2, 15, 2);

        script.verify("a flying creative player does not air-jump", () -> {
            List<Tick> ticks = flyingTicks.get();
            boolean sawTheBlockedTick = false;

            for (int i = 1; i < ticks.size(); i++) {
                Tick now = ticks.get(i);
                Tick before = ticks.get(i - 1);

                if (now.flying() && !now.grounded() && now.jumping() && !before.jumping()
                        && now.cooldownRemaining() <= 0) {
                    sawTheBlockedTick = true;
                    break;
                }
            }

            if (!sawTheBlockedTick) {
                throw new AssertionError("Setup failed: never saw a tick with creative flight on, the "
                        + "player airborne, the jump key freshly down and the cooldown ready. "
                        + describe(ticks));
            }

            List<Integer> armed = cooldownArmedTicks(ticks);

            if (!armed.isEmpty()) {
                throw new AssertionError("A flying creative player air-jumped: the cooldown was armed at "
                        + "tick(s) " + armed + ". " + describe(ticks));
            }
        });

        // Control: same place, same key, flight off - now it has to fire.
        clearCreativeFlight(script);
        Later<List<Tick>> falling = pressJumpWhileFalling(script, "the flight-off control", 12.0, 0.0, 3);

        script.verify("control: with flight switched off the same press does arm the cooldown", () -> {
            if (cooldownArmedTicks(falling.get()).size() != 1) {
                throw new AssertionError("Control failed: with creative flight switched off again the same "
                        + "press has to arm the cooldown exactly once, but it armed it at "
                        + cooldownArmedTicks(falling.get()) + ". " + describe(falling.get()));
            }
        });
    }

    // ------------------------------------------------------------------------------------------
    // What the air jump does
    // ------------------------------------------------------------------------------------------

    /**
     * The air jump sets the y velocity to exactly {@code 0.5} and clears {@code fallDistance}.
     *
     * <p>The player is dropped from height and the press is delayed until it has accumulated more
     * than one block of fall distance, so "fall distance is zero afterwards" is a statement and not
     * an accident.
     *
     * <p><b>This is the one assertion in the file that depends on <em>when</em> in the tick the
     * probe samples</b>, and therefore the one that says so in its failure message. The controller
     * writes {@code 0.5} at the end of the tick and the next tick's physics immediately turns it
     * into something else, so the value is only readable in the gap between the two - which is
     * where the probe samples if, and only if, the script step runs after the mod's own tick. If
     * that ordering ever fails to hold on a loader, this assertion is what reports it, and the
     * message points at the sampling order first because that is the far likelier cause than a
     * changed impulse.
     *
     * <p>What breaks this test: changing the impulse (0.5 is what the feature is balanced around),
     * multiplying instead of assigning it, or dropping {@code player.fallDistance = 0} - a player
     * would then still take the full fall damage of the fall the air jump interrupted.
     */
    private static void theAirJumpSetsUpwardVelocityAndClearsFallDistance(Script script) {
        setConfig(script, true, SHORT_COOLDOWN);
        equipBoots(script, 1);
        returnToRest(script);

        Later<List<Tick>> recorded = pressJumpWhileFalling(script, "the impulse measurement", 20.0, 1.0, 3);

        script.verify("the air jump sets the y velocity to " + AIR_JUMP_VELOCITY
                + " and clears the fall distance", () -> {
            List<Tick> ticks = recorded.get();
            List<Integer> armed = cooldownArmedTicks(ticks);

            if (armed.size() != 1) {
                throw new AssertionError("Setup failed: expected exactly one air jump while falling, the "
                        + "cooldown was armed at " + armed + ". " + describe(ticks));
            }

            assertNeverFlew(ticks, "while falling towards the air jump");

            int jump = armed.get(0);
            Tick atJump = ticks.get(jump);
            Tick before = ticks.get(jump - 1);

            if (before.fallDistance() <= 1.0) {
                throw new AssertionError("Setup failed: the player had only fallen " + before.fallDistance()
                        + " blocks before the air jump, so a fall distance of 0 afterwards would prove "
                        + "nothing. " + describe(ticks));
            }

            if (atJump.velocityY() != AIR_JUMP_VELOCITY) {
                throw new AssertionError("The air jump did not set the y velocity to " + AIR_JUMP_VELOCITY
                        + ": it was " + atJump.velocityY() + " at the tick the cooldown was armed (it was "
                        + before.velocityY() + " one tick earlier). Suspect the SAMPLING ORDER before the "
                        + "impulse: this value is only readable in the gap between the controller writing "
                        + "it at the end of a tick and the next tick's physics overwriting it, so a probe "
                        + "step that runs before the mod's own client tick on this loader reads the "
                        + "post-physics value instead and lands here. If the velocity one tick earlier is "
                        + AIR_JUMP_VELOCITY + ", that is exactly what happened. " + describe(ticks));
            }

            if (atJump.fallDistance() != 0.0) {
                throw new AssertionError("The air jump did not clear the fall distance: it was "
                        + atJump.fallDistance() + " at the tick the cooldown was armed, after "
                        + before.fallDistance() + " one tick earlier. " + describe(ticks));
            }
        });
    }

    /**
     * The cooldown counts down by exactly one per client tick and keeps counting after the player
     * has landed - which is the whole point of it, because otherwise landing would recharge the
     * air jump and it could be spammed to cancel every fall.
     *
     * <p>The jump happens low enough that the player lands well inside the recording window, and the
     * test requires the recording to contain a decent stretch of ticks that are both on the ground
     * and still ticking the counter down.
     *
     * <p>That "exactly one per tick" can be claimed at all rests on the recording being gap free,
     * which {@link Recording#snapshot} asserts when the window closes: a probe that missed a client
     * tick would see a drop of two and report the mod as broken.
     *
     * <p>What breaks this test: moving the {@code cooldownRemaining--} behind the grounded check,
     * resetting the counter on landing, or decrementing more than once per tick.
     */
    private static void theCooldownCountsDownEveryTickIncludingAfterLanding(Script script) {
        setConfig(script, true, SHORT_COOLDOWN);
        equipBoots(script, 1);
        returnToRest(script);

        dropFrom(script, "the landing measurement", 6.0);

        Recording landing = new Recording();
        Later<List<Tick>> recorded = pressJumpAndRecord(script, landing, "the landing measurement",
                2, 3, SHORT_COOLDOWN + 20);

        script.verify("the cooldown counts down by one per tick and keeps going after landing", () -> {
            List<Tick> ticks = recorded.get();
            List<Integer> armed = cooldownArmedTicks(ticks);

            if (armed.size() != 1) {
                throw new AssertionError("Setup failed: expected exactly one air jump, the cooldown was "
                        + "armed at " + armed + ". " + describe(ticks));
            }

            int jump = armed.get(0);

            if (ticks.get(jump).cooldownRemaining() != SHORT_COOLDOWN) {
                throw new AssertionError("The air jump armed the cooldown with "
                        + ticks.get(jump).cooldownRemaining() + " ticks instead of the configured "
                        + SHORT_COOLDOWN + ". " + describe(ticks));
            }

            int groundedWhileCountingDown = 0;

            for (int i = jump + 1; i < ticks.size(); i++) {
                int previous = ticks.get(i - 1).cooldownRemaining();
                int expected = Math.max(0, previous - 1);

                if (ticks.get(i).cooldownRemaining() != expected) {
                    throw new AssertionError("The cooldown did not count down by exactly one at tick " + i
                            + ": " + previous + " -> " + ticks.get(i).cooldownRemaining() + " (expected "
                            + expected + "). " + describe(ticks));
                }

                if (previous > 0 && ticks.get(i).onGround()) {
                    groundedWhileCountingDown++;
                }
            }

            if (groundedWhileCountingDown < 5) {
                throw new AssertionError("Setup failed: the cooldown was only counted down on "
                        + groundedWhileCountingDown + " ticks where the player already stood on the "
                        + "ground, which is too few to claim it keeps running after landing. "
                        + describe(ticks));
            }

            if (ticks.get(ticks.size() - 1).cooldownRemaining() != 0) {
                throw new AssertionError("The cooldown never reached 0 within " + (SHORT_COOLDOWN + 20)
                        + " ticks of an air jump with a " + SHORT_COOLDOWN + " tick cooldown. "
                        + describe(ticks));
            }
        });
    }

    // ------------------------------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------------------------------

    /**
     * {@code airJumpCooldownTicks} is the level 1 cooldown; from level 2 upwards it is halved.
     *
     * <p>Both levels are measured with the same configured value, so the assertion is about the
     * halving and not about a balancing number. The config value used here is deliberately not the
     * default (100): that also proves the option is read at all.
     *
     * <p>What breaks this test: hard-coding the cooldown, reading a different option, or dropping
     * the level scaling.
     */
    private static void theCooldownLengthComesFromTheConfigAndHalvesAtLevelTwo(Script script) {
        setConfig(script, true, SHORT_COOLDOWN);

        equipBoots(script, 1);
        returnToRest(script);
        Later<Integer> levelOne = armCooldownAndReadMaximum(script, "at enchantment level 1");

        script.verify("at level 1 the cooldown is the configured value", () -> {
            if (levelOne.get() != SHORT_COOLDOWN) {
                throw new AssertionError("At enchantment level 1 the cooldown must be the configured "
                        + SHORT_COOLDOWN + " ticks, but the controller armed " + levelOne.get() + ".");
            }
        });

        equipBoots(script, 2);
        returnToRest(script);
        Later<Integer> levelTwo = armCooldownAndReadMaximum(script, "at enchantment level 2");

        script.verify("at level 2 the cooldown is half the configured value", () -> {
            if (levelTwo.get() != SHORT_COOLDOWN / 2) {
                throw new AssertionError("At enchantment level 2 the cooldown must be half of the "
                        + "configured " + SHORT_COOLDOWN + " ticks, i.e. " + (SHORT_COOLDOWN / 2)
                        + ", but the controller armed " + levelTwo.get() + ".");
            }
        });
    }

    /**
     * A negative {@code airJumpCooldownTicks} is clamped to 0, which at level 1 means no cooldown
     * at all - the air jump can be repeated as often as the key edge allows. At level 2 the
     * {@code Math.max(1, base / 2)} floor still leaves a single tick.
     *
     * <p>With no cooldown the cooldown counters stop being an observable, so the repetition is
     * counted through the impulse instead: a tick whose y velocity is exactly {@code 0.5} while the
     * previous tick's was not can only come from the controller, because free fall and the vanilla
     * jump never produce that value.
     *
     * <p>What breaks this test: dropping the {@code Math.max(0, ...)} clamp (a negative cooldown
     * would make {@code cooldownRemaining} negative and {@code isOnCooldown} permanently false in a
     * different way, and at level 2 the halving of a negative number would go below the floor), or
     * dropping the {@code Math.max(1, ...)} floor.
     */
    private static void aNonPositiveCooldownConfigLetsTheAirJumpRepeatFreely(Script script) {
        setConfig(script, true, -10);
        equipBoots(script, 1);
        returnToRest(script);

        dropFrom(script, "the two uncooled air jumps", 22.0);

        Recording twice = new Recording();
        script.act("start recording the two uncooled air jumps", client -> twice.reset());
        recordTicks(script, twice, "record two ticks before the first press", 2);
        script.harness("hold the jump key for the first air jump", harness -> harness.holdKey(JUMP_KEY));
        recordTicks(script, twice, "hold the jump key for the first air jump", 3);
        script.harness("release the jump key after the first air jump",
                harness -> harness.releaseKey(JUMP_KEY));
        // At least 12 ticks between the two presses: in creative a second fresh press within 7 ticks
        // while airborne toggles vanilla flight, which would block the second air jump.
        recordTicks(script, twice, "wait 12 ticks so the second press cannot toggle creative flight", 12);
        script.harness("hold the jump key for the second air jump", harness -> harness.holdKey(JUMP_KEY));
        recordTicks(script, twice, "hold the jump key for the second air jump", 3);
        script.harness("release the jump key after the second air jump",
                harness -> harness.releaseKey(JUMP_KEY));
        recordTicks(script, twice, "record two ticks after the second release", 2);

        Later<List<Tick>> recorded = new Later<>("the recorded ticks of the two uncooled air jumps");
        script.act("stop recording the two uncooled air jumps", client -> recorded.set(twice.snapshot()));

        script.verify("a negative cooldown lets the air jump repeat and clamps both counters to 0", () -> {
            List<Tick> ticks = recorded.get();
            assertNeverFlew(ticks, "during the two uncooled air jumps");

            List<Integer> kicks = upwardKickTicks(ticks);

            if (kicks.size() != 2) {
                throw new AssertionError("With the cooldown configured to a negative value two separate "
                        + "jump presses have to produce two air jumps, but the " + AIR_JUMP_VELOCITY
                        + " impulse appeared at tick(s) " + kicks + ". " + describe(ticks));
            }

            for (int kick : kicks) {
                Tick tick = ticks.get(kick);

                if (tick.cooldownRemaining() != 0 || tick.cooldownMax() != 0) {
                    throw new AssertionError("A negative cooldown config has to clamp to 0, but at tick "
                            + kick + " the controller armed remaining=" + tick.cooldownRemaining()
                            + " max=" + tick.cooldownMax() + ". " + describe(ticks));
                }
            }
        });

        // Level 2 halves the clamped 0 but the floor keeps one tick.
        equipBoots(script, 2);
        returnToRest(script);
        Later<Integer> levelTwo = armCooldownAndReadMaximum(script, "at level 2 with a negative config");

        script.verify("at level 2 the cooldown never drops below one tick", () -> {
            if (levelTwo.get() != 1) {
                throw new AssertionError("At level 2 the cooldown must never drop below 1 tick, but with "
                        + "a negative config the controller armed " + levelTwo.get() + ".");
            }
        });

        // Put the config back to the value the rest of the file works with, so this case leaves
        // nothing behind even though every case that follows sets its own value anyway.
        setConfig(script, true, SHORT_COOLDOWN);
        equipBoots(script, 1);
    }

    /**
     * With {@code enableDoubleJump} off the controller does nothing and additionally wipes both
     * cooldown counters, so the HUD bar cannot be left hanging on screen.
     *
     * <p>Three steps: arm a cooldown, switch the option off and check that the counters are 0 two
     * ticks later (they would still be around {@code SHORT_COOLDOWN - 6} if only the natural
     * countdown were running, so this cannot pass by accident); then confirm that a full jump
     * attempt produces no impulse at all; then switch the option back on and repeat the attempt as
     * a control.
     *
     * <p>What breaks this test: returning early without clearing the counters, or checking the
     * option after the trigger instead of before it.
     */
    private static void disablingTheFeatureSkipsTheJumpAndWipesTheCooldown(Script script) {
        setConfig(script, true, SHORT_COOLDOWN);
        equipBoots(script, 1);
        returnToRest(script);

        Later<List<Tick>> armedTicks = pressJumpWhileFalling(script, "the cooldown to switch off on",
                20.0, 0.0, 3);

        script.verify("setup: there is a cooldown to switch the feature off on top of", () -> {
            if (cooldownArmedTicks(armedTicks.get()).size() != 1) {
                throw new AssertionError("Setup failed: could not arm a cooldown to switch the feature off "
                        + "on top of. " + describe(armedTicks.get()));
            }
        });

        Later<Integer> beforeFlip = new Later<>("the cooldown remaining just before the option was flipped");

        script.act("read the running cooldown before the option is switched off", client -> {
            int remaining = DoubleJumpController.getCooldownRemaining();

            if (remaining <= 0) {
                throw new AssertionError("Setup failed: the cooldown had already run out (" + remaining
                        + ") before the option was switched off, so clearing it proves nothing.");
            }

            beforeFlip.set(remaining);
        });

        setConfig(script, false, SHORT_COOLDOWN);
        script.idle("let two ticks pass so the controller can wipe the counters", 2);

        script.act("switching the feature off wiped both cooldown counters", client -> {
            int remaining = DoubleJumpController.getCooldownRemaining();
            int max = DoubleJumpController.getCooldownMax();

            if (remaining != 0 || max != 0) {
                throw new AssertionError("Switching enableDoubleJump off has to wipe the cooldown, but two "
                        + "ticks later remaining=" + remaining + " and max=" + max + " (it was "
                        + beforeFlip.get() + " before the flip).");
            }
        });

        Later<List<Tick>> disabled = pressJumpWhileFalling(script, "the disabled attempt", 20.0, 0.0, 3);

        script.verify("with the feature off the air jump does not fire at all", () -> {
            if (!upwardKickTicks(disabled.get()).isEmpty() || !cooldownArmedTicks(disabled.get()).isEmpty()) {
                throw new AssertionError("The air jump still fired with enableDoubleJump switched off: "
                        + "impulse at " + upwardKickTicks(disabled.get()) + ", cooldown armed at "
                        + cooldownArmedTicks(disabled.get()) + ". " + describe(disabled.get()));
            }
        });

        // Control: switching it back on in the same scene has to make the same press work again.
        setConfig(script, true, SHORT_COOLDOWN);
        Later<List<Tick>> enabled = pressJumpWhileFalling(script, "the re-enabled control", 20.0, 0.0, 3);

        script.verify("control: switching the feature back on makes the same press work again", () -> {
            if (cooldownArmedTicks(enabled.get()).size() != 1) {
                throw new AssertionError("Control failed: with enableDoubleJump switched back on the same "
                        + "press has to arm the cooldown exactly once, but it armed it at "
                        + cooldownArmedTicks(enabled.get()) + ". " + describe(enabled.get()));
            }
        });
    }

    // ------------------------------------------------------------------------------------------
    // Client to server
    // ------------------------------------------------------------------------------------------

    /**
     * An air jump reaches the server: {@code ModMessageHandlers#handleDoubleJump} spends one point
     * of boot durability for a non-creative player, and that is the only effect of the payload that
     * cannot be explained by anything the server does on its own.
     *
     * <p>This is the one case that runs in survival, purely because the creative branch of the
     * handler does nothing observable. The player is teleported back to the ground immediately
     * after the jump so that no fall damage can add a second source of armour wear, and freshly
     * equipped boots make the expected damage value exact rather than a lower bound.
     *
     * <p>The damage is read from the <em>client's</em> copy of its own boots, because the shared
     * harness has no server handle - see the "not covered" list in the class javadoc for what that
     * costs. The game mode and the boots are restored as ordinary steps afterwards, which - like
     * every other cleanup in this file - does not happen if a step above fails.
     *
     * <p>What breaks this test: dropping the {@code ClientNetworking.send(new DoubleJumpPayload())}
     * call, or sending it outside the branch that actually performed the jump.
     */
    private static void theAirJumpTellsTheServerWhichSpendsBootDurability(Script script) {
        setConfig(script, true, SHORT_COOLDOWN);
        returnToRest(script);

        script.command("gamemode survival @a");
        script.awaitPackets();
        script.idle("let the survival game mode reach the client", 5);

        // Equipped after the game mode switch, so the boots really are fresh for the measurement:
        // a durability point spent before the switch would make "damage 1 afterwards" ambiguous.
        equipBoots(script, 1);

        script.act("setup: the freshly equipped boots are undamaged", client -> {
            int damage = client.player == null ? -1
                    : client.player.getItemBySlot(EquipmentSlot.FEET).getDamageValue();

            if (damage != 0) {
                throw new AssertionError("Setup failed: the freshly equipped boots already had damage "
                        + damage + ", so a damage of 1 afterwards would prove nothing.");
            }
        });

        Later<List<Tick>> ticks = pressJumpWhileFalling(script, "the survival air jump", 8.0, 0.0, 3);

        script.verify("setup: an air jump really happened in survival", () -> {
            if (cooldownArmedTicks(ticks.get()).size() != 1) {
                throw new AssertionError("Setup failed: no air jump happened in survival, so the "
                        + "payload was never due. " + describe(ticks.get()));
            }
        });

        // Back to the ground before the fall can damage the armour from the other direction.
        script.command("tp @a " + HOME);
        script.awaitPackets();

        script.await("the server spent exactly one point of boot durability", 200,
                client -> client.player != null
                        && client.player.getItemBySlot(EquipmentSlot.FEET).getDamageValue() == 1,
                AirJumpClientTest::describePlayer);

        script.command("gamemode creative @a");
        script.awaitPackets();
        script.idle("let the creative game mode reach the client", 5);
        equipBoots(script, 1);
    }

    // ------------------------------------------------------------------------------------------
    // HUD
    // ------------------------------------------------------------------------------------------

    /**
     * The cooldown bar really reaches the framebuffer through the registered HUD element, and it
     * disappears again once the air jump has recharged.
     *
     * <p><b>The order of the four shots is the method, not a formality.</b> Two screenshots of the
     * resting scene first, to measure the noise floor and to pin down that the scene is static -
     * without that number a pixel difference proves nothing, because "the picture drifts anyway"
     * would explain it just as well. Then every trigger condition asserted: the cooldown is really
     * running, the player is really back at the exact baseline position and standing still. Then
     * the measurement shot. Then a control shot after the cooldown has run out, which has to be
     * back at the baseline - the difference is only attributable to the bar if taking the bar away
     * again restores the picture.
     *
     * <p>The screenshot paths come from {@link Script#shot} and are read in a <em>later</em> step,
     * because the file name is not derivable from the name: Fabric writes {@code 0004_name.png}
     * with a per-run counter, NeoForge writes {@code name.png}. The comparisons themselves are
     * {@code verify} steps - they need no game state, and on Fabric a harness call from inside a
     * client task is forbidden.
     *
     * <p>On top of "something was drawn" the changed pixels are required to sit inside the band the
     * overlay claims: the 82 pixel wide bordered bar plus the label above it, centred, with its
     * bottom edge 49 GUI pixels above the bottom of the screen, converted to framebuffer pixels
     * through the current GUI scale.
     *
     * <p>What breaks this test: unregistering the HUD element, returning early from
     * {@code render} while a cooldown is running, moving the bar somewhere else on screen, or
     * leaving it on screen after the cooldown ended.
     */
    private static void theHudBarReachesTheScreenWhileTheAirJumpRecharges(Script script) {
        setConfig(script, true, LONG_COOLDOWN);
        equipBoots(script, 1);
        returnToRest(script);
        script.idle("let the resting scene settle before the baseline shots", 20);

        script.act("setup: the baseline shots contain no cooldown bar", client -> {
            int restingCooldown = DoubleJumpController.getCooldownRemaining();

            if (restingCooldown != 0) {
                throw new AssertionError("Setup failed: the baseline screenshots would already contain a "
                        + "cooldown bar (remaining=" + restingCooldown + ").");
            }
        });

        Later<Path> baseline = script.shot("airjump-a-baseline");
        // Ten ticks, so the noise floor spans real screen time rather than two frames of the same
        // render pass; anything that animates in the scene has a chance to move within it.
        script.idle("let ten ticks pass between the two baseline shots", 10);
        Later<Path> baselineAgain = script.shot("airjump-b-baseline-again");

        Later<ScreenshotDiff.Diff> noiseFloor = new Later<>("the noise floor of the resting HUD scene");

        script.verify("measure the noise floor of the resting scene", () -> {
            ScreenshotDiff.Diff diff = ScreenshotDiff.compare(
                    "noise floor (resting, no cooldown)", baseline.get(), baselineAgain.get());
            ScreenshotDiff.assertUnchanged(diff);
            noiseFloor.set(diff);
        });

        Later<List<Tick>> ticks = pressJumpWhileFalling(script, "the bar measurement", 12.0, 0.0, 3);

        script.verify("setup: there is a cooldown to show a bar for", () -> {
            if (cooldownArmedTicks(ticks.get()).size() != 1) {
                throw new AssertionError("Setup failed: no cooldown to show a bar for. "
                        + describe(ticks.get()));
            }
        });

        clearCreativeFlight(script);
        script.command("tp @a " + HOME);
        script.awaitPackets();
        script.await("the player stands at the baseline position again", 100,
                client -> client.player != null && client.player.onGround()
                        && client.player.getDeltaMovement().y <= 0.0
                        && isAtHome(client.player),
                AirJumpClientTest::describePlayer);
        script.idle("let the camera come to rest before the measurement shot", 6);

        Later<HudMetrics> metrics = new Later<>("the GUI metrics at the time of the measurement shot");

        script.act("read the GUI metrics and check the cooldown is still running", client -> {
            metrics.set(new HudMetrics(
                    client.getWindow().getGuiScaledWidth(),
                    client.getWindow().getGuiScaledHeight(),
                    client.getWindow().getGuiScale()));

            int barRemaining = DoubleJumpController.getCooldownRemaining();

            if (barRemaining <= 0) {
                throw new AssertionError("Setup failed: the cooldown ran out (" + barRemaining
                        + ") before the screenshot could be taken.");
            }
        });

        Later<Path> withBar = script.shot("airjump-c-cooldown-bar");

        script.verify("the cooldown bar reached the framebuffer", () -> {
            ScreenshotDiff.Diff signal = ScreenshotDiff.compare(
                    "air jump cooldown bar", baseline.get(), withBar.get());
            ScreenshotDiff.assertDrew("DoubleJumpHudOverlay", noiseFloor.get(), signal);
            assertChangeStaysInsideTheBarBand(baseline.get(), withBar.get(), metrics.get(), noiseFloor.get());
        });

        // Control: once the cooldown has run out the bar has to be gone and the picture back at the
        // baseline, otherwise the difference above cannot be attributed to the bar.
        script.await("the air jump recharges", 400, client -> !DoubleJumpController.isOnCooldown(),
                AirJumpClientTest::describePlayer);
        script.idle("let ten ticks pass after the cooldown ran out", 10);

        Later<Path> recharged = script.shot("airjump-d-cooldown-elapsed");

        script.verify("control: with the cooldown elapsed the picture is back at the baseline", () -> {
            ScreenshotDiff.Diff residual = ScreenshotDiff.compare(
                    "control (cooldown elapsed, bar gone)", baseline.get(), recharged.get());
            // The same allowance the straight-line ancestor used, and the same one
            // ScreenshotDiff.assertBackToBaseline applies: four times the measured noise with a
            // floor of 200 pixels, never below one pixel in 20000 of the frame.
            ScreenshotDiff.assertBackToBaseline("the elapsed air jump cooldown", noiseFloor.get(), residual);
        });
    }

    /**
     * The exact geometry, colours and label of the bar, taken from the overlay itself.
     *
     * <p>{@code DoubleJumpHudOverlay.render} is called with a {@link GuiGraphicsExtractor} subclass
     * that records {@code fill} and {@code centeredText} instead of performing them, while the
     * controller state it reads is a real cooldown produced by a real air jump. Everything the
     * overlay promises is checked against an independent copy of the numbers: an 82x7 border box, an
     * 80x5 track, an amber fill of width {@code round(80 * (max - remaining) / max)}, the whole
     * thing centred with its top edge 55 pixels above the bottom of the screen, and the label
     * {@code "Air Jump"} ten pixels above it in white.
     *
     * <p>The second half waits until the cooldown has run out and records again: nothing at all may
     * be drawn then. That is the guard which makes the green {@code FILL_READY} branch unreachable,
     * so widening it turns this red.
     *
     * <p>What breaks this test: any change to the bar's size, position, colours or label, dropping
     * the clamp on the fill fraction, or drawing the bar when no cooldown is running.
     */
    private static void theHudBarGeometryColoursAndLabelAreExactlyWhatTheOverlayDraws(Script script) {
        setConfig(script, true, LONG_COOLDOWN);
        equipBoots(script, 1);
        returnToRest(script);

        Later<List<Tick>> ticks = pressJumpWhileFalling(script, "the overlay recording", 20.0, 0.0, 3);

        script.verify("setup: there is a cooldown to render", () -> {
            if (cooldownArmedTicks(ticks.get()).size() != 1) {
                throw new AssertionError("Setup failed: no cooldown to render. " + describe(ticks.get()));
            }
        });

        // Wait until the bar is a quarter full, so the fill rectangle is unambiguously non-empty.
        script.await("the cooldown bar is at least a quarter full", 300,
                client -> DoubleJumpController.isOnCooldown()
                        && DoubleJumpController.getCooldownRemaining()
                        <= DoubleJumpController.getCooldownMax() * 3 / 4,
                AirJumpClientTest::describePlayer);

        Later<HudFrame> frame = recordHudFrame(script, "while the cooldown runs");

        script.verify("the bar's geometry, colours and label are exactly what the overlay promises", () -> {
            HudFrame recorded = frame.get();

            if (recorded.max() <= 0 || recorded.remaining() <= 0) {
                throw new AssertionError("Setup failed: recorded the overlay with remaining="
                        + recorded.remaining() + " max=" + recorded.max()
                        + ", which is not a running cooldown.");
            }

            int x = (recorded.guiWidth() - BAR_WIDTH) / 2;
            int y = recorded.guiHeight() - BAR_BOTTOM_OFFSET;
            float charged = (float) (recorded.max() - recorded.remaining()) / (float) recorded.max();
            int fillWidth = Math.round(BAR_WIDTH * charged);

            if (fillWidth <= 0) {
                throw new AssertionError("Setup failed: the recorded cooldown state ("
                        + recorded.remaining() + "/" + recorded.max() + ") produces an empty fill "
                        + "rectangle, so the fill colour would not be drawn at all.");
            }

            List<HudFill> expectedFills = List.of(
                    new HudFill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, BORDER_COLOR),
                    new HudFill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, TRACK_COLOR),
                    new HudFill(x, y, x + fillWidth, y + BAR_HEIGHT, FILL_CHARGING));

            if (!recorded.fills().equals(expectedFills)) {
                throw new AssertionError("The cooldown bar was not drawn as specified at "
                        + recorded.remaining() + "/" + recorded.max() + " on a "
                        + recorded.guiWidth() + "x" + recorded.guiHeight() + " GUI.\nexpected "
                        + expectedFills + "\nbut drew  " + recorded.fills());
            }

            List<HudText> expectedTexts = List.of(
                    new HudText(LABEL, recorded.guiWidth() / 2, y - LABEL_OFFSET, LABEL_COLOR));

            if (!recorded.texts().equals(expectedTexts)) {
                throw new AssertionError("The cooldown bar label was not drawn as specified.\nexpected "
                        + expectedTexts + "\nbut drew  " + recorded.texts());
            }
        });

        // With the HUD hidden (F1) the same running cooldown has to draw nothing. On Fabric the
        // vanilla layer the element hangs in is switched off as a whole; on NeoForge nothing but
        // the overlay's own question does it, and the air jump bar of the bootstrap script was
        // found fading on a hidden HUD at the start of the next scene, one loader only. The
        // frame is recorded with the option set and unset again in the same step, so no
        // screenshot in this script ever sees the toggle.
        Later<HudFrame> hidden = new Later<>("the recorded overlay frame with the HUD hidden");

        script.act("record what the overlay draws with the HUD hidden while the cooldown runs", client -> {
            boolean wasHidden = client.gui.hud.isHidden();

            if (!wasHidden) {
                client.gui.hud.toggle();
            }

            try {
                HudRecorder recorder = new HudRecorder(client);
                int remaining = DoubleJumpController.getCooldownRemaining();
                int max = DoubleJumpController.getCooldownMax();
                DoubleJumpHudOverlay.render(recorder);
                hidden.set(new HudFrame(List.copyOf(recorder.fills), List.copyOf(recorder.texts),
                        remaining, max, recorder.guiWidth(), recorder.guiHeight()));
            } finally {
                if (!wasHidden) {
                    client.gui.hud.toggle();
                }
            }
        });

        script.verify("with the HUD hidden the overlay draws nothing while the cooldown runs", () -> {
            HudFrame recorded = hidden.get();

            if (recorded.remaining() <= 0) {
                throw new AssertionError("Setup failed: the cooldown had run out ("
                        + recorded.remaining() + ") when the hidden frame was recorded, so a blank "
                        + "frame proves nothing about F1.");
            }

            if (!recorded.fills().isEmpty() || !recorded.texts().isEmpty()) {
                throw new AssertionError("The cooldown bar is drawn on a hidden HUD: " + recorded.fills()
                        + " " + recorded.texts() + ". F1 hides vanilla's HUD as a whole; the mod's "
                        + "overlays have to follow it on every loader, not only where the element "
                        + "registry happens to sit inside a vanilla layer.");
            }
        });

        // Once recharged the overlay has to draw nothing at all - this is the guard that makes the
        // FILL_READY branch unreachable (see the class javadoc).
        script.await("the air jump recharges", 400, client -> !DoubleJumpController.isOnCooldown(),
                AirJumpClientTest::describePlayer);

        Later<HudFrame> idle = recordHudFrame(script, "after the cooldown ran out");

        script.verify("with the cooldown elapsed the overlay draws nothing at all", () -> {
            HudFrame recorded = idle.get();

            if (recorded.remaining() != 0) {
                throw new AssertionError("Setup failed: the cooldown was still running ("
                        + recorded.remaining() + ") when the idle frame was recorded.");
            }

            if (!recorded.fills().isEmpty() || !recorded.texts().isEmpty()) {
                throw new AssertionError("The cooldown bar is still drawn after the air jump recharged: "
                        + recorded.fills() + " " + recorded.texts()
                        + ". At a remaining cooldown of 0 the fill fraction is 1.0, which is exactly the "
                        + "state the overlay's unreachable green branch would render.");
            }
        });
    }

    // ------------------------------------------------------------------------------------------
    // Scene and player setup
    // ------------------------------------------------------------------------------------------

    /**
     * Adds the two structures the grounded-expression case needs, next to (not in front of) the
     * camera and long before the first screenshot, so they can never be part of a pixel difference:
     * a ladder column high enough that a sliding player stays climbable for the whole measurement,
     * and a stone block with a sealed water shaft in it, so the water cannot flow away.
     *
     * <p>Every fill is tolerated on purpose. All four sit inside the volume
     * {@link TestScene#build} clears, so on a first run they fill something and on a repeat within
     * the same scene they fill nothing - and "no blocks were filled" is a command error. Tolerating
     * it is safe here because the case that follows waits on the client actually reporting
     * {@code onClimbable()} and {@code isInWater()} before it measures anything.
     */
    private static void buildClimbableAndWaterShafts(Script script) {
        script.command("fill 13 0 16 13 12 16 minecraft:stone", true);
        script.command("fill 14 0 16 14 12 16 minecraft:ladder[facing=east]", true);
        script.command("fill 17 -1 15 19 6 17 minecraft:stone", true);
        script.command("fill 18 0 16 18 5 16 minecraft:water", true);

        script.awaitPackets();
        script.awaitChunks();
        script.idle("let the ladder and the water shaft reach the client", 10);
    }

    /**
     * Puts freshly enchanted boots on the player's feet and waits until the <em>client</em> agrees
     * about the level.
     *
     * <p><b>Why the detour through the main hand.</b> The Fabric-only ancestor built the stack in
     * Java on the server, because the enchantment component syntax is the part most likely to shift
     * between versions. The shared harness has no server handle, so it has to be commands - and
     * {@code /enchant} only ever touches the main hand. Hence: put plain boots in the hand, enchant
     * them there (which also makes the command itself an assertion - {@code /enchant} refuses an
     * enchantment the item does not support, and a strict command failure fails the test), copy the
     * enchanted stack onto the feet with {@code item replace ... from ...}, and empty the hand
     * again. Writing the component syntax into a command string was the alternative and is exactly
     * the brittle spelling the ancestor went out of its way to avoid.
     *
     * <p><b>Emptying the hand is not tidiness.</b> {@code getDoubleJumpLevel} scans every equipment
     * slot (see the known defects in the class javadoc), so boots left in the hand would satisfy
     * the level wait below while the feet slot was still empty - and the survival durability case,
     * which reads the feet slot on the server side, would then fail for a reason no message would
     * name.
     */
    private static void equipBoots(Script script, int level) {
        script.command("item replace entity @a weapon.mainhand with " + BOOTS_ITEM);
        script.command("enchant @a " + ModEnchantments.DOUBLE_JUMP.identifier() + " " + level);
        // The source of "from entity" is EntityArgument.entity(), which rejects a selector that
        // may match more than one - "@a" does not even parse there. "@p" is the single-entity
        // form; the target of the replace stays "@a", which takes EntityArgument.entities().
        script.command("item replace entity @a armor.feet from entity @p weapon.mainhand");
        script.command("item replace entity @a weapon.mainhand with minecraft:air");
        script.awaitPackets();

        script.await("the client sees double jump level " + level + " on the feet", 100,
                client -> client.player != null
                        && DoubleJumpController.getDoubleJumpLevel(client.player) == level
                        && !client.player.getItemBySlot(EquipmentSlot.FEET).isEmpty()
                        && client.player.getMainHandItem().isEmpty(),
                AirJumpClientTest::describePlayer);
    }

    /**
     * The jump binding really is the key {@link #JUMP_KEY} presses.
     *
     * <p>New in the shared form and not optional: the harness presses a raw GLFW code, so a jump
     * key that moved would leave the player standing while every case here believed it pressed
     * jump. The negative cases would then pass without proving anything at all, which is the worst
     * failure mode a test can have.
     */
    private static void assertJumpKeyIsBound(Script script) {
        script.act("the jump binding sits on the key the harness presses", client -> {
            if (!client.options.keyJump.matches(InputConstants.Type.KEYSYM.getOrCreate(JUMP_KEY))) {
                throw new AssertionError("The jump binding is not on GLFW key " + JUMP_KEY
                        + " any more (it says \"" + client.options.keyJump.saveString() + "\"), so holding "
                        + "that key would not make the player jump - and every case in this file that "
                        + "proves the air jump does NOT fire would pass for that reason instead.");
            }
        });
    }

    /**
     * Silences the two pieces of HUD chrome that change on their own between two screenshots.
     * {@link TestScene#makeRenderingDeterministic} does not have to care about either, because it
     * hides the whole HUD - the HUD cases need the HUD visible.
     *
     * <p><b>Chat.</b> The earlier cases switch the game mode, and vanilla sends "Your game mode has
     * been updated" to the chat. A chat line stays fully opaque for 200 ticks and only then fades
     * out. That is longer than the ten ticks between the two baseline shots, so the noise floor
     * measures zero and looks healthy - but shorter than the gap to the control shot on the far
     * side of a 200 tick cooldown, so the fade lands between baseline and control and moves ~20000
     * pixels in the chat band for a reason that has nothing to do with the overlay. Hiding chat and
     * dropping the backlog removes the whole band from every screenshot in this class.
     *
     * <p><b>Toasts.</b> The now-playing toast appears whenever a background music track starts,
     * which can happen at any moment during a screenshot pair. Setting the display state to
     * {@code NEVER} drops the toast instance entirely.
     *
     * <p>Both are vanilla behaviour, not the mod's, and both are put back by
     * {@link #restoreChatAndToasts} in the last steps of {@link #inWorld} - which do not run if a
     * step above them failed.
     */
    private static void silenceChatAndToasts(Script script) {
        script.act("silence the chat and the music toast", client -> {
            client.options.chatVisibility().set(ChatVisiblity.HIDDEN);
            client.options.musicToast().set(MusicToastDisplayState.NEVER);
            client.gui.hud.getChat().clearMessages(true);
            client.gui.toastManager().setMusicToastDisplayState(MusicToastDisplayState.NEVER);
            client.gui.toastManager().clear();
        });
        script.idle("let the silenced chat take effect", 2);
    }

    /** Undoes {@link #silenceChatAndToasts}. */
    private static void restoreChatAndToasts(Script script, Later<ChatVisiblity> chatVisibility,
                                             Later<MusicToastDisplayState> musicToast) {
        script.act("put the chat and the music toast back the way they were found", client -> {
            client.options.chatVisibility().set(chatVisibility.get());
            client.options.musicToast().set(musicToast.get());
            client.gui.toastManager().setMusicToastDisplayState(musicToast.get());
        });
    }

    /** Writes both air jump options straight into the live config object the controller reads. */
    private static void setConfig(Script script, boolean enabled, int cooldownTicks) {
        script.act("set enableDoubleJump to " + enabled + " and airJumpCooldownTicks to " + cooldownTicks,
                client -> {
                    Simplebuilding.getConfig().enableDoubleJump = enabled;
                    Simplebuilding.getConfig().airJumpCooldownTicks = cooldownTicks;
                });
        script.idle("let one tick pass so the controller sees the new config", 1);
    }

    /**
     * Puts both config values back the way they were found.
     *
     * <p>This is part of the former {@code finally} block, and it is now an ordinary step: <b>it
     * does not run when a step above it failed.</b> See {@link #inWorld} for what that leaves
     * behind.
     */
    private static void restoreConfig(Script script, Later<Boolean> enabled, Later<Integer> cooldownTicks) {
        script.act("put the air jump config back the way it was found", client -> {
            Simplebuilding.getConfig().enableDoubleJump = enabled.get();
            Simplebuilding.getConfig().airJumpCooldownTicks = cooldownTicks.get();
        });
    }

    /**
     * Brings the player back to the one resting state every case starts from: key released, no
     * creative flight, standing still at {@link #HOME}, air jump recharged. The recharge is waited
     * out rather than forced through {@code enableDoubleJump}, so no case depends on the branch
     * that {@link #disablingTheFeatureSkipsTheJumpAndWipesTheCooldown} is there to prove.
     */
    private static void returnToRest(Script script) {
        script.harness("release the jump key before the next case", harness -> harness.releaseKey(JUMP_KEY));
        clearCreativeFlight(script);
        script.command("tp @a " + HOME);
        script.awaitPackets();
        script.await("the player stands still at the resting position", 200,
                client -> client.player != null && client.player.onGround(),
                AirJumpClientTest::describePlayer);
        script.idle("let the landing settle", 4);
        script.await("the air jump is recharged again", 400,
                client -> !DoubleJumpController.isOnCooldown(),
                AirJumpClientTest::describePlayer);
    }

    /** Turns vanilla creative flight off again - see the harness notes in the class javadoc. */
    private static void clearCreativeFlight(Script script) {
        script.act("switch creative flight off", client -> {
            if (client.player != null && client.player.getAbilities().flying) {
                client.player.getAbilities().flying = false;
                client.player.onUpdateAbilities();
            }
        });
        script.idle("let the ability update reach the server", 2);
    }

    /** Drops the player from {@code fromY} and waits until it is unmistakably falling through air. */
    private static void dropFrom(Script script, String label, double fromY) {
        script.harness("release the jump key before the drop (" + label + ")",
                harness -> harness.releaseKey(JUMP_KEY));
        script.command("tp @a 10.5 " + fromY + " 16.5 0.0 0.0");
        script.awaitPackets();
        clearCreativeFlight(script);
        script.await("the player falls through free air (" + label + ")", 100,
                client -> client.player != null
                        && !client.player.onGround()
                        && !client.player.onClimbable()
                        && !client.player.isInWater()
                        && client.player.getDeltaMovement().y < -0.05,
                AirJumpClientTest::describePlayer);
        // Three more ticks: the trigger needs the previous tick to have been airborne too, and they
        // also keep consecutive presses more than 7 ticks apart, out of the creative flight toggle's
        // reach (see the harness notes in the class javadoc).
        script.idle("let three more ticks of free fall pass (" + label + ")", 3);
    }

    /**
     * Drops the player, waits until it has fallen at least {@code minFallDistance} blocks and then
     * holds the jump key for {@code holdTicks} ticks. Publishes everything the probe recorded from
     * two ticks before the press to two ticks after the release.
     */
    private static Later<List<Tick>> pressJumpWhileFalling(Script script, String label, double fromY,
                                                          double minFallDistance, int holdTicks) {
        dropFrom(script, label, fromY);

        if (minFallDistance > 0.0) {
            script.await("the player has fallen more than " + minFallDistance + " blocks (" + label + ")",
                    100,
                    client -> client.player != null && client.player.fallDistance > minFallDistance,
                    AirJumpClientTest::describePlayer);
        }

        return pressJumpAndRecord(script, new Recording(), label, 2, holdTicks, 2);
    }

    /**
     * One recording window: settle, hold the jump key, release it, settle again.
     *
     * <p><b>Why the window is built out of several steps and one shared {@link Recording}.</b> A
     * step list registers everything before anything runs, so the straight-line
     * "start / wait / press / wait / stop" cannot hand a list back in a return value; the samples
     * are collected into an object owned by these steps and published through a {@link Later} that
     * the assertion steps registered <em>after</em> this call read. Reading it earlier throws by
     * name instead of handing out an empty list - which would make every "the cooldown was never
     * armed" assertion pass for a measurement that never happened.
     *
     * <p>The key presses are {@code harness} steps between two recording steps, and they finish
     * within the same client tick as the recording step before them. That is exactly why
     * {@link Recording} deduplicates by {@code player.tickCount} instead of counting invocations.
     */
    private static Later<List<Tick>> pressJumpAndRecord(Script script, Recording recording, String label,
                                                        int beforeTicks, int holdTicks, int afterTicks) {
        Later<List<Tick>> result = new Later<>("the recorded ticks of " + label);

        script.act("start recording " + label, client -> recording.reset());
        recordTicks(script, recording, "record " + beforeTicks + " ticks before the press (" + label + ")",
                beforeTicks);
        script.harness("hold the jump key (" + label + ")", harness -> harness.holdKey(JUMP_KEY));
        recordTicks(script, recording, "hold the jump key for " + holdTicks + " ticks (" + label + ")",
                holdTicks);
        script.harness("release the jump key (" + label + ")", harness -> harness.releaseKey(JUMP_KEY));
        recordTicks(script, recording, "record " + afterTicks + " ticks after the release (" + label + ")",
                afterTicks);
        script.act("stop recording " + label, client -> result.set(recording.snapshot()));

        return result;
    }

    /**
     * Appends a step that samples the client state once per client tick until {@code ticks} fresh
     * samples have been taken.
     *
     * <p>The generous timeout is not slack for a slow machine: a duplicate invocation within one
     * client tick adds no sample, so the step needs room for more invocations than samples.
     */
    private static void recordTicks(Script script, Recording recording, String name, int ticks) {
        int[] taken = {0};

        script.await(name, ticks + 60, client -> {
            if (recording.sample(client)) {
                taken[0]++;
            }

            return taken[0] >= ticks;
        }, AirJumpClientTest::describePlayer);
    }

    /**
     * Performs one air jump from a fall and publishes the cooldown length the controller armed.
     *
     * <p>Falls back to the impulse as the marker when the configured cooldown is 0: a cooldown of 0
     * arms nothing observable in the counters, and that is precisely the configuration
     * {@link #aNonPositiveCooldownConfigLetsTheAirJumpRepeatFreely} needs a reading for.
     */
    private static Later<Integer> armCooldownAndReadMaximum(Script script, String label) {
        Later<List<Tick>> ticks = pressJumpWhileFalling(script, label, 20.0, 0.0, 3);
        Later<Integer> result = new Later<>("the cooldown maximum the controller armed " + label);

        script.verify("read the cooldown the controller armed " + label, () -> {
            List<Integer> armed = cooldownArmedTicks(ticks.get());

            if (armed.size() == 1) {
                result.set(ticks.get().get(armed.get(0)).cooldownMax());
                return;
            }

            List<Integer> kicks = upwardKickTicks(ticks.get());

            if (kicks.size() == 1) {
                result.set(ticks.get().get(kicks.get(0)).cooldownMax());
                return;
            }

            throw new AssertionError("Setup failed: expected exactly one air jump " + label
                    + ", saw cooldown arming at " + armed + " and impulses at " + kicks + ". "
                    + describe(ticks.get()));
        });

        return result;
    }

    /**
     * Teleports the player to {@code position}, waits until the client reports {@code state}, then
     * presses jump freshly and requires that nothing armed the cooldown - while insisting that the
     * probe really saw the situation {@code blocked} describes with a fresh key edge on top.
     */
    private static void assertNoAirJumpFrom(Script script, String position, String what,
                                            Script.ClientCondition state, Predicate<Tick> blocked) {
        script.harness("release the jump key before moving " + what,
                harness -> harness.releaseKey(JUMP_KEY));
        script.command("tp @a " + position);
        script.awaitPackets();
        clearCreativeFlight(script);
        script.await("the player is " + what, 100, state, AirJumpClientTest::describePlayer);
        script.idle("let the state settle before pressing jump " + what, 3);

        Later<List<Tick>> recorded = pressJumpAndRecord(script, new Recording(), "the press " + what,
                2, 20, 2);

        script.verify("the player does not air-jump " + what, () -> {
            List<Tick> ticks = recorded.get();
            boolean sawTheBlockedTick = false;

            for (int i = 1; i < ticks.size(); i++) {
                Tick now = ticks.get(i);
                Tick before = ticks.get(i - 1);

                if (blocked.test(now) && now.jumping() && !before.jumping()
                        && !now.flying() && now.cooldownRemaining() <= 0) {
                    sawTheBlockedTick = true;
                    break;
                }
            }

            if (!sawTheBlockedTick) {
                throw new AssertionError("Setup failed: never saw a tick with the player " + what
                        + ", off the ground, the jump key freshly down and the cooldown ready. Nothing "
                        + "follows from the absence of an air jump. " + describe(ticks));
            }

            List<Integer> armed = cooldownArmedTicks(ticks);

            if (!armed.isEmpty()) {
                throw new AssertionError("The player air-jumped while " + what
                        + ", which the grounded check has to prevent: cooldown armed at tick(s) " + armed
                        + ". " + describe(ticks));
            }
        });
    }

    /**
     * True while the player stands exactly where the baseline screenshots were taken.
     *
     * <p>Deliberately no velocity term: a player standing on the ground carries a y velocity of
     * about {@code -0.078} at the end of every tick, because vanilla applies gravity <em>after</em>
     * the collision has already clipped the movement and only the next {@code move} zeroes it
     * again. "At rest" here therefore means "at the baseline position and not moving upwards".
     * The camera really being identical is proven by the assertions, not by this wait: the control
     * screenshot has to return to the baseline and the changed pixels have to stay inside the bar's
     * band, and a moved camera fails both.
     */
    private static boolean isAtHome(LocalPlayer player) {
        return Math.abs(player.getX() - HOME_X) < 1.0E-6
                && Math.abs(player.getY() - HOME_Y) < 1.0E-6
                && Math.abs(player.getZ() - HOME_Z) < 1.0E-6;
    }

    /** Everything the waits in this class depend on, so a timeout says which term was missing. */
    private static String describePlayer(Minecraft client) {
        if (client.player == null) {
            return "no client player";
        }

        ItemStack boots = client.player.getItemBySlot(EquipmentSlot.FEET);

        return String.format(
                "onGround=%s ladder=%s water=%s fly=%s vy=%+.5f fall=%.3f cooldown=%d/%d "
                        + "level=%d boots=%s damage=%d mainHand=%s enabled=%s configuredCooldown=%d",
                client.player.onGround(), client.player.onClimbable(), client.player.isInWater(),
                client.player.getAbilities().flying, client.player.getDeltaMovement().y,
                client.player.fallDistance,
                DoubleJumpController.getCooldownRemaining(), DoubleJumpController.getCooldownMax(),
                DoubleJumpController.getDoubleJumpLevel(client.player),
                boots, boots.getDamageValue(), client.player.getMainHandItem(),
                Simplebuilding.getConfig().enableDoubleJump,
                Simplebuilding.getConfig().airJumpCooldownTicks);
    }

    // ------------------------------------------------------------------------------------------
    // Reading the recorded tick series
    // ------------------------------------------------------------------------------------------

    /** Ticks at which the cooldown counter went up, i.e. at which an air jump armed it. */
    private static List<Integer> cooldownArmedTicks(List<Tick> ticks) {
        List<Integer> result = new ArrayList<>();

        for (int i = 1; i < ticks.size(); i++) {
            if (ticks.get(i).cooldownRemaining() > ticks.get(i - 1).cooldownRemaining()) {
                result.add(i);
            }
        }

        return result;
    }

    /**
     * Ticks at which the y velocity became exactly the air jump impulse. Used where the cooldown is
     * configured away and stops being observable; neither free fall nor the vanilla jump ever
     * produces this value, and the probe reads it before any physics runs again.
     */
    private static List<Integer> upwardKickTicks(List<Tick> ticks) {
        List<Integer> result = new ArrayList<>();

        for (int i = 1; i < ticks.size(); i++) {
            if (ticks.get(i).velocityY() == AIR_JUMP_VELOCITY
                    && ticks.get(i - 1).velocityY() != AIR_JUMP_VELOCITY) {
                result.add(i);
            }
        }

        return result;
    }

    /** A tick where every trigger condition holds except that the jump key was already down. */
    private static int firstTickBlockedOnlyByTheKeyEdge(List<Tick> ticks) {
        for (int i = 1; i < ticks.size(); i++) {
            Tick now = ticks.get(i);
            Tick before = ticks.get(i - 1);

            if (!now.grounded() && !before.grounded() && now.jumping() && before.jumping()
                    && !now.flying() && now.cooldownRemaining() <= 0) {
                return i;
            }
        }

        return -1;
    }

    /** A tick where every trigger condition holds except that the previous tick was still grounded. */
    private static int firstTickBlockedOnlyByThePreviousGroundTick(List<Tick> ticks) {
        for (int i = 1; i < ticks.size(); i++) {
            Tick now = ticks.get(i);
            Tick before = ticks.get(i - 1);

            if (!now.grounded() && before.grounded() && now.jumping() && !before.jumping()
                    && !now.flying() && now.cooldownRemaining() <= 0) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Creative flight turning itself on would silently satisfy the {@code !flying} guard and make a
     * negative result meaningless, so the cases that must not fly say so out loud.
     */
    private static void assertNeverFlew(List<Tick> ticks, String where) {
        for (int i = 0; i < ticks.size(); i++) {
            if (ticks.get(i).flying()) {
                throw new AssertionError("Setup failed: vanilla creative flight switched itself on at "
                        + "tick " + i + " " + where + " (a second jump press within 7 ticks does that). "
                        + "The air jump would be blocked for a reason that has nothing to do with the "
                        + "mod. " + describe(ticks));
            }
        }
    }

    private static String describe(List<Tick> ticks) {
        StringBuilder text = new StringBuilder("Recorded ").append(ticks.size()).append(" ticks:");

        for (Tick tick : ticks) {
            text.append("\n  ").append(tick);
        }

        return text.toString();
    }

    // ------------------------------------------------------------------------------------------
    // HUD helpers
    // ------------------------------------------------------------------------------------------

    /** Calls the overlay with a recorder instead of a real graphics context, on the client thread. */
    private static Later<HudFrame> recordHudFrame(Script script, String label) {
        Later<HudFrame> result = new Later<>("the recorded overlay frame " + label);

        script.act("record what the overlay draws " + label, client -> {
            HudRecorder recorder = new HudRecorder(client);
            int remaining = DoubleJumpController.getCooldownRemaining();
            int max = DoubleJumpController.getCooldownMax();
            DoubleJumpHudOverlay.render(recorder);
            result.set(new HudFrame(List.copyOf(recorder.fills), List.copyOf(recorder.texts),
                    remaining, max, recorder.guiWidth(), recorder.guiHeight()));
        });

        return result;
    }

    /**
     * Requires practically every changed pixel to sit inside the screen band the overlay claims:
     * the bordered bar plus the label above it, converted from GUI to framebuffer pixels through
     * the current GUI scale, with two GUI pixels of slack on each side.
     */
    private static void assertChangeStaysInsideTheBarBand(Path baseline, Path withBar, HudMetrics metrics,
                                                          ScreenshotDiff.Diff noiseFloor) {
        BufferedImage first = readImage(baseline);
        BufferedImage second = readImage(withBar);

        // Derived from the image itself rather than from Window#getGuiScale, so the check does not
        // depend on the screenshot being exactly the framebuffer size. Two GUI pixels of slack on
        // every side absorb the rounding in getGuiScaledWidth/Height.
        double scaleX = first.getWidth() / (double) metrics.guiWidth();
        double scaleY = first.getHeight() / (double) metrics.guiHeight();
        int barX = (metrics.guiWidth() - BAR_WIDTH) / 2;
        int barY = metrics.guiHeight() - BAR_BOTTOM_OFFSET;
        int left = (int) Math.floor((barX - 2) * scaleX);
        int right = (int) Math.ceil((barX + BAR_WIDTH + 2) * scaleX);
        int top = (int) Math.floor((barY - LABEL_OFFSET - 1) * scaleY);
        int bottom = (int) Math.ceil((barY + BAR_HEIGHT + 2) * scaleY);

        int inside = 0;
        int outside = 0;

        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (!isChanged(first.getRGB(x, y), second.getRGB(x, y))) {
                    continue;
                }

                if (x >= left && x < right && y >= top && y < bottom) {
                    inside++;
                } else {
                    outside++;
                }
            }
        }

        int allowedOutside = Math.max(60, noiseFloor.changedPixels() * 2);

        TestLog.info("cooldown bar pixels: " + inside + " inside x[" + left + "," + right + ") y["
                + top + "," + bottom + "), " + outside + " outside (allowed " + allowedOutside
                + ", GUI scale " + metrics.guiScale() + ")");

        if (outside > allowedOutside) {
            throw new AssertionError("The cooldown bar is not where the overlay says it is: " + outside
                    + " changed pixels fall outside x[" + left + "," + right + ") y[" + top + ","
                    + bottom + "), only " + allowedOutside + " were allowed (" + inside
                    + " pixels changed inside the band).");
        }

        // The bordered bar alone is 82x7 GUI pixels, i.e. at least 574 framebuffer pixels even at
        // GUI scale 1, so anything under 300 means the band caught only a fringe of the change.
        if (inside < 300) {
            throw new AssertionError("Only " + inside + " pixels changed inside the band the overlay "
                    + "claims to draw into, which is too few for a " + BAR_WIDTH + "x" + BAR_HEIGHT
                    + " bar at GUI scale " + metrics.guiScale() + ".");
        }
    }

    private static boolean isChanged(int first, int second) {
        if (first == second) {
            return false;
        }

        int dr = Math.abs(((first >> 16) & 0xFF) - ((second >> 16) & 0xFF));
        int dg = Math.abs(((first >> 8) & 0xFF) - ((second >> 8) & 0xFF));
        int db = Math.abs((first & 0xFF) - (second & 0xFF));
        return Math.max(dr, Math.max(dg, db)) > CHANNEL_TOLERANCE;
    }

    private static BufferedImage readImage(Path path) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());

            if (image == null) {
                throw new AssertionError("Could not decode screenshot " + path);
            }

            return image;
        } catch (IOException e) {
            throw new AssertionError("Could not read screenshot " + path, e);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Recorded data
    // ------------------------------------------------------------------------------------------

    /**
     * The tick-by-tick series one measurement window produced.
     *
     * <p>Owned by the steps of one {@link #pressJumpAndRecord} call and published through a
     * {@link Later}. It is not static and not shared: two measurements in the same script build
     * their own, so a case can never read the case before it.
     */
    private static final class Recording {

        private final List<Tick> samples = new ArrayList<>();
        private int lastClientTick = Integer.MIN_VALUE;

        /** Throws the previous window away. */
        void reset() {
            samples.clear();
            lastClientTick = Integer.MIN_VALUE;
        }

        /**
         * Takes one sample, unless this client tick has already been sampled.
         *
         * @return true when a new sample was added
         */
        boolean sample(Minecraft client) {
            LocalPlayer player = client.player;

            if (player == null) {
                return false;
            }

            if (player.tickCount == lastClientTick) {
                return false;
            }

            lastClientTick = player.tickCount;

            samples.add(new Tick(
                    samples.size(),
                    player.tickCount,
                    player.onGround() || player.onClimbable() || player.isInWater(),
                    client.options.keyJump.isDown(),
                    player.onGround(),
                    player.isInWater(),
                    player.onClimbable(),
                    player.getAbilities().flying,
                    player.getDeltaMovement().y,
                    player.fallDistance,
                    DoubleJumpController.getCooldownRemaining(),
                    DoubleJumpController.getCooldownMax()));

            return true;
        }

        /**
         * The window as a list, after checking that it has no hole in it.
         *
         * <p>The Fabric-only ancestor got a gap free series for free: its probe was a client tick
         * listener and fired on every tick by construction. A script step does not, so the
         * property every "per tick" assertion rests on is asserted here instead of assumed. A
         * missed tick would show up as a cooldown that dropped by two and would be reported as a
         * broken mod.
         */
        List<Tick> snapshot() {
            for (int i = 1; i < samples.size(); i++) {
                int previous = samples.get(i - 1).clientTick();
                int current = samples.get(i).clientTick();

                if (current != previous + 1) {
                    throw new AssertionError("The probe missed a client tick: sample " + i
                            + " is from client tick " + current + " while the one before it is from "
                            + previous + ". Every per-tick assertion in this test would be measuring a "
                            + "hole rather than the mod. " + describe(samples));
                }
            }

            if (samples.isEmpty()) {
                throw new AssertionError("The probe recorded no ticks at all - there was no client "
                        + "player for the whole measurement window.");
            }

            return List.copyOf(samples);
        }
    }

    /**
     * One client tick as the probe saw it.
     *
     * <p>{@code clientTick} is {@code player.tickCount}, and it is in the record rather than only
     * in the dedup: it is what {@link Recording#snapshot} checks for gaps, and it is what a failure
     * message needs to show that the series really is consecutive.
     */
    private record Tick(int index, int clientTick, boolean grounded, boolean jumping, boolean onGround,
                        boolean inWater, boolean climbable, boolean flying,
                        double velocityY, double fallDistance,
                        int cooldownRemaining, int cooldownMax) {

        @Override
        public String toString() {
            return String.format(
                    "t%03d (client tick %d) grounded=%-5s jump=%-5s onGround=%-5s water=%-5s "
                            + "ladder=%-5s fly=%-5s vy=%+.4f fall=%.3f cooldown=%d/%d",
                    index, clientTick, grounded, jumping, onGround, inWater, climbable, flying,
                    velocityY, fallDistance, cooldownRemaining, cooldownMax);
        }
    }

    private record HudFill(int x1, int y1, int x2, int y2, int color) {

        @Override
        public String toString() {
            return String.format("fill(%d,%d -> %d,%d, #%08X)", x1, y1, x2, y2, color);
        }
    }

    private record HudText(String literal, int x, int y, int color) {

        @Override
        public String toString() {
            return String.format("text(\"%s\" at %d,%d, #%08X)", literal, x, y, color);
        }
    }

    private record HudFrame(List<HudFill> fills, List<HudText> texts,
                            int remaining, int max, int guiWidth, int guiHeight) {
    }

    private record HudMetrics(int guiWidth, int guiHeight, int guiScale) {
    }

    /**
     * A {@link GuiGraphicsExtractor} that records the two draw calls the overlay makes instead of
     * performing them. Both overrides are total, so nothing reaches the GPU; only {@code guiWidth}
     * and {@code guiHeight} are inherited, and those are the real window's values, which is exactly
     * what the overlay bases its layout on.
     */
    private static final class HudRecorder extends GuiGraphicsExtractor {

        private final List<HudFill> fills = new ArrayList<>();
        private final List<HudText> texts = new ArrayList<>();

        private HudRecorder(Minecraft client) {
            super(client, new GuiRenderState(), 0, 0);
        }

        @Override
        public void fill(int x1, int y1, int x2, int y2, int color) {
            fills.add(new HudFill(x1, y1, x2, y2, color));
        }

        @Override
        public void centeredText(Font font, Component text, int x, int y, int color) {
            texts.add(new HudText(text.getString(), x, y, color));
        }
    }
}
