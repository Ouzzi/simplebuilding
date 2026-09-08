package com.simplebuilding.clientgametest;

import java.util.List;
import java.util.function.Consumer;

/**
 * The one list of client tests, read by every loader's driver.
 *
 * <p>The server side has had this shape for a while: one catalogue, and each loader registers from
 * it, so a test cannot exist on one target and quietly not on another. The client side used to
 * have four separate sets instead, which is exactly how it drifted - Fabric on 26.2 grew to 84
 * checkpoints while the other three sat at 12 to 16, and nothing went red, because absence is what
 * a green run cannot show. The parity gate now fails on that drift; this list is what makes it
 * fixable.
 *
 * <p><b>All in-world tests share one world.</b> The driver creates a single player world once and
 * runs the whole list in it. That is not a compromise: every test begins by calling
 * {@link TestScene#build}, which fills the working volume with air, rebuilds the wall and floor,
 * kills every non-player entity, clears the inventory, sets the game mode and teleports the player
 * to a fixed spot. The scene is therefore rebuilt from scratch between tests anyway, and leaving
 * the world in between would buy nothing - while costing a non-blocking reimplementation of
 * {@code Minecraft.disconnect}, which pumps client ticks and therefore cannot be called from
 * inside one.
 *
 * <p>The world lifecycle is the driver's business for the same reason: entering and leaving a
 * world is the one thing the two loaders cannot express the same way.
 */
public final class ClientTests {

    private ClientTests() {
    }

    /** One entry: a name for the log, and the steps it contributes. */
    public record Entry(String name, Consumer<Script> build) {
    }

    /** Runs on the main menu, before any world exists. */
    public static List<Entry> beforeWorld() {
        return List.of(
                new Entry("boot", SmokeClientTest::beforeWorld));
    }

    /** Runs inside the shared single player world, in this order. */
    public static List<Entry> inWorld() {
        return List.of(
                new Entry("smoke", SmokeClientTest::inWorld));
    }
}
