package com.simplebuilding.neoforge.clienttest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simplebuilding.clientgametest.ClientTests;
import com.simplebuilding.clientgametest.Harness;
import com.simplebuilding.clientgametest.Script;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * Runs the shared client test scripts on NeoForge.
 *
 * <p>NeoForge has no client test API, so this is the whole harness: a state machine driven from
 * {@code ClientTickEvent.Post}, on the client thread, never blocking. That constraint is not a
 * style choice - {@code Minecraft.disconnect} pumps client ticks itself to draw its progress
 * screen and therefore never returns when it is called from inside one, which is exactly how an
 * earlier attempt at a NeoForge client test hung.
 *
 * <p>The scripts themselves are the same objects Fabric runs, from
 * {@code common/src/shared/clientgametest}. What differs is only this file and its Fabric twin.
 *
 * <p><b>Shutdown.</b> The run never logs out: it writes its result and halts the JVM (exit 0
 * proven, non-zero not proven). Leaving the world would mean {@code disconnect}, see above.
 */
final class SharedScriptRun implements Harness {

    /** How long to wait for the world to come up before giving up, in client ticks. */
    private static final int WORLD_TIMEOUT_TICKS = 600;

    private enum Phase { BEFORE_WORLD, CREATING_WORLD, WAITING_FOR_WORLD, IN_WORLD, DONE }

    private final List<ClientTests.Entry> beforeWorld = new ArrayList<>(ClientTests.beforeWorld());
    private final List<ClientTests.Entry> inWorld = new ArrayList<>(ClientTests.inWorld());
    private final Map<String, Shot> shots = new LinkedHashMap<>();
    private final Map<String, CommandJob> commands = new LinkedHashMap<>();

    private Phase phase = Phase.BEFORE_WORLD;
    private int listIndex;
    private Script script;
    private String scriptName = "";
    private int worldWaitTicks;
    private boolean finished;
    private boolean attacking;
    private boolean reportedInputLockout;
    private final List<String> failures = new ArrayList<>();

    void onClientTick() {
        if (finished) {
            return;
        }

        try {
            // Runs at the tail of Minecraft.tick(), so after handleKeybinds() acted on the held
            // attack button and before the frame is drawn. Clearing the lockout here means the
            // NEXT tick's handleKeybinds() sees a zero and actually mines.
            if (attacking) {
                clearInputLockout();
            }
            step();
        } catch (Throwable t) {
            fail(t);
        }
    }

    /**
     * Zeroes vanilla's input lockout so a held attack button is not ignored.
     *
     * <p>{@code Minecraft.missTime} is re-armed while any screen is open and only counts down by
     * one per tick, so after the world screen closes it can swallow more than a second of held
     * input - long enough for a mining step to time out while looking perfectly set up.
     */
    private void clearInputLockout() {
        try {
            java.lang.reflect.Field field = Minecraft.class.getDeclaredField("missTime");
            field.setAccessible(true);
            int before = field.getInt(Minecraft.getInstance());
            if (before <= 0) {
                return;
            }
            field.setInt(Minecraft.getInstance(), 0);
            if (!reportedInputLockout) {
                reportedInputLockout = true;
                Log.info("cleared vanilla's input lockout (Minecraft.missTime was " + before + ")");
            }
        } catch (Throwable t) {
            throw new AssertionError("could not clear Minecraft.missTime, so vanilla would ignore "
                    + "the held attack button", t);
        }
    }

    private void step() throws Exception {
        switch (phase) {
            case BEFORE_WORLD -> {
                if (!advance(beforeWorld)) {
                    phase = Phase.CREATING_WORLD;
                }
            }
            case CREATING_WORLD -> {
                createTestWorld();
                phase = Phase.WAITING_FOR_WORLD;
            }
            case WAITING_FOR_WORLD -> {
                if (worldIsUp()) {
                    Log.info("world is up, running " + inWorld.size() + " script(s) in it");
                    listIndex = 0;
                    script = null;
                    phase = Phase.IN_WORLD;
                } else if (++worldWaitTicks > WORLD_TIMEOUT_TICKS) {
                    throw new AssertionError("the test world did not come up within "
                            + WORLD_TIMEOUT_TICKS + " client ticks");
                }
            }
            case IN_WORLD -> {
                if (!advance(inWorld)) {
                    succeed();
                }
            }
            case DONE -> {
            }
        }
    }

