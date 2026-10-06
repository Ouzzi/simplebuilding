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
 * Crucible menu: the crucible slots in 3-column grids, then one hidden slot per crucible slot that
 * carries the reserved result (ghost item) to the client, then the player inventory. Slot states
 * and heat arrive through {@link ContainerData}.
 */
public class CrucibleMenu extends AbstractContainerMenu {
    // Layout v2 (owner addition 11, Vanilla container conventions): title at y 6, slot grids from y 18 on the
    // 18 px raster, the content block (heat column, grids, barrel) centred in the panel, inventory label 11 px
    // above the inventory, 7 px margin under the hotbar.
    public static final int GRID_GAP = 4;
    public static final int THERMO_WIDTH = 10;
    public static final int THERMO_GAP = 4;
    public static final int GRID_TOP = 18;
    public static final int FIRE_GAP = 2;
    public static final int FIRE_HEIGHT = 10;
    /** Room on the right for the 9 fields of an attached barrel (placeholders while none is attached). */
    public static final int BARREL_GAP = 8;
    public static final int MARGIN = 8;

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
        for (int i = 0; i < tier.slots(); i++) addSlot(new CrucibleSlot(container, i, slotX(tier, i), slotY(tier, i)));
        for (int i = 0; i < tier.slots(); i++) addSlot(new GhostSlot(ghosts, i, slotX(tier, i), slotY(tier, i)));
        for (int i = 0; i < BarrelTier.CRUCIBLE_SLOTS; i++) addSlot(new BarrelSlot(barrel, i, barrelX(tier, i), barrelY(i)));
        for (int i = 0; i < BarrelTier.CRUCIBLE_SLOTS; i++) addSlot(new GhostSlot(barrelGhosts, i, barrelX(tier, i), barrelY(i)));
        addStandardInventorySlots(inventory, inventoryLeft(tier), inventoryTop(tier));
        addDataSlots(data);
    }

    public CrucibleTier tier() { return tier; }

    public static int gridsWidth(CrucibleTier tier) {
        return tier.grids() * 54 + (tier.grids() - 1) * GRID_GAP;
    }

    /** Heat column + grids + barrel grid. */
    public static int contentWidth(CrucibleTier tier) {
        return THERMO_WIDTH + THERMO_GAP + gridsWidth(tier) + BARREL_GAP + 54;
    }

    public static int imageWidth(CrucibleTier tier) {
        return Math.max(176, contentWidth(tier) + 2 * MARGIN);
    }

    public static int contentLeft(CrucibleTier tier) {
        return (imageWidth(tier) - contentWidth(tier)) / 2;
    }

    public static int thermoLeft(CrucibleTier tier) {
        return contentLeft(tier);
    }

    public static int gridLeft(CrucibleTier tier) {
        return contentLeft(tier) + THERMO_WIDTH + THERMO_GAP;
    }

    public static int barrelLeft(CrucibleTier tier) {
        return gridLeft(tier) + gridsWidth(tier) + BARREL_GAP;
    }

    public static int fireTop(CrucibleTier tier) {
        return GRID_TOP + tier.rows() * 18 + FIRE_GAP;
    }

    /** Bottom of the crucible part: the fire strip or the 3x3 barrel grid, whichever is lower. */
    public static int contentBottom(CrucibleTier tier) {
        return Math.max(fireTop(tier) + FIRE_HEIGHT, GRID_TOP + 54);
    }

    public static int barrelX(CrucibleTier tier, int slot) {
        return barrelLeft(tier) + 1 + slot % 3 * 18;
    }

    public static int barrelY(int slot) {
        return GRID_TOP + 1 + slot / 3 * 18;
    }

    public static int inventoryTop(CrucibleTier tier) {
        return contentBottom(tier) + 15;
    }

    public static int imageHeight(CrucibleTier tier) {
        return inventoryTop(tier) + 76 + 7;
    }

    public static int inventoryLeft(CrucibleTier tier) {
        return (imageWidth(tier) - 162) / 2 + 1;
    }

    public static int slotX(CrucibleTier tier, int slot) {
        return gridLeft(tier) + 1 + tier.grid(slot) * (54 + GRID_GAP) + tier.column(slot) * 18;
    }

    public static int slotY(CrucibleTier tier, int slot) {
        return GRID_TOP + 1 + tier.row(slot) * 18;
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
        int inventoryStart = barrelStart + 2 * BarrelTier.CRUCIBLE_SLOTS;
        if (index < n || index >= barrelStart && index < barrelStart + BarrelTier.CRUCIBLE_SLOTS) {
            if (!moveItemStackTo(stack, inventoryStart, slots.size(), true)) return ItemStack.EMPTY;
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
        CrucibleSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

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
