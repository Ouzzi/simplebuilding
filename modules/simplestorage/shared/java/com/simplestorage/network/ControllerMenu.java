package com.simplestorage.network;

import com.simplestorage.registry.StorageMenus;
import java.util.List;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The window of the controller chest (BS29b-3): every kind of item of the network in one list ({@link #COLUMNS} x {@link #ROWS}
 * cells, scrolled by rows), the totals as data slots. A click takes a stack (left: a full stack, right: half), a click with an item
 * on the cursor puts it into the network, shift-click takes a stack to the inventory, shift-click in the inventory stores. A broken
 * controller (too many chests, a second controller, too big a network) shows its cause and gives no access.
 *
 * <p>The list cells are ordinary slots over a view container the server fills ({@link #refresh}, a few times a second while open),
 * so Vanilla syncs and draws them; their totals travel as pairs of 15-bit data slots (a data slot is sent as a short).
 */
public class ControllerMenu extends AbstractContainerMenu {
    public static final int COLUMNS = 9, ROWS = 5, CELLS = COLUMNS * ROWS;
    public static final int BUTTON_UP = 0, BUTTON_DOWN = 1, BUTTON_SORT = 2, BUTTON_SEARCH_CLEAR = 3, BUTTON_SEARCH_BACK = 4;
    /** Menu button ids: {@code SCROLL_TO + row} jumps to a row (the scrollbar), {@code CHAR + codepoint} appends a character to the search. */
    public static final int SCROLL_TO = 100, SCROLL_MAX = 999, CHAR = 1000;
    /** Window geometry (menu and screen use the same numbers). */
    public static final int IMAGE_WIDTH = 194, VIEW_X = 8, VIEW_Y = 30, INVENTORY_Y = 140, IMAGE_HEIGHT = 222, INVENTORY_X = 16;
    /** Data slot numbers. */
    public static final int D_STATUS = 0, D_CHESTS = 1, D_LIMIT = 2, D_REJECTED = 3, D_SCROLL = 4, D_MAX_SCROLL = 5, D_SORT = 6, D_KINDS = 7, D_STEP = 8, D_COUNTS = 9;
    private static final int DATA_COUNT = D_COUNTS + 2 * CELLS;
    private static final int REFRESH_TICKS = 5;

    private final SimpleContainer view = new SimpleContainer(CELLS);
    private final ContainerData data;
    private final @Nullable ControllerChestBlockEntity controller;
    private final Player player;
    private List<ChestNetwork.Entry> entries = List.of();
    private ChestNetwork.Sort sort = ChestNetwork.Sort.COUNT;
    private int scroll;
    private String search = "";
    private int ticks;
    private boolean dirty = true;

    /** Client. */
    public ControllerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public static ControllerMenu server(int containerId, Inventory inventory, ControllerChestBlockEntity controller) {
        return new ControllerMenu(containerId, inventory, controller);
    }

    private ControllerMenu(int containerId, Inventory inventory, @Nullable ControllerChestBlockEntity controller) {
        super(StorageMenus.CONTROLLER, containerId);
        this.controller = controller;
        this.player = inventory.player;
        this.data = new SimpleContainerData(DATA_COUNT);
        for (int i = 0; i < CELLS; i++) {
            addSlot(new Slot(this.view, i, VIEW_X + (i % COLUMNS) * 18, VIEW_Y + (i / COLUMNS) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
        addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
        addDataSlots(this.data);
        if (controller != null) {
            controller.startOpen(inventory.player);
            refresh();
        }
    }

    public @Nullable ControllerChestBlockEntity controller() {
        return this.controller;
    }

    // ------------------------------------------------------------------ what the screen reads

    public ChestNetwork.Status status() {
        return ChestNetwork.Status.values()[Math.floorMod(this.data.get(D_STATUS), ChestNetwork.Status.values().length)];
    }

    public int chests() {
        return this.data.get(D_CHESTS);
    }

    public int limit() {
        return this.data.get(D_LIMIT);
    }

    public int rejected() {
        return this.data.get(D_REJECTED);
    }

    public int scrollRow() {
        return this.data.get(D_SCROLL);
    }

    public int maxScroll() {
        return this.data.get(D_MAX_SCROLL);
    }

    public ChestNetwork.Sort sort() {
        return ChestNetwork.Sort.values()[Math.floorMod(this.data.get(D_SORT), ChestNetwork.Sort.values().length)];
    }

    /** The controller's step (its box colour). */
    public ControllerLevel step() {
        return ControllerLevel.values()[Math.floorMod(this.data.get(D_STEP), ControllerLevel.values().length)];
    }

    /** The search text the server filters by (tests). */
    public String search() {
        return this.search;
    }

    public int kinds() {
        return this.data.get(D_KINDS);
    }

    /** The total of the kind in list cell {@code cell} (0 for an empty cell). */
    public long countAt(int cell) {
        if (cell < 0 || cell >= CELLS || this.view.getItem(cell).isEmpty()) return 0;
        return (long) this.data.get(D_COUNTS + 2 * cell) | ((long) this.data.get(D_COUNTS + 2 * cell + 1) << 15);
    }

    // ------------------------------------------------------------------ server: the list

    private void set(int id, int value) {
        this.data.set(id, value);
    }

    /** Looks the network up again and fills the list cells (server only). */
    public void refresh() {
        this.dirty = false;
        ControllerChestBlockEntity controller = this.controller;
        Level level = this.player.level();
        if (controller == null || level.isClientSide()) return;
        ChestNetwork.Scan scan = controller.scan();
        ChestNetwork.Members members = ChestNetwork.members(level, controller.step().rank(), scan);
        ChestNetwork.Status status = controller.status(scan, members);
        set(D_STATUS, status.ordinal());
        set(D_CHESTS, members.chests().size());
        set(D_LIMIT, controller.step().maxChests());
        set(D_STEP, controller.step().ordinal());
        set(D_REJECTED, members.rejected());
        set(D_SORT, this.sort.ordinal());
        this.entries = status == ChestNetwork.Status.OK
                ? ChestNetwork.filter(new ChestNetwork.Storage(level, controller, members, this.player).entries(this.sort), this.search) : List.of();
        int maxScroll = Math.max(0, (this.entries.size() + COLUMNS - 1) / COLUMNS - ROWS);
        this.scroll = Math.max(0, Math.min(this.scroll, maxScroll));
        set(D_SCROLL, this.scroll);
        set(D_MAX_SCROLL, maxScroll);
        set(D_KINDS, this.entries.size());
        for (int cell = 0; cell < CELLS; cell++) {
            int index = this.scroll * COLUMNS + cell;
            ChestNetwork.Entry entry = index < this.entries.size() ? this.entries.get(index) : null;
            ItemStack shown = entry == null ? ItemStack.EMPTY : entry.kind().copyWithCount(1);
            if (!ItemStack.matches(this.view.getItem(cell), shown)) this.view.setItem(cell, shown);
            long count = entry == null ? 0 : Math.min(entry.count(), (1L << 30) - 1);
            set(D_COUNTS + 2 * cell, (int) (count & 0x7FFF));
            set(D_COUNTS + 2 * cell + 1, (int) (count >> 15));
        }
    }

    @Override
    public void broadcastChanges() {
        if (this.controller != null && !this.player.level().isClientSide() && (this.dirty || ++this.ticks % REFRESH_TICKS == 0)) refresh();
        super.broadcastChanges();
    }

    // ------------------------------------------------------------------ clicks

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.controller == null) return false;
        switch (id) {
            case BUTTON_UP -> this.scroll = Math.max(0, this.scroll - 1);
            case BUTTON_DOWN -> this.scroll = this.scroll + 1;
            case BUTTON_SORT -> this.sort = this.sort.next();
            case BUTTON_SEARCH_CLEAR -> {
                this.search = "";
                this.scroll = 0;
            }
            case BUTTON_SEARCH_BACK -> {
                if (!this.search.isEmpty()) this.search = this.search.substring(0, this.search.offsetByCodePoints(this.search.length(), -1));
                this.scroll = 0;
            }
            default -> {
                if (id >= SCROLL_TO && id <= SCROLL_MAX) {
                    this.scroll = id - SCROLL_TO;
                } else if (id >= CHAR && id <= CHAR + Character.MAX_CODE_POINT && this.search.codePointCount(0, this.search.length()) < ChestNetwork.MAX_SEARCH
                        && !Character.isISOControl(id - CHAR)) {
                    this.search += new String(Character.toChars(id - CHAR));
                    this.scroll = 0;
                } else {
                    return false;
                }
            }
        }
        refresh();
        return true;
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        if (slotIndex >= 0 && slotIndex < CELLS) {
            if (this.controller != null && !player.level().isClientSide()) cellClicked(slotIndex, buttonNum, input, player);
            return;
        }
        super.clicked(slotIndex, buttonNum, input, player);
        this.dirty = true;
    }

    private void cellClicked(int cell, int button, ContainerInput input, Player player) {
        ControllerChestBlockEntity controller = this.controller;
        refresh();
        if (controller == null || status() != ChestNetwork.Status.OK) return;
        ChestNetwork.Members members = ChestNetwork.members(player.level(), controller.step().rank(), controller.scan());
        ChestNetwork.Storage storage = new ChestNetwork.Storage(player.level(), controller, members, player);
        ItemStack carried = getCarried();
        if (input == ContainerInput.PICKUP && !carried.isEmpty()) {
            ItemStack toStore = button == 1 ? carried.copyWithCount(1) : carried.copy();
            ItemStack rest = storage.insert(toStore);
            int stored = toStore.getCount() - rest.getCount();
            if (stored > 0) {
                carried.shrink(stored);
                setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            }
        } else {
            int index = this.scroll * COLUMNS + cell;
            if (index >= this.entries.size()) {
                refresh();
                return;
            }
            ItemStack kind = this.entries.get(index).kind();
            long available = this.entries.get(index).count();
            if (input == ContainerInput.PICKUP && carried.isEmpty()) {
                ItemStack taken = storage.take(kind, ChestNetwork.takeAmount(kind, button == 1, available));
                if (!taken.isEmpty()) setCarried(taken);
            } else if (input == ContainerInput.QUICK_MOVE) {
                ItemStack taken = storage.take(kind, ChestNetwork.takeAmount(kind, false, available));
                if (!taken.isEmpty()) {
                    player.getInventory().add(taken);
                    if (!taken.isEmpty()) storage.insert(taken);
                }
            }
        }
        refresh();
    }

    /** Shift-click in the inventory stores the stack in the network. */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < CELLS || this.controller == null || player.level().isClientSide()) return ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ChestNetwork.Scan scan = this.controller.scan();
        ChestNetwork.Members members = ChestNetwork.members(player.level(), this.controller.step().rank(), scan);
        if (this.controller.status(scan, members) != ChestNetwork.Status.OK) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        ItemStack rest = new ChestNetwork.Storage(player.level(), this.controller, members, player).insert(stack);
        if (rest.getCount() == before.getCount()) return ItemStack.EMPTY;
        slot.setByPlayer(rest.isEmpty() ? ItemStack.EMPTY : rest);
        this.dirty = true;
        return before;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.controller == null || this.controller.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.controller != null) this.controller.stopOpen(player);
    }
}
