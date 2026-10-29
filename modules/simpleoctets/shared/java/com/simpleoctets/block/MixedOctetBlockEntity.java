package com.simpleoctets.block;

import com.simpleoctets.registry.OctetRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 * Die Materialien einer gemischten Achtelzelle ({@link MixedOctetBlock}, Queue N48): je
 * Achtel ein Materialzustand ({@link OctetCells#material}) oder null. Gespeichert als Palette ({@code Palette}, die
 * verschiedenen Materialien) plus acht Indizes ({@code Octets}, -1 = leer); welche Achtel belegt sind, sagt der
 * Blockzustand. Beim Laden faellt alles weg, was keine reine Achtelzelle (mehr) ist. Wird zum Client geschickt.
 */
public class MixedOctetBlockEntity extends BlockEntity {
    private static final String PALETTE_TAG = "Palette";
    private static final String OCTETS_TAG = "Octets";

    private final BlockState[] parts = new BlockState[8];

    public MixedOctetBlockEntity(BlockPos pos, BlockState state) {
        super(OctetRegistry.MIXED_OCTET_BE, pos, state);
    }

    /** Material des Achtels {@code index} (auch ein vom Zustand nicht belegtes kann noch eins tragen; Leser maskieren). */
    public @Nullable BlockState part(int index) {
        return this.parts[index & 7];
    }

    /** Kopie aller acht Teile. */
    public BlockState[] parts() {
        return Arrays.copyOf(this.parts, 8);
    }

    public void setParts(BlockState[] parts) {
        for (int i = 0; i < 8; i++) {
            this.parts[i] = OctetCells.material(parts[i]);
        }
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Arrays.fill(this.parts, null);
        // Entry by entry (REV2-B): one material of a removed mod must not take the others with it - a strict list codec
        // fails as a whole and left a cell without any material (invisible, no drops). Unreadable entries stay empty.
        List<com.mojang.serialization.Dynamic<?>> palette = input.read(PALETTE_TAG, com.mojang.serialization.Codec.PASSTHROUGH.listOf()).orElse(List.of());
        int[] octets = input.getIntArray(OCTETS_TAG).orElse(new int[0]);
        for (int i = 0; i < 8 && i < octets.length; i++) {
            int at = octets[i];
            this.parts[i] = at >= 0 && at < palette.size() ? OctetCells.material(BlockState.CODEC.parse(palette.get(at)).result().orElse(null)) : null;
        }
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        if (!level.isClientSide() && level.getServer() != null) {
            level.getServer().execute(this::dropGhostBits);
        }
    }

    /**
     * REV2-B-5: bits the state sets but no readable material backs (a removed mod's material, a damaged file) made an
     * invisible ghost cell with collision; after the load they are cleared (nothing left = air or water).
     */
    private void dropGhostBits() {
        net.minecraft.world.level.Level level = this.level;
        if (level == null || this.isRemoved() || !level.hasChunkAt(this.worldPosition) || level.getBlockEntity(this.worldPosition) != this) {
            return;
        }
        BlockState state = getBlockState();
        BlockState[] parts = OctetCells.parts(level, this.worldPosition, state);
        if (OctetCells.mask(parts) != OctetCellBlock.mask(state)) {
            OctetCells.set(level, this.worldPosition, parts, OctetCells.wet(state), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        List<BlockState> palette = new ArrayList<>();
        int[] octets = new int[8];
        for (int i = 0; i < 8; i++) {
            BlockState part = this.parts[i];
            if (part == null) {
                octets[i] = -1;
                continue;
            }
            int at = palette.indexOf(part);
            if (at < 0) {
                at = palette.size();
                palette.add(part);
            }
            octets[i] = at;
        }
        output.store(PALETTE_TAG, BlockState.CODEC.listOf(), palette);
        output.putIntArray(OCTETS_TAG, octets);
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
