package com.simplebuilding.modules.simplelayers.backrooms.api;

import com.simplelib.api.portal.PortalRule;

/** The door rule of a level pair n↔n+1, decided by the upper level (owner 10.11., g3 a): Lobby two-way, Poolrooms activate, below breaks. */
public enum DoorRule {
    TWO_WAY, ACTIVATE_ONCE, BREAK_ON_TRANSIT;

    public PortalRule portalRule() {
        return switch (this) {
            case TWO_WAY -> PortalRule.TWO_WAY;
            case ACTIVATE_ONCE -> PortalRule.ACTIVATE_ONCE;
            case BREAK_ON_TRANSIT -> PortalRule.BREAK_ON_TRANSIT;
        };
    }
}
