package com.simplelib.block;

import com.simplelib.registry.LibMaterials;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Carries the owner of a Netherite / Enderite pressure plate (key {@code Owner}, a UUID as int array, the same as Simple Pads'
 * plates stored before N48) and sends it to the client so the break progress (owner fast, stranger slow) is the same on both sides.
 */
public class FilterPlateBlockEntity extends BlockEntity {
    private @Nullable UUID owner;

    public FilterPlateBlockEntity(BlockPos pos, BlockState state) {
        super(LibMaterials.filterPlateType(), pos, state);
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public @Nullable UUID getOwner() {
        return owner;
    }

    public boolean isOwner(Player player) {
        return owner != null && owner.equals(player.getUUID());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) output.store("Owner", UUIDUtil.CODEC, owner);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
