package com.simplebuilding.clientgametest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.client.Minecraft;

/**
 * A client test written as a list of steps, advanced one client tick at a time.
 *
 * <p><b>Why a step list and not straight-line code.</b> Fabric hands a client test its own thread
 * and lets it block; NeoForge has no client test API at all, so its tests are driven from the
 * client tick and can never block - {@code Minecraft.disconnect} pumps client ticks itself and
 * re-enters {@code runTick} if you call it from inside one, which is how an earlier attempt at a
 * NeoForge client test deadlocked. Fabric can only offer its blocking shape because the framework
 * replaces the tick loop with a four phase barrier: 21 mixin injection points into 14 vanilla
 * methods. Reproducing that under NeoForge would be writing a framework, not an adapter.
 *
 * <p>So the shared form is the weaker one. A step list runs on Fabric in a loop
 * ({@code while (!script.tick(log)) context.waitTick();}) and on NeoForge straight off the tick
 * event; the other direction does not exist. Each test body is written once, against this class,
 * and every target drives it with a driver of a few dozen lines.
 *
 * <p><b>Two kinds of step, and the difference matters.</b> {@link #act} runs on the client thread;
 * {@link #harness} runs wherever the harness wants its own calls to happen - on Fabric that is the
 * test thread, and Fabric enforces it: {@code TestInput} calls
 * {@code ThreadingImpl.checkOnGametestThread} and throws if you drive input from inside
 * {@code runOnClient}. Screenshots, input and server commands are therefore harness steps;
 * everything that touches the game state directly is an act.
 *
 * <p>Every step carries its own tick budget, and running out of it fails with the step's name in
 * the message. A hung test says <em>where</em> it hung instead of just stopping.
 */
public final class Script {

    /** One step. Gets the ticks it has already been running; returns true when it is done. */
    @FunctionalInterface
    public interface Step {
        boolean tick(int ticksInStep) throws Exception;
    }

    /** Work on the client thread. */
    @FunctionalInterface
    public interface ClientWork {
        void run(Minecraft client) throws Exception;
    }

    /** A question asked of the client, on the client thread. */
    @FunctionalInterface
    public interface ClientCondition {
        boolean test(Minecraft client) throws Exception;
    }

    /** Work that goes through the harness - screenshots, input, server commands. */
    @FunctionalInterface
    public interface HarnessWork {
        void run(Harness harness) throws Exception;
    }

    /**
     * One step's body, as the harness gets it.
     *
     * <p>Its own type rather than {@code Supplier<Boolean>} because a step is allowed to throw:
     * an assertion inside a step is how a client test fails, and swallowing it here would turn a
     * failing test into a hanging one.
     */
    @FunctionalInterface
    public interface Body {
        boolean run() throws Exception;
    }

    /** Work that needs neither the client nor the harness - a comparison, an assertion. */
    @FunctionalInterface
    public interface Work {
        void run() throws Exception;
    }

    /** How a step wants to be run. */
    public enum Where {
        /** On the client thread. */
        CLIENT,
        /** Wherever the harness runs its own calls - never inside a client task. */
        HARNESS,
    }

    private record Entry(String name, int timeoutTicks, Where where, Step step,
                         Function<Minecraft, String> diagnosis) {
        Entry(String name, int timeoutTicks, Where where, Step step) {
            this(name, timeoutTicks, where, step, null);
        }
    }

    private final String testName;
    private final List<Entry> entries = new ArrayList<>();
    private int index;
    private int ticksInStep;

    public Script(String testName) {
        this.testName = testName;
    }

    public String testName() {
        return testName;
    }

    /** A step that does its work on the client thread in one go. */
    public void act(String name, ClientWork work) {
        entries.add(new Entry(name, 2, Where.CLIENT, ticks -> {
            work.run(Minecraft.getInstance());
            return true;
        }));
    }

