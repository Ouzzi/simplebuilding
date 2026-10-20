package com.simpleshops.block;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.logic.EnderShops;
import com.simpleshops.logic.PriceRow;
import com.simpleshops.logic.ShopGuard;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.ContainerHelper;

/**
 * The cash register of a shop: owner, the price row (templates, see {@link PriceRow}) and the 27-slot earnings store.
 * Not a container (see {@link ListContainer}). Since the shops rework (KONZ20 S1 A) the price row, the earnings, the revision
 * and the one-player lock live in the world data ({@link EnderShops}); this block entity is the window onto that entry and
 * keeps its own lists only as the client view (prices) and for blocks saved before the rework (moved over on first use).
 * Clients get the owner, the price templates and nothing else; the earnings never leave the server.
 */
public class SalesBlockEntity extends BlockEntity implements EarningsStore {
    public static final int EARNINGS_SIZE = EarningsStore.EARNINGS_SIZE;
    private @Nullable UUID owner;
    private String ownerName = "";
    private final NonNullList<ItemStack> prices = PriceRow.empty();
    private final NonNullList<ItemStack> earnings = NonNullList.withSize(EARNINGS_SIZE, ItemStack.EMPTY);
    /** Revision while no world entry exists (client, removed block entity). */
    private int revision;
    /** Id of the server shop ({@code s001} ...) or empty for a player shop; set only by the operator command, saved server side only. */
    private String serverId = "";
    private final VaultLink link = new VaultLink();

    public SalesBlockEntity(BlockPos pos, BlockState state) {
        super(ShopsRegistry.SALES_BE, pos, state);
    }

    /** The world entry of this shop (server side; created on first use, old contents move in). {@code null} on the client. */
    public EnderShops.@Nullable Vault vault() {
        return this.link.resolve(this, EnderShops.Kind.SALES, v -> {
            v.setOwner(this.owner, this.ownerName);
            v.setServerId(this.serverId);
            if (PriceRow.valid(this.prices)) for (int i = 0; i < PriceRow.SIZE; i++) v.prices.set(i, this.prices.get(i));
            for (int i = 0; i < EARNINGS_SIZE; i++) v.items.set(i, this.earnings.get(i));
            for (int i = 0; i < PriceRow.SIZE; i++) this.prices.set(i, ItemStack.EMPTY);
            for (int i = 0; i < EARNINGS_SIZE; i++) this.earnings.set(i, ItemStack.EMPTY);
        });
    }

    /** The entry if it exists, without creating one (orphan check). */
    public EnderShops.@Nullable Vault peekVault() {
        return this.link.peek(this, EnderShops.Kind.SALES);
    }

