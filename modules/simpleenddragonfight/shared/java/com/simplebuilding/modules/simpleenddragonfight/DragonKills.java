package com.simplebuilding.modules.simpleenddragonfight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * World-wide count of defeated dragons plus the players who already got the Ender Heart (WI26-6). Stored in the
 * overworld's saved data; the last defeated dragon's UUID makes a reload during the death animation count only once.
 */
public final class DragonKills extends SavedData {
    public static final Codec<DragonKills> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("kills", 0).forGetter(d -> d.kills),
            UUIDUtil.CODEC.optionalFieldOf("last_dragon").forGetter(d -> Optional.ofNullable(d.lastDragon)),
            UUIDUtil.CODEC.listOf().optionalFieldOf("rewarded", List.of()).forGetter(d -> new ArrayList<>(d.rewarded))
    ).apply(i, (k, last, r) -> new DragonKills(k, last.orElse(null), r)));
    public static final SavedDataType<DragonKills> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(DragonFightRegistry.ID, "dragon_kills"), DragonKills::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private int kills;
    private @Nullable UUID lastDragon;
    private final Set<UUID> rewarded = new HashSet<>();

    public DragonKills() { this(0, null, List.of()); }

    public DragonKills(int kills, @Nullable UUID lastDragon, List<UUID> rewarded) {
        this.kills = Math.max(0, kills);
        this.lastDragon = lastDragon;
        this.rewarded.addAll(rewarded);
    }

    public static DragonKills get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int kills() { return kills; }

    /** Notes a defeated dragon; the same dragon twice counts once. Returns the total. */
    public int defeat(UUID dragon) {
        if (dragon.equals(lastDragon)) return kills;
        lastDragon = dragon;
        kills++;
        setDirty();
        return kills;
    }

    public boolean hasReward(UUID player) { return rewarded.contains(player); }

    public void markReward(UUID player) {
        if (rewarded.add(player)) setDirty();
    }
}
