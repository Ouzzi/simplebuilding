package com.simplestorage.backpack;

import com.simplestorage.bundle.DyedStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

/**
 * Loader-neutraler Teil des Oeffnens: wer ein Rucksack-Menue bekommt und wie es gebaut wird.
 * Der Loader-Einstieg setzt {@link #opener} und schickt es damit nur noch ab - Fabric per
 * {@code ExtendedMenuProvider}, NeoForge/Forge per {@code openMenu(provider, buffer)} (split stage 8c-2: was
 * SimpleBuilding's {@code platform/BackpackMenus}).
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

    /** Sends an opening to the client (set by the loader entry; the opening data travel in the loader's menu buffer). */
    public static java.util.function.BiConsumer<ServerPlayer, Opening> opener = (player, opening) -> {
        throw new IllegalStateException("no backpack menu opener");
    };

    public static void open(ServerPlayer player, Opening opening) {
        opener.accept(player, opening);
    }

    /** The backpack key's request (server side): opens the carried backpack when {@link #canOpenCarried} allows it. */
    public static void openCarriedIfAllowed(ServerPlayer player) {
        if (canOpenCarried(player)) open(player, carried(player));
    }

    /**
     * Darf die Rucksack-Taste ein Menue oeffnen? Nur mit einem Rucksack am Koerper - getragen oder,
     * seit 2026-09-28 (Besitzer), irgendwo im Inventar ({@link BackpackItem#carriedBackpackSlot}); a
     * backpack in an accessory slot (Curios/Trinkets) counts as worn -, lebend, nicht als Zuschauer und nur, wenn gerade kein anderes Menue offen ist. Der Server
     * prueft das selbst, egal was der Client meint.
     */
    public static boolean canOpenCarried(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && player.containerMenu == player.inventoryMenu
                && BackpackItem.carriedBackpackSlot(player) != -1;
    }

    /** Menue des getragenen Rucksacks. Voraussetzung: ein Rucksack im Brust-Slot. */
    public static Opening worn(ServerPlayer player) {
        return carried(player, BackpackItem.CHEST_INVENTORY_SLOT);
    }

    /** Menue des Rucksacks, den die Taste oeffnet. Voraussetzung: {@link #canOpenCarried}. */
    public static Opening carried(ServerPlayer player) {
        return carried(player, BackpackItem.carriedBackpackSlot(player));
    }

    /**
     * Menue des Rucksacks im Inventar-Slot {@code slot} (38 = Brust); der Slot bleibt gesperrt.
     * {@link BackpackItem#ACCESSORY_SLOT} opens the backpack in an accessory slot.
     */
    public static Opening carried(ServerPlayer player, int slot) {
        ItemStack backpack = BackpackItem.carriedStack(player, slot);
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
