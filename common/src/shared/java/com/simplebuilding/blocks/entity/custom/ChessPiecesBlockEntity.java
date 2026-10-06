package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.custom.ChessPiecesBlock;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.items.custom.ChessPieceItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Die Figuren einer Figurenzelle ({@link ChessPiecesBlock}): vier Plaetze (Viertel der Blockoberseite, Index
 * {@code qx | qz << 1}) mit je hoechstens einer Figur und ihrer Drehung (0-3 Vierteldrehungen im Uhrzeigersinn ab der
 * Blickrichtung {@code facing} der Zelle). Wird zum Client geschickt; die Trefferform haengt an den Figuren und wird
 * hier zwischengespeichert. Beim Laden wird gekappt: nur Figuren-Items, je Platz eins.
 */
public class ChessPiecesBlockEntity extends BlockEntity {
    public static final int SLOTS = 4;
    private static final String PIECES_TAG = "Pieces";
    private static final String ROTATIONS_TAG = "Rotations";

    private final ItemStack[] pieces = new ItemStack[SLOTS];
    private final int[] rotations = new int[SLOTS];
    private @Nullable VoxelShape shape;

    public ChessPiecesBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHESS_PIECES_BE, pos, state);
        Arrays.fill(this.pieces, ItemStack.EMPTY);
    }

    public ItemStack piece(int slot) {
        return this.pieces[slot];
    }

    /** Vierteldrehungen im Uhrzeigersinn ab der Blickrichtung der Zelle. */
    public int rotation(int slot) {
        return this.rotations[slot];
    }

    public int count() {
        int count = 0;
        for (ItemStack piece : this.pieces) {
            if (!piece.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /** Alle Figuren in Platzreihenfolge (ohne leere Plaetze). */
    public List<ItemStack> all() {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack piece : this.pieces) {
            if (!piece.isEmpty()) {
                out.add(piece.copy());
            }
        }
        return out;
    }

    /** Stellt eine Figur (eins von {@code stack}, nicht verbraucht) auf einen freien Platz. */
    public boolean put(int slot, ItemStack stack, int rotation) {
        if (!(stack.getItem() instanceof ChessPieceItem) || !this.pieces[slot].isEmpty()) {
            return false;
        }
        this.pieces[slot] = stack.copyWithCount(1);
        this.rotations[slot] = Math.floorMod(rotation, 4);
        changed();
        return true;
    }

    /** Nimmt die Figur eines Platzes weg (leer, wenn dort keine steht). */
    public ItemStack take(int slot) {
        ItemStack old = this.pieces[slot];
        if (!old.isEmpty()) {
            this.pieces[slot] = ItemStack.EMPTY;
            this.rotations[slot] = 0;
            changed();
        }
        return old;
    }

    /** Dreht die Figur eines Platzes um eine Vierteldrehung im Uhrzeigersinn. */
    public boolean rotate(int slot) {
        if (this.pieces[slot].isEmpty()) {
            return false;
        }
        this.rotations[slot] = (this.rotations[slot] + 1) & 3;
        changed();
        return true;
    }

    /** Setzt die Plaetze der Reihe nach (leere Stapel lassen einen Platz frei), alle ohne Drehung. */
    public void setPieces(List<ItemStack> stacks) {
        Arrays.fill(this.pieces, ItemStack.EMPTY);
        Arrays.fill(this.rotations, 0);
        for (int i = 0; i < SLOTS && i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.getItem() instanceof ChessPieceItem) {
                this.pieces[i] = stack.copyWithCount(1);
            }
        }
        changed();
    }

    private void changed() {
        this.shape = null;
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Trefferform: je Figur ihr Fuss mal ihre Hoehe; ohne Figuren die Form der Zelle ohne Figur. */
    public VoxelShape shape() {
        VoxelShape cached = this.shape;
        if (cached == null) {
            cached = Shapes.empty();
            for (int slot = 0; slot < SLOTS; slot++) {
                if (this.pieces[slot].getItem() instanceof ChessPieceItem item) {
                    cached = Shapes.or(cached, ChessPiecesBlock.slotBox(slot, item.kind().piece().height(item.kind().flat())));
                }
            }
            cached = cached.isEmpty() ? ChessPiecesBlock.EMPTY_SHAPE : cached.optimize();
            this.shape = cached;
        }
        return cached;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Arrays.fill(this.pieces, ItemStack.EMPTY);
        Arrays.fill(this.rotations, 0);
        input.read(PIECES_TAG, ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list -> {
            for (int i = 0; i < SLOTS && i < list.size(); i++) {
                ItemStack stack = list.get(i);
                this.pieces[i] = stack.getItem() instanceof ChessPieceItem ? stack.copyWithCount(1) : ItemStack.EMPTY;
            }
        });
        int[] rots = input.getIntArray(ROTATIONS_TAG).orElse(new int[0]);
        for (int i = 0; i < SLOTS && i < rots.length; i++) {
            this.rotations[i] = rots[i] & 3;
        }
        this.shape = null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(PIECES_TAG, ItemStack.OPTIONAL_CODEC.listOf(), List.of(this.pieces));
        output.putIntArray(ROTATIONS_TAG, this.rotations.clone());
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
