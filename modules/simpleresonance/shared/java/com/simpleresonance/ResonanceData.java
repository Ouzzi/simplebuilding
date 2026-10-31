package com.simpleresonance;

import com.mojang.serialization.Codec;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** The saved state of Simple Resonance: one {@link PlayerRecord} per player UUID, in the Overworld's data storage (Vanilla writes it atomically). */
public final class ResonanceData extends SavedData {
    public static final Codec<ResonanceData> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerRecord.CODEC)
            .xmap(ResonanceData::new, d -> d.players).fieldOf("players").codec();

    public static final SavedDataType<ResonanceData> TYPE = new SavedDataType<>(
            SimpleResonance.id("players"), ResonanceData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, PlayerRecord> players = new LinkedHashMap<>();

    public ResonanceData() {}

    private ResonanceData(Map<UUID, PlayerRecord> read) {
        this.players.putAll(read);
    }

    public static ResonanceData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The record of a player; {@code null} when the player never joined. */
    public PlayerRecord find(UUID player) {
        return this.players.get(player);
    }

    /** The record of a player, created empty on first use. */
    public PlayerRecord record(UUID player) {
        PlayerRecord r = this.players.get(player);
        if (r == null) {
            r = new PlayerRecord();
            this.players.put(player, r);
            setDirty();
        }
        return r;
    }

    public int size() {
        return this.players.size();
    }
}
