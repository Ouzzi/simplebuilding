package com.simplebuilding.client.gui.recipebook;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Nur clientseitig: Vanillas {@code RecipeBookComponent} verlangt ein {@link RecipeBookMenu}, der Schmiedetisch ist aber
 * ein {@link SmithingMenu}. Dieser Adapter wird nie registriert oder geoeffnet; er liefert der Komponente nur Buchtyp und
 * Bestand. Platziert wird serverseitig ({@code SmithingPlacement}), weil der Klick ans echte Menue geht.
 *
 * <p>Zum Bestand zaehlen auch beschaedigte, verzauberte und benannte Inventar-Items: Vanilla laesst sie beim Herstellen
 * aus, beim Schmieden sind sie aber genau die Basis (verzauberte Diamantruestung).
 */
public final class SmithingBookMenu extends RecipeBookMenu {
    private final SmithingMenu smithing;
    private final Inventory inventory;

    public SmithingBookMenu(SmithingMenu smithing, Inventory inventory) {
        super(null, smithing.containerId);
        this.smithing = smithing;
        this.inventory = inventory;
    }

    @Override
    public PostPlaceAction handlePlacement(boolean useMaxItems, boolean allowDroppingItemsToClear, RecipeHolder<?> recipe,
                                           ServerLevel level, Inventory inventory) {
        return PostPlaceAction.NOTHING;
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents contents) {
        for (int i = 0; i < 3; i++) {
            contents.accountStack(this.smithing.getSlot(i).getItem());
        }
        for (ItemStack stack : this.inventory.getNonEquipmentItems()) {
            if (!stack.isEmpty() && !Inventory.isUsableForCrafting(stack)) {
                contents.accountStack(stack);
            }
        }
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
