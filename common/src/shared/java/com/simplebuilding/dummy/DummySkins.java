package com.simplebuilding.dummy;

import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Trainingspuppe mit Spielernamen (Besitzer 2026-10-08, docs/ai/PLAN-STAENDER-2026-10-09.md): heisst die Puppe wie ein
 * Spieler, zeigt der Client dessen Haut ({@code client.DummySkinLayer}). Nur gueltige Minecraft-Namen (3 bis 16 Zeichen,
 * Buchstaben, Ziffern, Unterstrich) werden nachgeschlagen; alles andere bleibt die normale Puppe.
 */
public final class DummySkins {
    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

    private DummySkins() {
    }

    /** Der Spielername, nach dem die Haut gesucht wird, oder leer. */
    public static Optional<String> playerName(@Nullable Component name) {
        if (name == null) {
            return Optional.empty();
        }
        String text = name.getString().strip();
        return PLAYER_NAME.matcher(text).matches() ? Optional.of(text) : Optional.empty();
    }
}
