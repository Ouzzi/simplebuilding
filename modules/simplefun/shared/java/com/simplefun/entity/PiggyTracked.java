package com.simplefun.entity;

/**
 * Implemented by Player (via mixin). Exposes a server-to-client synced piggy flag.
 *
 * Player status effects are only synced to that player's own client in vanilla, not to
 * tracking clients. So the renderer cannot read remote players' piggy effect via hasEffect().
 * Instead the state is distributed through a synced data value (broadcast to all trackers).
 */
public interface PiggyTracked {
    boolean simplefun$isPiggyTracked();
}
