package com.simplebuilding.modules.simpleenddragonfight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * World-wide count of dragon summonings (owner 2026-10-11): every further dragon - one respawned with End crystals after
 * a kill - makes the fight harder for good. Stored in the overworld's saved data; the last dragon's UUID makes a server
 * restart or an unloaded dragon count only once.
 */
public final class DragonSummons extends SavedData {
    public static final Codec<DragonSummons> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("summons", 0).forGetter(d -> d.summons),
            UUIDUtil.CODEC.optionalFieldOf("last_dragon").forGetter(d -> Optional.ofNullable(d.lastDragon))
    ).apply(i, (s, last) -> new DragonSummons(s, last.orElse(null))));
    public static final SavedDataType<DragonSummons> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(DragonFightRegistry.ID, "dragon_summons"), DragonSummons::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private int summons;
    private @Nullable UUID lastDragon;

    public DragonSummons() { this(0, null); }

    public DragonSummons(int summons, @Nullable UUID lastDragon) {
        this.summons = Math.max(0, summons);
        this.lastDragon = lastDragon;
    }

    public static DragonSummons get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int summons() { return summons; }

    /**
     * Notes the dragon of the main fight. A dragon not seen before counts as a further summoning when there was a dragon
     * before it (or the fight says one was already killed, for worlds that got the module later); the very first does not.
     */
    public int record(UUID dragon, boolean previouslyKilled) {
        if (dragon.equals(lastDragon)) return summons;
        if (lastDragon != null || previouslyKilled) summons++;
        lastDragon = dragon;
        setDirty();
        return summons;
    }
}
