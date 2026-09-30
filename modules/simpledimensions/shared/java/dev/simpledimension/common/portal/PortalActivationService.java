package dev.simpledimension.common.portal;

import java.util.List;
import java.util.Optional;

/**
 * Single entry point for portal matching. Activation is purely structural — a complete
 * frame with an empty interior is enough, like a vanilla nether portal; no light required.
 *
 * <p>If the dimension config defines explicit {@link PortalRecipe}s they take precedence;
 * otherwise the flexible rectangular scanner is used. Both produce a {@link MatchedPortal},
 * so callers never branch on the mode.
 */
public final class PortalActivationService {

    private PortalActivationService() {
    }

    /** Find a portal whose interior contains the given (ignition or existing-portal) cell. */
    public static Optional<MatchedPortal> match(PortalWorldView world, DimensionPortalConfig config, int x, int y, int z) {
        List<PortalRecipe> recipes = config.buildRecipes();
        if (!recipes.isEmpty()) {
            return PortalRecipeMatcher.match(world, recipes, x, y, z);
        }
        return config.buildScanner().find(world, x, y, z)
                .map(frame -> MatchedPortal.fromRectangular(frame, config.frameBlock));
    }
}
