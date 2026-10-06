package com.simplelib.crucible;

import com.simplelib.registry.LibMenus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Crucible menu: the crucible slots (compact layout), one hidden slot per crucible slot that carries
 * the reserved result (ghost item) to the client, the attached barrel's 9 fields and their ghosts, the
 * crucible slots again (layout with barrel), then the player inventory for both layouts. Slot states
 * and heat arrive through {@link ContainerData}.
 */
public class CrucibleMenu extends AbstractContainerMenu {
    // Layout v3 (owner feedback 2026-10-06): the crucible slots form one contiguous chest-like grid (3 columns per
    // grid, 18 px pitch); the 9 fields of an attached barrel sit beside it with a gap and only take room while a barrel
    // is attached - before that everything is compact and centred. Slots cannot move, so the menu holds both layouts
    // (crucible and inventory slots twice, only the current one active); the screen reserves the larger box and draws
    // the current panel in it.
    // N12/N12b (owner, images 3/4): two boxes like image 3 - the crucible box on top (title y 6, grid from y 18, the
    // flame strip under and behind the grid) and, BOX_GAP below it, a separate inventory box (label 6 px under its
    // top, inventory 17 px under it). Both boxes have image 4's thick frame: 5 px at the top and the sides, 7 px at the
    // bottom (2 px shadow), see CrucibleScreen.box.
    public static final int GRID_TOP = 18;
    /** Room under the slots (inside the 7 px bottom frame) where the flame strip shows. */
    public static final int FIRE_ROOM = 12;
    public static final int BARREL_GAP = 10;
    public static final int MARGIN = 8;
    /** Gap between the crucible box and the inventory box. */
    public static final int BOX_GAP = 2;
    /** Inventory box: label and first slot row below the box top, total height. */
    public static final int INVENTORY_LABEL = 6, INVENTORY_TOP = 17, INVENTORY_BOX = 100;

    /** One panel layout; all positions relative to the reserved box (the screen's image). */
    public record Layout(int x, int y, int width, int height, int gridLeft, int barrelLeft, int sectionHeight,
                         int inventoryLeft, int inventoryTop) {
        public int slotX(CrucibleTier tier, int slot) {
            return gridLeft + 1 + (tier.grid(slot) * CrucibleTier.COLUMNS + tier.column(slot)) * 18;
        }

        public int slotY(CrucibleTier tier, int slot) {
            return y + GRID_TOP + 1 + tier.row(slot) * 18;
        }

        public int barrelX(int slot) {
            return barrelLeft + 1 + slot % 3 * 18;
        }

        public int barrelY(int slot) {
            return y + GRID_TOP + 1 + slot / 3 * 18;
        }

        /** Top of the inventory box. */
        public int inventoryBoxTop() {
            return y + sectionHeight + BOX_GAP;
        }
    }

    public static int gridsWidth(CrucibleTier tier) {
        return tier.grids() * CrucibleTier.COLUMNS * 18;
    }

    private static int contentWidth(CrucibleTier tier, boolean barrel) {
        return gridsWidth(tier) + (barrel ? BARREL_GAP + 54 : 0);
    }

    private static int panelWidth(CrucibleTier tier, boolean barrel) {
        return Math.max(176, contentWidth(tier, barrel) + 2 * MARGIN);
    }

    private static int sectionHeight(CrucibleTier tier, boolean barrel) {
        return GRID_TOP + Math.max(tier.rows() * 18, barrel ? 54 : 0) + FIRE_ROOM;
    }

    private static int panelHeight(CrucibleTier tier, boolean barrel) {
        return sectionHeight(tier, barrel) + BOX_GAP + INVENTORY_BOX;
    }

    /** Width of the reserved box: the wider of both layouts. */
    public static int imageWidth(CrucibleTier tier) {
        return Math.max(panelWidth(tier, false), panelWidth(tier, true));
    }

    public static int imageHeight(CrucibleTier tier) {
        return Math.max(panelHeight(tier, false), panelHeight(tier, true));
    }

    public static Layout layout(CrucibleTier tier, boolean barrel) {
        int w = panelWidth(tier, barrel), h = panelHeight(tier, barrel);
        int x = (imageWidth(tier) - w) / 2, y = (imageHeight(tier) - h) / 2;
        int gridLeft = x + (w - contentWidth(tier, barrel)) / 2;
        int section = sectionHeight(tier, barrel);
        return new Layout(x, y, w, h, gridLeft, gridLeft + gridsWidth(tier) + BARREL_GAP, section,
                x + (w - 162) / 2 + 1, y + section + BOX_GAP + INVENTORY_TOP);
    }