    /** A step that goes through the harness - screenshot, input, server command. */
    public void harness(String name, HarnessWork work) {
        entries.add(new Entry(name, 2, Where.HARNESS, ticks -> {
            work.run(currentHarness);
            return true;
        }));
    }

    /** Waits until the client answers yes, failing after {@code timeoutTicks}. */
    public void await(String name, int timeoutTicks, ClientCondition condition) {
        await(name, timeoutTicks, condition, null);
    }

    /**
     * Waits, and says what the client actually looked like if the wait runs out.
     *
     * <p>The diagnosis is worth its weight here. A bare "step timed out" on a scene setup step
     * tells the reader nothing; the same failure with the player position, the view angles and
     * what the crosshair really hit usually names the cause outright.
     */
    public void await(String name, int timeoutTicks, ClientCondition condition,
                      Function<Minecraft, String> diagnosis) {
        entries.add(new Entry(name, timeoutTicks, Where.CLIENT,
                ticks -> condition.test(Minecraft.getInstance()), diagnosis));
    }

    /** Lets {@code ticks} client ticks pass. */
    public void idle(String name, int ticks) {
        entries.add(new Entry(name, ticks + 20, Where.CLIENT, t -> t >= ticks));
    }

    /**
     * Takes a screenshot under {@code name}.
     *
     * <p>The name is the unit the test runner counts: it reads the names out of the sources and
     * expects a file per name, newer than the start of the run. A test that died half way through
     * takes the later shots with it, and the missing names say where it stopped.
     */
    public Later<Path> shot(String name) {
        Later<Path> path = new Later<>("the file of screenshot '" + name + "'");
        entries.add(new Entry("shot " + name, 200, Where.HARNESS, ticks -> {
            if (!currentHarness.screenshot(name, ticks)) {
                return false;
            }
            // Asked for right after the shot finished, because that is the only moment both
            // loaders can answer it: Fabric numbers its files (0004_name.png) with a per-run
            // counter, NeoForge writes name.png. Deriving the path from the name in shared code
            // would quietly compare the wrong files on one of them.
            path.set(currentHarness.screenshotPath(name));
            return true;
        }));
        return path;
    }

    /**
     * A step that computes and asserts without touching the game - comparing screenshots, checking
     * a value an earlier step captured.
     *
     * <p>Runs off the client thread on Fabric, which is where such work belongs: it needs no game
     * state, and putting it inside a client task would forbid every harness call around it.
     */
    public void verify(String name, Work work) {
        entries.add(new Entry(name, 20, Where.HARNESS, ticks -> {
            work.run();
            return true;
        }));
    }

