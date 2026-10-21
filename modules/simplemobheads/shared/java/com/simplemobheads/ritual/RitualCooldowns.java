package com.simplemobheads.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The Herobrine ritual cooldowns in the world save ({@code data/simplemobheads_herobrine_ritual.dat} of the overworld), so a
 * restart frees neither a shrine nor a player. Times are game ticks ({@code getGameTime}, the same in every dimension); an
 * entry that has run out is dropped on the next write, so the file stays as small as the shrines used within one cooldown.
 */
public final class RitualCooldowns extends SavedData {
    /** Hard bound per list (the codec refuses more); far above any real server. */
    public static final int MAX_ENTRIES = 100_000;

    record Shrine(String id, long ready) {
        static final Codec<Shrine> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("shrine").forGetter(Shrine::id),
                Codec.LONG.fieldOf("ready").forGetter(Shrine::ready)).apply(i, Shrine::new));
    }

    record Player(UUID id, long ready) {
        static final Codec<Player> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(Player::id),
                Codec.LONG.fieldOf("ready").forGetter(Player::ready)).apply(i, Player::new));
    }

    public static final Codec<RitualCooldowns> CODEC = RecordCodecBuilder.create(i -> i.group(
            Shrine.CODEC.listOf(0, MAX_ENTRIES).optionalFieldOf("shrines", List.of())
                    .forGetter(c -> c.shrines.entrySet().stream().map(e -> new Shrine(e.getKey(), e.getValue())).toList()),
            Player.CODEC.listOf(0, MAX_ENTRIES).optionalFieldOf("players", List.of())
                    .forGetter(c -> c.players.entrySet().stream().map(e -> new Player(e.getKey(), e.getValue())).toList()))
            .apply(i, RitualCooldowns::new));
    public static final SavedDataType<RitualCooldowns> TYPE = new SavedDataType<>(Identifier.parse("simplemobheads:herobrine_ritual"),
            RitualCooldowns::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<String, Long> shrines = new HashMap<>();
    private final Map<UUID, Long> players = new HashMap<>();

    public RitualCooldowns() {
    }

    private RitualCooldowns(List<Shrine> shrines, List<Player> players) {
        for (Shrine s : shrines) this.shrines.put(s.id(), s.ready());
        for (Player p : players) this.players.put(p.id(), p.ready());
    }

    public static RitualCooldowns get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Whether the shrine or the player still waits at game time {@code now}. */
    public boolean waiting(String shrine, UUID player, long now) {
        return this.shrines.getOrDefault(shrine, Long.MIN_VALUE) > now || this.players.getOrDefault(player, Long.MIN_VALUE) > now;
    }

    /** Starts the cooldown of both until {@code ready}; run-out entries go. */
    public void start(String shrine, UUID player, long now, long ready) {
        this.shrines.values().removeIf(t -> t <= now);
        this.players.values().removeIf(t -> t <= now);
        if (this.shrines.size() < MAX_ENTRIES) this.shrines.put(shrine, ready);
        if (this.players.size() < MAX_ENTRIES) this.players.put(player, ready);
        setDirty();
    }

    public int size() {
        return this.shrines.size() + this.players.size();
    }

    void clear() {
        this.shrines.clear();
        this.players.clear();
        setDirty();
    }
}
