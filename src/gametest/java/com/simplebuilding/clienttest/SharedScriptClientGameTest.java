package com.simplebuilding.clienttest;

import com.simplebuilding.clientgametest.BreakingStateRecorder;
import com.simplebuilding.clientgametest.ClientTests;
import com.simplebuilding.clientgametest.Harness;
import com.simplebuilding.clientgametest.Script;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives the shared client test scripts on Fabric.
 *
 * <p>Fabric hands a client test its own thread and lets it block, so the driver is a loop: ask the
 * script for one step, let a client tick pass, ask again. What it must not do is run everything on
 * one side of the fence - {@code TestInput} calls {@code ThreadingImpl.checkOnGametestThread} and
 * throws if input is driven from inside {@code runOnClient}, while anything touching game state
 * has to be inside it. That is exactly the split {@link Script.Where} carries, and
 * {@link #run} is where it is honoured.
 *
 * <p>The whole in-world list runs in one world. See {@link ClientTests} for why that costs nothing:
 * every test rebuilds the scene from scratch before it looks at anything.
 */
public final class SharedScriptClientGameTest implements FabricClientGameTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("simplebuilding-clienttest");

    @Override
    public void runTest(ClientGameTestContext context) {
        installBreakingStateRecorder(context);

        for (ClientTests.Entry entry : ClientTests.beforeWorld()) {
            runScript(context, null, entry);
        }

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            for (ClientTests.Entry entry : ClientTests.inWorld()) {
                runScript(context, singleplayer, entry);
            }
        }
    }

    /**
     * Lets the shared breaking state recorder see what the mod appended to the render state.
     *
     * <p>Registered once, from the test thread, before anything runs. Fabric keeps event
     * registration order and the mod registers its own listener during client initialisation, so
     * a listener added here runs after it and therefore sees its additions. Registering it late -
     * or not at all - leaves the recorder empty, and an empty recorder makes the control case
     * ("the vanilla pickaxe tore nothing loose") trivially true. That is a false green, and it is
     * exactly what the adversarial review of the first port caught.
     */
    private static void installBreakingStateRecorder(ClientGameTestContext context) {
        context.runOnClient(client -> LevelExtractionEvents.END_EXTRACTION.register(
                extraction -> BreakingStateRecorder.observe(extraction.levelState())));
    }

    private void runScript(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                           ClientTests.Entry entry) {
        Script script = new Script(entry.name());
        entry.build().accept(script);

        LOGGER.info("[{}] {} steps", entry.name(), script.stepCount());
        FabricHarness harness = new FabricHarness(context, singleplayer);
        Script.StepLogger logger = new Script.StepLogger() {
            @Override
            public void stepFinished(int number, int total, String name, int ticks) {
                LOGGER.info("[{}] {}/{} {} ({} ticks)", entry.name(), number, total, name, ticks);
            }

            @Override
            public void stillWaiting(int number, int total, String name, int ticks, int timeoutTicks) {
                LOGGER.info("[{}] {}/{} {} still waiting, {}/{} ticks",
                        entry.name(), number, total, name, ticks, timeoutTicks);
            }
        };

        try {
            while (!script.tick(harness, logger)) {
                context.waitTick();
            }
        } catch (Exception e) {
            throw new AssertionError(entry.name() + " failed at step '"
                    + script.currentStepName() + "': " + e.getMessage(), e);
        }
    }

    /** Carries a step's checked exception out of {@code computeOnClient}. */
    private static final class StepFailed extends RuntimeException {
        StepFailed(Exception cause) {
            super(cause);
        }
    }

    /** The Fabric half of {@link Harness}. */
    private static final class FabricHarness implements Harness {

        private final ClientGameTestContext context;
        private final TestSingleplayerContext singleplayer;
        private final java.util.Map<String, java.nio.file.Path> lastShots = new java.util.HashMap<>();

        FabricHarness(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
            this.context = context;
            this.singleplayer = singleplayer;
        }

        @Override
        public boolean run(Script.Where where, Script.Body body) throws Exception {
            // CLIENT work has to be inside runOnClient; HARNESS work has to be outside it. Getting
            // this backwards throws on Fabric rather than misbehaving quietly, which is the one
            // mercy the framework grants here.
            if (where != Script.Where.CLIENT) {
                return body.run();
            }
            // computeOnClient takes a plain function, so a checked exception from the step has to
            // travel wrapped and be unwrapped on this side - otherwise an assertion inside a step
            // would surface as an opaque runtime error with the real cause buried.
            try {
                return context.computeOnClient(client -> {
                    try {
                        return body.run();
                    } catch (Exception e) {
                        throw new StepFailed(e);
                    }
                });
            } catch (StepFailed wrapped) {
                throw (Exception) wrapped.getCause();
            }
        }

        @Override
        public boolean screenshot(String name, int ticksInStep) {
            // Fabric's call does the whole thing and returns, so one poll is enough - and it
            // hands back the file it wrote, which is the only reliable way to name it: the
            // filename carries a per-run counter (0004_name.png) that shared code cannot guess.
            lastShots.put(name, context.takeScreenshot(name));
            return true;
        }

        @Override
        public java.nio.file.Path screenshotPath(String name) {
            java.nio.file.Path path = lastShots.get(name);
            if (path == null) {
                throw new IllegalStateException("no screenshot has been taken under the name '"
                        + name + "' yet");
            }
            return path;
        }

        @Override
        public boolean runCommand(String command, boolean mayMatchNothing, int ticksInStep) {
            requireWorld("runCommand").getServer().runCommand(command);
            return true;
        }

        @Override
        public boolean packetsSettled() {
            requireWorld("packetsSettled").getConnection().waitForClientboundPackets();
            return true;
        }

        @Override
        public boolean chunksRendered() {
            requireWorld("chunksRendered").getConnection().waitForChunksRender();
            return true;
        }

        @Override
        public void holdKey(int glfwKeyCode) {
            context.getInput().holdKey(glfwKeyCode);
        }

        @Override
        public void releaseKey(int glfwKeyCode) {
            context.getInput().releaseKey(glfwKeyCode);
        }

        @Override
        public void pressKey(int glfwKeyCode) {
            context.getInput().pressKey(glfwKeyCode);
        }

        @Override
        public void holdMouse(int button) {
            context.getInput().holdMouse(button);
        }

        @Override
        public void releaseMouse(int button) {
            context.getInput().releaseMouse(button);
        }

        @Override
        public void pressMouse(int button) {
            context.getInput().pressMouse(button);
        }

        @Override
        public void scroll(double amount) {
            context.getInput().scroll(amount);
        }

        @Override
        public void setCursorPos(double x, double y) {
            context.getInput().setCursorPos(x, y);
        }

        private TestSingleplayerContext requireWorld(String what) {
            if (singleplayer == null) {
                throw new IllegalStateException(what + " needs a world, but this script runs "
                        + "before one exists - move the step into ClientTests.inWorld()");
            }
            return singleplayer;
        }
    }
}
