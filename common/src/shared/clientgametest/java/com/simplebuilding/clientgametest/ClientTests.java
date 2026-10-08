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
        return only(allBeforeWorld());
    }

    private static List<Entry> allBeforeWorld() {
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
        return only(allInWorld());
    }

    private static List<Entry> allInWorld() {
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
                new Entry("mod-screens", ModScreensClientTest::inWorld),
                new Entry("backpack", BackpackClientTest::inWorld),
                // The mod screens in the container style (26.3), one picture per screen.
                new Entry("mod-ui-style", ModUiStyleClientTest::inWorld),
                new Entry("blueprint-editor", BlueprintEditorClientTest::inWorld),
                new Entry("mega-guide", MegaGuideClientTest::inWorld),
                new Entry("placed-bundle", PlacedBundleClientTest::inWorld),
                // Reads only the baked block models, so it needs no scene and leaves none behind.
                new Entry("piston-textures", PistonTextureClientTest::inWorld),
                // Reads the stitched items atlas, then shows trimmed armour in the inventory and
                // puts the hotbar back empty.
                new Entry("trim-textures", TrimTextureClientTest::inWorld));
    }

    /**
     * Narrows a run to the entries named in {@code SIMPLEBUILDING_CLIENT_ONLY} (comma separated,
     * before-world names included), for checking one test while working on it. Unset - as in every
     * gate and runner call - the full list runs. A name that matches nothing fails loudly instead
     * of turning into an empty, green run.
     */
    private static List<Entry> only(List<Entry> all) {
        String only = System.getenv("SIMPLEBUILDING_CLIENT_ONLY");
        if (only == null || only.isBlank()) {
            return all;
        }
        java.util.Set<String> names = new java.util.TreeSet<>();
        for (String name : only.split(",")) {
            names.add(name.strip());
        }
        java.util.Set<String> known = new java.util.TreeSet<>();
        allBeforeWorld().forEach(entry -> known.add(entry.name()));
        allInWorld().forEach(entry -> known.add(entry.name()));
        if (!known.containsAll(names)) {
            throw new IllegalArgumentException("SIMPLEBUILDING_CLIENT_ONLY names unknown client tests: " + names
                    + ", known: " + known);
        }
        return all.stream().filter(entry -> names.contains(entry.name())).toList();
    }
}
