package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.util.DyedStorage;
import com.simplebuilding.util.PlacedBundles;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Das abgestellte Buendel ({@code PlacedBundleBlock}): haelt den Buendel-Stapel samt Inhalt und allen
 * Komponenten (Schluessel {@code Bundle}) und welches Item gerade oben gezeigt wird ({@code Shown}).
 * Beides geht zum Client - das gezeigte Item fuer den {@code PlacedBundleRenderer}, die Farbe fuer die
 * Toenung des Chunk-Meshes ({@code BackpackBlockTint}).
 *
 * <p>Name ({@link Nameable}): der Name des Buendels, fuer Jade und Co.
 */
public class PlacedBundleBlockEntity extends BlockEntity implements Nameable {
    private static final String BUNDLE_TAG = "Bundle";
    private static final String SHOWN_TAG = "Shown";

    private ItemStack bundle = ItemStack.EMPTY;
    /** Index des gezeigten ("obersten") Items im Inhalt. */
    private int shown;
    /** Farbe fuer die Toenung (0xRRGGBB) oder {@link DyedStorage#UNDYED}; aus dem Stapel abgeleitet. */
    private int dyeColor = DyedStorage.UNDYED;

    public PlacedBundleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLACED_BUNDLE_BE, pos, state);
    }

    public ItemStack getBundle() {
        return this.bundle;
    }

    public void setBundle(ItemStack stack) {
        this.bundle = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        this.dyeColor = PlacedBundles.dyeColor(this.bundle);
        this.shown = clamp(this.shown);
        changed();
    }

    /** Der Inhalt als Kopien, oberstes Item zuerst. */
    public List<ItemStack> contents() {
        return PlacedBundles.contents(this.bundle);
    }

    public boolean isEmpty() {
        return contents().isEmpty();
    }

    /** Index des gezeigten Items (immer gueltig, solange der Inhalt nicht leer ist). */
    public int shownIndex() {
        return clamp(this.shown);
    }

    /** Das gezeigte ("oberste") Item als Kopie, leer bei leerem Buendel. */
    public ItemStack shownItem() {
        List<ItemStack> items = contents();
        return items.isEmpty() ? ItemStack.EMPTY : items.get(clamp(this.shown));
    }

    /** Zum naechsten Item weiterschalten (reihum); liefert den neuen Index. */
    public int cycle() {
        int size = contents().size();
        this.shown = size == 0 ? 0 : (clamp(this.shown) + 1) % size;
        changed();
        return this.shown;
    }

    public void setShownIndex(int index) {
        this.shown = clamp(index);
        changed();
    }

    public int dyeColor() {
        return this.dyeColor;
    }

    private int clamp(int index) {
        int size = contents().size();
        return size == 0 ? 0 : Math.floorMod(index, size);
    }

    private void changed() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PlacedBundleBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % PlacedBundles.CYCLE_TICKS == 0) {
            PlacedBundles.tickCycle(level, pos, be);
        }
    }

    // --- Name -------------------------------------------------------------------------------------

    @Override
    public Component getName() {
        return this.bundle.isEmpty() ? getBlockState().getBlock().getName() : this.bundle.getHoverName();
    }

    @Override
    public @Nullable Component getCustomName() {
        return this.bundle.isEmpty() ? null : this.bundle.getHoverName();
    }

    // --- Speichern und Senden ---------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.bundle = input.read(BUNDLE_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.shown = input.getIntOr(SHOWN_TAG, 0);
        int previous = this.dyeColor;
        this.dyeColor = PlacedBundles.dyeColor(this.bundle);
        // Clientseitig: die Farbe steckt im Chunk-Mesh, also neu bauen.
        if (previous != this.dyeColor && this.level != null && this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.bundle.isEmpty()) {
            output.store(BUNDLE_TAG, ItemStack.CODEC, this.bundle);
        }
        if (this.shown != 0) {
            output.putInt(SHOWN_TAG, this.shown);
        }
    }

    /** Der Client braucht das Buendel (gezeigtes Item, Farbe) und den Index. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
