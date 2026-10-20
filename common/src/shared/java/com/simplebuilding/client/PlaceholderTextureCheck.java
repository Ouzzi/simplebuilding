package com.simplebuilding.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owner 19.10.: grey checkers instead of textures. A build that could not draw the Vanilla-derived textures (no Python with
 * Pillow/numpy or no 26.3 client jar, AGENTS.md rule 7) ships grey placeholders plus the marker
 * {@code assets/simplebuilding/vanilla_derived_placeholders.txt} (mc26_3/resources.gradle). The client says so loudly in its
 * log instead of leaving the checkers unexplained.
 */
public final class PlaceholderTextureCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger("simplebuilding");
    static final String MARKER = "/assets/simplebuilding/vanilla_derived_placeholders.txt";

    private PlaceholderTextureCheck() {
    }

    /** Logs an error when this jar carries placeholder textures; true then. */
    public static boolean warnIfPlaceholders() {
        if (PlaceholderTextureCheck.class.getResource(MARKER) == null) {
            return false;
        }
        LOGGER.error("SimpleBuilding was built WITHOUT its Vanilla-derived textures: vaults, pistons, rails, carts, music discs, "
                + "Color Brush and more show grey checkers. Rebuild with Python 3.8+ and Pillow + numpy "
                + "(py -3 -m pip install pillow numpy) or use a jar from the Zentrale downloads. See docs/ai/LAPTOP-SETUP.md.");
        return true;
    }
}
