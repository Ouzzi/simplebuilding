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
    boolean screenshot(String name) throws Exception;

    /**
     * Works towards "everything the server sent has arrived and been handled"; true when settled.
     *
     * <p>Fabric answers this exactly, because its framework runs the client and server task queues
     * in a fixed order and knows when they are empty. NeoForge has no such knowledge and says so:
     * its implementation answers true straight away, and the {@code idle} steps around the call
     * are what actually give the packets time. That is weaker, and it is written down here rather
     * than papered over.
     */
    boolean packetsSettled() throws Exception;

    /** Works towards "the chunks around the player are built and rendered"; true when they are. */
    boolean chunksRendered() throws Exception;

    /** Runs a command on the integrated server, as the server itself. */
    void runCommand(String command) throws Exception;

    /** Presses and holds a key, given by its GLFW key code. */
    void holdKey(int glfwKeyCode) throws Exception;

    /** Releases a key held by {@link #holdKey}. */
    void releaseKey(int glfwKeyCode) throws Exception;

    /** Presses and releases a key in one step. */
    void pressKey(int glfwKeyCode) throws Exception;

    /** Presses and holds a mouse button (0 = left, 1 = right, 2 = middle). */
    void holdMouse(int button) throws Exception;

    /** Releases a mouse button held by {@link #holdMouse}. */
    void releaseMouse(int button) throws Exception;

    /** Presses and releases a mouse button in one step. */
    void pressMouse(int button) throws Exception;

    /** Turns the mouse wheel; positive is up / away from the player. */
    void scroll(double amount) throws Exception;

    /** Moves the cursor to a position in the window, in scaled screen coordinates. */
    void setCursorPos(double x, double y) throws Exception;
}
