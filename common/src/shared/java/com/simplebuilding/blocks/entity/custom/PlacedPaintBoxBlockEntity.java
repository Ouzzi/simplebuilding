package com.simplebuilding.blocks.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Der abgestellte Farbkasten ({@code PlacedPaintBoxBlock}, W24-2): haelt den Farbkasten-Stapel samt Inhalt und allen
 * Komponenten (Schluessel {@code Box}). Name ({@link Nameable}) = der Name des Kastens, fuer Jade und Co.
 *
 * <p>{@link #TYPE} setzt die Loader-Registrierung (Fabric: ModBlockEntities); ohne sie stellt der Kasten nichts ab.
 */
public class PlacedPaintBoxBlockEntity extends BlockEntity implements Nameable {
    /** Registriert vom Loader; null, solange dieser Loader den Block nicht anbietet. */
    public static @Nullable BlockEntityType<PlacedPaintBoxBlockEntity> TYPE;
    private static final String BOX_TAG = "Box";

    private ItemStack box = ItemStack.EMPTY;

    public PlacedPaintBoxBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    public ItemStack getBox() {
        return this.box;
    }

    public void setBox(ItemStack stack) {
        this.box = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
    }

    @Override
    public Component getName() {
        return this.box.isEmpty() ? getBlockState().getBlock().getName() : this.box.getHoverName();
    }

    @Override
    public @Nullable Component getCustomName() {
        return this.box.isEmpty() ? null : this.box.getHoverName();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.box = input.read(BOX_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.box.isEmpty()) {
            output.store(BOX_TAG, ItemStack.CODEC, this.box);
        }
    }

    /** Der Client braucht den Kasten fuer Name und Mittelklick. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
