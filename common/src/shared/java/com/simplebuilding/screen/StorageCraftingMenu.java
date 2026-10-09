package com.simplebuilding.screen;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.StorageCraftingTableBlockEntity;
import java.util.List;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

/**
 * Menu of the Storage Crafting Table (queue N26): the crafting table's menu - same slot layout, recipe book, shift
 * clicks and result rules (Vanilla's {@code CraftingMenu#slotChangedCraftingGrid}) - but on the server the nine grid
 * slots are the table's shared grid ({@link StorageCraftingTableBlockEntity#grid()}) instead of a grid of its own.
 * Closing therefore returns nothing to the player: the items stay on the table. The client builds the menu without a
 * table and shows what the server sends, like any container.
 */
public class StorageCraftingMenu extends AbstractCraftingMenu {
    public static final int RESULT_SLOT = 0;
    private static final int GRID_END = 10;
    private static final int INV_START = 10;
    private static final int INV_END = 37;
    private static final int USE_ROW_END = 46;

    private final ContainerLevelAccess access;
    private final Player player;
    private final @Nullable StorageCraftingTableBlockEntity table;
    /** The table's grid on the server, the menu's own grid on the client. */
    private final CraftingContainer grid;
    private boolean placingRecipe;

    /** Client side (from the open screen packet). */
    public StorageCraftingMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL, null);
    }

    public StorageCraftingMenu(int containerId, Inventory inventory, ContainerLevelAccess access, @Nullable StorageCraftingTableBlockEntity table) {
        super(ModScreenHandlers.STORAGE_CRAFTING_TABLE_MENU, containerId, 3, 3);
        this.access = access;
        this.player = inventory.player;
        this.table = table;
        this.grid = table != null ? table.grid() : this.craftSlots;
        this.addSlot(new ResultSlot(this.player, this.grid, this.resultSlots, 0, 124, 35));
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                this.addSlot(new Slot(this.grid, x + y * 3, 30 + x * 18, 17 + y * 18));
            }
        }
        this.addStandardInventorySlots(inventory, 8, 84);
        if (table != null) {
            table.open(this);
            slotsChanged(this.grid);
        }
    }

    /** The grid this menu crafts from (the table's on the server). */
    public CraftingContainer grid() {
        return this.grid;
    }

    @Override
    public void slotsChanged(Container container) {
        if (!this.placingRecipe) {
            this.access.execute((level, pos) -> {
                if (level instanceof ServerLevel serverLevel) {
                    Vanilla.update(this, serverLevel, this.player, this.grid, this.resultSlots, null);
                }
            });
        }
    }

    @Override
    public RecipeBookMenu.PostPlaceAction handlePlacement(boolean useMaxItems, boolean allowDroppingItemsToClear, RecipeHolder<?> recipe,
            ServerLevel level, Inventory inventory) {
        @SuppressWarnings("unchecked")
        RecipeHolder<CraftingRecipe> typed = (RecipeHolder<CraftingRecipe>) recipe;
        this.placingRecipe = true;
        try {
            List<Slot> inputs = this.getInputGridSlots();
            return ServerPlaceRecipe.placeRecipe(new ServerPlaceRecipe.CraftingMenuAccess<CraftingRecipe>() {
                @Override
                public void fillCraftSlotsStackedContents(StackedItemContents contents) {
                    StorageCraftingMenu.this.fillCraftSlotsStackedContents(contents);
                }

                @Override
                public void clearCraftingContent() {
                    StorageCraftingMenu.this.resultSlots.clearContent();
                    StorageCraftingMenu.this.grid.clearContent();
                }

                @Override
                public boolean recipeMatches(RecipeHolder<CraftingRecipe> candidate) {
                    return candidate.value().matches(StorageCraftingMenu.this.grid.asCraftInput(), StorageCraftingMenu.this.player.level());
                }
            }, 3, 3, inputs, inputs, inventory, typed, useMaxItems, allowDroppingItemsToClear);
        } finally {
            this.placingRecipe = false;
            Vanilla.update(this, level, this.player, this.grid, this.resultSlots, typed);
        }
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents contents) {
        this.grid.fillStackedContents(contents);
    }

    /** Unlike the crafting table, nothing goes back to the player: the grid stays on the table. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.table != null) {
            this.table.close(this);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return ModBlocks.STORAGE_CRAFTING_TABLE != null && stillValid(this.access, player, ModBlocks.STORAGE_CRAFTING_TABLE)
                && (this.table == null || !this.table.isRemoved());
    }

    /** Vanilla's crafting table rules: result to the inventory (hotbar last), inventory into the grid, grid out. */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex == RESULT_SLOT) {
                stack.getItem().onCraftedBy(stack, player);
                if (!this.moveItemStackTo(stack, INV_START, USE_ROW_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
            } else if (slotIndex >= INV_START && slotIndex < USE_ROW_END) {
                if (!this.moveItemStackTo(stack, 1, GRID_END, false)) {
                    if (slotIndex < INV_END) {
                        if (!this.moveItemStackTo(stack, INV_END, USE_ROW_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(stack, INV_START, USE_ROW_END, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
            if (slotIndex == RESULT_SLOT) {
                // What did not fit: into the inventory or onto the ground (same on both lines, no Prediction API).
                if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack);
                }
            }
        }
        return clicked;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.resultSlots && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public Slot getResultSlot() {
        return this.slots.get(RESULT_SLOT);
    }

    @Override
    public List<Slot> getInputGridSlots() {
        return this.slots.subList(1, GRID_END);
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    protected Player owner() {
        return this.player;
    }

    /**
     * Access to Vanilla's {@code protected static CraftingMenu#slotChangedCraftingGrid} (a subclass may call it):
     * the result is computed exactly like at the crafting table, including the mod's mixins on that method. Never
     * instantiated.
     */
    private static final class Vanilla extends CraftingMenu {
        private Vanilla() {
            super(0, null);
        }

        static void update(AbstractContainerMenu menu, ServerLevel level, Player player, CraftingContainer grid, ResultContainer result,
                @Nullable RecipeHolder<CraftingRecipe> hint) {
            slotChangedCraftingGrid(menu, level, player, grid, result, hint);
        }
    }
}