    /** Runs one tick of the current script in {@code list}. Returns false when the list is done. */
    private boolean advance(List<ClientTests.Entry> list) throws Exception {
        if (listIndex >= list.size()) {
            return false;
        }

        if (script == null) {
            ClientTests.Entry entry = list.get(listIndex);
            scriptName = entry.name();
            script = new Script(scriptName);
            entry.build().accept(script);
            Log.info("[" + scriptName + "] " + script.stepCount() + " steps");
        }

        try {
            if (script.tick(this, LOGGER)) {
                Log.info("[" + scriptName + "] done");
                script = null;
                listIndex++;
            }
        } catch (Throwable t) {
            // One failing script must not take the rest of the suite with it. Before this, the
            // first failure halted the JVM and the four scripts after it never ran - 46 of 85
            // checkpoints lost to one known-open case. Each script is independent anyway: the
            // scene is rebuilt at the start of every one.
            String where = scriptName + " at step '" + script.currentStepName() + "'";
            Log.info("FAILED in " + where + ": " + t);
            failures.add(where + ": " + t);
            setAttacking(false);
            script = null;
            listIndex++;
        }
        return true;
    }

    // ------------------------------------------------------------------ Harness

    @Override
    public boolean run(Script.Where where, Script.Body body) throws Exception {
        // One thread for everything here, so the split Fabric enforces costs nothing to honour.
        return body.run();
    }

    @Override
    public boolean screenshot(String name, int ticksInStep) {
        return shots.computeIfAbsent(name, Shot::new).poll();
    }

    @Override
    public Path screenshotPath(String name) {
        Shot shot = shots.get(name);
        if (shot == null) {
            throw new IllegalStateException("no screenshot has been taken under the name '"
                    + name + "' yet");
        }
        return shot.path();
    }

    @Override
    public boolean packetsSettled() {
        // NeoForge has no way to know when the client has drained what the server sent - Fabric
        // knows because its framework runs the two task queues in a fixed order. Saying "yes"
        // straight away is honest about that: the idle steps around this call are what actually
        // give the packets time, and the scripts are written with that in mind.
        return true;
    }

