package com.simplebuilding.modules.simplechallenges.preset;

import java.util.List;

/**
 * A validated preset (data, like a data pack file). {@code unlock} is null when the preset declares only modes this version
 * does not run yet (it is listed but cannot be activated). {@code json} is the original text, stored in the world as the snapshot.
 */
public record Preset(String id, String name, String icon, String family, UnlockMode unlock, List<String> warnings, String json) {
    public boolean runnable() {
        return unlock != null;
    }
}
