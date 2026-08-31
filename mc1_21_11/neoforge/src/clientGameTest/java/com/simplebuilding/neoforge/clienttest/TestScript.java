package com.simplebuilding.neoforge.clienttest;

import java.util.ArrayList;
import java.util.List;

/**
 * A linear list of steps that is advanced from the client tick, one tick at a time.
 *
 * <p><b>Why it looks like this.</b> An earlier attempt at a NeoForge client test drove the game
 * from a second thread and handed work to the client thread through semaphores. That deadlocked:
 * {@code Minecraft.disconnect} pumps client ticks itself in order to draw its progress screen, so
 * calling it from inside a client tick never returns. This class removes the whole class of
 * problem - there is no second thread, no handshake and no blocking call. Everything runs on the
 * client thread, and a step that is not finished yet simply returns {@code false} and is asked
 * again on the next tick.
 *
 * <p>Every step carries its own tick budget. Running out of it is a failure with the step's name
 * in the message, so a hang always reports <em>where</em> it hung instead of just stopping.
 */
final class TestScript {

    /** One step. Gets the number of ticks it has already been running; returns true when done. */
    @FunctionalInterface
    interface Step {
        boolean tick(int ticksInStep) throws Exception;
    }

    @FunctionalInterface
    interface Action {
        void run() throws Exception;
    }

    @FunctionalInterface
    interface Condition {
        boolean test() throws Exception;
    }

    private record Entry(String name, int timeoutTicks, Step step) {
    }

    private final List<Entry> entries = new ArrayList<>();
    private int index;
    private int ticksInStep;

    /** A step that does its work in one go. */
    void act(String name, Action action) {
        entries.add(new Entry(name, 2, ticks -> {
            action.run();
            return true;
        }));
    }

    /** A step that waits for a condition, failing after {@code timeoutTicks}. */
    void await(String name, int timeoutTicks, Condition condition) {
        entries.add(new Entry(name, timeoutTicks, ticks -> condition.test()));
    }

    /** A step that simply lets {@code ticks} client ticks pass. */
    void idle(String name, int ticks) {
        entries.add(new Entry(name, ticks + 20, t -> t >= ticks));
    }

    /** A step with full control over its own progress. */
    void step(String name, int timeoutTicks, Step step) {
        entries.add(new Entry(name, timeoutTicks, step));
    }

    String currentStepName() {
        return index < entries.size() ? entries.get(index).name() : "<finished>";
    }

    int currentStepTicks() {
        return ticksInStep;
    }

    int stepCount() {
        return entries.size();
    }

    int currentStepIndex() {
        return index;
    }

    /**
     * Advances the script by one client tick.
     *
     * @return true once every step has finished
     */
    boolean tick(StepLogger logger) throws Exception {
        while (index < entries.size()) {
            Entry entry = entries.get(index);

            if (ticksInStep > entry.timeoutTicks()) {
                throw new AssertionError("Step " + (index + 1) + "/" + entries.size() + " '"
                        + entry.name() + "' timed out after " + ticksInStep + " client ticks");
            }

            boolean done = entry.step().tick(ticksInStep);

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

    interface StepLogger {
        void stepFinished(int number, int total, String name, int ticks);

        void stillWaiting(int number, int total, String name, int ticks, int timeoutTicks);
    }
}
