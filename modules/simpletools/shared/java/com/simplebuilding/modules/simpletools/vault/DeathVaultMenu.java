package com.simplebuilding.modules.simpletools.vault;

import com.simplebuilding.modules.simpletools.SimpleTools;
import com.simplebuilding.modules.simpletools.ToolsRegistry;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Death Vault menu (chest style, four rows): 27 display fields show the player's lost stacks (redeemable first, newest
 * death first; the rest still lies in the world and is drawn dimmed), one offering slot and the button "Offer". Display
 * fields can never be taken from or put into; everything is decided on the server ({@link DeathVault#offer}).
 */
public class DeathVaultMenu extends AbstractContainerMenu {
    public static final int ROWS = 4, SHOWN = 27, WIDTH = 176, HEIGHT = 114 + ROWS * 18;
    public static final int OFFER_SLOT = SHOWN, INVENTORY_START = SHOWN + 1, INVENTORY_END = INVENTORY_START + 36;
    public static final int OFFER_X = 44, OFFER_Y = 18 + 3 * 18, BUTTON_X = 70, BUTTON_W = 98;
    public static final int BUTTON_OFFER = 0;
    /** Data: redeemable among the shown fields, all redeemable, all still lying, cost per item, returns per offering. */
    public static final int D_SHOWN_LOST = 0, D_LOST = 1, D_LYING = 2, D_COST = 3, D_MAX = 4;

    private final ContainerLevelAccess access;
    private final Player player;
    private final SimpleContainer shown = new SimpleContainer(SHOWN);
    private final SimpleContainer offering = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final ContainerData data;
    private int refreshTicks;

    /** Client side (menu type factory). */
    public DeathVaultMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public DeathVaultMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ToolsRegistry.DEATH_VAULT_MENU, id);
        this.access = access;
        this.player = inventory.player;
        this.data = new SimpleContainerData(5);
        for (int i = 0; i < SHOWN; i++) addSlot(new Shown(shown, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        addSlot(new Slot(offering, 0, OFFER_X, OFFER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(DeathVault.OFFERINGS);
            }
        });
        addStandardInventorySlots(inventory, 8, 18 + ROWS * 18 + 13);
        addDataSlots(data);
        refresh();
    }

    /** Display field: shows a lost stack, never moves an item. */
    static final class Shown extends Slot {
        Shown(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    /** Server: refills the display fields and numbers from the ledger. */
    public void refresh() {
        if (!(player instanceof ServerPlayer server)) return;
        DeathLedger ledger = DeathLedger.get(server.level().getServer());
        List<DeathVault.Lost> lost = DeathVault.lost(ledger, player.getUUID());
        List<ItemStack> lying = DeathVault.lying(ledger, player.getUUID());
        int i = 0;
        for (DeathVault.Lost l : lost) if (i < SHOWN) shown.setItem(i++, l.entry().stack.copy());
        int shownLost = i;
        for (ItemStack s : lying) if (i < SHOWN) shown.setItem(i++, s.copy());
        while (i < SHOWN) shown.setItem(i++, ItemStack.EMPTY);
        data.set(D_SHOWN_LOST, shownLost);
        data.set(D_LOST, Math.min(lost.size(), Short.MAX_VALUE));
        data.set(D_LYING, Math.min(lying.size(), Short.MAX_VALUE));
        data.set(D_COST, SimpleTools.config().deathVaultCostPerItem);
        data.set(D_MAX, SimpleTools.config().deathVaultMaxPerOffering);
    }

    public int data(int index) {
        return data.get(index);
    }

    public ItemStack offering() {
        return offering.getItem(0);
    }

    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        if (id != BUTTON_OFFER || !(clicker instanceof ServerPlayer server) || clicker != player || !DeathVault.active()) return false;
        DeathLedger ledger = DeathLedger.get(server.level().getServer());
        ItemStack paid = offering.getItem(0);
        List<ItemStack> back = DeathVault.offer(ledger, player.getUUID(), paid, server.getRandom());
        if (back.isEmpty()) return false;
        offering.setItem(0, paid.isEmpty() ? ItemStack.EMPTY : paid);
        for (ItemStack stack : back) player.getInventory().placeItemBackInInventory(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        access.execute((level, pos) -> {
            level.playSound(null, pos, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 1.0F, 0.8F);
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.25, 0.2, 0.25, 0.02);
            }
        });
        refresh();
        broadcastChanges();
        return true;
    }

    @Override
    public void broadcastChanges() {
        if (++refreshTicks >= 20) {
            refreshTicks = 0;
            refresh();
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player who, int index) {
        if (index < SHOWN) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        if (index == OFFER_SLOT) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) return ItemStack.EMPTY;
        } else if (!stack.is(DeathVault.OFFERINGS) || !moveItemStackTo(stack, OFFER_SLOT, OFFER_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == before.getCount()) return ItemStack.EMPTY;
        slot.onTake(who, stack);
        return before;
    }

    @Override
    public void removed(Player who) {
        super.removed(who);
        access.execute((level, pos) -> clearContainer(who, offering));
    }

    @Override
    public boolean stillValid(Player who) {
        return stillValid(access, who, ToolsRegistry.DEATH_VAULT);
    }
}