    @Override
    public boolean chunksRendered() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return false;
        }
        // No render-completion signal here either, so this asks the strongest thing that is
        // available: the chunk the player stands in has arrived on the client. Asked through the
        // block position because ChunkPos.x/z are not accessible under these mappings.
        return client.level.hasChunkAt(client.player.blockPosition());
    }

    @Override
    public boolean runCommand(String command, boolean mayMatchNothing, int ticksInStep) {
        CommandJob job = commands.computeIfAbsent(command, key -> new CommandJob(key, mayMatchNothing));
        boolean done = job.poll();
        if (done) {
            // Freed for a script that legitimately runs the same command twice.
            commands.remove(command);
        }
        return done;
    }

    @Override
    public void holdKey(int glfwKeyCode) {
        Input.holdKey(glfwKeyCode);
    }

    @Override
    public void releaseKey(int glfwKeyCode) {
        Input.releaseKey(glfwKeyCode);
    }

    @Override
    public void pressKey(int glfwKeyCode) {
        Input.clickKey(glfwKeyCode);
    }

    @Override
    public void setAttacking(boolean attacking) {
        Minecraft client = Minecraft.getInstance();
        attacking = attacking && client.player != null;
        if (attacking) {
            clearInputLockout();
            client.mouseHandler.grabMouse();
            // set() alone only makes isDown() true. Vanilla's handleKeybinds starts a new break
            // through startAttack(), which it reaches via consumeClick() - and that returns true
            // only for a click that was actually registered. Without this the held button keeps
            // an ALREADY running break going but never begins one.
            client.options.keyAttack.setDown(true);
            KeyMapping.click(client.options.keyAttack.getKey());
        } else {
            client.mouseHandler.releaseMouse();
            // Releasing the button is not enough: vanilla keeps destroying until it is told to
            // stop, so without this the block keeps breaking after the test moved on - and the
            // wall the next case aims at is gone. That is exactly how the first shared run failed
            // here, with "block (10,1,20) is minecraft:air" three cases later.
            if (client.gameMode != null) {
                client.gameMode.stopDestroyBlock();
            }
        }
        client.options.keyAttack.setDown(attacking);
        this.attacking = attacking;
    }

    @Override
    public void holdMouse(int button) {
        Input.holdMouse(button);
    }

    @Override
    public void releaseMouse(int button) {
        Input.releaseMouse(button);
    }

    @Override
    public void pressMouse(int button) {
        Input.clickMouse(button);
    }

    @Override
    public void scroll(double amount) {
        Input.scroll(amount);
    }

    @Override
    public void setCursorPos(double x, double y) {
        Input.setCursorPos(x, y);
    }

    // ------------------------------------------------------------------ world

    private void createTestWorld() {
        Minecraft client = Minecraft.getInstance();
        // The name has to differ per run, or the second run of the day opens the first one's
        // world and inherits whatever it left behind.
        String levelName = "sb-client-tests-" + System.nanoTime();

        LevelSettings settings = new LevelSettings(levelName, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, true), true,
                WorldDataConfiguration.DEFAULT);

        Log.info("creating a flat singleplayer world '" + levelName + "'");

        client.createWorldOpenFlows().createFreshLevel(levelName, settings,
                new WorldOptions(0L, false, false),
                provider -> provider.lookupOrThrow(Registries.WORLD_PRESET)
                        .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                client.gui.screen());
    }

    private boolean worldIsUp() {
        Minecraft client = Minecraft.getInstance();
        MinecraftServer server = client.getSingleplayerServer();
        return client.level != null
                && client.player != null
                && client.gameMode != null
                && server != null
                && server.isReady()
                && client.gui.screen() == null;
    }

    /** One command, handed to the server thread and then polled for its outcome. */
    private static final class CommandJob {
        private final String command;
        private final boolean mayMatchNothing;
        private volatile boolean done;
        private volatile Throwable error;
        private boolean submitted;

        CommandJob(String command, boolean mayMatchNothing) {
            this.command = command;
            this.mayMatchNothing = mayMatchNothing;
        }

        boolean poll() {
            if (error != null) {
                throw new AssertionError("command failed: " + command, error);
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
                throw new AssertionError("no integrated server - the test world was not opened");
            }

            server.execute(() -> {
                try {
                    CommandSourceStack source = server.createCommandSourceStack();
                    // execute(), not the convenience path: that one swallows brigadier errors, so
                    // a misspelled command looks exactly like a working one. Eight game rules in
                    // this very suite were silently dead that way.
                    server.getCommands().getDispatcher().execute(command, source);
                    done = true;
                } catch (CommandSyntaxException e) {
                    // A selector that matched nobody is the one failure some commands are allowed
                    // to have; anything else is a real one and has to reach the driver.
                    if (mayMatchNothing) {
                        done = true;
                    } else {
                        error = e;
                    }
                } catch (Throwable t) {
                    error = t;
                }
            });
        }
    }

    // ------------------------------------------------------------------ result

    private void succeed() {
        if (failures.isEmpty()) {
            writeResult("PROVEN", null);
            halt(0);
            return;
        }
        Log.info(failures.size() + " script(s) failed:");
        for (String failure : failures) {
            Log.info("  " + failure);
        }
        writeResult("NOT PROVEN", String.join(" | ", failures));
        halt(1);
    }

    private void fail(Throwable t) {
        String where = script == null ? scriptName
                : scriptName + " at step '" + script.currentStepName() + "'";
        Log.info("FAILED in " + where + ": " + t);
        writeResult("NOT PROVEN", where + ": " + t);
        halt(1);
    }

    private void writeResult(String verdict, String detail) {
        try {
            Path file = Minecraft.getInstance().gameDirectory.toPath().resolve("client-test-result.txt");
            StringBuilder text = new StringBuilder(verdict).append('\n');
            if (detail != null) {
                text.append(detail).append('\n');
            }
            for (String name : shots.keySet()) {
                text.append("shot ").append(name).append('\n');
            }
            Files.writeString(file, text.toString());
        } catch (Throwable ignored) {
            // The exit code carries the verdict too; a missing note must not mask it.
        }
    }

    private void halt(int exitCode) {
        finished = true;
        phase = Phase.DONE;
        Log.info("halting with exit code " + exitCode);
        Runtime.getRuntime().halt(exitCode);
    }

    private static final Script.StepLogger LOGGER = new Script.StepLogger() {
        @Override
        public void stepFinished(int number, int total, String name, int ticks) {
            Log.info("step " + number + "/" + total + " done after " + ticks + " ticks: " + name);
        }

        @Override
        public void stillWaiting(int number, int total, String name, int ticks, int timeoutTicks) {
            Log.info("step " + number + "/" + total + " still waiting (" + ticks + "/"
                    + timeoutTicks + "): " + name);
        }
    };
}
