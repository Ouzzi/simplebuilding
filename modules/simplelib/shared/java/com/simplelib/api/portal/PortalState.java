package com.simplelib.api.portal;

/**
 * The state of a portal link (BACK22 core, docs/ai/KONZEPT-B-BACK22-KERN-2026-11-10.md §5): open (passable), dormant (needs an
 * activation first) or broken (needs a repair). Both ends of a link read the same state from {@link PortalStates}.
 */
public enum PortalState {
    OPEN, DORMANT, BROKEN
}
