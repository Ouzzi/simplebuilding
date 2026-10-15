package com.simpleshops.block;

import com.simpleshops.ShopsRegistry;
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
 * mod pipe sees it. It only pulls from the block directly above, and only when that block has the same owner.
 */
public class SalesStorageBlockEntity extends BlockEntity implements EarningsStore {
    /** Pull interval in ticks (like a hopper's 8). */
    public static final int PERIOD = 8;
    private @Nullable UUID owner;
    private String ownerName = "";
    private final NonNullList<ItemStack> earnings = NonNullList.withSize(EARNINGS_SIZE, ItemStack.EMPTY);

    public SalesStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ShopsRegistry.STORAGE_BE, pos, state);
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

    /** Placement: owner from the placing player (never from item data), empty. */
    public void initFor(ServerPlayer placer) {
        this.owner = placer.getUUID();
        this.ownerName = placer.getGameProfile().name();
        for (int i = 0; i < EARNINGS_SIZE; i++) this.earnings.set(i, ItemStack.EMPTY);
        changed();
    }

    @Override
    public NonNullList<ItemStack> earnings() {
        return this.earnings;
    }

    public ListContainer earningsView() {
        return new ListContainer(this.earnings, this::changed);
    }

    @Override
    public void changed() {
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SalesStorageBlockEntity be) {
        if (level.isClientSide() || level.getGameTime() % PERIOD != 0) return;
        ShopStorage.pull(be);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        this.ownerName = input.getStringOr("owner_name", "");
        for (int i = 0; i < EARNINGS_SIZE; i++) this.earnings.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input.childOrEmpty("earnings"), this.earnings);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.owner != null) output.store("owner", UUIDUtil.CODEC, this.owner);
        output.putString("owner_name", this.ownerName);
        ContainerHelper.saveAllItems(output.child("earnings"), this.earnings, false);
    }

    /** Server: the contents drop exactly once when the block goes away. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) {
            Containers.dropContents(this.level, pos, this.earnings);
            for (int i = 0; i < EARNINGS_SIZE; i++) this.earnings.set(i, ItemStack.EMPTY);
        }
    }
}
