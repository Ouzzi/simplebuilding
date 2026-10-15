package com.simplebuilding.modules.simplestats;

/** Server settings ({@code config/simplestats.json}); clients only display what the items carry, so nothing is synchronized. */
public final class StatsConfig {
    /** Master switch: off means nothing is counted and no component appears on any item (existing statistics stay and stay visible). */
    public boolean enabled = true;
    public boolean trackMining = true, trackCombat = true, trackArmor = true, trackDistances = true, trackFishing = true, trackTools = true;
    /** Players in Creative or Spectator mode count when true (default: they do not). */
    public boolean countCreative = false;
    /** Machine players (fake players of other mods) count when true (default: they do not). */
    public boolean countFakePlayers = false;
    /** Player kills are counted (as a number only, never a name) when true. */
    public boolean trackPlayerKills = true;
    /** Byte budget of one item's statistics (technical protection against oversized item data); 2,048 to 262,144. */
    public int maxBytes = 32768;

    public void normalize() {
        maxBytes = Math.max(2048, Math.min(maxBytes, 262144));
    }
}