    private final CrucibleTier tier;
    private final Container container;
    private final ContainerData data;
    private final @Nullable CrucibleBlockEntity crucible;

    /** Client constructor (one menu type per tier). */
    public static CrucibleMenu client(CrucibleTier tier, int id, Inventory inventory) {
        return new CrucibleMenu(tier, id, inventory, new SimpleContainer(tier.slots()), new SimpleContainer(tier.slots()),
                new SimpleContainer(BarrelTier.CRUCIBLE_SLOTS), new SimpleContainer(BarrelTier.CRUCIBLE_SLOTS),
                new SimpleContainerData(tier.slots() + 4), null);
    }

    public CrucibleMenu(int id, Inventory inventory, CrucibleBlockEntity crucible) {
        this(crucible.tier(), id, inventory, crucible, new GhostView(crucible, false), new BarrelView(crucible),
                new GhostView(crucible, true), crucible.data(), crucible);
    }

    private CrucibleMenu(CrucibleTier tier, int id, Inventory inventory, Container container, Container ghosts,
                         Container barrel, Container barrelGhosts, ContainerData data, @Nullable CrucibleBlockEntity crucible) {
        super(LibMenus.forTier(tier), id);
        this.tier = tier;
        this.container = container;
        this.data = data;
        this.crucible = crucible;
        checkContainerSize(container, tier.slots());
        container.startOpen(inventory.player);
        Layout compact = layout(tier, false), wide = layout(tier, true);
        for (int i = 0; i < tier.slots(); i++) addSlot(new CrucibleSlot(container, i, compact.slotX(tier, i), compact.slotY(tier, i), false));
        for (int i = 0; i < tier.slots(); i++) addSlot(new GhostSlot(ghosts, i, compact.slotX(tier, i), compact.slotY(tier, i)));
        for (int i = 0; i < BarrelTier.CRUCIBLE_SLOTS; i++) addSlot(new BarrelSlot(barrel, i, wide.barrelX(i), wide.barrelY(i)));
        for (int i = 0; i < BarrelTier.CRUCIBLE_SLOTS; i++) addSlot(new GhostSlot(barrelGhosts, i, wide.barrelX(i), wide.barrelY(i)));
        for (int i = 0; i < tier.slots(); i++) addSlot(new CrucibleSlot(container, i, wide.slotX(tier, i), wide.slotY(tier, i), true));
        inventorySlots(inventory, compact, false);
        inventorySlots(inventory, wide, true);
        addDataSlots(data);
    }

    public CrucibleTier tier() { return tier; }

