package com.simplestorage.client;

import com.simplestorage.backpack.BackpackItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;

/**
 * Die Rucksack-Taste (Standard B), von beiden Loadern einmal pro Client-Tick gerufen - dasselbe
 * Muster wie {@link ClientToggleKeys}.
 *
 * <ul>
 *   <li>Mit einem Rucksack am Koerper - getragen oder (seit 2026-09-28) irgendwo im Inventar, der
 *       getragene zuerst ({@link BackpackItem#carriedBackpackSlot}) - fragt sie den Server nach dem
 *       Rucksack-Menue ({@link OpenBackpackPayload}); der Server prueft und waehlt selbst.</li>
 *   <li>Ohne Rucksack oeffnet sie das normale Inventar - mit genau dem Code, den Vanilla fuer die
 *       Inventartaste E benutzt (inklusive vom Server gesteuertem Inventar, etwa beim Reiten).</li>
 * </ul>
 *
 * <p>Absichtlich {@code gui.setScreen} und nicht {@code setScreenAndShow}: letzteres zeichnet
 * sofort einen Frame bzw. faehrt einen verschachtelten Client-Tick und ist fuer Ladebildschirme
 * gedacht, nicht fuer einen Aufruf mitten aus dem Tick heraus.
 */
public final class BackpackKeyHandler {
    /** Translation key of the key mapping (Standard B); the loaders register it in Vanilla's inventory category. */
    public static final String KEY_NAME = "key.simplestorage.open_backpack";
    /** The backpack key, set by the loader's client entry (split stage 8c-2: was SimpleBuilding's {@code ClientState.backpackKey}). */
    public static net.minecraft.client.KeyMapping KEY;

    private BackpackKeyHandler() {
    }

    /** The key mapping (B), in Vanilla's inventory category. */
    public static net.minecraft.client.KeyMapping create() {
        return new net.minecraft.client.KeyMapping(KEY_NAME, com.mojang.blaze3d.platform.InputConstants.KEY_B,
                net.minecraft.client.KeyMapping.Category.INVENTORY);
    }

    /**
     * Schliesst die Rucksack-Taste diesen Bildschirm? Ja fuer die Bildschirme, die sie oeffnet: Rucksack,
     * Vanilla-Inventar und das Kreativ-Inventar, an das Vanilla im Kreativmodus weiterreicht. Andere
     * Behaelter (Truhe, Ofen) bleiben offen - B ist dort keine Inventartaste.
     */
    public static boolean closesOnBackpackKey(net.minecraft.client.gui.screens.Screen screen, net.minecraft.client.input.KeyEvent event) {
        if (KEY == null || !KEY.matches(event)) {
            return false;
        }
        return screen instanceof InventoryScreen
                || screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
                || screen instanceof com.simplestorage.client.BackpackScreen;
    }

    public static void tick(Minecraft client) {
        while (KEY != null && KEY.consumeClick()) {
            LocalPlayer player = client.player;
            if (player == null || client.gui.screen() != null) {
                continue;
            }
            if (BackpackItem.carriedBackpackSlot(player) != -1) {
                com.simplestorage.backpack.BackpackNet.toServer.accept(new com.simplestorage.backpack.BackpackNet.OpenPayload());
            } else if (client.gameMode != null && client.gameMode.isServerControlledInventory()) {
                player.sendOpenInventory();
            } else {
                client.getTutorial().onOpenInventory();
                client.gui.setScreen(new InventoryScreen(player));
            }
        }
    }
}
