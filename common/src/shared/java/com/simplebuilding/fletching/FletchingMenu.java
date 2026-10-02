package com.simplebuilding.fletching;

import com.simplebuilding.screen.ModScreenHandlers;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import java.util.List;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Menue des Befiederungstischs (B14): Spitze, Schaft und Befiederung liegen diagonal wie ein Pfeil (Befiederung unten
 * links, Spitze oben rechts), rechts das Ergebnis mit {@link ArrowParts#ARROWS_PER_CRAFT} Pfeilen. Jeder Slot nimmt nur
 * seine Teile an; das Ergebnis rechnet allein der Server aus den Slots aus.
 *
 * <p>Rezeptbuch wie an der Werkbank ({@link RecipeBookMenu}): ein Klick auf einen Pfeil legt Spitze, Schaft und
 * Befiederung aus dem eigenen Inventar ein (Vanilla-Platzierung, nichts entsteht neu); fehlt ein Teil, zeigt der Client
 * das Geisterrezept. Die Rezepte ({@link FletchingRecipe}) dienen nur dem Buch; das Ergebnis rechnet der Server aus den Slots.
 */
public class FletchingMenu extends RecipeBookMenu {
    public static final int TIP_SLOT = 0;
    public static final int SHAFT_SLOT = 1;
    public static final int FLETCHING_SLOT = 2;
    public static final int RESULT_SLOT = 3;
    private static final int INV_START = 4;
    private static final int INV_END = 31;
    private static final int HOTBAR_END = 40;

    private static final Identifier[] EMPTY_ICONS = {
            Identifier.fromNamespaceAndPath("simplebuilding", "container/slot/arrow_tip"),
            Identifier.fromNamespaceAndPath("simplebuilding", "container/slot/arrow_shaft"),
            Identifier.fromNamespaceAndPath("simplebuilding", "container/slot/arrow_fletching")};

    private final ContainerLevelAccess access;
    private long lastSoundTime;
    /** Waehrend der Rezeptbuch-Platzierung wird das Ergebnis erst am Ende einmal berechnet. */
    private boolean placingRecipe;
    public final Container parts = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            FletchingMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer result = new ResultContainer();

    public FletchingMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public FletchingMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModScreenHandlers.FLETCHING_MENU, containerId);
        this.access = access;
        // Wie die Werkbank: die drei Teile liegen auf der Diagonale ihres 3x3-Gitters, das Ergebnis rechts daneben.
        this.addSlot(new PartSlot(this.parts, TIP_SLOT, 66, 17));
        this.addSlot(new PartSlot(this.parts, SHAFT_SLOT, 48, 35));
        this.addSlot(new PartSlot(this.parts, FLETCHING_SLOT, 30, 53));
        this.addSlot(new Slot(this.result, 0, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack taken) {
                taken.onCraftedBy(player, taken.getCount());
                FletchingMenu.this.parts.removeItem(TIP_SLOT, 1);
                FletchingMenu.this.parts.removeItem(SHAFT_SLOT, 1);
                FletchingMenu.this.parts.removeItem(FLETCHING_SLOT, 1);
                FletchingMenu.this.access.execute((level, pos) -> {
                    if (FletchingMenu.this.lastSoundTime != level.getGameTime()) {
                        level.playSound(null, pos, SoundEvents.VILLAGER_WORK_FLETCHER, SoundSource.BLOCKS, 1.0F, 1.0F);
                        FletchingMenu.this.lastSoundTime = level.getGameTime();
                    }
                });
                super.onTake(player, taken);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    /** Welcher Teil-Slot dieses Item annimmt, sonst -1. */
    public static int partSlotFor(ItemStack stack) {
        if (ArrowParts.tipFor(stack) != null) return TIP_SLOT;
        if (ArrowParts.shaftFor(stack) != null) return SHAFT_SLOT;
        if (ArrowParts.fletchingFor(stack) != null) return FLETCHING_SLOT;
        return -1;
    }

    /** Das Ergebnis aus drei Slot-Stapeln, leer wenn ein Teil fehlt oder nicht passt. */
    public static ItemStack resultFor(ItemStack tip, ItemStack shaft, ItemStack fletching) {
        ArrowParts.Tip t = ArrowParts.tipFor(tip);
        ArrowParts.Shaft s = ArrowParts.shaftFor(shaft);
        ArrowParts.Fletching f = ArrowParts.fletchingFor(fletching);
        if (t == null || s == null || f == null) {
            return ItemStack.EMPTY;
        }
        return ArrowParts.stack(new ArrowParts.Parts(t, s, f), ArrowParts.ARROWS_PER_CRAFT);
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == this.parts && !this.placingRecipe) {
            this.result.setItem(0, resultFor(this.parts.getItem(TIP_SLOT), this.parts.getItem(SHAFT_SLOT), this.parts.getItem(FLETCHING_SLOT)));
            this.broadcastChanges();
        }
        super.slotsChanged(container);
    }

    /** Rezeptbuch (Vanilla {@code ServerPlaceRecipe}, 3x1-Gitter Spitze, Schaft, Befiederung): Shift = so viele wie moeglich. */
    @Override
    @SuppressWarnings("unchecked")
    public RecipeBookMenu.PostPlaceAction handlePlacement(boolean useMaxItems, boolean allowDroppingItemsToClear, RecipeHolder<?> recipe,
                                                         ServerLevel level, Inventory inventory) {
        if (!(recipe.value() instanceof FletchingRecipe)) {
            return RecipeBookMenu.PostPlaceAction.NOTHING;
        }
        List<Slot> grid = List.of(this.slots.get(TIP_SLOT), this.slots.get(SHAFT_SLOT), this.slots.get(FLETCHING_SLOT));
        this.placingRecipe = true;
        try {
            return ServerPlaceRecipe.placeRecipe(new ServerPlaceRecipe.CraftingMenuAccess<FletchingRecipe>() {
                @Override
                public void fillCraftSlotsStackedContents(StackedItemContents contents) {
                    FletchingMenu.this.fillCraftSlotsStackedContents(contents);
                }

                @Override
                public void clearCraftingContent() {
                    FletchingMenu.this.parts.clearContent();
                }

                @Override
                public boolean recipeMatches(RecipeHolder<FletchingRecipe> holder) {
                    return holder.value().matches(new SmithingRecipeInput(FletchingMenu.this.parts.getItem(TIP_SLOT),
                            FletchingMenu.this.parts.getItem(SHAFT_SLOT), FletchingMenu.this.parts.getItem(FLETCHING_SLOT)), level);
                }
            }, 3, 1, grid, grid, inventory, (RecipeHolder<FletchingRecipe>) recipe, useMaxItems, allowDroppingItemsToClear);
        } finally {
            this.placingRecipe = false;
            this.slotsChanged(this.parts);
        }
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents contents) {
        for (int i = 0; i < this.parts.getContainerSize(); i++) {
            contents.accountSimpleStack(this.parts.getItem(i));
        }
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, Blocks.FLETCHING_TABLE);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.result && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (slotIndex == RESULT_SLOT) {
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, moved);
        } else if (slotIndex < RESULT_SLOT) {
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            int part = partSlotFor(stack);
            if (part >= 0) {
                if (!this.moveItemStackTo(stack, part, part + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex < INV_END) {
                if (!this.moveItemStackTo(stack, INV_END, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == moved.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return moved;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.result.removeItemNoUpdate(0);
        this.access.execute((level, pos) -> this.clearContainer(player, this.parts));
    }

    /** Teil-Slot: nimmt nur Teile seiner Art an. */
    private static final class PartSlot extends Slot {
        PartSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return partSlotFor(stack) == this.getContainerSlot();
        }

        /** Silhouette im leeren Slot wie bei den Ruestungsslots: Spitze, Schaft, Befiederung. */
        @Override
        public Identifier getNoItemIcon() {
            return EMPTY_ICONS[this.getContainerSlot()];
        }
    }
}
