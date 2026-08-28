package com.simplebuilding.client.render;

/**
 * Decides whether the vanilla block selection outline should be hidden while one of the mod's own
 * highlights is on screen.
 *
 * <p>The answer is always "no", matching the pre-multiloader behaviour: both mod renderers in
 * {@link BlockHighlightRenderer} deliberately leave the targeted block alone — the sledgehammer pass
 * skips {@code centerPos}, and the octant pass bails out entirely until a position is set — because
 * they rely on vanilla to outline whatever the player is actually looking at. Suppressing it left the
 * targeted block with no outline at all, which was especially bad for a fresh octant: the player has
 * to aim at pos1 with no selection feedback whatsoever.
 *
 * <p>Kept as a hook (rather than dropping the event registrations) so both loaders keep a single
 * place to opt into suppression should a future highlight actually replace the targeted block.
 */
public final class BlockOutlineSupport {
    private BlockOutlineSupport() {
    }

    /** Suppress vanilla block selection outline when a custom highlight is drawn instead. */
    public static boolean suppressVanillaBlockOutline() {
        return false;
    }
}
