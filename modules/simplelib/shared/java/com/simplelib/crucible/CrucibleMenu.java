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
    public static final int GRID_GAP = 4;
    public static final int THERMO_WIDTH = 12;
    public static final int GRID_LEFT = 8 + THERMO_WIDTH + 6;
    public static final int GRID_TOP = 18;
    public static final int FIRE_HEIGHT = 12;

    private final CrucibleTier tier;
    private final Container container;
    private final ContainerData data;
    private final @Nullable CrucibleBlockEntity crucible;

    /** Client constructor (one menu type per tier). */
    public static CrucibleMenu client(CrucibleTier tier, int id, Inventory inventory) {
        return new CrucibleMenu(tier, id, inventory, new SimpleContainer(tier.slots()), new SimpleContainer(tier.slots()),
                new SimpleContainerData(tier.slots() + 3), null);
    }

    public CrucibleMenu(int id, Inventory inventory, CrucibleBlockEntity crucible) {
        this(crucible.tier(), id, inventory, crucible, new GhostView(crucible), crucible.data(), crucible);
    }

    private CrucibleMenu(CrucibleTier tier, int id, Inventory inventory, Container container, Container ghosts,
                         ContainerData data, @Nullable CrucibleBlockEntity crucible) {
        super(LibMenus.forTier(tier), id);
        this.tier = tier;
        this.container = container;
        this.data = data;
        this.crucible = crucible;
        checkContainerSize(container, tier.slots());
        container.startOpen(inventory.player);
        for (int i = 0; i < tier.slots(); i++) addSlot(new CrucibleSlot(container, i, slotX(tier, i), slotY(tier, i)));
        for (int i = 0; i < tier.slots(); i++) addSlot(new GhostSlot(ghosts, i, slotX(tier, i), slotY(tier, i)));
        addStandardInventorySlots(inventory, inventoryLeft(tier), inventoryTop(tier));
        addDataSlots(data);
    }

    public CrucibleTier tier() { return tier; }

    public static int gridsWidth(CrucibleTier tier) {
        return tier.grids() * 54 + (tier.grids() - 1) * GRID_GAP;
    }

    public static int imageWidth(CrucibleTier tier) {
        return Math.max(176, GRID_LEFT + gridsWidth(tier) + 8);
    }

    public static int imageHeight(CrucibleTier tier) {
        return GRID_TOP + tier.rows() * 18 + FIRE_HEIGHT + 14 + 76 + 8;
    }

    public static int inventoryLeft(CrucibleTier tier) {
        return (imageWidth(tier) - 162) / 2 + 1;
    }

    public static int inventoryTop(CrucibleTier tier) {
        return imageHeight(tier) - 82;
    }

    public static int slotX(CrucibleTier tier, int slot) {
        return GRID_LEFT + 1 + tier.grid(slot) * (54 + GRID_GAP) + tier.column(slot) * 18;
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
        int inventoryStart = 2 * n;
        if (index < n) {
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

    /** Hidden slot that only syncs the reserved result to the client screen. */
    private static final class GhostSlot extends Slot {
        GhostSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean isActive() { return false; }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
    }

    /** Read-only view of the crucible's reserved results. */
    private static final class GhostView extends SimpleContainer {
        private final CrucibleBlockEntity crucible;

        GhostView(CrucibleBlockEntity crucible) {
            super(crucible.tier().slots());
            this.crucible = crucible;
        }

        @Override
        public ItemStack getItem(int slot) {
            return crucible.ghost(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) { return ItemStack.EMPTY; }
        @Override
        public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
        @Override
        public void setItem(int slot, ItemStack stack) {}
    }
}
