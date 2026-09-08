package com.simplebuilding.clientgametest;

import java.util.ArrayList;
import java.util.List;

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

    /** How a step wants to be run. */
    public enum Where {
        /** On the client thread. */
        CLIENT,
        /** Wherever the harness runs its own calls - never inside a client task. */
        HARNESS,
    }

    private record Entry(String name, int timeoutTicks, Where where, Step step) {
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
        entries.add(new Entry(name, timeoutTicks, Where.CLIENT,
                ticks -> condition.test(Minecraft.getInstance())));
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
    public void shot(String name) {
        entries.add(new Entry("shot " + name, 200, Where.HARNESS,
                ticks -> currentHarness.screenshot(name)));
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

    /** Runs a server command in the single player world. */
    public void command(String command) {
        harness("command " + command, h -> h.runCommand(command));
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
                throw new AssertionError("Step " + (index + 1) + "/" + entries.size() + " '"
                        + entry.name() + "' of " + testName + " timed out after " + ticksInStep
                        + " client ticks");
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
