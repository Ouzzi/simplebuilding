package com.simplebuilding.screen;

import com.simplebuilding.blocks.entity.custom.BackpackBlockEntity;
import com.simplebuilding.items.custom.BackpackItem;
import com.simplebuilding.util.DyedStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

/**
 * Loader-neutraler Teil des Oeffnens: wer ein Rucksack-Menue bekommt und wie es gebaut wird.
 * Die Plattform-Klasse {@code com.simplebuilding.platform.BackpackMenus} (je Loader) schickt es
 * nur noch ab - Fabric per {@code ExtendedMenuProvider}, NeoForge/Forge per
 * {@code openMenu(provider, buffer)}.
 *
 * <p>Getrennt, damit Spieltests das Menue ueber {@link #worn}/{@link #placed} bauen koennen: ein
 * Mock-Spieler nimmt NeoForges {@code advanced_open_screen}-Paket nicht an.
 */
public final class BackpackMenuProviders {
    private BackpackMenuProviders() {
    }

    /** Ein fertiges Menue samt den Daten, die der Client zum Aufbau braucht. */
    public record Opening(MenuProvider provider, BackpackOpenData data) {
    }

    /**
     * Darf die Rucksack-Taste ein Menue oeffnen? Nur mit einem Rucksack am Koerper - getragen oder,
     * seit 2026-09-28 (Besitzer), irgendwo im Inventar ({@link BackpackItem#carriedBackpackSlot}) -,
     * lebend, nicht als Zuschauer und nur, wenn gerade kein anderes Menue offen ist. Der Server
     * prueft das selbst, egal was der Client meint.
     */
    public static boolean canOpenCarried(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && player.containerMenu == player.inventoryMenu
                && BackpackItem.carriedBackpackSlot(player) >= 0;
    }

    /** Menue des getragenen Rucksacks. Voraussetzung: ein Rucksack im Brust-Slot. */
    public static Opening worn(ServerPlayer player) {
        return carried(player, BackpackItem.CHEST_INVENTORY_SLOT);
    }

    /** Menue des Rucksacks, den die Taste oeffnet. Voraussetzung: {@link #canOpenCarried}. */
    public static Opening carried(ServerPlayer player) {
        return carried(player, BackpackItem.carriedBackpackSlot(player));
    }

    /** Menue des Rucksacks im Inventar-Slot {@code slot} (38 = Brust); der Slot bleibt gesperrt. */
    public static Opening carried(ServerPlayer player, int slot) {
        ItemStack backpack = player.getInventory().getItem(slot);
        BackpackItem item = (BackpackItem) backpack.getItem();
        int multiplier = BackpackItem.stackMultiplier(backpack, player.level());
        BackpackOpenData data = BackpackOpenData.carried(item.getTier(), multiplier, DyedStorage.colour(backpack), slot);
        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new BackpackMenu(containerId, inventory,
                        new WornBackpackContainer(menuPlayer, backpack, multiplier, slot), data),
                backpack.getHoverName());
        return new Opening(provider, data);
    }

    /** Menue eines abgestellten Rucksacks. */
    public static Opening placed(BackpackBlockEntity backpack) {
        BackpackOpenData data = BackpackOpenData.placed(backpack.getBlockPos(), backpack.tier(), backpack.stackMultiplier(),
                backpack.dyeColor());
        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new BackpackMenu(containerId, inventory, backpack.container(), data),
                backpack.getDisplayName());
        return new Opening(provider, data);
    }
}
