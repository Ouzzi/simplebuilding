package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.custom.PlacedSmallPartsBlock;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.util.PlacedSmallParts;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Die Teile eines Kleinteil-Haeufchens ({@link PlacedSmallPartsBlock}): bis zu {@link PlacedSmallParts#MAX_PARTS}
 * Stapel mit je einem Item, in der Reihenfolge des Ablegens (Schluessel {@code Parts}). Wird zum Client geschickt, der
 * sie zeichnet; die Trefferform haengt an den Teilen und wird hier zwischengespeichert. Beim Laden wird gekappt:
 * leere Stapel fallen weg, mehr als vier werden nicht gelesen, jeder Stapel zaehlt eins.
 */
public class PlacedSmallPartsBlockEntity extends BlockEntity {
    private static final String PARTS_TAG = "Parts";

    private final List<ItemStack> parts = new ArrayList<>();
    private @Nullable VoxelShape shape;
    private @Nullable Direction shapeFacing;

    public PlacedSmallPartsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLACED_SMALL_PARTS_BE, pos, state);
    }

    /** Die Teile (unveraenderliche Sicht), zuerst abgelegtes zuerst. */
    public List<ItemStack> parts() {
        return Collections.unmodifiableList(this.parts);
    }

    /** Legt ein Teil (eins von {@code stack}, nicht verbraucht) dazu; false, wenn der Fleck voll ist. */
    public boolean add(ItemStack stack) {
        if (stack.isEmpty() || this.parts.size() >= PlacedSmallParts.MAX_PARTS) {
            return false;
        }
        this.parts.add(stack.copyWithCount(1));
        changed();
        return true;
    }

    /** Ersetzt alle Teile (gekappt wie beim Laden). */
    public void setParts(List<ItemStack> stacks) {
        this.parts.clear();
        accept(stacks);
        changed();
    }

    private void accept(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && this.parts.size() < PlacedSmallParts.MAX_PARTS) {
                this.parts.add(stack.copyWithCount(1));
            }
        }
        this.shape = null;
    }

    private void changed() {
        this.shape = null;
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Trefferform der Teile, neu gerechnet nur, wenn sich Teile oder Richtung geaendert haben. */
    public VoxelShape shape() {
        BlockState state = getBlockState();
        Direction facing = state.getBlock() instanceof PlacedSmallPartsBlock ? state.getValue(PlacedSmallPartsBlock.FACING) : Direction.NORTH;
        VoxelShape cached = this.shape;
        if (cached == null || facing != this.shapeFacing) {
            cached = PlacedSmallParts.shape(this.parts, facing);
            this.shape = cached;
            this.shapeFacing = facing;
        }
        return cached;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.parts.clear();
        input.read(PARTS_TAG, ItemStack.CODEC.listOf()).ifPresent(this::accept);
        this.shape = null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.parts.isEmpty()) {
            output.store(PARTS_TAG, ItemStack.CODEC.listOf(), List.copyOf(this.parts));
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
