package com.simplelib.api.portal;

import net.minecraft.util.StringRepresentable;

/**
 * What a portal door shows (block state {@code portal}): the link state, or {@code unframed} while its frame is incomplete. Only
 * for the client (symbol glow, veil); the server always decides from {@link PortalStates} and {@link FrameRule}.
 */
public enum PortalLook implements StringRepresentable {
    ACTIVE("active"), DORMANT("dormant"), BROKEN("broken"), UNFRAMED("unframed");

    private final String name;

    PortalLook(String name) {
        this.name = name;
    }

    public static PortalLook of(PortalState state) {
        return switch (state) {
            case OPEN -> ACTIVE;
            case DORMANT -> DORMANT;
            case BROKEN -> BROKEN;
        };
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
