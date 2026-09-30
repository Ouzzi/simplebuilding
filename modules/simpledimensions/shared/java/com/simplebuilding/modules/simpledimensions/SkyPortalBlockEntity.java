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
    public String definition="", linkDimension="";
    public BlockPos anchor=BlockPos.ZERO, link=BlockPos.ZERO;
    public boolean generated=false, linked=false;
    public void define(String id,BlockPos anchor,boolean generated){this.definition=id;this.anchor=anchor.immutable();this.generated=generated;setChanged();}
    public void connect(String dimension,BlockPos safe){linkDimension=validDestination(dimension);link=safe.immutable();linked=true;setChanged();}


    public SkyPortalBlockEntity(BlockPos pos, BlockState state) {
        super(state.is(DimensionRegistry.LEGACY)?DimensionRegistry.LEGACY_ENTITY:DimensionRegistry.PORTAL_ENTITY, pos, state);
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
        output.putString("Definition",definition);output.putString("LinkDimension",linkDimension);
        output.putLong("Anchor",anchor.asLong());output.putLong("Link",link.asLong());output.putBoolean("Generated",generated);output.putBoolean("Linked",linked);
        output.putInt("Color", color);
        output.putString("Destination", destination);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        definition=input.getStringOr("Definition","");linkDimension=input.getStringOr("LinkDimension","");
        anchor=BlockPos.of(input.getLongOr("Anchor",0));link=BlockPos.of(input.getLongOr("Link",0));
        generated=input.getBooleanOr("Generated",false);linked=input.getBooleanOr("Linked",false)&&net.minecraft.resources.Identifier.tryParse(linkDimension)!=null;
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
