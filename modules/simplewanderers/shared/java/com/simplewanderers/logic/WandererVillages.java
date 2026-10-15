package com.simplewanderers.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplewanderers.SimpleWanderers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Which village has (had) its wanderer: one entry per village start (dimension + start chunk), alive with the entity's
 * UUID or dead since a game time. An entry exists from the first spawn on; the world never forgets a village.
 */
public final class WandererVillages extends SavedData {
    public static final int MAX_VILLAGES = 100_000;
    public static final long NOT_DEAD = -1L;

    /** {@code wanderer} present = alive; otherwise {@code deadSince} is the game time of the loss. */
    public record Entry(String key, Optional<UUID> wanderer, long deadSince) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("village").forGetter(Entry::key),
                UUIDUtil.CODEC.optionalFieldOf("wanderer").forGetter(Entry::wanderer),
                Codec.LONG.optionalFieldOf("dead_since", NOT_DEAD).forGetter(Entry::deadSince)).apply(i, Entry::new));

        public boolean alive() {
            return wanderer.isPresent();
        }
    }

    public static final Codec<WandererVillages> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf(0, MAX_VILLAGES).optionalFieldOf("villages", List.of()).forGetter(WandererVillages::entries)).apply(i, WandererVillages::new));
    public static final SavedDataType<WandererVillages> TYPE = new SavedDataType<>(SimpleWanderers.id("villages"),
            WandererVillages::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<String, Entry> villages = new LinkedHashMap<>();

    public WandererVillages() {}

    private WandererVillages(List<Entry> entries) {
        entries.forEach(e -> villages.put(e.key(), e));
    }

    public List<Entry> entries() {
        return new ArrayList<>(villages.values());
    }

    public static WandererVillages get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static String key(ServerLevel level, long villageId) {
        return level.dimension().identifier() + "/" + villageId;
    }

    public Entry get(String key) {
        return villages.get(key);
    }

    public int size() {
        return villages.size();
    }

    /** The village now has this live wanderer (spawn, or adopting one that is found without a record). */
    public boolean setAlive(String key, UUID wanderer) {
        Entry old = villages.get(key);
        if (old == null && villages.size() >= MAX_VILLAGES) return false;
        villages.put(key, new Entry(key, Optional.of(wanderer), NOT_DEAD));
        setDirty();
        return true;
    }

    /** Marks the village dead, but only if {@code wanderer} is the one on record (duplicates never kill the record). */
    public boolean markDead(String key, UUID wanderer, long gameTime) {
        Entry old = villages.get(key);
        if (old == null || old.wanderer().isEmpty() || !old.wanderer().get().equals(wanderer)) return false;
        villages.put(key, new Entry(key, Optional.empty(), gameTime));
        setDirty();
        return true;
    }

    /** The recorded wanderer is gone without a trace (never found while a player stood at the bell). */
    public void markLost(String key, long gameTime) {
        Entry old = villages.get(key);
        if (old == null || old.wanderer().isEmpty()) return;
        villages.put(key, new Entry(key, Optional.empty(), gameTime));
        setDirty();
    }
}
