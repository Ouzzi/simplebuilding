package com.simplebuilding.modules.simplemobs.trials;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * What the Evil Omen left in one dimension that no block state can hold (TR31-R): the kind and stage of Vanilla Trial Spawners turned evil
 * (config evilOnVanillaSpawners) and the Evil / Void Raids that still wait for their Looter Chest, by Vanilla raid id. Saved with the level, so a
 * server restart keeps both. A world without the file (older worlds) starts empty; entries without a stage are dropped on load.
 */
public final class EvilTrialsData extends SavedData {
    /** {@code kind} + {@code stage} of one turned spawner or one Evil Raid. */
    public record Mark(TrialKind kind, int stage) {
        static boolean valid(TrialKind kind, int stage) {
            return kind != TrialKind.WEATHERED && stage > 0;
        }
    }

    private static final Codec<TrialKind> KIND = StringRepresentable.fromEnum(TrialKind::values);

    private record Spawner(BlockPos pos, TrialKind kind, int stage) {
        static final Codec<Spawner> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Spawner::pos),
                KIND.fieldOf("kind").forGetter(Spawner::kind),
                Codec.INT.fieldOf("stage").forGetter(Spawner::stage)).apply(i, Spawner::new));
    }

    private record RaidMark(int id, TrialKind kind, int stage) {
        static final Codec<RaidMark> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("id").forGetter(RaidMark::id),
                KIND.fieldOf("kind").forGetter(RaidMark::kind),
                Codec.INT.fieldOf("stage").forGetter(RaidMark::stage)).apply(i, RaidMark::new));
    }

    public static final Codec<EvilTrialsData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Spawner.CODEC.listOf().optionalFieldOf("vanilla_spawners", List.of())
                    .forGetter(d -> d.spawners.entrySet().stream().map(e -> new Spawner(BlockPos.of(e.getKey()), e.getValue().kind(), e.getValue().stage())).toList()),
            RaidMark.CODEC.listOf().optionalFieldOf("raids", List.of())
                    .forGetter(d -> d.raids.entrySet().stream().map(e -> new RaidMark(e.getKey(), e.getValue().kind(), e.getValue().stage())).toList()))
            .apply(i, EvilTrialsData::new));
    public static final SavedDataType<EvilTrialsData> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Trials.ID, "evil_trials"),
            EvilTrialsData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<Long, Mark> spawners = new HashMap<>();
    private final Map<Integer, Mark> raids = new HashMap<>();
    /** Not saved: whether the raids of this (freshly loaded) data were matched to the live Vanilla raids yet. */
    boolean hydrated;

    public EvilTrialsData() {
        this(List.of(), List.of());
    }

    private EvilTrialsData(List<Spawner> spawners, List<RaidMark> raids) {
        for (Spawner s : spawners) if (Mark.valid(s.kind(), s.stage())) this.spawners.put(s.pos().asLong(), new Mark(s.kind(), Math.min(Trials.STAGES, s.stage())));
        for (RaidMark r : raids) if (Mark.valid(r.kind(), r.stage())) this.raids.put(r.id(), new Mark(r.kind(), Math.min(Trials.STAGES, r.stage())));
    }

    public static EvilTrialsData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // --- Vanilla spawners
    public @Nullable Mark spawner(BlockPos pos) {
        return spawners.isEmpty() ? null : spawners.get(pos.asLong());
    }

    public void putSpawner(BlockPos pos, TrialKind kind, int stage) {
        if (!Mark.valid(kind, stage)) return;
        spawners.put(pos.asLong(), new Mark(kind, stage));
        setDirty();
    }

    public void removeSpawner(BlockPos pos) {
        if (!spawners.isEmpty() && spawners.remove(pos.asLong()) != null) setDirty();
    }

    // --- Evil Raids
    public Map<Integer, Mark> raids() {
        return java.util.Collections.unmodifiableMap(raids);
    }

    public void putRaid(int id, TrialKind kind, int stage) {
        if (!Mark.valid(kind, stage)) return;
        raids.put(id, new Mark(kind, stage));
        setDirty();
    }

    public void removeRaid(int id) {
        if (!raids.isEmpty() && raids.remove(id) != null) setDirty();
    }
}
