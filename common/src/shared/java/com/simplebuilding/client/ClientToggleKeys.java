package com.simplebuilding.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Shared handling of the two Simplebuilding toggle keys (highlights + octant figure).
 *
 * <p>Called once per client tick by every loader - Fabric, NeoForge and Forge - the same pattern
 * as {@link DoubleJumpController#tick(Minecraft)}. Until 2026-09-27 only NeoForge called it and
 * Fabric and Forge carried their own copies, which had drifted apart twice: once in where the
 * status went (chat vs. actionbar), then in the queue handling (NeoForge left the key queue
 * undrained while no player existed, so presses made on the title screen fired all at once on
 * joining; Fabric drained it). Audit 2026-09-26 #38.
 *
 * <p>Queue rule, the same everywhere now: the queue is always drained and each press toggles;
 * the status line is only shown when there is a player to show it to.
 *
 * <p>Status goes to the actionbar via {@code Player#sendOverlayMessage(Component)} - in MC 26.2
 * that is the only actionbar entry point ({@code displayClientMessage} no longer exists). The
 * text is translatable ({@code message.simplebuilding.toggle.*}) with the vanilla ON/OFF words.
 */
public final class ClientToggleKeys {
    private ClientToggleKeys() {
    }

    /** Drain the toggle key queues for this client tick. Safe to call every tick. */
    public static void tick(Minecraft client) {
        Player player = client.player;

        while (ClientState.highlightToggleKey != null && ClientState.highlightToggleKey.consumeClick()) {
            ClientState.showHighlights = !ClientState.showHighlights;
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("message.simplebuilding.toggle.highlights",
                        CommonComponents.optionStatus(ClientState.showHighlights)));
            }
        }

        while (ClientState.hudToggleKey != null && ClientState.hudToggleKey.consumeClick()) {
            // Kein Text (Besitzer-Regel): das HUD verschwindet oder kommt wieder, ein Klick bestaetigt.
            com.simplebuilding.client.gui.ModHud.toggle(client);
        }

        while (ClientState.octantFigureToggleKey != null && ClientState.octantFigureToggleKey.consumeClick()) {
            // Eigener Schalter: nur die gefuellte Oktant-Figur (frueher derselbe wie Highlights).
            ClientState.showOctantFigure = !ClientState.showOctantFigure;
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("message.simplebuilding.toggle.octant_figure",
                        CommonComponents.optionStatus(ClientState.showOctantFigure)));
            }
        }
    }
}