    /** Player inventory (27 + hotbar) at {@code layout}; active only in that layout. */
    private void inventorySlots(Inventory inventory, Layout layout, boolean barrelLayout) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new LayoutSlot(inventory, col + row * 9 + 9, layout.inventoryLeft() + col * 18, layout.inventoryTop() + row * 18, barrelLayout));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new LayoutSlot(inventory, col, layout.inventoryLeft() + col * 18, layout.inventoryTop() + 58, barrelLayout));
        }
    }

    /** First index of the crucible slots of the layout with an attached barrel. */
    public int wideCrucibleStart() { return 2 * tier.slots() + 2 * BarrelTier.CRUCIBLE_SLOTS; }

    private int inventoryStart() { return wideCrucibleStart() + tier.slots(); }

    /** The crucible slot index (0..slots-1) a menu slot shows, or -1 for any other slot. */
    public int crucibleIndex(Slot slot) {
        return slot instanceof CrucibleSlot ? slot.getContainerSlot() : -1;
    }

    public int slotState(int slot) { return data.get(slot) / 128; }
    public int slotPercent(int slot) { return data.get(slot) % 128; }
    public HeatLevel heat() { return HeatLevel.byId(data.get(tier.slots())); }
    public int afterglow() { return data.get(tier.slots() + 1); }
    public boolean twoBelow() { return data.get(tier.slots() + 2) != 0; }
    public ItemStack ghost(int slot) { return slots.get(tier.slots() + slot).getItem(); }
    public boolean barrelAttached() { return data.get(tier.slots() + 3) != 0; }
    public int barrelStart() { return 2 * tier.slots(); }
    public ItemStack barrelGhost(int slot) { return slots.get(2 * tier.slots() + BarrelTier.CRUCIBLE_SLOTS + slot).getItem(); }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        int n = tier.slots();
        Slot slot = slots.get(index);
        if (slot instanceof GhostSlot || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int barrelStart = 2 * n;
        int inventoryStart = inventoryStart();
        // Both inventory layouts show the same 36 slots; moving into the first copy is enough.
        if (slot instanceof CrucibleSlot || slot instanceof BarrelSlot) {
            if (!moveItemStackTo(stack, inventoryStart, inventoryStart + 36, true)) return ItemStack.EMPTY;
            slot.onTake(player, copy);
        } else if (!moveItemStackTo(stack, 0, n, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    /** A crucible slot; taking a finished result pays out the stored experience (owner F20). */
    private final class CrucibleSlot extends Slot {
        private final boolean barrelLayout;

        CrucibleSlot(Container container, int index, int x, int y, boolean barrelLayout) {
            super(container, index, x, y);
            this.barrelLayout = barrelLayout;
        }

        @Override public boolean isActive() { return barrelAttached() == barrelLayout; }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return container.getMaxStackSize(stack);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            if (crucible != null && player.level() instanceof ServerLevel server && crucible.isResult(getContainerSlot())) {
                crucible.awardExperience(server, Vec3.atCenterOf(crucible.getBlockPos()));
            }
            super.onTake(player, stack);
        }
    }

    /** A player inventory slot of one of the two layouts. */
    private final class LayoutSlot extends Slot {
        private final boolean barrelLayout;

        LayoutSlot(Container container, int index, int x, int y, boolean barrelLayout) {
            super(container, index, x, y);
            this.barrelLayout = barrelLayout;
        }

        @Override public boolean isActive() { return barrelAttached() == barrelLayout; }
    }

    /** One of the 9 fields of an attached barrel; hidden while no barrel is attached. */
    private final class BarrelSlot extends Slot {
        BarrelSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean isActive() { return barrelAttached(); }
        @Override public boolean mayPlace(ItemStack stack) { return barrelAttached(); }
        @Override public boolean mayPickup(Player player) { return barrelAttached(); }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return container.getMaxStackSize(stack);
        }
    }

    /** Hidden slot that only syncs the reserved result to the client screen. */
    private static final class GhostSlot extends Slot {
        GhostSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean isActive() { return false; }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
    }

    /** Read-only view of the crucible's reserved results (its own slots or the barrel's). */
    private static final class GhostView extends SimpleContainer {
        private final CrucibleBlockEntity crucible;
        private final boolean barrel;

        GhostView(CrucibleBlockEntity crucible, boolean barrel) {
            super(barrel ? BarrelTier.CRUCIBLE_SLOTS : crucible.tier().slots());
            this.crucible = crucible;
            this.barrel = barrel;
        }

        @Override
        public ItemStack getItem(int slot) {
            return barrel ? crucible.barrelGhost(slot) : crucible.ghost(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) { return ItemStack.EMPTY; }
        @Override
        public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
        @Override
        public void setItem(int slot, ItemStack stack) {}
    }

    /** The first 9 slots of the attached barrel, or nothing while none is attached. */
    private static final class BarrelView implements Container {
        private final CrucibleBlockEntity crucible;

        BarrelView(CrucibleBlockEntity crucible) {
            this.crucible = crucible;
        }

        private @Nullable CrucibleBarrelBlockEntity barrel() {
            return crucible.barrel();
        }

        @Override public int getContainerSize() { return BarrelTier.CRUCIBLE_SLOTS; }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < getContainerSize(); i++) if (!getItem(i).isEmpty()) return false;
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            var b = barrel();
            return b == null ? ItemStack.EMPTY : b.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            var b = barrel();
            return b == null ? ItemStack.EMPTY : b.removeItem(slot, count);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            var b = barrel();
            return b == null ? ItemStack.EMPTY : b.removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            var b = barrel();
            if (b != null) b.setItem(slot, stack);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            var b = barrel();
            return b == null ? stack.getMaxStackSize() : b.getMaxStackSize(stack);
        }

        @Override
        public void setChanged() {
            var b = barrel();
            if (b != null) b.setChanged();
        }

        @Override public boolean stillValid(Player player) { return true; }

        @Override public void clearContent() {}
    }
}
