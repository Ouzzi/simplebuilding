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
     * Darf die Rucksack-Taste ein Menue oeffnen? Nur mit getragenem Rucksack, lebend, nicht als
     * Zuschauer und nur, wenn gerade kein anderes Menue offen ist.
     */
    public static boolean canOpenWorn(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && player.containerMenu == player.inventoryMenu
                && !BackpackItem.wornBackpack(player).isEmpty();
    }

    /** Menue des getragenen Rucksacks. Voraussetzung: {@link #canOpenWorn}. */
    public static Opening worn(ServerPlayer player) {
        ItemStack chest = BackpackItem.wornBackpack(player);
        BackpackItem item = (BackpackItem) chest.getItem();
        int multiplier = BackpackItem.stackMultiplier(chest, player.level());
        BackpackOpenData data = BackpackOpenData.worn(item.getTier(), multiplier, DyedStorage.colour(chest));
        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new BackpackMenu(containerId, inventory,
                        new WornBackpackContainer(menuPlayer, chest, multiplier), data),
                chest.getHoverName());
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