    /**
     * Waits until two frames a few ticks apart are the same picture, rebuilding the chunks in
     * between when they are not.
     *
     * <p>This is the barrier every pixel test in the suite silently assumed and none of the other
     * waits could give: "the packets arrived" and "the chunks are built" are both true of a scene
     * that is still changing on screen. It was paid for twice - a ladder column from an earlier
     * test still fading out of the corner of the frame seven seconds after the fill that removed
     * it, and on another loader the very same corner again after a chunk rebuild and a render
     * barrier had been added. The scene's own noise floor step then failed with a true and useless
     * sentence ("4950 changed pixels while nothing changed on screen").
     *
     * <p>So the scene proves itself still before anything is measured: a frame, {@code gapTicks}
     * of waiting, a second frame, and the two have to agree by the same rule the noise floor
     * uses. When they do not, {@code betweenAttempts} runs on the client (the scene build passes
     * a full chunk rebuild) and the pair is taken again, up to {@code attempts} times. Only then
     * does it fail - and it fails with WHERE the picture changed and what entities the client has
     * near the player, which is the sentence the noise floor could not say.
     *
     * <p>The settle shots are named without a hyphen on purpose: the test runner reads every
     * hyphenated string out of this file as a promised checkpoint.
     */
    public void awaitStableFrame(String what, int gapTicks, int attempts, ClientWork betweenAttempts) {
        int[] attempt = {0};
        int[] phase = {0};
        int[] phaseStart = {0};
        Path[] first = {null};
        String[] lastVerdict = {""};
        // Unique across the whole run, not just across attempts. The NeoForge driver answers a
        // screenshot name it has already taken with that earlier file, immediately - so a barrier
        // that reused "settle0a" ran for real in the first scene build of the run and was a no-op
        // in every one after it, and the second script's noise floor failed on a scene the
        // barrier had never looked at.
        int serial = SETTLE_SERIAL.incrementAndGet();

        entries.add(new Entry("the " + what + " holds still between two frames", (gapTicks + 260) * attempts + 60,
                Where.HARNESS, ticks -> {
            String nameA = "settle" + serial + "x" + attempt[0] + "a";
            String nameB = "settle" + serial + "x" + attempt[0] + "b";

            if (phase[0] == 0) {
                // The rebuild between attempts empties the render queue and refills it over the
                // next ticks; a first frame taken in that window is a frame of sky, and every
                // second frame then differs from it in all of its pixels - which is what one
                // driver reported four attempts running, so the barrier is here and not left
                // to the screenshot.
                if (!currentHarness.chunksRendered()
                        || !currentHarness.screenshot(nameA, ticks - phaseStart[0])) {
                    return false;
                }
                first[0] = currentHarness.screenshotPath(nameA);
                phase[0] = 1;
                phaseStart[0] = ticks;
                return false;
            }

            if (phase[0] == 1) {
                if (ticks - phaseStart[0] < gapTicks) {
                    return false;
                }
                phase[0] = 2;
                phaseStart[0] = ticks;
                return false;
            }

            if (!currentHarness.screenshot(nameB, ticks - phaseStart[0])) {
                return false;
            }

            ScreenshotDiff.ChangedArea area = ScreenshotDiff.changedArea(
                    what + " between two frames " + gapTicks + " ticks apart (attempt " + (attempt[0] + 1) + ")",
                    first[0], currentHarness.screenshotPath(nameB));

            if (ScreenshotDiff.withinNoise(area.changedPixels(), area.frameWidth() * area.frameHeight())) {
                return true;
            }

            lastVerdict[0] = area.toString();
            attempt[0]++;

            if (attempt[0] >= attempts) {
                String verdict = lastVerdict[0];
                currentHarness.run(Where.CLIENT, () -> {
                    lastVerdict[0] = describeNearbyEntities(Minecraft.getInstance());
                    return true;
                });
                throw new AssertionError("The " + what + " kept changing: " + verdict
                        + " after " + attempts + " attempts with a chunk rebuild between them. "
                        + lastVerdict[0]);
            }

            currentHarness.run(Where.CLIENT, () -> {
                betweenAttempts.run(Minecraft.getInstance());
                return true;
            });
            phase[0] = 0;
            phaseStart[0] = ticks;
            return false;
        }));
    }

    /** Numbers the settle shots of a run; see {@link #awaitStableFrame}. */
    private static final java.util.concurrent.atomic.AtomicInteger SETTLE_SERIAL =
            new java.util.concurrent.atomic.AtomicInteger();

    /** Every entity but the player within sixteen blocks of him, for a message about a moving scene. */
    private static String describeNearbyEntities(Minecraft client) {
        if (client.level == null || client.player == null) {
            return "No client level or player to look around.";
        }

        StringBuilder text = new StringBuilder("Entities near the player: ");
        int count = 0;

        for (net.minecraft.world.entity.Entity entity : client.level.entitiesForRendering()) {
            if (entity == client.player || entity.distanceTo(client.player) > 16.0f) {
                continue;
            }
            count++;
            text.append(entity.getType().toShortString()).append(" at ")
                    .append(String.format(java.util.Locale.ROOT, "%.1f/%.1f/%.1f", entity.getX(), entity.getY(), entity.getZ()))
                    .append("; ");
        }

        return count == 0 ? "No entities but the player within sixteen blocks." : text.toString();
    }

