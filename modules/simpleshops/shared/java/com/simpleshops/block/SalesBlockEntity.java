package com.simpleshops.block;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.logic.PriceRow;
import com.simpleshops.logic.ShopGuard;
import java.lang.ref.WeakReference;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.ContainerHelper;

/**
 * The cash register of a shop: owner, the price row (templates, see {@link PriceRow}) and the nine-slot earnings store.
 * Not a container (see {@link ListContainer}). Clients get the owner, the price templates and nothing else; the earnings
 * never leave the server. The one-player lock is runtime only and never saved.
 */
public class SalesBlockEntity extends BlockEntity {
    public static final int EARNINGS_SIZE = 9;
    private @Nullable UUID owner;
    private String ownerName = "";
    private final NonNullList<ItemStack> prices = PriceRow.empty();
    private final NonNullList<ItemStack> earnings = NonNullList.withSize(EARNINGS_SIZE, ItemStack.EMPTY);
    private int revision;
    /** Id of the server shop ({@code s001} ...) or empty for a player shop; set only by the operator command, saved server side only. */
    private String serverId = "";
    private @Nullable WeakReference<ServerPlayer> lockPlayer;
    private @Nullable WeakReference<AbstractContainerMenu> lockMenu;

    public SalesBlockEntity(BlockPos pos, BlockState state) {
        super(ShopsRegistry.SALES_BE, pos, state);
    }

    public @Nullable UUID owner() {
        return this.owner;
    }

    public String ownerName() {
        return this.ownerName;
    }

    public boolean isOwner(UUID player) {
        return this.owner != null && this.owner.equals(player);
    }

    /** Server (unlimited) shop: no owner, the id from {@link com.simpleshops.logic.ServerShops}. */
    public boolean isServerShop() {
        return !this.serverId.isEmpty();
    }

    public String serverId() {
        return this.serverId;
    }

    /** Who edits prices here: the owner, or an operator for a server shop (the edit command opens the menu). */
    public boolean mayEdit(net.minecraft.world.entity.player.Player player) {
        return isOwner(player.getUUID()) || (isServerShop() && ShopGuard.isAdmin(player));
    }

    /** Turns this block into a server shop: no owner, empty prices and earnings, never saved to clients. */
    public void initServer(String id) {
        this.owner = null;
        this.ownerName = "Server";
        this.serverId = id;
        for (int i = 0; i < PriceRow.SIZE; i++) this.prices.set(i, ItemStack.EMPTY);
        for (int i = 0; i < EARNINGS_SIZE; i++) this.earnings.set(i, ItemStack.EMPTY);
        this.revision = 0;
        changed();
    }

    /** Placement: owner from the placing player (never from item data), everything else cleared. */
    public void initFor(ServerPlayer placer) {
        this.owner = placer.getUUID();
        this.ownerName = placer.getGameProfile().name();
        for (int i = 0; i < PriceRow.SIZE; i++) this.prices.set(i, ItemStack.EMPTY);
        for (int i = 0; i < EARNINGS_SIZE; i++) this.earnings.set(i, ItemStack.EMPTY);
        this.revision = 0;
        changed();
    }

    public NonNullList<ItemStack> prices() {
        return this.prices;
    }

    public NonNullList<ItemStack> earnings() {
        return this.earnings;
    }

    public ListContainer earningsView() {
        return new ListContainer(this.earnings, this::changed);
    }

    public int revision() {
        return this.revision;
    }

    public void bumpRevision() {
        this.revision++;
    }

    /** At least one price, so the case above can sell. */
    public boolean hasPrice() {
        return PriceRow.filled(this.prices) > 0;
    }

    /** Marks the data dirty and pushes the client-visible part (owner, prices) to nearby players. */
    public void changed() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    // --- one-player lock (runtime only) -------------------------------------------------------------------------

    /** The player who currently holds the shop, or {@code null} when it is free; a stale lock is dropped here. */
    public @Nullable ServerPlayer lockHolder() {
        ServerPlayer p = this.lockPlayer == null ? null : this.lockPlayer.get();
        AbstractContainerMenu menu = this.lockMenu == null ? null : this.lockMenu.get();
        if (p == null || menu == null) {
            this.lockPlayer = null;
            this.lockMenu = null;
            return null;
        }
        boolean live = !isRemoved() && this.level != null && !p.isRemoved() && p.isAlive() && p.level() == this.level
                && p.containerMenu == menu && p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(this.worldPosition)) <= 64.0 + 4.0;
        if (!live) {
            this.lockPlayer = null;
            this.lockMenu = null;
            return null;
        }
        return p;
    }

    public boolean lockFree() {
        return lockHolder() == null;
    }

    public void lock(ServerPlayer player, AbstractContainerMenu menu) {
        this.lockPlayer = new WeakReference<>(player);
        this.lockMenu = new WeakReference<>(menu);
    }

    /** Releases the lock if {@code menu} holds it. */
    public void unlock(AbstractContainerMenu menu) {
        if (this.lockMenu != null && this.lockMenu.get() == menu) {
            this.lockPlayer = null;
            this.lockMenu = null;
        }
    }

    // --- persistence ---------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        this.ownerName = input.getStringOr("owner_name", "");
        this.serverId = input.getStringOr("server_id", "");
        for (int i = 0; i < PriceRow.SIZE; i++) this.prices.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input.childOrEmpty("prices"), this.prices);
        // A damaged row (hand-edited data) would break the run rule: empty prices are safe (no sale).
        if (!PriceRow.valid(this.prices)) for (int i = 0; i < PriceRow.SIZE; i++) this.prices.set(i, ItemStack.EMPTY);
        for (int i = 0; i < EARNINGS_SIZE; i++) this.earnings.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input.childOrEmpty("earnings"), this.earnings);
    }

    private void savePublic(ValueOutput output) {
        if (this.owner != null) output.store("owner", UUIDUtil.CODEC, this.owner);
        output.putString("owner_name", this.ownerName);
        ContainerHelper.saveAllItems(output.child("prices"), this.prices, false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        savePublic(output);
        if (isServerShop()) output.putString("server_id", this.serverId);
        ContainerHelper.saveAllItems(output.child("earnings"), this.earnings, false);
    }

    /** The client part only: no earnings. */
    @Override
    public void saveCustomOnly(ValueOutput output) {
        savePublic(output);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Server: earnings drop exactly once when the block goes away; price templates are never dropped. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) {
            // A server shop holds nothing real (its earnings are a sink): nothing drops.
            if (!isServerShop()) Containers.dropContents(this.level, pos, this.earnings);
            for (int i = 0; i < EARNINGS_SIZE; i++) this.earnings.set(i, ItemStack.EMPTY);
            com.simpleshops.logic.ShopLedger.forget(this.level, pos);
            if (isServerShop()) com.simpleshops.logic.ServerShops.forgetAt(this.level, pos);
        }
    }
}
