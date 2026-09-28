package com.simplebuilding.screen;

import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.items.custom.BackpackItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Das Menue der Mod-Truhen: bis zu 18 Spalten mal 6 Reihen, darunter das Spielerinventar mittig,
 * alles ohne Blaettern sichtbar (Geometrie siehe {@link ChestTier} und {@link #imageWidth()}).
 *
 * <p>Wie {@code ChestMenu}, mit zwei Ergaenzungen fuer uebergrosse Stapel (Netherit x2, Enderit
 * x4), die {@link BackpackMenu} genauso macht: ein uebergrosser Stapel geht nie im Ganzen auf den
 * Cursor ({@link TieredChestSlot#tryRemove}), nie per Zifferntaste in einen belegten Hotbar-Platz
 * und tauscht nicht mit einem anderen Cursor-Stapel.
 */
public class TieredChestMenu extends AbstractContainerMenu {
    public static final int SLOT = 18;
    public static final int VANILLA_WIDTH = 176;

    private final Container container;
    private final ChestTier tier;
    private final boolean isDouble;
    private final int columns;
    private final int rows;
    private final int chestSlots;

    /** Client: aus den Oeffnungsdaten, mit einem Container, der nichts abschneidet. */
    public TieredChestMenu(int containerId, Inventory inventory, TieredChestOpenData data) {
        this(containerId, inventory, new ClientStorage(data.tier().slots() * (data.isDouble() ? 2 : 1),
                data.tier().stackMultiplier()), data.tier(), data.isDouble());
    }

    /** Server: {@code container} ist die Truhe oder, fuer eine Doppeltruhe, Vanillas {@code CompoundContainer}. */
    public static TieredChestMenu server(int containerId, Inventory inventory, Container container, ChestTier tier, boolean isDouble) {
        return new TieredChestMenu(containerId, inventory, container, tier, isDouble);
    }

    private TieredChestMenu(int containerId, Inventory inventory, Container container, ChestTier tier, boolean isDouble) {
        super(ModScreenHandlers.TIERED_CHEST_MENU, containerId);
        this.tier = tier;
        this.isDouble = isDouble;
        this.columns = tier.columns(isDouble);
        this.rows = tier.rows(isDouble);
        this.chestSlots = this.columns * this.rows;
        checkContainerSize(container, this.chestSlots);
        this.container = container;
        container.startOpen(inventory.player);
        int left = (imageWidth() - (14 + SLOT * this.columns)) / 2 + 8;
        for (int row = 0; row < this.rows; row++) {
            for (int column = 0; column < this.columns; column++) {
                this.addSlot(new TieredChestSlot(container, column + row * this.columns,
                        left + column * SLOT, 18 + row * SLOT, tier.stackMultiplier()));
            }
        }
        this.addStandardInventorySlots(inventory, inventoryLeft(), 18 + this.rows * SLOT + 13);
    }

    // --- Geometrie (Menue und Bildschirm rechnen mit denselben Zahlen) ---

    public int imageWidth() {
        return Math.max(VANILLA_WIDTH, 14 + SLOT * this.columns);
    }

    public int imageHeight() {
        return 114 + this.rows * SLOT;
    }

    public int inventoryLeft() {
        return (imageWidth() - VANILLA_WIDTH) / 2 + 8;
    }

    public ChestTier tier() {
        return this.tier;
    }

    public boolean isDouble() {
        return this.isDouble;
    }

    public int columns() {
        return this.columns;
    }

    public int rows() {
        return this.rows;
    }

    public int chestSlotCount() {
        return this.chestSlots;
    }

    public Container getContainer() {
        return this.container;
    }

    /** Zeigt dieses Menue {@code chest} (allein oder als Haelfte einer Doppeltruhe)? */
    public boolean shows(Container chest) {
        return this.container == chest
                || this.container instanceof CompoundContainer compound && compound.contains(chest);
    }

    // --- Klicks ---

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (slotIndex >= 0 && slotIndex < this.chestSlots) {
            Slot slot = this.slots.get(slotIndex);
            ItemStack inSlot = slot.getItem();
            if (!inSlot.isEmpty() && inSlot.getCount() > inSlot.getMaxStackSize()) {
                if (containerInput == ContainerInput.SWAP) {
                    swapOversized(slot, buttonNum, player);
                    return;
                }
                if (containerInput == ContainerInput.PICKUP && !getCarried().isEmpty()
                        && !ItemStack.isSameItemSameComponents(getCarried(), inSlot)) {
                    return;
                }
            }
        }
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    /** Zifferntaste auf einem uebergrossen Stapel: ein normaler Stapel in einen leeren Hotbar-Platz. */
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

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < this.chestSlots) {
            if (!this.moveItemStackTo(stack, this.chestSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, this.chestSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    /**
     * Der Container auf dem Client: wie {@code SimpleContainer}, aber ohne dessen Kuerzen in
     * {@code setItem} - sonst kaeme ein 256er-Stapel vom Server als 64er an.
     */
    static final class ClientStorage implements Container {
        private final NonNullList<ItemStack> items;
        private final int multiplier;

        ClientStorage(int size, int multiplier) {
            this.items = NonNullList.withSize(size, ItemStack.EMPTY);
            this.multiplier = multiplier;
        }

        @Override
        public int getContainerSize() {
            return this.items.size();
        }

        @Override
        public boolean isEmpty() {
            return this.items.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= 0 && slot < this.items.size() ? this.items.get(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            return ContainerHelper.removeItem(this.items, slot, count);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(this.items, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            this.items.set(slot, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 99 * this.multiplier;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return BackpackItem.maxStackSizeIn(stack, this.multiplier);
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            this.items.clear();
        }
    }
}
