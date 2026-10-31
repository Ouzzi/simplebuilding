package com.simplefoundry;

/** Heat language of the crucibles (MEDIUM, HIGH); EXTREME (Soul Lava, Netherite) comes with stage 3. */
public enum Heat {
    MEDIUM, HIGH;

    public boolean atLeast(Heat required) {
        return ordinal() >= required.ordinal();
    }
}
