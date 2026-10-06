package com.simplebuilding.client;

/**
 * A sound played by an amplifier (Jukebox/Note Amplifier, {@code SpeakerBoost}): its channel gain may exceed vanilla's
 * cap of 1.0 by {@link #simplebuilding$gain()} (at most {@code SpeakerBoost.MAX_GAIN}). The player's volume sliders
 * still apply. Read by {@code SoundEngineAmplifierMixin} on 26.3.
 */
public interface AmplifiedSound {
    /** Extra gain on top of vanilla's capped volume (1 = vanilla). */
    float simplebuilding$gain();
}
