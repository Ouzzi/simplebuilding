package com.simpleshops.block;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.logic.EnderShops;
import com.simpleshops.logic.ShopStorage;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Extra earnings store of a shop (chest size). Not a container for the world (see {@link ListContainer}): no hopper, minecart or
 * mod pipe sees it. It only pulls from the block it faces (its sales block, SP22-7), and only when that block has the same owner. The 27 slots live
 * in the world data ({@link EnderShops}) since the shops rework; old storages move their items over on first use.
 */
public class SalesStorageBlockEntity extends BlockEntity implements EarningsStore {
    /** Pull interval in ticks (like a hopper's 8). */
    public static final int PERIOD = 8;
    private @Nullable UUID owner;
    private String ownerName = "";
    private final NonNullList<ItemStack> earnings = NonNullList.withSize(EARNINGS_SIZE, ItemStack.EMPTY);
    private final VaultLink link = new VaultLink();
    /** Placed (or checked once) since SP22-7; storages of older worlds turn to face up on their first tick ({@link ShopStorage#aimLegacy}). */
    private boolean aimed;

    public SalesStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ShopsRegistry.STORAGE_BE, pos, state);
    }

    public EnderShops.@Nullable Vault vault() {
        return this.link.resolve(this, EnderShops.Kind.STORAGE, v -> {
            v.setOwner(this.owner, this.ownerName);
            for (int i = 0; i < EARNINGS_SIZE; i++) {
                v.items.set(i, this.earnings.get(i));
                this.earnings.set(i, ItemStack.EMPTY);
            }
        });
    }

    public EnderShops.@Nullable Vault peekVault() {
        return this.link.peek(this, EnderShops.Kind.STORAGE);
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

    /** Placement: owner from the placing player (never from item data), empty (a new world entry). */
    public void initFor(ServerPlayer placer) {
        this.link.reset(this, EnderShops.Kind.STORAGE);
        this.owner = placer.getUUID();
        this.ownerName = placer.getGameProfile().name();
        this.aimed = true;
        for (int i = 0; i < EARNINGS_SIZE; i++) this.earnings.set(i, ItemStack.EMPTY);
        vault();
        changed();
    }

    @Override
    public NonNullList<ItemStack> earnings() {
        EnderShops.Vault v = vault();
        return v != null ? v.items : this.earnings;
    }

    /** Tests: this storage as saved before SP22-7 (no facing check yet). */
    public void forgetAim() {
        this.aimed = false;
    }

    public ListContainer earningsView() {
        return new ListContainer(earnings(), this::changed);
    }

    @Override
    public void changed() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) vault();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SalesStorageBlockEntity be) {
        if (level.isClientSide()) return;
        if (!be.link.linked()) be.vault();
        if (!be.aimed) {
            be.aimed = true;
            ShopStorage.aimLegacy(be);
            be.setChanged();
        }
        if (level.getGameTime() % PERIOD != 0) return;
        ShopStorage.pull(be);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        this.ownerName = input.getStringOr("owner_name", "");
        this.link.read(input);
        this.aimed = input.getBooleanOr("aimed", false);
        for (int i = 0; i < EARNINGS_SIZE; i++) this.earnings.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input.childOrEmpty("earnings"), this.earnings);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.owner != null) output.store("owner", UUIDUtil.CODEC, this.owner);
        output.putString("owner_name", this.ownerName);
        this.link.write(output);
        if (this.aimed) output.putBoolean("aimed", true);
        if (!this.link.linked()) ContainerHelper.saveAllItems(output.child("earnings"), this.earnings, false);
    }

    /** Client and item data: the owner only (the earnings stay on the server). */
    @Override
    public void saveCustomOnly(ValueOutput output) {
        if (this.owner != null) output.store("owner", UUIDUtil.CODEC, this.owner);
        output.putString("owner_name", this.ownerName);
    }

    /** Server: the contents drop exactly once when the block goes away. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) {
            // The entry even of a block entity already marked removed (a chunk unload in between): its items must drop, not vanish.
            EnderShops.Vault v = this.link.peek(this, EnderShops.Kind.STORAGE);
            if (v == null) v = vault();
            Containers.dropContents(this.level, pos, v != null ? v.items : this.earnings);
            if (v != null) for (int i = 0; i < EARNINGS_SIZE; i++) v.items.set(i, ItemStack.EMPTY);
            this.link.drop(this, EnderShops.Kind.STORAGE);
            for (int i = 0; i < EARNINGS_SIZE; i++) this.earnings.set(i, ItemStack.EMPTY);
        }
    }
}
