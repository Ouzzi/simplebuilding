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

    /**
     * Runs on the main menu, before any world exists.
     *
     * <p>Not just a place for main menu screenshots: a test whose subject is what the client sends
     * <em>on join</em> has to change the thing it measures before the world is created. The armor
     * trim config is the case in point - flipped after the join it would be compared against the
     * value the server already has, and the assertion could not fail. That test guards its own
     * setup and says so, which is how the missing registration here was found rather than passing
     * for the wrong reason.
     */
    public static List<Entry> beforeWorld() {
        return List.of(
                new Entry("boot", SmokeClientTest::beforeWorld),
                new Entry("client-bootstrap-setup", ClientBootstrapClientTest::beforeWorld));
    }

    /**
     * Runs inside the shared single player world, in this order.
     *
     * <p>Order matters a little: the three renderer tests each leave the scene rebuilt behind
     * them, but they also change mod config values (highlight opacity, the inverted octant sneak)
     * and put them back as their last steps. A test that fails takes the whole run down on both
     * loaders, so a half restored config never reaches the next entry - but that is the reason
     * the restore steps are there rather than a nicety.
     */
    public static List<Entry> inWorld() {
        return List.of(
                // FIRST, and that is not cosmetic: its beforeWorld phase switches
                // enableArmorTrimBenefits off so the value the client reports on join differs from
                // the server's default - and only its inWorld phase puts it back. Anything running
                // in between runs with armor trim benefits disabled, which is a visible difference:
                // the first ordering had it late and hud-and-tooltip failed its noise floor with
                // 2150 changed pixels where 60 are allowed.
                new Entry("client-bootstrap", ClientBootstrapClientTest::inWorld),
                new Entry("smoke", SmokeClientTest::inWorld),
                new Entry("block-highlight", BlockHighlightClientTest::inWorld),
                new Entry("building-wand-preview", BuildingWandPreviewClientTest::inWorld),
                new Entry("multi-block-breaking", MultiBlockBreakingClientTest::inWorld),
                new Entry("air-jump", AirJumpClientTest::inWorld),
                new Entry("hud-and-tooltip", HudAndTooltipClientTest::inWorld),
                new Entry("item-rendering", ItemRenderingClientTest::inWorld),
                new Entry("mod-screens", ModScreensClientTest::inWorld));
    }
}
