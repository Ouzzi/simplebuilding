package com.simplebuilding.modules.simplelayers.vault;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/** Where each player came into the Bedrock Vault (Overworld position): the ascent and the open gate lead back above it. */
public final class VaultEntries extends SavedData {
    record Entry(UUID player, BlockPos pos) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(Entry::player),
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos)).apply(i, Entry::new));
    }

    public static final Codec<VaultEntries> CODEC = Entry.CODEC.listOf().optionalFieldOf("entries", List.of())
            .xmap(VaultEntries::new, d -> d.entries.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList()).codec();
    public static final SavedDataType<VaultEntries> TYPE = new SavedDataType<>(SimpleLayers.id("vault_entries"),
            VaultEntries::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, BlockPos> entries = new HashMap<>();

    public VaultEntries() {
        this(List.of());
    }

    private VaultEntries(List<Entry> list) {
        list.forEach(e -> entries.put(e.player(), e.pos()));
    }

    public static VaultEntries get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void put(UUID player, BlockPos pos) {
        entries.put(player, pos.immutable());
        setDirty();
    }

    public @Nullable BlockPos get(UUID player) {
        return entries.get(player);
    }

    public void remove(UUID player) {
        if (entries.remove(player) != null) setDirty();
    }
}