    /** Waits until everything the server sent has arrived and been handled on the client. */
    public void awaitPackets() {
        entries.add(new Entry("wait for the server's packets", 200, Where.HARNESS,
                ticks -> currentHarness.packetsSettled()));
    }

    /** Waits until the chunks around the player are built and rendered. */
    public void awaitChunks() {
        entries.add(new Entry("wait for the chunks to render", 400, Where.HARNESS,
                ticks -> currentHarness.chunksRendered()));
    }

    /** Runs a server command; a command that fails fails the test. */
    public void command(String command) {
        command(command, false);
    }

    /**
     * Runs a server command, optionally tolerating "matched nothing".
     *
     * <p>The tolerant form exists for selectors: {@code kill @e[type=!minecraft:player]} throws
     * when there is nothing to kill, and an empty room is precisely what the scene is after. It is
     * a separate call rather than a blanket "ignore command errors", because ignoring them is how
     * eight misspelled game rules in this suite were silently dead for months.
     */
    public void command(String command, boolean mayMatchNothing) {
        entries.add(new Entry("command " + command, 100, Where.HARNESS,
                ticks -> currentHarness.runCommand(command, mayMatchNothing, ticks)));
    }

    /** Fails the test with {@code message} unless the client answers yes, right now. */
    public void check(String message, ClientCondition condition) {
        act("check " + message, client -> {
            if (!condition.test(client)) {
                throw new AssertionError(message);
            }
        });
    }

    public String currentStepName() {
        return index < entries.size() ? entries.get(index).name() : "<finished>";
    }

    public int stepCount() {
        return entries.size();
    }

    public int currentStepIndex() {
        return index;
    }

    /** Set by the driver before each tick so harness steps can reach it. */
    private Harness currentHarness;

    /**
     * Advances the script by one client tick.
     *
     * @return true once every step has finished
     */
    public boolean tick(Harness harness, StepLogger logger) throws Exception {
        this.currentHarness = harness;

        while (index < entries.size()) {
            Entry entry = entries.get(index);

            if (ticksInStep > entry.timeoutTicks()) {
                String detail = "";
                if (entry.diagnosis() != null) {
                    // On the client thread, like the step itself: Fabric's driver ticks the script
                    // from its test thread, where Minecraft.getInstance() refuses - and a
                    // diagnosis that "itself failed" is the one sentence a timed out step had.
                    String[] text = {""};
                    try {
                        harness.run(Where.CLIENT, () -> {
                            text[0] = entry.diagnosis().apply(Minecraft.getInstance());
                            return true;
                        });
                        detail = " - " + text[0];
                    } catch (Exception e) {
                        detail = " - the diagnosis itself failed: " + e;
                    }
                }
                throw new AssertionError("Step " + (index + 1) + "/" + entries.size() + " '"
                        + entry.name() + "' of " + testName + " timed out after " + ticksInStep
                        + " client ticks" + detail);
            }

            boolean done = harness.run(entry.where(), () -> entry.step().tick(ticksInStep));

            if (!done) {
                ticksInStep++;
                // A heartbeat, so a step that is waiting for something is visible in the log
                // instead of looking like a frozen client.
                if (ticksInStep % 60 == 0) {
                    logger.stillWaiting(index + 1, entries.size(), entry.name(), ticksInStep,
                            entry.timeoutTicks());
                }
                return false;
            }

            logger.stepFinished(index + 1, entries.size(), entry.name(), ticksInStep);
            index++;
            ticksInStep = 0;
        }

        return true;
    }

    public interface StepLogger {
        void stepFinished(int number, int total, String name, int ticks);

        void stillWaiting(int number, int total, String name, int ticks, int timeoutTicks);
    }
}
