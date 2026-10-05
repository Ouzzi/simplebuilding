package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.custom.HammockLayout;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The hammock a hammock block belongs to (docs/ai/PLAN-HAENGEMATTE-WINKEL-2026-10-02.md): in every cloth and rope cell,
 * the first anchor relative to this block and the second anchor's offset from the first. Sent to the client, where the
 * cloth head's renderer draws the hammock from it. Without data (or with an offset beyond the limits) the block counts
 * as broken and falls at its next check.
 */
public class HammockBlockEntity extends BlockEntity {
    private static final String ANCHOR_X = "AnchorX";
    private static final String ANCHOR_Y = "AnchorY";
    private static final String ANCHOR_Z = "AnchorZ";
    private static final String SPAN_X = "SpanX";
    private static final String SPAN_Z = "SpanZ";
    /** Relative offsets are small (a hammock spans at most 5 blocks); anything beyond is junk and ignored. */
    private static final int MAX_OFFSET = 8;

    private int anchorX;
    private int anchorY;
    private int anchorZ;
    private int spanX;
    private int spanZ;
    private boolean linked;

    public HammockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HAMMOCK_BE, pos, state);
    }

    /** The hammock (absolute), or null before it was linked. */
    public HammockLayout.@Nullable Spot spot() {
        return linked ? new HammockLayout.Spot(this.worldPosition.offset(anchorX, anchorY, anchorZ), spanX, spanZ) : null;
    }

    public void setSpot(HammockLayout.Spot spot) {
        BlockPos rel = spot.anchor().subtract(this.worldPosition);
        this.anchorX = rel.getX();
        this.anchorY = rel.getY();
        this.anchorZ = rel.getZ();
        this.spanX = spot.dx();
        this.spanZ = spot.dz();
        this.linked = true;
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Before this block goes: the rest of the hammock checks itself (it falls and the cloth head drops the item once). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level instanceof ServerLevel server) {
            HammockLayout.scheduleChecks(server, spot(), pos);
        }
    }

    /**
     * Forge 26.3 ({@code IForgeBlockEntity}): frustum culling of the globally rendered hammock by everything it draws.
     * No {@code @Override}: Fabric and NeoForge have no such method on block entities.
     */
    public AABB getRenderBoundingBox() {
        HammockLayout.Spot spot = spot();
        return spot != null ? spot.renderBounds() : new AABB(this.worldPosition);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int ax = input.getIntOr(ANCHOR_X, 0);
        int ay = input.getIntOr(ANCHOR_Y, 0);
        int az = input.getIntOr(ANCHOR_Z, 0);
        int sx = input.getIntOr(SPAN_X, 0);
        int sz = input.getIntOr(SPAN_Z, 0);
        this.linked = (sx != 0 || sz != 0) && Math.abs(ax) <= MAX_OFFSET && Math.abs(ay) <= 1 && Math.abs(az) <= MAX_OFFSET
                && Math.abs(sx) <= MAX_OFFSET && Math.abs(sz) <= MAX_OFFSET;
        this.anchorX = ax;
        this.anchorY = ay;
        this.anchorZ = az;
        this.spanX = sx;
        this.spanZ = sz;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (linked) {
            output.putInt(ANCHOR_X, anchorX);
            output.putInt(ANCHOR_Y, anchorY);
            output.putInt(ANCHOR_Z, anchorZ);
            output.putInt(SPAN_X, spanX);
            output.putInt(SPAN_Z, spanZ);
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
