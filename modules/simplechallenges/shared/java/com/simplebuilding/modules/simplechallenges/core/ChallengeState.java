package com.simplebuilding.modules.simplechallenges.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import com.simplebuilding.modules.simplechallenges.preset.UnlockMode;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

/**
 * The world state of a challenge ({@code data/simplechallenges_state.dat} in the Overworld storage). It exists only after a
 * challenge was chosen: {@link #peek} never creates it, so a world that never used a challenge has no file (safety lock).
 * It holds the preset snapshot (later edits of the preset file do not change the world), the free cells per dimension,
 * the credits and everything the prices need to stay stable.
 */
public final class ChallengeState extends SavedData {
    public static final String NONE = "none";

    public static final Codec<ChallengeState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("challenge", NONE).forGetter(s -> s.challenge),
            Codec.STRING.optionalFieldOf("preset", "").forGetter(s -> s.presetJson),
            Codec.BOOL.optionalFieldOf("initialized", false).forGetter(s -> s.initialized),
            Codec.LONG.optionalFieldOf("seed", 0L).forGetter(s -> s.seed),
            Codec.INT.optionalFieldOf("credits", 0).forGetter(s -> s.credits),
            Codec.LONG.optionalFieldOf("lastUnlockTick", 0L).forGetter(s -> s.lastUnlockTick),
            Codec.INT.optionalFieldOf("unlockCount", 0).forGetter(s -> s.unlockCount),
            Codec.STRING.listOf().optionalFieldOf("rewarded", List.of()).forGetter(s -> new ArrayList<>(s.rewarded)),
            Codec.STRING.listOf().optionalFieldOf("baselined", List.of()).forGetter(s -> new ArrayList<>(s.baselined)),
            Codec.unboundedMap(Codec.STRING, Codec.LONG.listOf()).optionalFieldOf("free", Map.of()).forGetter(ChallengeState::freeEncoded),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("prices", Map.of()).forGetter(s -> s.prices)
    ).apply(i, ChallengeState::new));

    public static final SavedDataType<ChallengeState> TYPE = new SavedDataType<>(SimpleChallenges.id("state"), ChallengeState::new, CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    /** Chosen preset id or {@link #NONE}. */
    public String challenge = NONE;
    /** The preset file as it was when the challenge was chosen. */
    public String presetJson = "";
    /** Set once a challenge was chosen; a world with this flag never takes the {@code defaultChallenge} again. */
    public boolean initialized;
    public long seed;
    /** Team credits (S2: one shared pool). */
    public int credits;
    public long lastUnlockTick;
    public int unlockCount;
    /** Advancement ids whose credits are paid (or that were already done at the start). */
    public final Set<String> rewarded = new LinkedHashSet<>();
    /** Player uuids whose earlier advancements were taken as the baseline (no credits for old ones). */
    public final Set<String> baselined = new LinkedHashSet<>();
    /** Cached prices {@code dimension|cx|cz} -> {@link com.simplebuilding.modules.simplechallenges.price.Price#encode()}; frontier cells only. */
    public final Map<String, String> prices = new LinkedHashMap<>();

    private final Map<String, long[]> rawFree = new HashMap<>();
    /** Live grids by dimension, built by {@link #bind}. */
    final Map<String, UnlockGrid> grids = new HashMap<>();

    public ChallengeState() {}

    private ChallengeState(String challenge, String presetJson, boolean initialized, long seed, int credits, long lastUnlockTick,
                           int unlockCount, List<String> rewarded, List<String> baselined, Map<String, List<Long>> free,
                           Map<String, String> prices) {
        this.challenge = challenge;
        this.presetJson = presetJson;
        this.initialized = initialized;
        this.seed = seed;
        this.credits = credits;
        this.lastUnlockTick = lastUnlockTick;
        this.unlockCount = unlockCount;
        this.rewarded.addAll(rewarded);
        this.baselined.addAll(baselined);
        free.forEach((dim, list) -> {
            long[] keys = new long[list.size()];
            for (int k = 0; k < keys.length; k++) keys[k] = list.get(k);
            rawFree.put(dim, keys);
        });
        this.prices.putAll(prices);
    }

    private Map<String, List<Long>> freeEncoded() {
        Map<String, List<Long>> out = new LinkedHashMap<>();
        Set<String> dims = new LinkedHashSet<>(rawFree.keySet());
        dims.addAll(grids.keySet());
        for (String dim : dims) {
            long[] keys = grids.containsKey(dim) ? grids.get(dim).keys() : rawFree.get(dim);
            List<Long> list = new ArrayList<>(keys.length);
            for (long k : keys) list.add(k);
            out.put(dim, list);
        }
        return out;
    }

    /** The free cells of a dimension as sorted keys (live grid when bound, else the saved ones). */
    public long[] cells(String dim) {
        UnlockGrid g = grids.get(dim);
        return g != null ? g.keys() : rawFree.getOrDefault(dim, new long[0]);
    }

    public boolean activated() {
        return !NONE.equals(challenge);
    }

    /** Builds the live grids for the mode's dimensions from the saved cells. */
    void bind(UnlockMode mode) {
        grids.clear();
        for (String dim : mode.dimensions()) grids.put(dim, UnlockGrid.fromKeys(mode.cell(), rawFree.getOrDefault(dim, new long[0])));
    }

    /** Forgets everything of the chosen challenge (switching to another, or off). */
    void reset() {
        challenge = NONE;
        presetJson = "";
        credits = 0;
        unlockCount = 0;
        rewarded.clear();
        baselined.clear();
        prices.clear();
        rawFree.clear();
        grids.clear();
        setDirty();
    }

    /** The saved state if the world has one; never creates it. */
    public static @Nullable ChallengeState peek(MinecraftServer server) {
        return server.overworld().getDataStorage().get(TYPE);
    }

    public static ChallengeState create(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
