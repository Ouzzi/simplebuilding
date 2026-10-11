package com.simplebuilding.clientgametest;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.KeyMapping;

/**
 * Key map hygiene (audit C9/C10): every mapping in {@code options.keyMappings} appears once, the
 * Simple Mods keys are really registered on this loader, and no default key of ours is shared with
 * a vanilla key or with another mod key of ours. Needs no scene and no screenshot.
 */
public final class KeyMappingClientTest {
    private static final String MOD_PREFIX = "key.simple";

    private KeyMappingClientTest() {
    }

    public static void inWorld(Script script) {
        script.check("key mapping names are unique in options.keyMappings", client -> {
            Set<String> seen = new HashSet<>();
            List<String> duplicates = new ArrayList<>();
            for (KeyMapping mapping : client.options.keyMappings) {
                if (!seen.add(mapping.getName())) {
                    duplicates.add(mapping.getName());
                }
            }
            if (!duplicates.isEmpty()) {
                throw new AssertionError("duplicate key mappings: " + duplicates);
            }
            return true;
        });
        script.check("Simple Mods keys are registered", client -> {
            long ours = java.util.Arrays.stream(client.options.keyMappings).filter(KeyMappingClientTest::isOurs).count();
            if (ours < 4) {
                throw new AssertionError("expected at least 4 Simple Mods key mappings, found " + ours);
            }
            return true;
        });
        script.check("no default key of ours collides with a vanilla or other Simple Mods key", client -> {
            Map<String, String> owner = new HashMap<>();
            for (KeyMapping mapping : client.options.keyMappings) {
                if (!isOurs(mapping)) {
                    owner.putIfAbsent(slot(mapping), mapping.getName());
                }
            }
            List<String> clashes = new ArrayList<>();
            for (KeyMapping mapping : client.options.keyMappings) {
                if (!isOurs(mapping) || mapping.getDefaultKey().equals(InputConstants.UNKNOWN)) {
                    continue;
                }
                String previous = owner.putIfAbsent(slot(mapping), mapping.getName());
                if (previous != null) {
                    clashes.add(mapping.getName() + " vs " + previous + " on " + mapping.getDefaultKey().getName());
                }
            }
            if (!clashes.isEmpty()) {
                throw new AssertionError("default key collisions: " + clashes);
            }
            return true;
        });
    }

    private static boolean isOurs(KeyMapping mapping) {
        return mapping.getName().startsWith(MOD_PREFIX);
    }

    private static String slot(KeyMapping mapping) {
        return mapping.getDefaultKey().getName();
    }
}
