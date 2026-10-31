package com.simpleblocks.snow;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * The block a {@link SnowloggedBlock} shares its cell with, saved as a block state ({@code Held}) and sent to the client for
 * the renderer. An unreadable state (a removed mod's fence) leaves the cell as plain snow.
 */
public class SnowloggedBlockEntity extends BlockEntity {
    private static final String HELD_TAG = "Held";

    private @Nullable BlockState held;

    public SnowloggedBlockEntity(BlockPos pos, BlockState state) {
        super(SnowBlocks.SNOWLOGGED_BE, pos, state);
    }

    public @Nullable BlockState held() {
        return held;
    }

    public void setHeld(@Nullable BlockState state) {
        this.held = state;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        held = input.read(HELD_TAG, BlockState.CODEC).filter(s -> !s.isAir()).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (held != null) {
            output.store(HELD_TAG, BlockState.CODEC, held);
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
