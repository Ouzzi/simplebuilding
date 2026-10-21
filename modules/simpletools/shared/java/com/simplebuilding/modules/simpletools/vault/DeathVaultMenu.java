package com.simplebuilding.modules.simpletools.vault;

import com.simplebuilding.modules.simpletools.SimpleTools;
import com.simplebuilding.modules.simpletools.ToolsRegistry;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.item.component.ItemLore;

/**
 * Death Vault menu (Vanilla large chest layout): 45 display fields show what became of the drops of the player's last
 * death (one fate per field, see {@link Fate}; still lying drops first), the band below holds the future offering slot
 * and button (hidden unless the redeem option is on), the inventory below that. Display fields can never be taken from or
 * put into. The server sends per field the fate, the despawn age and whether the timer runs through {@link ContainerData}
 * (the name of a picking player rides in the tooltip lore); everything is decided on the server.
 */
public class DeathVaultMenu extends AbstractContainerMenu {
    public static final int COLS = 9, GRID_ROWS = 5, SHOWN = COLS * GRID_ROWS, ROWS = GRID_ROWS + 1, WIDTH = 176, HEIGHT = 114 + ROWS * 18;
    public static final int OFFER_SLOT = SHOWN, INVENTORY_START = SHOWN + 1, INVENTORY_END = INVENTORY_START + 36;
    public static final int GRID_X = 8, GRID_Y = 18, BAND_Y = GRID_Y + GRID_ROWS * 18;
    public static final int OFFER_X = 44, OFFER_Y = BAND_Y, BUTTON_X = 70, BUTTON_W = 98;
    public static final int BUTTON_OFFER = 0;
    /**
     * Data: redeem option on (offering shown), redeemable count, cost per item, returns per offering, shown fields, further
     * drops not shown, minutes since the death (-1 none); then per field {@link #D_ROWS} + 3 * i: flags, age in ticks, despawn age (lifespan) in ticks.
     */
    public static final int D_OFFER = 0, D_LOST = 1, D_COST = 2, D_MAX = 3, D_COUNT = 4, D_MORE = 5, D_SINCE = 6, D_ROWS = 7;

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
        this.data = new SimpleContainerData(D_ROWS + 3 * SHOWN);
        for (int i = 0; i < SHOWN; i++) addSlot(new Shown(shown, i, GRID_X + (i % COLS) * 18, GRID_Y + (i / COLS) * 18, () -> data.get(D_COUNT)));
        addSlot(new Slot(offering, 0, OFFER_X, OFFER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return offerEnabled() && stack.is(DeathVault.OFFERINGS);
            }

            @Override
            public boolean isActive() {
                return offerEnabled();
            }
        });
        addStandardInventorySlots(inventory, 8, 18 + ROWS * 18 + 13);
        addDataSlots(data);
        refresh();
    }

    /** Display field: shows a drop of the last death, never moves an item; fields beyond the shown count are inactive (not drawn, not hoverable). */
    static final class Shown extends Slot {
        private final java.util.function.IntSupplier count;

        Shown(Container container, int index, int x, int y, java.util.function.IntSupplier count) {
            super(container, index, x, y);
            this.count = count;
        }

        @Override
        public boolean isActive() {
            return getContainerSlot() < count.getAsInt();
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

    public boolean offerEnabled() {
        return data.get(D_OFFER) != 0;
    }

    /** Server: refills the display fields and numbers from the ledger. */
    public void refresh() {
        if (!(player instanceof ServerPlayer server)) return;
        var minecraft = server.level().getServer();
        DeathLedger ledger = DeathLedger.get(minecraft);
        List<DeathVault.Row> rows = DeathVault.rows(minecraft, player.getUUID());
        int n = Math.min(rows.size(), SHOWN);
        for (int i = 0; i < SHOWN; i++) {
            if (i >= n) {
                shown.setItem(i, ItemStack.EMPTY);
                data.set(D_ROWS + 3 * i, 0);
                data.set(D_ROWS + 3 * i + 1, 0);
                data.set(D_ROWS + 3 * i + 2, DeathLedger.LIFETIME);
                continue;
            }
            DeathVault.Row row = rows.get(i);
            ItemStack copy = row.entry().stack.copy();
            copy.set(DataComponents.LORE, copy.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).withLineAdded(row.fate().line(row.entry().who)));
            shown.setItem(i, copy);
            data.set(D_ROWS + 3 * i, row.flags());
            data.set(D_ROWS + 3 * i + 1, row.age());
            data.set(D_ROWS + 3 * i + 2, row.lifespan());
        }
        long since = -1;
        var last = ledger.last(player.getUUID());
        if (last.isPresent()) since = Math.min(Short.MAX_VALUE, Math.max(0, (minecraft.overworld().getGameTime() - last.get().time) / 1200));
        data.set(D_OFFER, DeathVault.redeemActive() ? 1 : 0);
        data.set(D_LOST, Math.min(DeathVault.lost(ledger, player.getUUID()).size(), Short.MAX_VALUE));
        data.set(D_COST, SimpleTools.config().deathVaultCostPerItem);
        data.set(D_MAX, SimpleTools.config().deathVaultMaxPerOffering);
        data.set(D_COUNT, n);
        data.set(D_MORE, Math.min(rows.size() - n, Short.MAX_VALUE));
        data.set(D_SINCE, (int) since);
    }

    public int data(int index) {
        return data.get(index);
    }

    public ItemStack offering() {
        return offering.getItem(0);
    }

    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        if (id != BUTTON_OFFER || !(clicker instanceof ServerPlayer server) || clicker != player || !DeathVault.redeemActive()) return false;
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
        } else if (!offerEnabled() || !stack.is(DeathVault.OFFERINGS) || !moveItemStackTo(stack, OFFER_SLOT, OFFER_SLOT + 1, false)) {
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