    @Override
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
        this.link.reset(this, EnderShops.Kind.SALES);
        this.owner = null;
        this.ownerName = "Server";
        this.serverId = id;
        clearLocal();
        vault();
        changed();
    }

    /** Placement: owner from the placing player (never from item data), everything else cleared (a new world entry). */
    public void initFor(ServerPlayer placer) {
        this.link.reset(this, EnderShops.Kind.SALES);
        this.owner = placer.getUUID();
        this.ownerName = placer.getGameProfile().name();
        this.serverId = "";
        clearLocal();
        vault();
        changed();
    }

    private void clearLocal() {
        for (int i = 0; i < PriceRow.SIZE; i++) this.prices.set(i, ItemStack.EMPTY);
        for (int i = 0; i < EARNINGS_SIZE; i++) this.earnings.set(i, ItemStack.EMPTY);
        this.revision = 0;
    }

    public NonNullList<ItemStack> prices() {
        EnderShops.Vault v = vault();
        return v != null ? v.prices : this.prices;
    }

    @Override
    public NonNullList<ItemStack> earnings() {
        EnderShops.Vault v = vault();
        return v != null ? v.items : this.earnings;
    }

    public ListContainer earningsView() {
        return new ListContainer(earnings(), this::changed);
    }

    public int revision() {
        EnderShops.Vault v = vault();
        return v != null ? v.revision() : this.revision;
    }

    public void bumpRevision() {
        EnderShops.Vault v = vault();
        if (v != null) v.bumpRevision();
        else this.revision++;
    }

    /** At least one price, so the case above can sell. */
    public boolean hasPrice() {
        return PriceRow.filled(prices()) > 0;
    }

    /** Marks the data dirty and pushes the client-visible part (owner, prices) to nearby players. */
    @Override
    public void changed() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            vault();
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    /** Server tick: moves an old block's contents into the world data at once and keeps the links for the trade terminal fresh. */
    public static void tick(Level level, BlockPos pos, BlockState state, SalesBlockEntity be) {
        if (level.isClientSide()) return;
        if (!be.link.linked()) be.vault();
        if (Math.floorMod(level.getGameTime() + pos.asLong(), 40L) == 0) EnderShops.link(be);
    }

    // --- one-player lock (runtime only, in the world entry: the same lock on site and from a trade terminal) ------

    /** The player who currently holds the shop, or {@code null} when it is free; a stale lock is dropped here. */
    public @Nullable ServerPlayer lockHolder() {
        EnderShops.Vault v = vault();
        return v == null ? null : v.lockHolder();
    }

    public boolean lockFree() {
        return lockHolder() == null;
    }

    public void lock(ServerPlayer player, AbstractContainerMenu menu) {
        EnderShops.Vault v = vault();
        if (v != null) v.lock(player, menu, false);
    }

    /** Releases the lock if {@code menu} holds it. */
    public void unlock(AbstractContainerMenu menu) {
        EnderShops.Vault v = vault();
        if (v != null) v.unlock(menu);
    }

    // --- persistence ---------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        this.ownerName = input.getStringOr("owner_name", "");
        this.serverId = input.getStringOr("server_id", "");
        this.link.read(input);
        for (int i = 0; i < PriceRow.SIZE; i++) this.prices.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input.childOrEmpty("prices"), this.prices);
        // A damaged row (hand-edited data) would break the run rule: empty prices are safe (no sale).
        if (!PriceRow.valid(this.prices)) for (int i = 0; i < PriceRow.SIZE; i++) this.prices.set(i, ItemStack.EMPTY);
        // Old blocks (before the rework) and nothing else carry earnings: they move into the world data on first use.
        for (int i = 0; i < EARNINGS_SIZE; i++) this.earnings.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input.childOrEmpty("earnings"), this.earnings);
    }

    private void savePublic(ValueOutput output) {
        if (this.owner != null) output.store("owner", UUIDUtil.CODEC, this.owner);
        output.putString("owner_name", this.ownerName);
        ContainerHelper.saveAllItems(output.child("prices"), prices(), false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.owner != null) output.store("owner", UUIDUtil.CODEC, this.owner);
        output.putString("owner_name", this.ownerName);
        if (isServerShop()) output.putString("server_id", this.serverId);
        this.link.write(output);
        // Not moved yet (no world entry): the block keeps its own items, nothing is lost.
        if (!this.link.linked()) {
            ContainerHelper.saveAllItems(output.child("prices"), this.prices, false);
            ContainerHelper.saveAllItems(output.child("earnings"), this.earnings, false);
        }
    }

    /** The client part only: no earnings, no world entry id. */
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

    /** Server: earnings drop exactly once when the block goes away and the world entry is deleted; price templates are never dropped. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) {
            // The entry even of a block entity already marked removed (a chunk unload in between): its items must drop, not vanish.
            EnderShops.Vault v = this.link.peek(this, EnderShops.Kind.SALES);
            if (v == null) v = vault();
            // A server shop holds nothing real (its earnings are a sink): nothing drops.
            if (!isServerShop()) Containers.dropContents(this.level, pos, v != null ? v.items : this.earnings);
            if (v != null) for (int i = 0; i < EARNINGS_SIZE; i++) v.items.set(i, ItemStack.EMPTY);
            this.link.drop(this, EnderShops.Kind.SALES);
            clearLocal();
            com.simpleshops.logic.ShopLedger.forget(this.level, pos);
            if (isServerShop()) com.simpleshops.logic.ServerShops.forgetAt(this.level, pos);
        }
    }
}
