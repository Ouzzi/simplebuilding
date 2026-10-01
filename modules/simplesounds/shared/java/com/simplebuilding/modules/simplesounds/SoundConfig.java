package com.simplebuilding.modules.simplesounds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/** Local cosmetics only. No value in this class is sent to a server. */
public final class SoundConfig {
    public static final float MAX_VOLUME = .25f;
    public static final int MIN_COOLDOWN = 20;
    public static final int MAX_COOLDOWN = 1200;

    public enum Level {
        OFF(0), SUBTLE(.35f), NORMAL(.6f), STRONG(.8f), MAXIMUM(1);
        private final float gain;
        Level(float gain) { this.gain = gain; }
        public float gain() { return gain; }
    }

    public Level globalLevel = Level.SUBTLE;
    public boolean followVisuals = true;
    public Map<String, Level> overrides = new HashMap<>();
    public float volumeCap = MAX_VOLUME;
    public int soundsPerTick = 2;
    public int soundsPerPlayer = 1;
    public int cooldownTicks = 40;

    public void normalize() {
        if (globalLevel == null) globalLevel = Level.SUBTLE;
        volumeCap = Float.isFinite(volumeCap) ? Math.clamp(volumeCap, 0, MAX_VOLUME) : MAX_VOLUME;
        soundsPerTick = Math.clamp(soundsPerTick, 0, SoundBudget.MAX_PER_TICK);
        soundsPerPlayer = Math.clamp(soundsPerPlayer, 0, SoundBudget.MAX_PER_PLAYER);
        cooldownTicks = Math.clamp(cooldownTicks, MIN_COOLDOWN, MAX_COOLDOWN);
        if (overrides == null) overrides = new HashMap<>();
        var ids = new HashSet<String>();
        for (var effect : SoundsRegistry.ALL) ids.add(effect.id());
        overrides.entrySet().removeIf(e -> !ids.contains(e.getKey()) || e.getValue() == null);
    }

    public float volume(SoundsRegistry.Effect effect) {
        var visuals = followVisuals ? com.simplebuilding.framework.api.CosmeticIntensity.current("simplevisuals") : null;
        return volume(effect, visuals == null ? null : Level.valueOf(visuals.name()));
    }

    public Level level(String effectId, Level visualsLevel) {
        normalize();
        Level inherited = followVisuals && visualsLevel != null ? visualsLevel : globalLevel;
        return overrides.getOrDefault(effectId, inherited);
    }

    public float volume(SoundsRegistry.Effect effect, Level visualsLevel) {
        Level level = level(effect.id(), visualsLevel);
        return Math.min(volumeCap, effect.volume() * level.gain());
    }
}
