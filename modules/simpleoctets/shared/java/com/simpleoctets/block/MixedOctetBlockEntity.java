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
            this.parts[i] = at >= 0 && at < palette.size() ? OctetCells.material(readPart(palette.get(at))) : null;
        }
    }

    /**
     * One palette entry. The game writes every property; an entry typed by hand (/setblock, /data, a datapack or another
     * tool: only {@code Name}, or some properties) fails the strict codec - O8 audit 2: the cell came out empty and the
     * ghost-bit check then removed it. Such entries are read leniently (missing properties = defaults); an unknown block
     * reads as air, which is no material, so a removed mod's entry still stays empty.
     */
    private static @Nullable BlockState readPart(com.mojang.serialization.Dynamic<?> entry) {
        BlockState strict = BlockState.CODEC.parse(entry).result().orElse(null);
        if (strict != null) {
            return strict;
        }
        net.minecraft.nbt.Tag tag = entry.convert(net.minecraft.nbt.NbtOps.INSTANCE).getValue();
        return tag instanceof CompoundTag compound
                ? net.minecraft.nbt.NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK, compound) : null;
    }

    /**
     * a-hang50 (owner 14.11.: the world hung, closing hung, reopening took ages): setLevel runs while the chunk is still in its
     * FULL step ({@code SerializableChunkData.postLoadChunk -> LevelChunk.setBlockEntity}). {@code server.execute} ran the check
     * INLINE there (the server thread was not inside a task), and its {@code level.getBlockEntity} waited for exactly that chunk
     * ({@code ServerChunkCache.getChunk -> managedBlock}) forever. {@code schedule} always queues; the check itself never loads.
     */
    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        if (!level.isClientSide() && level.getServer() != null) {
            net.minecraft.server.MinecraftServer server = level.getServer();
            server.schedule(new net.minecraft.server.TickTask(server.getTickCount(), this::dropGhostBits));
        }
    }

    /**
     * REV2-B-5: bits the state sets but no readable material backs (a removed mod's material, a damaged file) made an
     * invisible ghost cell with collision; after the load they are cleared (nothing left = air or water). a-hang50: only on a
     * chunk that is already FULL ({@code getChunkNow}, never a blocking load - a chunk still loading is skipped, the next load
     * repairs it), and without neighbour or shape updates, so no other chunk is touched.
     */
    void dropGhostBits() {
        if (!(this.level instanceof net.minecraft.server.level.ServerLevel level) || this.isRemoved()) {
            return;
        }
        BlockPos pos = this.worldPosition;
        net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null || chunk.getBlockEntity(pos, net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK) != this) {
            return;
        }
        BlockState state = chunk.getBlockState(pos);
        BlockState[] parts = OctetCells.parts(chunk, pos, state);
        if (OctetCells.mask(parts) != OctetCellBlock.mask(state)) {
            OctetCells.set(level, pos, parts, OctetCells.wet(state), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
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
