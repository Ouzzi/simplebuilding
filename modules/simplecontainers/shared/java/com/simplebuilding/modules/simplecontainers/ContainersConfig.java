package com.simplebuilding.modules.simplecontainers;

import java.util.HashMap;
import java.util.Map;

/**
 * Local look only ({@code config/simplecontainers.json}); nothing here reaches a server. Master switch plus one switch
 * per screen kind (the style ids of {@code ContainerStyles}); a kind not in the map is on.
 */
public final class ContainersConfig {
    /** Master switch: off = every screen exactly Vanilla. */
    public boolean enabled = true;
    /** Shows the development style comparison button outside styled screens. */
    public boolean showStyleToggle = false;
    /** Per screen kind (style id), default on. */
    public Map<String, Boolean> screens = new HashMap<>();

    public void normalize() {
        if (screens == null) screens = new HashMap<>();
        screens.values().removeIf(java.util.Objects::isNull);
    }

    /** Whether the style {@code id} is drawn. */
    public boolean isOn(String id) {
        normalize();
        return enabled && screens.getOrDefault(id, true);
    }
}
