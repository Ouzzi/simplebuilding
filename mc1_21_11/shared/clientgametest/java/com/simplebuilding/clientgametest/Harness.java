package com.simplebuilding.clientgametest;

/**
 * The few things a client test needs from its loader, and nothing else.
 *
 * <p>Everything a test does to the <em>game</em> it does through {@code Minecraft} directly, on
 * both loaders, because that part is vanilla and identical. What is not identical is how a test
 * gets onto the client thread, how it takes a screenshot, how it presses a key and how it talks to
 * the integrated server. That is this interface, and it is deliberately small: every method here
 * has to be implemented twice, so each one has to earn its place.
 *
 * <p><b>{@link #run} is the load bearing one.</b> Fabric requires that input and screenshots
 * happen on the test thread and that game state is touched inside {@code runOnClient}; it throws
 * if you mix them up. NeoForge has one thread for both. The {@link Script.Where} a step declares
 * therefore travels down to here, and each driver decides what it means.
 *
 * <p><b>Three methods poll instead of blocking.</b> A screenshot, settled packets and rendered
 * chunks are all things Fabric can simply wait for on its test thread, and NeoForge cannot wait
 * for at all - it has only the client tick, and blocking in it stops the very loop that would make
 * the waiting end. So they are asked once per tick until they answer true, which is the shape both
 * loaders can serve.
 */
public interface Harness {

    /**
     * Runs one step's body where the step asked to be run, and returns what it returned.
     *
     * <p>Implementations must not swallow exceptions: a failing step has to reach the driver,
     * which turns it into a failed test naming the step.
     */
    boolean run(Script.Where where, Script.Body body) throws Exception;

    /**
     * Works towards a screenshot under this name; true once the PNG is on disk.
     *
     * <p>Polled once per tick with the same name until it answers true. On NeoForge a screenshot
     * is not a call that returns: {@code Screenshot.takeScreenshot} copies the colour texture into
     * a mapped GPU buffer and calls back only once that copy has completed, which is not in the
     * same frame - and blocking for it would stall the render loop that has to finish it. Fabric
     * does the whole thing in one call and answers true the first time.
     *
     * <p>The runner reads these names out of the test sources and requires a fresh file per name,
     * so the name is not decoration - it is the checkpoint the run is counted in.
     */
    boolean screenshot(String name, int ticksInStep) throws Exception;

    /**
     * Where the screenshot just taken under {@code name} landed.
     *
     * <p>Only valid straight after {@link #screenshot} answered true, and that restriction is the
     * reason this exists at all instead of a rule for building the path: Fabric writes
     * {@code 0004_name.png} with a per-run counter, NeoForge writes {@code name.png}. Shared code
     * that guessed would compare the wrong files on one loader and say nothing about it.
     */
    java.nio.file.Path screenshotPath(String name) throws Exception;

    /**
     * Works towards "everything the server sent has arrived and been handled"; true when settled.
     *
     *
     * <p>On this Minecraft line NEITHER driver answers this exactly, and both say so rather than
     * pretending. NeoForge has no knowledge of the queues at all. Fabric here runs against
     * fabric-client-gametest-api-v1 4.3.5, which does not yet expose
     * {@code TestSingleplayerContext#getConnection()} or {@code waitForClientboundPackets()} - the
     * 26.2 line does, and there the same method is an exact barrier. Both implementations
     * therefore answer true straight away and the {@code idle} steps around every call are what
     * actually give the packets time. That is weaker than the 26.2 Fabric answer, and it is
     * written down here rather than papered over.
     */
    boolean packetsSettled() throws Exception;

    /** Works towards "the chunks around the player are built and rendered"; true when they are. */
    boolean chunksRendered() throws Exception;

    /**
     * Works towards running a command on the integrated server; true once it has run.
     *
     * <p>Polled like the others, because on NeoForge a command is handed to the server thread and
     * completes later - and a syntax error in it has to come back as a failure rather than
     * disappear.
     *
     * <p>Both drivers go through the command dispatcher directly for that reason, not through the
     * convenience path either loader offers: those catch the brigadier error and return quietly,
     * so a misspelled game rule looks exactly like a working one. That already cost this suite
     * eight silently dead commands once. Fabric's client test API has the same swallowing
     * behaviour, and using it here would have made the flag below meaningless on that loader
     * while it meant something on the other.
     */
    boolean runCommand(String command, boolean mayMatchNothing, int ticksInStep) throws Exception;

    /** Presses and holds a key, given by its GLFW key code. */
    void holdKey(int glfwKeyCode) throws Exception;

    /** Releases a key held by {@link #holdKey}. */
    void releaseKey(int glfwKeyCode) throws Exception;

    /** Presses and releases a key in one step. */
    void pressKey(int glfwKeyCode) throws Exception;

    /**
     * Holds or releases the attack input - the one that mines a block.
     *
     * <p>Its own method rather than {@code holdMouse(0)}, because mining is the place where the
     * two loaders differ most. Fabric drives the real mouse path and vanilla does the rest. On
     * NeoForge three things have to be true at once, and none of them follows from a held button:
     * the mouse has to be grabbed (an ungrabbed mouse leaves the crosshair pointing at nothing),
     * the attack key mapping has to be down, and {@code Minecraft.missTime} - vanilla's input
     * lockout, re-armed whenever a screen was open and counting down one per tick - has to be
     * cleared, or the held button is ignored for the next second and a half.
     *
     * <p>The first shared run on NeoForge failed exactly there: "the player never reached destroy
     * stage 1 ... isDestroying=false, the crosshair reports nothing".
     */
    void setAttacking(boolean attacking) throws Exception;

    /** Presses and holds a mouse button (0 = left, 1 = right, 2 = middle). */
    void holdMouse(int button) throws Exception;

    /** Releases a mouse button held by {@link #holdMouse}. */
    void releaseMouse(int button) throws Exception;

    /** Presses and releases a mouse button in one step. */
    void pressMouse(int button) throws Exception;

    /** Turns the mouse wheel; positive is up / away from the player. */
    void scroll(double amount) throws Exception;

    /**
     * Puts every key and mouse button this harness is holding back up, and stops attacking.
     *
     * <p>Called by the driver after every script - the ones that passed as well as the ones that
     * failed - and that second half is the whole point. A script releases what it held as one of
     * its last steps, and a step after a failed one does not run, so a failure leaves the input
     * exactly as it was at the moment things went wrong. That is not a tidiness argument: a sneak
     * key left down by a timed out mining case made the next two scripts fail for a reason that
     * had nothing to do with them ("the player is sneaking, which swaps the forward and backward
     * tables" in item-rendering, and a 2071 pixel noise floor in hud-and-tooltip), and the mod
     * would have been blamed for both.
     *
     * <p>Implementations must be tolerant: this runs after a failure, so the world may be gone,
     * the player may be null, and the same key may already have been released.
     */
    void releaseAllInput() throws Exception;

    /** Moves the cursor to a position in the window, in scaled screen coordinates. */
    void setCursorPos(double x, double y) throws Exception;
}
