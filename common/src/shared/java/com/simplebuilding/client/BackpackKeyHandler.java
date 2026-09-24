package com.simplebuilding.client;

import com.simplebuilding.items.custom.BackpackItem;
import com.simplebuilding.networking.OpenBackpackPayload;
import com.simplebuilding.platform.ClientNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;

/**
 * Die Rucksack-Taste (Standard B), von beiden Loadern einmal pro Client-Tick gerufen - dasselbe
 * Muster wie {@link ClientToggleKeys}.
 *
 * <ul>
 *   <li>Mit getragenem Rucksack fragt sie den Server nach dem Rucksack-Menue
 *       ({@link OpenBackpackPayload}); der Server prueft selbst noch einmal.</li>
 *   <li>Ohne Rucksack oeffnet sie das normale Inventar - mit genau dem Code, den Vanilla fuer die
 *       Inventartaste E benutzt (inklusive vom Server gesteuertem Inventar, etwa beim Reiten).</li>
 * </ul>
 *
 * <p>Absichtlich {@code gui.setScreen} und nicht {@code setScreenAndShow}: letzteres zeichnet
 * sofort einen Frame bzw. faehrt einen verschachtelten Client-Tick und ist fuer Ladebildschirme
 * gedacht, nicht fuer einen Aufruf mitten aus dem Tick heraus.
 */
public final class BackpackKeyHandler {
    private BackpackKeyHandler() {
    }

    public static void tick(Minecraft client) {
        while (ClientState.backpackKey != null && ClientState.backpackKey.consumeClick()) {
            LocalPlayer player = client.player;
            if (player == null || client.gui.screen() != null) {
                continue;
            }
            if (!BackpackItem.wornBackpack(player).isEmpty()) {
                ClientNetworking.send(new OpenBackpackPayload());
            } else if (client.gameMode != null && client.gameMode.isServerControlledInventory()) {
                player.sendOpenInventory();
            } else {
                client.getTutorial().onOpenInventory();
                client.gui.setScreen(new InventoryScreen(player));
            }
        }
    }
}
