package com.simplebuilding.modules.simpledimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Stores per-portal data: the tint applied to the nether-style texture and the
 * dimension this portal sends players to. The colour is synced to the client so
 * the block tint factory can hue the texture; the destination is read on the
 * server when a player walks through.
 */
public final class SkyPortalBlockEntity extends BlockEntity {

    private static final int DEFAULT_COLOR = 0x66D9FF;
    private static final String DEFAULT_DESTINATION = "minecraft:overworld";

    // color is read from chunk-mesh threads by the block tint factory, so keep it visible.
    private volatile int color = DEFAULT_COLOR;
    private String destination = DEFAULT_DESTINATION;

    public SkyPortalBlockEntity(BlockPos pos, BlockState state) {
        super(DimensionRegistry.PORTAL_ENTITY, pos, state);
    }

    private static String validDestination(String value) {
        var id = value == null ? null : net.minecraft.resources.Identifier.tryParse(value);
        return id == null ? DEFAULT_DESTINATION : id.toString();
    }
    public int getColor() {
        return color;
    }

    public String getDestination() {
        return destination;
    }

    public void configure(int color, String destination) {
        this.color = color & 0xFFFFFF;
        this.destination = validDestination(destination);
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Color", color);
        output.putString("Destination", destination);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        color = input.getIntOr("Color", DEFAULT_COLOR) & 0xFFFFFF;
        destination = validDestination(input.getStringOr("Destination", DEFAULT_DESTINATION));
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
