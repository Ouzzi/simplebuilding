package com.simpleshops.block;

import com.simpleshops.ShopsRegistry;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** The goods of a shop: owner and up to five stacks, shown by the renderer. Not a container (see {@link ListContainer}). */
public class DisplayCaseBlockEntity extends BlockEntity {
    public static final int SIZE = 5;
    private @Nullable UUID owner;
    private String ownerName = "";
    private final NonNullList<ItemStack> goods = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public DisplayCaseBlockEntity(BlockPos pos, BlockState state) {
        super(ShopsRegistry.CASE_BE, pos, state);
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
        this.owner = placer.getUUID();
        this.ownerName = placer.getGameProfile().name();
        for (int i = 0; i < SIZE; i++) this.goods.set(i, ItemStack.EMPTY);
        changed();
    }

    public NonNullList<ItemStack> goods() {
        return this.goods;
    }

    public ListContainer goodsView() {
        return new ListContainer(this.goods, this::changed);
    }

    public int stocked() {
        int n = 0;
        for (ItemStack s : this.goods) if (!s.isEmpty()) n++;
        return n;
    }

    /** First fresh slot index or {@code -1}. */
    public int freeSlot() {
        for (int i = 0; i < SIZE; i++) if (this.goods.get(i).isEmpty()) return i;
        return -1;
    }

    public void changed() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        this.ownerName = input.getStringOr("owner_name", "");
        for (int i = 0; i < SIZE; i++) this.goods.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.goods);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.owner != null) output.store("owner", UUIDUtil.CODEC, this.owner);
        output.putString("owner_name", this.ownerName);
        ContainerHelper.saveAllItems(output, this.goods, false);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Server: the goods drop exactly once when the case goes away. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) {
            Containers.dropContents(this.level, pos, this.goods);
            for (int i = 0; i < SIZE; i++) this.goods.set(i, ItemStack.EMPTY);
            com.simpleshops.logic.ShopLedger.forget(this.level, pos);
        }
    }
}
