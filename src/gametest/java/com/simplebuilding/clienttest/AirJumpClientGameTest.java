package com.simplebuilding.clienttest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import javax.imageio.ImageIO;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.DoubleJumpController;
import com.simplebuilding.client.gui.DoubleJumpHudOverlay;
import com.simplebuilding.enchantment.ModEnchantments;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MusicToastDisplayState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Covers the air-jump (double jump) feature end to end on a real client: the key edge detection and
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
 * Instead a second {@code ClientTickEvents.END_CLIENT_TICK} listener is registered from the test
 * mod. Fabric events keep their registration order and the mod registers its own listener during
 * client initialisation, so the probe always runs <em>after</em> {@code DoubleJumpController#tick}
 * in the same tick and records exactly the state the controller just produced: the same
 * {@code grounded} expression the controller evaluates, the same {@code keyJump.isDown()}, the
 * y velocity, the fall distance and both cooldown counters. Every assertion below is made against
 * that tick-by-tick series. This is the same trade-off (and the same technique) as in
 * {@link MultiBlockBreakingClientGameTest}, which observes render state instead of pixels.
 *
 * <p>Because the probe samples inside the same event dispatch, {@code getDeltaMovement().y} is
 * still the literal the controller assigned and can be compared bit-exactly against {@code 0.5}:
 * no physics runs between the mod's write and the probe's read.
 *
 * <p><b>The HUD is covered twice, on purpose.</b>
 * <ul>
 *   <li>{@code theHudBarReachesTheScreenWhileTheAirJumpRecharges} is a screenshot difference test
 *       in the frozen {@link RendererTestScene}, with a measured noise floor, the bar as the only
 *       change and a control step after the cooldown has run out. It proves the overlay is really
 *       attached as a HUD element and really reaches the framebuffer - which a direct call never
 *       could - and it checks that essentially all changed pixels sit inside the screen band the
 *       overlay claims to draw into.</li>
 *   <li>{@code theHudBarGeometryColoursAndLabelAreExactlyWhatTheOverlayDraws} calls
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
 * <p><b>Harness trap this test has to dodge.</b> {@code TestInput#pressKey} presses and releases
 * within one gametest step, so {@code keyJump.isDown()} is false again by the time the client
 * ticks - the controller would never see the key at all. Every press below is therefore
 * {@code holdKey} / {@code waitTicks} / {@code releaseKey}. And in creative a second fresh jump
 * press within 7 ticks of the first, while airborne, toggles vanilla creative flight, which would
 * silently block the air jump; presses are therefore at least 12 ticks apart and
 * {@code clearCreativeFlight} runs before every phase, with the probe additionally asserting that
 * flight never turned itself on.
 *
 * <p><b>Second harness trap, this one specific to the HUD phases.</b> They need the HUD visible, so
 * {@link RendererTestScene}'s "hide everything" trick is not available and the moving parts of the
 * vanilla HUD have to be switched off one by one - see {@link #silenceChatAndToasts}. A noise floor
 * measured ten ticks apart does not catch them: a chat line only starts fading 200 ticks after it
 * arrived.
 *
 * <p><b>Known defect (reported, not written into any assertion).</b>
 * <ul>
 *   <li>{@code DoubleJumpHudOverlay.FILL_READY} (green, line 52) is dead code. The overlay returns
 *       early unless {@code DoubleJumpController.isOnCooldown()}, i.e. unless
 *       {@code remaining > 0}, and its fill fraction is {@code (max - remaining) / max}, which
 *       reaches 1.0 only at {@code remaining == 0}. The {@code charged >= 1.0f} branch can
 *       therefore never be taken.</li>
 *   <li>{@code DoubleJumpController#getDoubleJumpLevel} scans <em>all</em> {@code EquipmentSlot}
 *       values, so enchanted boots merely <em>held in a hand</em> let the client air-jump, while
 *       the server side {@code ModMessageHandlers#handleDoubleJump} only ever looks at
 *       {@code EquipmentSlot.FEET} and will not spend durability for it.</li>
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
 *       is the boundary that makes the branch dead - the recorder test asserts that at
 *       {@code remaining == 0} the overlay draws nothing at all, so widening the guard to
 *       {@code remaining >= 0} (the one realistic way to reach the green) turns it red.</li>
 *   <li><b>That an air jump sends {@code DoubleJumpPayload}.</b> The handler's only effect for a
 *       creative player is {@code player.fallDistance = 0} on the server, and the server zeroes
 *       fall distance by itself the moment the player moves upwards - the observation would not be
 *       attributable to the packet. The survival-mode effect (one point of boot durability) is
 *       covered by the phase below, which is the only reason that phase switches game modes.</li>
 *   <li><b>That Fabric and NeoForge call the same {@code tick} and register the same HUD layer.</b>
 *       That is loader wiring outside this client's control; it belongs to the client bootstrap
 *       gap list, not here.</li>
 *   <li><b>The exact drawn colour on screen.</b> The recorder sees the colour arguments the
 *       overlay passes; the framebuffer check only sees that pixels changed inside the expected
 *       band. Blending {@code 0xC0000000} over an arbitrary world background is not something a
 *       stable assertion can be built on at an unknown GUI scale.</li>
 * </ul>
 */
public final class AirJumpClientGameTest implements FabricClientGameTest {

    /** Upward velocity {@code DoubleJumpController} assigns on an air jump (line 59). */
    private static final double AIR_JUMP_VELOCITY = 0.5;

    /** Cooldown used for the behaviour phases - short enough that recharging between them is cheap. */
    private static final int SHORT_COOLDOWN = 40;

    /** Cooldown used for the HUD phases - long enough to settle the scene and still see the bar. */
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

    /** The one position every phase returns to: standing still on the scene floor, facing the wall. */
    private static final double HOME_X = 10.5;
    private static final double HOME_Y = 0.0;
    private static final double HOME_Z = 16.5;

    /** The same three numbers as a {@code /tp} argument, so there is only one source for them. */
    private static final String HOME = HOME_X + " " + HOME_Y + " " + HOME_Z + " 0.0 0.0";

    /** Per channel difference at which a pixel counts as changed - same value as {@link ScreenshotDiff}. */
    private static final int CHANNEL_TOLERANCE = 12;

    @Override
    public void runTest(ClientGameTestContext context) {
        TickProbe.install(context);

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            RendererTestScene.build(context, singleplayer, "minecraft:stone", "creative");
            buildClimbableAndWaterShafts(context, singleplayer);

            boolean originalEnabled = context.computeOnClient(client ->
                    Simplebuilding.getConfig().enableDoubleJump);
            int originalCooldown = context.computeOnClient(client ->
                    Simplebuilding.getConfig().airJumpCooldownTicks);
            ChatVisiblity originalChat = context.computeOnClient(client ->
                    client.options.chatVisibility().get());
            MusicToastDisplayState originalMusicToast = context.computeOnClient(client ->
                    client.options.musicToast().get());

            try {
                equipDoubleJumpBoots(context, singleplayer, 1);
                setConfig(context, true, SHORT_COOLDOWN);

                theAirJumpNeedsTheJumpKeyToGoDownAgainWhileAlreadyAirborne(context, singleplayer);
                theAirJumpNeedsThePlayerToHaveBeenAirborneInThePreviousTickToo(context, singleplayer);
                laddersAndWaterCountAsGroundAndBlockTheAirJump(context, singleplayer);
                creativeFlightBlocksTheAirJump(context, singleplayer);
                theAirJumpSetsUpwardVelocityAndClearsFallDistance(context, singleplayer);
                theCooldownCountsDownEveryTickIncludingAfterLanding(context, singleplayer);
                theCooldownLengthComesFromTheConfigAndHalvesAtLevelTwo(context, singleplayer);
                aNonPositiveCooldownConfigLetsTheAirJumpRepeatFreely(context, singleplayer);
                disablingTheFeatureSkipsTheJumpAndWipesTheCooldown(context, singleplayer);
                theAirJumpTellsTheServerWhichSpendsBootDurability(context, singleplayer);

                RendererTestScene.showHudAgain(context);
                silenceChatAndToasts(context);
                theHudBarReachesTheScreenWhileTheAirJumpRecharges(context, singleplayer);
                theHudBarGeometryColoursAndLabelAreExactlyWhatTheOverlayDraws(context, singleplayer);
            } finally {
                context.getInput().releaseKey(options -> options.keyJump);
                clearCreativeFlight(context);
                setConfig(context, originalEnabled, originalCooldown);
                restoreChatAndToasts(context, originalChat, originalMusicToast);
                RendererTestScene.showHudAgain(context);
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Trigger conditions
    // ------------------------------------------------------------------------------------------

    /**
     * The air jump fires on a <em>new</em> jump key press only. Holding the key does not repeat it,
     * even while the player is airborne with the cooldown ready.
     *
     * <p>Phase 1 holds the key for 60 ticks starting from the ground. Vanilla makes the player hop
     * over and over, so there are plenty of ticks where everything the controller wants is true
     * except the key edge - and the probe is required to have seen such a tick, otherwise this is
     * reported as a broken setup rather than as passing. Not one of them may arm the cooldown.
     *
     * <p>Phase 2 is the control: the same scene, the same key, but released and pressed again while
     * falling. Now the cooldown has to be armed exactly once.
     *
     * <p>What breaks this test: dropping {@code !jumpKeyPressed} from the trigger, or forgetting to
     * write {@code jumpKeyPressed = jumping} at the end of the tick - phase 1 would then arm the
     * cooldown on the first airborne tick of every hop.
     */
    private void theAirJumpNeedsTheJumpKeyToGoDownAgainWhileAlreadyAirborne(
            ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        returnToRest(context, singleplayer);

        TickProbe.start();
        context.waitTicks(2);
        context.getInput().holdKey(options -> options.keyJump);
        context.waitTicks(60);
        context.getInput().releaseKey(options -> options.keyJump);
        context.waitTicks(2);
        List<Tick> held = TickProbe.stop();

        int qualifying = firstTickBlockedOnlyByTheKeyEdge(held);

        if (qualifying < 0) {
            throw new AssertionError("Setup failed: holding the jump key never produced a tick where "
                    + "the player was airborne in this and the previous tick, with the key down and the "
                    + "cooldown ready. Nothing about the key edge can be concluded from this run. "
                    + describe(held));
        }

        List<Integer> armed = cooldownArmedTicks(held);

        if (!armed.isEmpty()) {
            throw new AssertionError("A held jump key armed the air-jump cooldown at tick(s) " + armed
                    + ", but the trigger requires the key to go down again. Tick " + qualifying
                    + " was airborne with the key already held. " + describe(held));
        }

        assertNeverFlew(held, "while the jump key was held from the ground");

        // Control: release, fall, press again - the very same scene now has to arm the cooldown.
        List<Tick> fresh = pressJumpWhileFalling(context, singleplayer, 12.0, 0.0, 3);
        List<Integer> freshArmed = cooldownArmedTicks(fresh);

        if (freshArmed.size() != 1) {
            throw new AssertionError("Control failed: a fresh jump press while falling has to arm the "
                    + "cooldown exactly once, but it was armed at tick(s) " + freshArmed
                    + ". The negative result above would not be attributable to the key edge. "
                    + describe(fresh));
        }

        assertNeverFlew(fresh, "during the fresh press control");
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
    private void theAirJumpNeedsThePlayerToHaveBeenAirborneInThePreviousTickToo(
            ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        returnToRest(context, singleplayer);

        TickProbe.start();
        context.waitTicks(3);
        context.getInput().holdKey(options -> options.keyJump);
        context.waitTicks(2);
        context.getInput().releaseKey(options -> options.keyJump);
        context.waitTicks(3);
        List<Tick> ticks = TickProbe.stop();

        int takeOff = firstTickBlockedOnlyByThePreviousGroundTick(ticks);

        if (takeOff < 0) {
            throw new AssertionError("Setup failed: tapping jump from standing never produced the "
                    + "take-off tick (airborne now, grounded in the previous tick, key freshly down, "
                    + "cooldown ready). Without it nothing can be said about the previous-tick term. "
                    + describe(ticks));
        }

        List<Integer> armed = cooldownArmedTicks(ticks);

        if (!armed.isEmpty()) {
            throw new AssertionError("The ordinary vanilla jump already armed the air-jump cooldown at "
                    + "tick(s) " + armed + ". Take-off tick was " + takeOff + ", where the player had "
                    + "still been on the ground one tick earlier. " + describe(ticks));
        }
    }

    /**
     * {@code grounded} is {@code onGround() || onClimbable() || isInWater()}, so a ladder and water
     * both block the air jump exactly like solid ground does.
     *
     * <p>Both halves put the player where {@code onGround()} is false and only the second or third
     * term of that expression is true, wait until the client actually reports that state, then
     * press jump freshly. Neither may arm the cooldown. The control for both is every other phase
     * in this class, which arms the cooldown from free air with the very same key press.
     *
     * <p>What breaks this test: shortening {@code grounded} to plain {@code onGround()}. The player
     * could then air-jump off a ladder and out of water, and both halves go red.
     */
    private void laddersAndWaterCountAsGroundAndBlockTheAirJump(
            ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        returnToRest(context, singleplayer);
        assertNoAirJumpFrom(context, singleplayer, "14.5 8.0 16.5 0.0 0.0", "a ladder",
                client -> client.player != null && client.player.onClimbable()
                        && !client.player.onGround() && !client.player.isInWater(),
                tick -> tick.climbable() && !tick.onGround() && !tick.inWater());

        returnToRest(context, singleplayer);
        assertNoAirJumpFrom(context, singleplayer, "18.5 3.0 16.5 0.0 0.0", "inside water",
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
    private void creativeFlightBlocksTheAirJump(ClientGameTestContext context,
                                                TestSingleplayerContext singleplayer) {
        returnToRest(context, singleplayer);

        singleplayer.getServer().runCommand("tp @a 10.5 12.0 16.5 0.0 0.0");
        singleplayer.getConnection().waitForClientboundPackets();
        context.runOnClient(client -> {
            if (client.player != null) {
                client.player.getAbilities().flying = true;
                client.player.onUpdateAbilities();
            }
        });
        waitOnClient(context, "the client player reports creative flight",
                client -> client.player != null && client.player.getAbilities().flying
                        && !client.player.onGround(), 100);

        TickProbe.start();
        context.waitTicks(2);
        context.getInput().holdKey(options -> options.keyJump);
        context.waitTicks(15);
        context.getInput().releaseKey(options -> options.keyJump);
        context.waitTicks(2);
        List<Tick> flying = TickProbe.stop();

        boolean sawTheBlockedTick = false;

        for (int i = 1; i < flying.size(); i++) {
            Tick now = flying.get(i);
            Tick before = flying.get(i - 1);

            if (now.flying() && !now.grounded() && now.jumping() && !before.jumping()
                    && now.cooldownRemaining() <= 0) {
                sawTheBlockedTick = true;
                break;
            }
        }

        if (!sawTheBlockedTick) {
            throw new AssertionError("Setup failed: never saw a tick with creative flight on, the "
                    + "player airborne, the jump key freshly down and the cooldown ready. "
                    + describe(flying));
        }

        List<Integer> armed = cooldownArmedTicks(flying);

        if (!armed.isEmpty()) {
            throw new AssertionError("A flying creative player air-jumped: the cooldown was armed at "
                    + "tick(s) " + armed + ". " + describe(flying));
        }

        // Control: same place, same key, flight off - now it has to fire.
        clearCreativeFlight(context);
        List<Tick> falling = pressJumpWhileFalling(context, singleplayer, 12.0, 0.0, 3);

        if (cooldownArmedTicks(falling).size() != 1) {
            throw new AssertionError("Control failed: with creative flight switched off again the same "
                    + "press has to arm the cooldown exactly once, but it armed it at "
                    + cooldownArmedTicks(falling) + ". " + describe(falling));
        }
    }

    // ------------------------------------------------------------------------------------------
    // What the air jump does
    // ------------------------------------------------------------------------------------------

    /**
     * The air jump sets the y velocity to exactly {@code 0.5} and clears {@code fallDistance}.
     *
     * <p>The player is dropped from height and the press is delayed until it has accumulated more
     * than one block of fall distance, so "fall distance is zero afterwards" is a statement and not
     * an accident. The probe reads both values in the same tick the controller wrote them, before
     * any physics runs again, which is why {@code 0.5} can be compared exactly.
     *
     * <p>What breaks this test: changing the impulse (0.5 is what the feature is balanced around),
     * multiplying instead of assigning it, or dropping {@code player.fallDistance = 0} - a player
     * would then still take the full fall damage of the fall the air jump interrupted.
     */
    private void theAirJumpSetsUpwardVelocityAndClearsFallDistance(
            ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        returnToRest(context, singleplayer);

        List<Tick> ticks = pressJumpWhileFalling(context, singleplayer, 20.0, 1.0, 3);
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
                    + before.velocityY() + " one tick earlier). " + describe(ticks));
        }

        if (atJump.fallDistance() != 0.0) {
            throw new AssertionError("The air jump did not clear the fall distance: it was "
                    + atJump.fallDistance() + " at the tick the cooldown was armed, after "
                    + before.fallDistance() + " one tick earlier. " + describe(ticks));
        }
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
     * <p>What breaks this test: moving the {@code cooldownRemaining--} behind the grounded check,
     * resetting the counter on landing, or decrementing more than once per tick.
     */
    private void theCooldownCountsDownEveryTickIncludingAfterLanding(
            ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        returnToRest(context, singleplayer);

        dropFrom(context, singleplayer, 6.0);
        TickProbe.start();
        context.waitTicks(2);
        context.getInput().holdKey(options -> options.keyJump);
        context.waitTicks(3);
        context.getInput().releaseKey(options -> options.keyJump);
        context.waitTicks(SHORT_COOLDOWN + 20);
        List<Tick> ticks = TickProbe.stop();

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
    }

    // ------------------------------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------------------------------

    /**
     * {@code airJumpCooldownTicks} is the level 1 cooldown; from level 2 upwards it is halved.
     *
     * <p>Both levels are measured with the same configured value, so the assertion is about the
     * halving and not about a balancing number. The config value used here is deliberately not the
     * default: that also proves the option is read at all.
     *
     * <p>What breaks this test: hard-coding the cooldown, reading a different option, or dropping
     * the level scaling.
     */
    private void theCooldownLengthComesFromTheConfigAndHalvesAtLevelTwo(
            ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        setConfig(context, true, SHORT_COOLDOWN);

        returnToRest(context, singleplayer);
        equipDoubleJumpBoots(context, singleplayer, 1);
        int levelOne = armCooldownAndReadMaximum(context, singleplayer);

        if (levelOne != SHORT_COOLDOWN) {
            throw new AssertionError("At enchantment level 1 the cooldown must be the configured "
                    + SHORT_COOLDOWN + " ticks, but the controller armed " + levelOne + ".");
        }

        returnToRest(context, singleplayer);
        equipDoubleJumpBoots(context, singleplayer, 2);
        int levelTwo = armCooldownAndReadMaximum(context, singleplayer);

        if (levelTwo != SHORT_COOLDOWN / 2) {
            throw new AssertionError("At enchantment level 2 the cooldown must be half of the "
                    + "configured " + SHORT_COOLDOWN + " ticks, i.e. " + (SHORT_COOLDOWN / 2)
                    + ", but the controller armed " + levelTwo + ".");
        }

        returnToRest(context, singleplayer);
        equipDoubleJumpBoots(context, singleplayer, 1);
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
    private void aNonPositiveCooldownConfigLetsTheAirJumpRepeatFreely(
            ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        setConfig(context, true, -10);
        returnToRest(context, singleplayer);
        equipDoubleJumpBoots(context, singleplayer, 1);

        dropFrom(context, singleplayer, 22.0);
        TickProbe.start();
        context.waitTicks(2);
        context.getInput().holdKey(options -> options.keyJump);
        context.waitTicks(3);
        context.getInput().releaseKey(options -> options.keyJump);
        // At least 12 ticks between the two presses: in creative a second fresh press within 7 ticks
        // while airborne toggles vanilla flight, which would block the second air jump.
        context.waitTicks(12);
        context.getInput().holdKey(options -> options.keyJump);
        context.waitTicks(3);
        context.getInput().releaseKey(options -> options.keyJump);
        context.waitTicks(2);
        List<Tick> ticks = TickProbe.stop();

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

        // Level 2 halves the clamped 0 but the floor keeps one tick.
        returnToRest(context, singleplayer);
        equipDoubleJumpBoots(context, singleplayer, 2);
        int levelTwo = armCooldownAndReadMaximum(context, singleplayer);

        if (levelTwo != 1) {
            throw new AssertionError("At level 2 the cooldown must never drop below 1 tick, but with "
                    + "a negative config the controller armed " + levelTwo + ".");
        }

        returnToRest(context, singleplayer);
        equipDoubleJumpBoots(context, singleplayer, 1);
        setConfig(context, true, SHORT_COOLDOWN);
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
    private void disablingTheFeatureSkipsTheJumpAndWipesTheCooldown(
            ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        setConfig(context, true, SHORT_COOLDOWN);
        returnToRest(context, singleplayer);

        List<Tick> armedTicks = pressJumpWhileFalling(context, singleplayer, 20.0, 0.0, 3);

        if (cooldownArmedTicks(armedTicks).size() != 1) {
            throw new AssertionError("Setup failed: could not arm a cooldown to switch the feature off "
                    + "on top of. " + describe(armedTicks));
        }

        int beforeFlip = context.computeOnClient(client -> DoubleJumpController.getCooldownRemaining());

        if (beforeFlip <= 0) {
            throw new AssertionError("Setup failed: the cooldown had already run out (" + beforeFlip
                    + ") before the option was switched off, so clearing it proves nothing.");
        }

        setConfig(context, false, SHORT_COOLDOWN);
        context.waitTicks(2);

        int remaining = context.computeOnClient(client -> DoubleJumpController.getCooldownRemaining());
        int max = context.computeOnClient(client -> DoubleJumpController.getCooldownMax());

        if (remaining != 0 || max != 0) {
            throw new AssertionError("Switching enableDoubleJump off has to wipe the cooldown, but two "
                    + "ticks later remaining=" + remaining + " and max=" + max + " (it was "
                    + beforeFlip + " before the flip).");
        }

        List<Tick> disabled = pressJumpWhileFalling(context, singleplayer, 20.0, 0.0, 3);

        if (!upwardKickTicks(disabled).isEmpty() || !cooldownArmedTicks(disabled).isEmpty()) {
            throw new AssertionError("The air jump still fired with enableDoubleJump switched off: "
                    + "impulse at " + upwardKickTicks(disabled) + ", cooldown armed at "
                    + cooldownArmedTicks(disabled) + ". " + describe(disabled));
        }

        // Control: switching it back on in the same scene has to make the same press work again.
        setConfig(context, true, SHORT_COOLDOWN);
        List<Tick> enabled = pressJumpWhileFalling(context, singleplayer, 20.0, 0.0, 3);

        if (cooldownArmedTicks(enabled).size() != 1) {
            throw new AssertionError("Control failed: with enableDoubleJump switched back on the same "
                    + "press has to arm the cooldown exactly once, but it armed it at "
                    + cooldownArmedTicks(enabled) + ". " + describe(enabled));
        }
    }

    // ------------------------------------------------------------------------------------------
    // Client to server
    // ------------------------------------------------------------------------------------------

    /**
     * An air jump reaches the server: {@code ModMessageHandlers#handleDoubleJump} spends one point
     * of boot durability for a non-creative player, and that is the only effect of the payload that
     * cannot be explained by anything the server does on its own.
     *
     * <p>This is the one phase that runs in survival, purely because the creative branch of the
     * handler does nothing observable. The player is teleported back to the ground immediately
     * after the jump so that no fall damage can add a second source of armour wear, and freshly
     * equipped boots make the expected damage value exact rather than a lower bound.
     *
     * <p>What breaks this test: dropping the {@code ClientNetworking.send(new DoubleJumpPayload())}
     * call, or sending it outside the branch that actually performed the jump.
     */
    private void theAirJumpTellsTheServerWhichSpendsBootDurability(
            ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        setConfig(context, true, SHORT_COOLDOWN);
        returnToRest(context, singleplayer);

        singleplayer.getServer().runCommand("gamemode survival @a");
        singleplayer.getConnection().waitForClientboundPackets();
        context.waitTicks(5);

        try {
            equipDoubleJumpBoots(context, singleplayer, 1);

            int before = context.computeOnClient(client -> client.player == null ? -1
                    : client.player.getItemBySlot(EquipmentSlot.FEET).getDamageValue());

            if (before != 0) {
                throw new AssertionError("Setup failed: the freshly equipped boots already had damage "
                        + before + ", so a damage of 1 afterwards would prove nothing.");
            }

            List<Tick> ticks = pressJumpWhileFalling(context, singleplayer, 8.0, 0.0, 3);

            if (cooldownArmedTicks(ticks).size() != 1) {
                throw new AssertionError("Setup failed: no air jump happened in survival, so the "
                        + "payload was never due. " + describe(ticks));
            }

            // Back to the ground before the fall can damage the armour from the other direction.
            singleplayer.getServer().runCommand("tp @a " + HOME);
            singleplayer.getConnection().waitForClientboundPackets();

            singleplayer.getServer().waitFor(server -> {
                List<ServerPlayer> players = server.getPlayerList().getPlayers();
                return !players.isEmpty()
                        && players.get(0).getItemBySlot(EquipmentSlot.FEET).getDamageValue() == 1;
            }, 200);
        } finally {
            singleplayer.getServer().runCommand("gamemode creative @a");
            singleplayer.getConnection().waitForClientboundPackets();
            context.waitTicks(5);
            equipDoubleJumpBoots(context, singleplayer, 1);
        }
    }

    // ------------------------------------------------------------------------------------------
    // HUD
    // ------------------------------------------------------------------------------------------

    /**
     * The cooldown bar really reaches the framebuffer through the registered HUD element, and it
     * disappears again once the air jump has recharged.
     *
     * <p>Standard difference test: two screenshots of the resting scene measure the noise floor and
     * pin down that the scene is static, a third is taken while the cooldown runs, and a fourth
     * after it has run out has to be back at the baseline. The player is teleported back to the
     * exact resting position and left to settle before the third shot, so the bar is the only
     * difference between baseline and signal.
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
    private void theHudBarReachesTheScreenWhileTheAirJumpRecharges(
            ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        setConfig(context, true, LONG_COOLDOWN);
        returnToRest(context, singleplayer);
        context.waitTicks(20);

        int restingCooldown = context.computeOnClient(client ->
                DoubleJumpController.getCooldownRemaining());

        if (restingCooldown != 0) {
            throw new AssertionError("Setup failed: the baseline screenshots would already contain a "
                    + "cooldown bar (remaining=" + restingCooldown + ").");
        }

        Path baseline = context.takeScreenshot("airjump-a-baseline");
        context.waitTicks(10);
        Path baselineAgain = context.takeScreenshot("airjump-b-baseline-again");

        ScreenshotDiff.Diff noiseFloor = ScreenshotDiff.compare(
                "noise floor (resting, no cooldown)", baseline, baselineAgain);
        ScreenshotDiff.assertUnchanged(noiseFloor);

        List<Tick> ticks = pressJumpWhileFalling(context, singleplayer, 12.0, 0.0, 3);

        if (cooldownArmedTicks(ticks).size() != 1) {
            throw new AssertionError("Setup failed: no cooldown to show a bar for. " + describe(ticks));
        }

        clearCreativeFlight(context);
        singleplayer.getServer().runCommand("tp @a " + HOME);
        singleplayer.getConnection().waitForClientboundPackets();
        waitOnClient(context, "the player stands at the baseline position again",
                client -> client.player != null && client.player.onGround()
                        && client.player.getDeltaMovement().y <= 0.0
                        && isAtHome(client.player), 100);
        context.waitTicks(6);

        HudMetrics metrics = readHudMetrics(context);
        int barRemaining = context.computeOnClient(client ->
                DoubleJumpController.getCooldownRemaining());

        if (barRemaining <= 0) {
            throw new AssertionError("Setup failed: the cooldown ran out (" + barRemaining
                    + ") before the screenshot could be taken.");
        }

        Path withBar = context.takeScreenshot("airjump-c-cooldown-bar");
        ScreenshotDiff.Diff signal = ScreenshotDiff.compare("air-jump cooldown bar", baseline, withBar);
        ScreenshotDiff.assertDrew("DoubleJumpHudOverlay", noiseFloor, signal);

        assertChangeStaysInsideTheBarBand(baseline, withBar, metrics, noiseFloor);

        // Control: once the cooldown has run out the bar has to be gone and the picture back at the
        // baseline, otherwise the difference above cannot be attributed to the bar.
        waitOnClient(context, "the air jump recharges",
                client -> !DoubleJumpController.isOnCooldown(), 400);
        context.waitTicks(10);

        Path recharged = context.takeScreenshot("airjump-d-cooldown-elapsed");
        ScreenshotDiff.Diff residual = ScreenshotDiff.compare(
                "control (cooldown elapsed, bar gone)", baseline, recharged);
        int allowed = Math.max(noiseFloor.changedPixels() * 4 + 200, residual.totalPixels() / 20000);

        if (residual.changedPixels() > allowed) {
            throw new AssertionError("Control failed: after the cooldown ran out the picture did not "
                    + "return to the baseline (" + residual + ", allowed " + allowed + " pixels). The "
                    + "measured difference cannot be attributed to the cooldown bar.");
        }
    }

    /**
     * The exact geometry, colours and label of the bar, taken from the overlay itself.
     *
     * <p>{@code DoubleJumpHudOverlay.render} is called with a {@link GuiGraphicsExtractor} subclass
     * that records {@code fill} and {@code centeredText} instead of performing them, while the
     * controller state it reads is a real cooldown produced by a real air jump. Everything the
     * overlay promises is checked against an independent copy of the numbers: a 82x7 border box, a
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
    private void theHudBarGeometryColoursAndLabelAreExactlyWhatTheOverlayDraws(
            ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        setConfig(context, true, LONG_COOLDOWN);
        returnToRest(context, singleplayer);

        List<Tick> ticks = pressJumpWhileFalling(context, singleplayer, 20.0, 0.0, 3);

        if (cooldownArmedTicks(ticks).size() != 1) {
            throw new AssertionError("Setup failed: no cooldown to render. " + describe(ticks));
        }

        // Wait until the bar is a quarter full, so the fill rectangle is unambiguously non-empty.
        waitOnClient(context, "the cooldown bar is at least a quarter full",
                client -> DoubleJumpController.isOnCooldown()
                        && DoubleJumpController.getCooldownRemaining()
                        <= DoubleJumpController.getCooldownMax() * 3 / 4, 300);

        HudFrame frame = recordHudFrame(context);

        if (frame.max() <= 0 || frame.remaining() <= 0) {
            throw new AssertionError("Setup failed: recorded the overlay with remaining="
                    + frame.remaining() + " max=" + frame.max() + ", which is not a running cooldown.");
        }

        int x = (frame.guiWidth() - BAR_WIDTH) / 2;
        int y = frame.guiHeight() - BAR_BOTTOM_OFFSET;
        float charged = (float) (frame.max() - frame.remaining()) / (float) frame.max();
        int fillWidth = Math.round(BAR_WIDTH * charged);

        if (fillWidth <= 0) {
            throw new AssertionError("Setup failed: the recorded cooldown state ("
                    + frame.remaining() + "/" + frame.max() + ") produces an empty fill rectangle, so "
                    + "the fill colour would not be drawn at all.");
        }

        List<HudFill> expectedFills = List.of(
                new HudFill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, BORDER_COLOR),
                new HudFill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, TRACK_COLOR),
                new HudFill(x, y, x + fillWidth, y + BAR_HEIGHT, FILL_CHARGING));

        if (!frame.fills().equals(expectedFills)) {
            throw new AssertionError("The cooldown bar was not drawn as specified at "
                    + frame.remaining() + "/" + frame.max() + " on a "
                    + frame.guiWidth() + "x" + frame.guiHeight() + " GUI.\nexpected " + expectedFills
                    + "\nbut drew  " + frame.fills());
        }

        List<HudText> expectedTexts = List.of(
                new HudText(LABEL, frame.guiWidth() / 2, y - LABEL_OFFSET, LABEL_COLOR));

        if (!frame.texts().equals(expectedTexts)) {
            throw new AssertionError("The cooldown bar label was not drawn as specified.\nexpected "
                    + expectedTexts + "\nbut drew  " + frame.texts());
        }

        // Once recharged the overlay has to draw nothing at all - this is the guard that makes the
        // FILL_READY branch unreachable (see the class javadoc).
        waitOnClient(context, "the air jump recharges",
                client -> !DoubleJumpController.isOnCooldown(), 400);

        HudFrame idle = recordHudFrame(context);

        if (idle.remaining() != 0) {
            throw new AssertionError("Setup failed: the cooldown was still running (" + idle.remaining()
                    + ") when the idle frame was recorded.");
        }

        if (!idle.fills().isEmpty() || !idle.texts().isEmpty()) {
            throw new AssertionError("The cooldown bar is still drawn after the air jump recharged: "
                    + idle.fills() + " " + idle.texts()
                    + ". At a remaining cooldown of 0 the fill fraction is 1.0, which is exactly the "
                    + "state the overlay's unreachable green branch would render.");
        }
    }

    // ------------------------------------------------------------------------------------------
    // Scene and player setup
    // ------------------------------------------------------------------------------------------

    /**
     * Adds the two structures the grounded-expression phases need, next to (not in front of) the
     * camera and long before the first screenshot, so they can never be part of a pixel difference:
     * a ladder column high enough that a sliding player stays climbable for the whole measurement,
     * and a stone block with a sealed water shaft in it, so the water cannot flow away.
     */
    private void buildClimbableAndWaterShafts(ClientGameTestContext context,
                                              TestSingleplayerContext singleplayer) {
        String[] commands = {
                "fill 13 0 16 13 12 16 minecraft:stone",
                "fill 14 0 16 14 12 16 minecraft:ladder[facing=east]",
                "fill 17 -1 15 19 6 17 minecraft:stone",
                "fill 18 0 16 18 5 16 minecraft:water",
        };

        for (String command : commands) {
            singleplayer.getServer().runCommand(command);
        }

        singleplayer.getConnection().waitForClientboundPackets();
        singleplayer.getConnection().waitForChunksRender();
        context.waitTicks(10);
    }

    /**
     * Puts freshly enchanted boots on the player's feet and waits until the <em>client</em> agrees
     * about the level. Built in Java on the server rather than through a command: the enchantment
     * component syntax is the part most likely to shift between versions, and the client gametest
     * API's {@code runCommand} swallows brigadier errors instead of failing.
     */
    private void equipDoubleJumpBoots(ClientGameTestContext context,
                                      TestSingleplayerContext singleplayer, int level) {
        singleplayer.getServer().runOnServer(server -> {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();

            if (players.isEmpty()) {
                throw new AssertionError("Setup failed: no server player to equip.");
            }

            Holder<Enchantment> doubleJump = server.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(ModEnchantments.DOUBLE_JUMP);
            ItemStack boots = new ItemStack(Items.NETHERITE_BOOTS);
            boots.enchant(doubleJump, level);
            players.get(0).setItemSlot(EquipmentSlot.FEET, boots);
        });

        singleplayer.getConnection().waitForClientboundPackets();
        waitOnClient(context, "the client sees double jump level " + level,
                client -> client.player != null
                        && DoubleJumpController.getDoubleJumpLevel(client.player) == level, 100);
    }

    /**
     * Silences the two pieces of HUD chrome that change on their own between two screenshots.
     * {@link RendererTestScene#makeRenderingDeterministic} does not have to care about either,
     * because it hides the whole HUD - these phases need the HUD visible.
     *
     * <p><b>Chat.</b> The earlier phases switch the game mode, and vanilla sends "Your game mode has
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
     * <p>Both are vanilla behaviour, not the mod's, and both are restored in the {@code finally} of
     * {@link #runTest} so the classes that run after this one see the client they expect.
     */
    private void silenceChatAndToasts(ClientGameTestContext context) {
        context.runOnClient(client -> {
            client.options.chatVisibility().set(ChatVisiblity.HIDDEN);
            client.options.musicToast().set(MusicToastDisplayState.NEVER);
            client.gui.hud.getChat().clearMessages(true);
            client.gui.toastManager().setMusicToastDisplayState(MusicToastDisplayState.NEVER);
            client.gui.toastManager().clear();
        });
        context.waitTicks(2);
    }

    /** Undoes {@link #silenceChatAndToasts}. */
    private void restoreChatAndToasts(ClientGameTestContext context, ChatVisiblity chatVisibility,
                                      MusicToastDisplayState musicToast) {
        context.runOnClient(client -> {
            client.options.chatVisibility().set(chatVisibility);
            client.options.musicToast().set(musicToast);
            client.gui.toastManager().setMusicToastDisplayState(musicToast);
        });
        context.waitTick();
    }

    /** Writes both air-jump options straight into the live config object the controller reads. */
    private void setConfig(ClientGameTestContext context, boolean enabled, int cooldownTicks) {
        context.runOnClient(client -> {
            Simplebuilding.getConfig().enableDoubleJump = enabled;
            Simplebuilding.getConfig().airJumpCooldownTicks = cooldownTicks;
        });
        context.waitTick();
    }

    /**
     * Brings the player back to the one resting state every phase starts from: key released, no
     * creative flight, standing still at {@link #HOME}, air jump recharged. The recharge is waited
     * out rather than forced through {@code enableDoubleJump}, so no phase depends on the branch
     * that {@code disablingTheFeatureSkipsTheJumpAndWipesTheCooldown} is there to prove.
     */
    private void returnToRest(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        context.getInput().releaseKey(options -> options.keyJump);
        clearCreativeFlight(context);
        singleplayer.getServer().runCommand("tp @a " + HOME);
        singleplayer.getConnection().waitForClientboundPackets();
        waitOnClient(context, "the player stands still at the resting position",
                client -> client.player != null && client.player.onGround(), 200);
        context.waitTicks(4);
        waitOnClient(context, "the air jump recharges",
                client -> !DoubleJumpController.isOnCooldown(), 400);
    }

    /** Turns vanilla creative flight off again - see the harness note in the class javadoc. */
    private void clearCreativeFlight(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (client.player != null && client.player.getAbilities().flying) {
                client.player.getAbilities().flying = false;
                client.player.onUpdateAbilities();
            }
        });
        context.waitTicks(2);
    }

    /** Drops the player from {@code fromY} and waits until it is unmistakably falling through air. */
    private void dropFrom(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                          double fromY) {
        context.getInput().releaseKey(options -> options.keyJump);
        singleplayer.getServer().runCommand("tp @a 10.5 " + fromY + " 16.5 0.0 0.0");
        singleplayer.getConnection().waitForClientboundPackets();
        clearCreativeFlight(context);
        waitOnClient(context, "the player falls through free air",
                client -> client.player != null
                        && !client.player.onGround()
                        && !client.player.onClimbable()
                        && !client.player.isInWater()
                        && client.player.getDeltaMovement().y < -0.05, 100);
        // Three more ticks: the trigger needs the previous tick to have been airborne too, and they
        // also keep consecutive presses more than 7 ticks apart, out of the creative flight toggle's
        // reach (see the harness note in the class javadoc).
        context.waitTicks(3);
    }

    /**
     * Drops the player, waits until it has fallen at least {@code minFallDistance} blocks and then
     * holds the jump key for {@code holdTicks} ticks. Returns everything the probe recorded from two
     * ticks before the press to two ticks after the release.
     */
    private List<Tick> pressJumpWhileFalling(ClientGameTestContext context,
                                             TestSingleplayerContext singleplayer,
                                             double fromY, double minFallDistance, int holdTicks) {
        dropFrom(context, singleplayer, fromY);

        if (minFallDistance > 0.0) {
            waitOnClient(context, "the player has fallen more than " + minFallDistance + " blocks",
                    client -> client.player != null && client.player.fallDistance > minFallDistance, 100);
        }

        TickProbe.start();
        context.waitTicks(2);
        context.getInput().holdKey(options -> options.keyJump);
        context.waitTicks(holdTicks);
        context.getInput().releaseKey(options -> options.keyJump);
        context.waitTicks(2);
        return TickProbe.stop();
    }

    /** Performs one air jump from a fall and returns the cooldown length the controller armed. */
    private int armCooldownAndReadMaximum(ClientGameTestContext context,
                                          TestSingleplayerContext singleplayer) {
        List<Tick> ticks = pressJumpWhileFalling(context, singleplayer, 20.0, 0.0, 3);
        List<Integer> armed = cooldownArmedTicks(ticks);

        if (armed.size() == 1) {
            return ticks.get(armed.get(0)).cooldownMax();
        }

        // A cooldown of 0 arms nothing observable, so fall back to the impulse as the marker.
        List<Integer> kicks = upwardKickTicks(ticks);

        if (kicks.size() == 1) {
            return ticks.get(kicks.get(0)).cooldownMax();
        }

        throw new AssertionError("Setup failed: expected exactly one air jump, saw cooldown arming at "
                + armed + " and impulses at " + kicks + ". " + describe(ticks));
    }

    /**
     * Teleports the player to {@code position}, waits until the client reports {@code state}, then
     * presses jump freshly and requires that nothing armed the cooldown - while insisting that the
     * probe really saw the situation {@code blocked} describes with a fresh key edge on top.
     */
    private void assertNoAirJumpFrom(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                                     String position, String what, Predicate<Minecraft> state,
                                     Predicate<Tick> blocked) {
        context.getInput().releaseKey(options -> options.keyJump);
        singleplayer.getServer().runCommand("tp @a " + position);
        singleplayer.getConnection().waitForClientboundPackets();
        clearCreativeFlight(context);
        waitOnClient(context, "the player is " + what, state, 100);
        context.waitTicks(3);

        TickProbe.start();
        context.waitTicks(2);
        context.getInput().holdKey(options -> options.keyJump);
        context.waitTicks(20);
        context.getInput().releaseKey(options -> options.keyJump);
        context.waitTicks(2);
        List<Tick> ticks = TickProbe.stop();

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
    }

    private void waitOnClient(ClientGameTestContext context, String what,
                              Predicate<Minecraft> condition, int timeoutTicks) {
        try {
            context.waitFor(condition, timeoutTicks);
        } catch (RuntimeException | AssertionError e) {
            throw new AssertionError("Scene setup failed: " + what + " never became true within "
                    + timeoutTicks + " ticks. " + describePlayer(context) + " "
                    + RendererTestScene.describeAim(context), e);
        }
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
    private static String describePlayer(ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            if (client.player == null) {
                return "no client player";
            }

            return String.format(
                    "onGround=%s ladder=%s water=%s fly=%s vy=%+.5f fall=%.3f cooldown=%d/%d "
                            + "level=%d enabled=%s configuredCooldown=%d",
                    client.player.onGround(), client.player.onClimbable(), client.player.isInWater(),
                    client.player.getAbilities().flying, client.player.getDeltaMovement().y,
                    client.player.fallDistance,
                    DoubleJumpController.getCooldownRemaining(), DoubleJumpController.getCooldownMax(),
                    DoubleJumpController.getDoubleJumpLevel(client.player),
                    Simplebuilding.getConfig().enableDoubleJump,
                    Simplebuilding.getConfig().airJumpCooldownTicks);
        });
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
     * Ticks at which the y velocity became exactly the air-jump impulse. Used where the cooldown is
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
     * negative result meaningless, so the phases that must not fly say so out loud.
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

    private HudMetrics readHudMetrics(ClientGameTestContext context) {
        return context.computeOnClient(client -> new HudMetrics(
                client.getWindow().getGuiScaledWidth(),
                client.getWindow().getGuiScaledHeight(),
                client.getWindow().getGuiScale()));
    }

    private HudFrame recordHudFrame(ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            HudRecorder recorder = new HudRecorder(client);
            int remaining = DoubleJumpController.getCooldownRemaining();
            int max = DoubleJumpController.getCooldownMax();
            DoubleJumpHudOverlay.render(recorder);
            return new HudFrame(List.copyOf(recorder.fills), List.copyOf(recorder.texts),
                    remaining, max, recorder.guiWidth(), recorder.guiHeight());
        });
    }

    /**
     * Requires practically every changed pixel to sit inside the screen band the overlay claims:
     * the bordered bar plus the label above it, converted from GUI to framebuffer pixels through
     * the current GUI scale, with one GUI pixel of slack on each side.
     */
    private void assertChangeStaysInsideTheBarBand(Path baseline, Path withBar, HudMetrics metrics,
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

        System.out.println("[simplebuilding-test] cooldown bar pixels: " + inside + " inside "
                + "x[" + left + "," + right + ") y[" + top + "," + bottom + "), " + outside + " outside "
                + "(allowed " + allowedOutside + ", GUI scale " + metrics.guiScale() + ")");

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

    /** One client tick as the probe saw it, immediately after {@code DoubleJumpController#tick}. */
    private record Tick(int index, boolean grounded, boolean jumping, boolean onGround,
                        boolean inWater, boolean climbable, boolean flying,
                        double velocityY, double fallDistance,
                        int cooldownRemaining, int cooldownMax) {

        @Override
        public String toString() {
            return String.format(
                    "t%03d grounded=%-5s jump=%-5s onGround=%-5s water=%-5s ladder=%-5s fly=%-5s "
                            + "vy=%+.4f fall=%.3f cooldown=%d/%d",
                    index, grounded, jumping, onGround, inWater, climbable, flying,
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

    /**
     * Records the client state at the end of every client tick, right after
     * {@code DoubleJumpController#tick} has run.
     *
     * <p>Registered lazily from the test thread: Fabric events keep their registration order and the
     * mod registers its own {@code END_CLIENT_TICK} listener during client initialisation, so this
     * listener always runs after it and sees the state the controller just produced. {@code grounded}
     * is recomputed with the very same expression the controller uses, so an assertion can say which
     * term of the trigger was the blocking one.
     */
    private static final class TickProbe {

        private static final List<Tick> SAMPLES = Collections.synchronizedList(new ArrayList<>());
        private static volatile boolean installed;
        private static volatile boolean recording;

        private TickProbe() {
        }

        static void install(ClientGameTestContext context) {
            context.runOnClient(client -> {
                if (installed) {
                    return;
                }

                installed = true;
                ClientTickEvents.END_CLIENT_TICK.register(TickProbe::sample);
            });
        }

        private static void sample(Minecraft client) {
            if (!recording) {
                return;
            }

            LocalPlayer player = client.player;

            if (player == null) {
                return;
            }

            SAMPLES.add(new Tick(
                    SAMPLES.size(),
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
        }

        static void start() {
            recording = false;

            synchronized (SAMPLES) {
                SAMPLES.clear();
            }

            recording = true;
        }

        static List<Tick> stop() {
            recording = false;

            synchronized (SAMPLES) {
                return List.copyOf(SAMPLES);
            }
        }
    }
}
