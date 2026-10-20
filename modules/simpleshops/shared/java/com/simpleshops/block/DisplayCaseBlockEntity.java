package com.simpleshops.block;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.logic.EnderShops;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
 * The goods of a shop: owner and up to five stacks, shown by the renderer. Not a container (see {@link ListContainer}).
 * Since the shops rework the goods live in the world data ({@link EnderShops}); the block entity is the window onto them and
 * keeps its own list only as the client view and for cases saved before the rework (moved over on first use).
 */
public class DisplayCaseBlockEntity extends BlockEntity {
    public static final int SIZE = 5;
    private @Nullable UUID owner;
    private String ownerName = "";
    private final NonNullList<ItemStack> goods = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    /** Server (unlimited) shop: the goods are templates that are copied per purchase, never dropped. */
    private boolean server;
    private final VaultLink link = new VaultLink();

    public DisplayCaseBlockEntity(BlockPos pos, BlockState state) {
        super(ShopsRegistry.CASE_BE, pos, state);
    }

    /** The world entry of this case (server side; created on first use, old goods move in). {@code null} on the client. */
    public EnderShops.@Nullable Vault vault() {
        return this.link.resolve(this, EnderShops.Kind.CASE, v -> {
            v.setOwner(this.owner, this.ownerName);
            v.setServerId(this.server ? "server" : "");
            for (int i = 0; i < SIZE; i++) {
                v.items.set(i, this.goods.get(i));
                this.goods.set(i, ItemStack.EMPTY);
            }
        });
    }

    public EnderShops.@Nullable Vault peekVault() {
        return this.link.peek(this, EnderShops.Kind.CASE);
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

    public void initFor(ServerPlayer placer) {
        this.link.reset(this, EnderShops.Kind.CASE);
        this.owner = placer.getUUID();
        this.ownerName = placer.getGameProfile().name();
        this.server = false;
        for (int i = 0; i < SIZE; i++) this.goods.set(i, ItemStack.EMPTY);
        vault();
        linkBelow();
        changed();
    }

    public boolean isServerShop() {
        return this.server;
    }

    public boolean mayEdit(net.minecraft.world.entity.player.Player player) {
        return isOwner(player.getUUID()) || (this.server && com.simpleshops.logic.ShopGuard.isAdmin(player));
    }

    public void initServer() {
        this.link.reset(this, EnderShops.Kind.CASE);
        this.owner = null;
        this.ownerName = "Server";
        this.server = true;
        for (int i = 0; i < SIZE; i++) this.goods.set(i, ItemStack.EMPTY);
        vault();
        linkBelow();
        changed();
    }

    private void linkBelow() {
        if (this.level != null && this.level.getBlockEntity(this.worldPosition.below()) instanceof SalesBlockEntity sales) EnderShops.link(sales);
    }

    public NonNullList<ItemStack> goods() {
        EnderShops.Vault v = vault();
        return v != null ? v.items : this.goods;
    }

    public ListContainer goodsView() {
        return new ListContainer(goods(), this::changed);
    }

    public int stocked() {
        int n = 0;
        for (ItemStack s : goods()) if (!s.isEmpty()) n++;
        return n;
    }

    /** First fresh slot index or {@code -1}. */
    public int freeSlot() {
        NonNullList<ItemStack> list = goods();
        for (int i = 0; i < SIZE; i++) if (list.get(i).isEmpty()) return i;
        return -1;
    }

    public void changed() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            vault();
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    /** Server tick: moves an old case's goods into the world data at once. */
    public static void tick(Level level, BlockPos pos, BlockState state, DisplayCaseBlockEntity be) {
        if (!level.isClientSide() && !be.link.linked()) be.vault();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        this.ownerName = input.getStringOr("owner_name", "");
        this.server = input.getBooleanOr("server", false);
        this.link.read(input);
        // Client view, or the goods of a case saved before the rework (they move into the world data on first use).
        for (int i = 0; i < SIZE; i++) this.goods.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.goods);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.owner != null) output.store("owner", UUIDUtil.CODEC, this.owner);
        output.putString("owner_name", this.ownerName);
        if (this.server) output.putBoolean("server", true);
        this.link.write(output);
        // Not moved yet: the case keeps its goods itself (nothing is lost).
        if (!this.link.linked()) ContainerHelper.saveAllItems(output, this.goods, false);
    }

    /** The client part: owner and the goods to draw, never the world entry id. */
    @Override
    public void saveCustomOnly(ValueOutput output) {
        if (this.owner != null) output.store("owner", UUIDUtil.CODEC, this.owner);
        output.putString("owner_name", this.ownerName);
        if (this.server) output.putBoolean("server", true);
        ContainerHelper.saveAllItems(output, goods(), false);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Server: the goods (and goods on their way to a trade terminal) drop exactly once when the case goes away. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) {
            // The entry even of a block entity already marked removed (a chunk unload in between): its items must drop, not vanish.
            EnderShops.Vault v = this.link.peek(this, EnderShops.Kind.CASE);
            if (v == null) v = vault();
            if (v != null) {
                var all = EnderShops.takeAll(v);
                if (!this.server) for (ItemStack s : all) Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), s);
            } else if (!this.server) {
                Containers.dropContents(this.level, pos, this.goods);
            }
            this.link.drop(this, EnderShops.Kind.CASE);
            for (int i = 0; i < SIZE; i++) this.goods.set(i, ItemStack.EMPTY);
            com.simpleshops.logic.ShopLedger.forget(this.level, pos);
        }
    }
}
