package dev.simpledimension.common.portal;

import java.util.List;
import java.util.Optional;

/**
 * Single entry point for portal matching. Activation is purely structural — a complete
 * frame with an empty interior is enough, like a vanilla nether portal; no light required.
 *
 * <p>Explicit pattern {@link PortalRecipe}s are tried first; then the frame-based forms of
 * {@link DimensionPortalConfig#buildShapeRules()} (N33: rectangle, arch or any - the fallback frame when there
 * are no recipes, or recipes with a {@code shape}). Every path produces a {@link MatchedPortal}, so callers
 * never branch on the mode.
 */
public final class PortalActivationService {

    private PortalActivationService() {
    }

    /** Find a portal whose interior contains the given (ignition or existing-portal) cell. */
    public static Optional<MatchedPortal> match(PortalWorldView world, DimensionPortalConfig config, int x, int y, int z) {
        List<PortalRecipe> recipes = config.buildRecipes();
        if (!recipes.isEmpty()) {
            Optional<MatchedPortal> pattern = PortalRecipeMatcher.match(world, recipes, x, y, z);
            if (pattern.isPresent()) {
                return pattern;
            }
        }
        for (PortalShapeRule rule : config.buildShapeRules()) {
            Optional<MatchedPortal> shaped = rule.find(world, x, y, z);
            if (shaped.isPresent()) {
                return shaped;
            }
        }
        return Optional.empty();
    }

    /** A refused attempt at one of the config's frame-based forms: the rule and why (for the action bar). */
    public record Refusal(PortalShapeRule rule, ArchPortalScanner.Verdict verdict) {
    }

    /** Why lighting at this cell opens none of the config's frame-based forms, if it looks like an attempt. */
    public static Optional<Refusal> explain(PortalWorldView world, DimensionPortalConfig config, int x, int y, int z) {
        for (PortalShapeRule rule : config.buildShapeRules()) {
            Optional<ArchPortalScanner.Verdict> verdict = rule.explain(world, x, y, z);
            if (verdict.isPresent()) {
                return Optional.of(new Refusal(rule, verdict.get()));
            }
        }
        return Optional.empty();
    }
}
