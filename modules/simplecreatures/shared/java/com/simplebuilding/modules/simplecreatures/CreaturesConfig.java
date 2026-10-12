package com.simplebuilding.modules.simplecreatures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** {@code config/simplecreatures.json} (formerly {@code qOL.*} in Simple QoL). */
public final class CreaturesConfig {
    /** Entity type ids that are always silent, e.g. {@code minecraft:cow}. */
    public List<String> mutedEntities = new ArrayList<>();
    /** A custom name ending in one of these silences the mob. */
    public List<String> nametagMuteSuffixes = new ArrayList<>(Arrays.asList("_mute", "_shhh"));
    /** A custom name ending in one of these keeps a baby young. */
    public List<String> nametagBabySuffixes = new ArrayList<>(Arrays.asList("_baby", "_small"));
    /** Piglins accept armour with a gold trim as gold armour. */
    public boolean piglinsIgnoreGoldTrims = true;
    /** Piglins accept a golden tool in either hand as gold. */
    public boolean piglinsIgnoreGoldTools = true;

    public void normalizeNumbers() {
        if (mutedEntities == null) mutedEntities = new ArrayList<>();
        if (nametagMuteSuffixes == null) nametagMuteSuffixes = new ArrayList<>();
        if (nametagBabySuffixes == null) nametagBabySuffixes = new ArrayList<>();
    }

    public void normalize() {
        mutedEntities = boundedList(mutedEntities);
        nametagMuteSuffixes = boundedList(nametagMuteSuffixes);
        nametagBabySuffixes = boundedList(nametagBabySuffixes);
    }

    private static List<String> boundedList(List<String> values) {
        if (values == null) return new ArrayList<>();
        return new ArrayList<>(values.stream().filter(v -> v != null && !v.isBlank() && v.length() <= 64).distinct().limit(64).toList());
    }
}
