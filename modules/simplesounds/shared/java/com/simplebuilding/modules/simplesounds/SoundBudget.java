package com.simplebuilding.modules.simplesounds;

import java.util.HashMap;
import java.util.Map;

/** No queues: excess requests are dropped, never replayed as a burst. */
public final class SoundBudget {
    public static final int MAX_PER_TICK = 4;
    public static final int MAX_PER_PLAYER = 2;
    public static final int MAX_PLAYERS = 8;
    private int used;
    private final Map<Integer, Integer> players = new HashMap<>();
    private final Map<String, Long> last = new HashMap<>();

    public void reset() {
        begin();
        last.clear();
    }

    public void begin() {
        used = 0;
        players.clear();
    }

    public boolean claim(int player, String effect, long tick, SoundConfig config, int interval) {
        config.normalize();
        if (SoundsRegistry.ALL.stream().noneMatch(e -> e.id().equals(effect))) return false;
        if (used >= config.soundsPerTick
                || players.getOrDefault(player, 0) >= config.soundsPerPlayer
                || (!players.containsKey(player) && players.size() >= MAX_PLAYERS)) return false;

        String key = player + ":" + effect;
        Long previous = last.get(key);
        if (previous != null && tick - previous < Math.max(config.cooldownTicks, interval)) return false;
        // Also bound history across ticks. The client resets this on world/player changes.
        if (last.size() >= MAX_PLAYERS * SoundsRegistry.ALL.size() && !last.containsKey(key)) return false;
        last.put(key, tick);
        players.merge(player, 1, Integer::sum);
        used++;
        return true;
    }

    public int used() { return used; }
}
