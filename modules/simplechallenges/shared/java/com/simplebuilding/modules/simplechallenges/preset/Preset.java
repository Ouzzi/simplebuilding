package com.simplebuilding.modules.simplechallenges.preset;

import java.util.List;

/**
 * A validated preset (data, like a data pack file). {@code unlock} / {@code world} / {@code lucky} / {@code skyline} are null when the preset has no such mode (all null = only modes this version
 * does not run yet: it is listed but cannot be activated). {@code json} is the original text, stored in the world as the snapshot.
 */
public record Preset(String id, String name, String icon, String family, UnlockMode unlock, WorldMode world, LuckyMode lucky,
                     SkylineMode skyline, List<String> warnings, String json) {
    public boolean runnable() {
        return unlock != null || world != null || lucky != null || skyline != null;
    }
}
