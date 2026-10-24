package com.simplemusic.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class MusicClientConfigTest {
    private static final String HASH = "a".repeat(64);

    @Test
    void defaultsToOnAndAllowsEverything() {
        MusicClientConfig.reset();
        assertTrue(MusicClientConfig.loadForeignMusic);
        assertTrue(MusicClientConfig.allowed(HASH));
    }

    @Test
    void offOnlyAllowsOwnUploadsAndHintsOnce() {
        MusicClientConfig.reset();
        JsonObject json = new JsonObject();
        json.addProperty(MusicClientConfig.KEY, false);
        MusicClientConfig.apply(json);
        assertFalse(MusicClientConfig.allowed(HASH));
        assertTrue(MusicClientConfig.firstRefusal(HASH));
        assertFalse(MusicClientConfig.firstRefusal(HASH));
        MusicClientConfig.own(HASH);
        assertTrue(MusicClientConfig.allowed(HASH));
        assertEquals(false, MusicClientConfig.toJson().get(MusicClientConfig.KEY).getAsBoolean());
        MusicClientConfig.reset();
    }

    @Test
    void brokenValueFallsBackToOn() {
        MusicClientConfig.reset();
        JsonObject json = new JsonObject();
        json.addProperty(MusicClientConfig.KEY, "nope");
        MusicClientConfig.apply(json);
        assertTrue(MusicClientConfig.loadForeignMusic);
    }
}
