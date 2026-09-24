package com.simplebuilding.screen;

import com.simplebuilding.items.custom.BackpackTier;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Das Rucksack-Menue: exakt das Spielerinventar ({@code InventoryMenu}, Slots 0 bis 45 in derselben
 * Reihenfolge - Ergebnis, 2x2-Raster, Ruestung, Hauptinventar, Hotbar, Nebenhand) und dahinter ab
 * {@link #BACKPACK_SLOT_START} die Slots des Rucksacks.
 *
 * <p>{@code InventoryMenu} selbst laesst sich nicht wiederverwenden: sein Konstruktor legt die
 * Container-Id fest auf 0 (das Spielerinventar ist immer offen). Deshalb ein eigenes
 * {@link AbstractCraftingMenu} mit derselben Slot-Anordnung; das Crafting-Ergebnis berechnet
 * Vanillas eigene {@code CraftingMenu#slotChangedCraftingGrid} (siehe {@link CraftingGrid}).
 *
 * <p>Getragener Rucksack: der Brust-Slot ist gesperrt, solange das Menue offen ist
 * ({@link BackpackArmorSlot}). Abgestellter Rucksack: kein Slot gesperrt; das Menue schliesst,
 * sobald der Block weg ist oder der Spieler sich entfernt.
 */
public class BackpackMenu extends AbstractCraftingMenu {
    public static final int RESULT_SLOT = InventoryMenu.RESULT_SLOT;
    public static final int ARMOR_SLOT_START = InventoryMenu.ARMOR_SLOT_START;
    public static final int INV_SLOT_START = InventoryMenu.INV_SLOT_START;
    public static final int INV_SLOT_END = InventoryMenu.INV_SLOT_END;
    public static final int USE_ROW_SLOT_START = InventoryMenu.USE_ROW_SLOT_START;
    public static final int USE_ROW_SLOT_END = InventoryMenu.USE_ROW_SLOT_END;
    public static final int SHIELD_SLOT = InventoryMenu.SHIELD_SLOT;
    public static final int BACKPACK_SLOT_START = SHIELD_SLOT + 1;

    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<EquipmentSlot, Identifier> EMPTY_ARMOR_ICONS = Map.of(
            EquipmentSlot.FEET, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
            EquipmentSlot.LEGS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            EquipmentSlot.CHEST, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            EquipmentSlot.HEAD, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET);

    private final Player owner;
    private final BackpackContainer backpack;
    private final BackpackOpenData data;
    private final BackpackLayout layout;
    private final int backpackSlotEnd;

    /** Client: der Server schickt Stufe und Faktor, den Inhalt liefern die normalen Slot-Pakete. */
    public BackpackMenu(int containerId, Inventory inventory, BackpackOpenData data) {
        this(containerId, inventory, new BackpackContainer(data.tier(), data.stackMultiplier()), data);
    }

    /** Server: {@code backpack} ist der Container des getragenen oder des abgestellten Rucksacks. */
    public BackpackMenu(int containerId, Inventory inventory, BackpackContainer backpack, BackpackOpenData data) {
        super(ModScreenHandlers.BACKPACK_MENU, containerId, 2, 2);
        this.owner = inventory.player;
        this.backpack = backpack;
        this.data = data;
        this.layout = new BackpackLayout(backpack.tier());
        int vx = this.layout.vanillaX();

        // 0: Ergebnis, 1-4: Raster - wie InventoryMenu
        this.addResultSlot(this.owner, vx + 154, 28);
        this.addCraftingGridSlots(vx + 98, 18);

        // 5-8: Ruestung; im Modus "getragen" ist die Brust gesperrt
        boolean worn = !data.placed();
        for (int i = 0; i < 4; i++) {
            EquipmentSlot slot = ARMOR_SLOTS[i];
            boolean lockable = worn && slot == EquipmentSlot.CHEST;
            this.addSlot(new BackpackArmorSlot(inventory, this.owner, slot, 39 - i, vx + 8, 8 + i * 18,
                    EMPTY_ARMOR_ICONS.get(slot), () -> lockable));
        }

        // 9-35: Hauptinventar, 36-44: Hotbar
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + (row + 1) * 9, this.layout.gridX(column), this.layout.mainRowY(row)));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, this.layout.gridX(column), this.layout.hotbarY()));
        }

        // 45: Nebenhand
        Player owner = this.owner;
        this.addSlot(new Slot(inventory, Inventory.SLOT_OFFHAND, vx + 77, 62) {
            @Override
            public void setByPlayer(ItemStack itemStack, ItemStack previous) {
                owner.onEquipItem(EquipmentSlot.OFFHAND, previous, itemStack);
                super.setByPlayer(itemStack, previous);
            }

            @Override
            public Identifier getNoItemIcon() {
                return InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;
            }
        });

        // 46..: Rucksack (erst die Reihen, dann die Zusatzspalten)
        for (int i = 0; i < backpack.getContainerSize(); i++) {
            this.addSlot(new BackpackSlot(backpack, i, this.layout.backpackSlotX(i), this.layout.backpackSlotY(i)));
        }
        this.backpackSlotEnd = this.slots.size();
    }

    public BackpackTier tier() {
        return this.backpack.tier();
    }

    public BackpackLayout layout() {
        return this.layout;
    }

    public BackpackOpenData openData() {
        return this.data;
    }

    public BackpackContainer backpack() {
        return this.backpack;
    }

    public int backpackSlotEnd() {
        return this.backpackSlotEnd;
    }

    public boolean isBackpackSlot(int slotIndex) {
        return slotIndex >= BACKPACK_SLOT_START && slotIndex < this.backpackSlotEnd;
    }

    // =================================================================================
    // Klicks
    // =================================================================================

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        // Hat der Zauberstab, Trichter oder Konstrukteurs Hand inzwischen aus der Komponente
        // genommen, gilt das - vor jedem Lesen neu laden.
        this.backpack.syncFromSource();
        if (isBackpackSlot(slotIndex)) {
            Slot slot = this.slots.get(slotIndex);
            ItemStack inSlot = slot.getItem();
            if (!inSlot.isEmpty() && inSlot.getCount() > inSlot.getMaxStackSize()) {
                // Ein uebergrosser Stapel (Tiefe Taschen) darf nie ganz auf den Cursor oder in die Hotbar.
                if (containerInput == ContainerInput.SWAP) {
                    swapOversized(slot, buttonNum, player);
                    return;
                }
                if (containerInput == ContainerInput.PICKUP && !getCarried().isEmpty()
                        && !ItemStack.isSameItemSameComponents(getCarried(), inSlot)) {
                    // Vanilla wuerde Cursor und Slot tauschen und den ganzen Stapel auf den Cursor legen.
                    return;
                }
            }
        }
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    /** Zifferntaste auf einem uebergrossen Stapel: ein normaler Stapel in einen leeren Hotbar-Slot. */
    private void swapOversized(Slot slot, int buttonNum, Player player) {
        if (!(buttonNum >= 0 && buttonNum < 9 || buttonNum == Inventory.SLOT_OFFHAND)) {
            return;
        }
        Inventory inventory = player.getInventory();
        if (!inventory.getItem(buttonNum).isEmpty()) {
            return;
        }
        ItemStack taken = slot.safeTake(slot.getItem().getMaxStackSize(), Integer.MAX_VALUE, player);
        if (!taken.isEmpty()) {
            inventory.setItem(buttonNum, taken);
        }
    }

    /**
     * Schnellverschieben (Shift-Klick):
     * <ul>
     *   <li>aus Hauptinventar und Hotbar zuerst in den Rucksack, was dort nicht hinpasst wie im
     *       Vanilla-Inventar (Ruestung anlegen, Hauptinventar/Hotbar);</li>
     *   <li>aus dem Rucksack ins Hauptinventar, dann in die Hotbar - nie direkt in einen
     *       Ruestungs-Slot, wie bei einer Truhe;</li>
     *   <li>Ergebnis, Raster, Ruestung und Nebenhand wie Vanilla ins Inventar, notfalls in den
     *       Rucksack.</li>
     * </ul>
     */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(clicked);
        int start = BACKPACK_SLOT_START;
        int end = this.backpackSlotEnd;

        if (slotIndex == RESULT_SLOT) {
            if (!this.moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_END, true)
                    && !this.moveItemStackTo(stack, start, end, false)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, clicked);
        } else if (slotIndex > RESULT_SLOT && slotIndex < INV_SLOT_START) {
            if (!this.moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_END, false)
                    && !this.moveItemStackTo(stack, start, end, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isBackpackSlot(slotIndex)) {
            if (!this.moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END, false)
                    && !this.moveItemStackTo(stack, USE_ROW_SLOT_START, USE_ROW_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex < USE_ROW_SLOT_END && this.moveItemStackTo(stack, start, end, false)) {
            // Hauptinventar/Hotbar -> Rucksack hat (wenigstens teilweise) gegriffen.
        } else if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR
                && !this.slots.get(8 - equipmentSlot.getIndex()).hasItem()) {
            int pos = 8 - equipmentSlot.getIndex();
            if (!this.moveItemStackTo(stack, pos, pos + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (equipmentSlot == EquipmentSlot.OFFHAND && !this.slots.get(SHIELD_SLOT).hasItem() && slotIndex != SHIELD_SLOT) {
            if (!this.moveItemStackTo(stack, SHIELD_SLOT, SHIELD_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= INV_SLOT_START && slotIndex < INV_SLOT_END) {
            if (!this.moveItemStackTo(stack, USE_ROW_SLOT_START, USE_ROW_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= USE_ROW_SLOT_START && slotIndex < USE_ROW_SLOT_END) {
            if (!this.moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_END, false)
                && !this.moveItemStackTo(stack, start, end, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, clicked);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == clicked.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        if (slotIndex == RESULT_SLOT) {
            player.drop(stack, false);
        }
        return clicked;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.resultSlots && super.canTakeItemForPickAll(carried, target);
    }

    // =================================================================================
    // Lebenszyklus
    // =================================================================================

    @Override
    public void slotsChanged(net.minecraft.world.Container container) {
        if (this.owner.level() instanceof ServerLevel level) {
            CraftingGrid.update(this, level, this.owner, this.craftSlots, this.resultSlots);
        }
    }

    @Override
    public void broadcastChanges() {
        this.backpack.syncFromSource();
        // Faengt Aenderungen ohne setChanged ab, etwa die Buendel-Auswahl per Mausrad.
        this.backpack.flush();
        super.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.resultSlots.clearContent();
        if (!player.level().isClientSide()) {
            this.clearContainer(player, this.craftSlots);
            this.backpack.flush();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.backpack.stillValid(player);
    }

    // =================================================================================
    // Rezeptbuch (wie InventoryMenu)
    // =================================================================================

    @Override
    public Slot getResultSlot() {
        return this.slots.get(RESULT_SLOT);
    }

    @Override
    public List<Slot> getInputGridSlots() {
        return this.slots.subList(1, 5);
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    protected Player owner() {
        return this.owner;
    }

    /**
     * Zugang zu Vanillas {@code protected static CraftingMenu#slotChangedCraftingGrid}: eine
     * Unterklasse darf geschuetzte statische Methoden ihrer Oberklasse aufrufen. Instanziert wird
     * sie nie - so rechnet das 2x2-Raster exakt wie im Spielerinventar, ohne Mixin und ohne Kopie.
     */
    private static final class CraftingGrid extends CraftingMenu {
        private CraftingGrid() {
            super(0, null);
        }

        static void update(AbstractContainerMenu menu, ServerLevel level, Player player,
                           CraftingContainer craftSlots, ResultContainer resultSlots) {
            RecipeHolder<CraftingRecipe> noHint = null;
            slotChangedCraftingGrid(menu, level, player, craftSlots, resultSlots, noHint);
        }
    }
}
