package com.simplebuilding.tweaks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * DW04C-3 (docs/ai/PLAN-B-SHELVES01-2026-11-04.md): Vanilla's chiseled bookshelf keeps its books on the server only (the block
 * state just says "occupied"). The client needs them to draw a Blaze or Nihilit Book in its slot (ChiseledBooksRenderer), so the
 * block entity sends its books like Vanilla's {@code ShelfBlockEntity}: with the chunk ({@link #getUpdateTag}) and after every
 * change (the slot state changes with each book, the chunk holder then sends {@link #getUpdatePacket}).
 */
@Mixin(ChiseledBookShelfBlockEntity.class)
public abstract class ChiseledBookShelfSyncMixin extends BlockEntity {
    private ChiseledBookShelfSyncMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }
}
