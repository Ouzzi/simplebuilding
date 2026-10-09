package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.custom.GoatHornHolderBlock;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The goat horn of a {@link GoatHornHolderBlock} (with its instrument and name, key {@code Horn}) and the item stuck
 * into it (key {@code Held}, one torch or rod). Both go to the client, which draws the held item; the light of the held
 * item is mirrored into the block state ({@link GoatHornHolderBlock#LIGHT}). Named like the horn (Jade shows
 * "Ponder Goat Horn").
 */
public class GoatHornHolderBlockEntity extends BlockEntity implements Nameable {
    private static final String HORN_TAG = "Horn";
    private static final String HELD_TAG = "Held";

    private ItemStack horn = ItemStack.EMPTY;
    private ItemStack held = ItemStack.EMPTY;

    public GoatHornHolderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GOAT_HORN_HOLDER_BE, pos, state);
    }

    public ItemStack horn() {
        return this.horn;
    }

    public ItemStack held() {
        return this.held;
    }

    public void setHorn(ItemStack stack) {
        this.horn = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        changed();
    }

    /** Puts an item into the horn (or empties it) and sets the block's light to the item's. */
    public void setHeld(ItemStack stack) {
        this.held = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = getBlockState();
            int light = GoatHornHolderBlock.lightOf(this.held);
            if (state.getBlock() instanceof GoatHornHolderBlock && state.getValue(GoatHornHolderBlock.LIGHT) != light) {
                this.level.setBlock(this.worldPosition, state.setValue(GoatHornHolderBlock.LIGHT, light), Block.UPDATE_ALL);
            }
        }
        changed();
    }

    private void changed() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public Component getName() {
        return this.horn.isEmpty() ? getBlockState().getBlock().getName() : this.horn.getHoverName();
    }

    @Override
    public @Nullable Component getCustomName() {
        return this.horn.isEmpty() ? null : this.horn.getHoverName();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.horn = input.read(HORN_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.held = input.read(HELD_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.horn.isEmpty()) {
            output.store(HORN_TAG, ItemStack.CODEC, this.horn);
        }
        if (!this.held.isEmpty()) {
            output.store(HELD_TAG, ItemStack.CODEC, this.held);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
