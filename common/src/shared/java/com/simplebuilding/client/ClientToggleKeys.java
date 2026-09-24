package com.simplebuilding.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Shared handling of the two Simplebuilding toggle keys (highlights + octant figure).
 *
 * <p>Called once per client tick by each loader — same pattern as
 * {@link DoubleJumpController#tick(Minecraft)} — so the toggle status messages cannot drift
 * apart between loaders again. They previously did: the NeoForge copy pushed both toggles into
 * the chat while Fabric put them on the actionbar.
 *
 * <p>Status goes to the actionbar via {@code Player#sendOverlayMessage(Component)} — in MC 26.2
 * that is the only actionbar entry point ({@code displayClientMessage} no longer exists).
 */
public final class ClientToggleKeys {
    private ClientToggleKeys() {
    }

    /** Drain the toggle key queues for this client tick. Safe to call every tick. */
    public static void tick(Minecraft client) {
        Player player = client.player;
        if (player == null) {
            return;
        }

        while (ClientState.highlightToggleKey != null && ClientState.highlightToggleKey.consumeClick()) {
            ClientState.showHighlights = !ClientState.showHighlights;
            player.sendOverlayMessage(Component.literal("Highlights: " + (ClientState.showHighlights ? "ON" : "OFF")));
        }

        while (ClientState.octantFigureToggleKey != null && ClientState.octantFigureToggleKey.consumeClick()) {
            // Eigener Schalter: nur die gefuellte Oktant-Figur (frueher derselbe wie Highlights).
            ClientState.showOctantFigure = !ClientState.showOctantFigure;
            player.sendOverlayMessage(Component.literal("Octant Figure: " + (ClientState.showOctantFigure ? "ON" : "OFF")));
        }
    }
}
