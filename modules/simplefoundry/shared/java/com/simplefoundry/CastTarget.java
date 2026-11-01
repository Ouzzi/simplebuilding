package com.simplefoundry;

/**
 * What a spout can pour into (owner SA5 = C: vessel and reinforced cauldron). The casting tray is the built-in target; a block entity of
 * another mod (the reinforced cauldron of Simple Crucibles) implements this and is then poured into like the tray, without Simple Foundry
 * knowing that mod. The spout draws {@link Metal#INGOT} units per pour.
 */
public interface CastTarget {
    /** Whether one more ingot of {@code metal} fits. */
    boolean accepts(Metal metal);

    /** Takes one ingot of {@code metal}; {@code now} is the game time, {@code castTicks} the setting time. */
    void add(Metal metal, long now, int castTicks);
}
