package com.simplebuilding.modules.simplemobs;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Pure rules of the cloak of deception (owner 11.10.): detection radii, reveal times, wear and the disguise spec
 * string that is synced to clients. No world access, so it is unit-testable.
 */
public final class CloakLogic {
    /** Worn: mobs notice the wearer only this close (vanilla follow range is 16-35). */
    public static final double DETECT_RADIUS = 3.0;
    /** After an attack every other mob notices the wearer within this radius for {@link #REVEAL_TICKS}. */
    public static final double REVEALED_RADIUS = 8.0;
    public static final int REVEAL_TICKS = 600;
    /** The attacked mob and those around it know the wearer for this long. */
    public static final int SPOTTED_TICKS = 1200;
    /** Half the side of the 5x5 area around the attacked mob. */
    public static final double SPOT_HALF = 2.5;
    public static final int DURABILITY = 240, MORPH_COST = 1, REVEAL_COST = 10;
    public static final String PLAYER_PREFIX = "player:";

    private CloakLogic() {}

    /** Whether a mob (not a boss) fails to notice a cloaked player. */
    public static boolean hidden(boolean sameKindAsDisguise, boolean spotted, boolean revealed, double distanceSqr) {
        if (spotted) return false;
        if (sameKindAsDisguise) return true;
        double r = revealed ? REVEALED_RADIUS : DETECT_RADIUS;
        return distanceSqr > r * r;
    }

    /** Whether {@code dx/dz/dy} (mob minus attacked mob) lies in the 5x5 area that recognises the attacker. */
    public static boolean inSpotArea(double dx, double dy, double dz) {
        return Math.abs(dx) <= SPOT_HALF && Math.abs(dz) <= SPOT_HALF && Math.abs(dy) <= SPOT_HALF;
    }

    public static String playerSpec(UUID id, String name) {
        return PLAYER_PREFIX + id + ":" + name;
    }

    public static boolean isPlayerSpec(String spec) {
        return spec.startsWith(PLAYER_PREFIX);
    }

    public static @Nullable UUID specUuid(String spec) {
        if (!isPlayerSpec(spec)) return null;
        String rest = spec.substring(PLAYER_PREFIX.length());
        int cut = rest.indexOf(':');
        try { return UUID.fromString(cut < 0 ? rest : rest.substring(0, cut)); } catch (IllegalArgumentException e) { return null; }
    }

    public static String specName(String spec) {
        if (!isPlayerSpec(spec)) return "";
        String rest = spec.substring(PLAYER_PREFIX.length());
        int cut = rest.indexOf(':');
        return cut < 0 ? "" : rest.substring(cut + 1);
    }
}
