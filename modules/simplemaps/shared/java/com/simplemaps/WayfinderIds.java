package com.simplemaps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Hands out wayfinder map ids (own counter, independent of Vanilla map ids). */
public final class WayfinderIds extends SavedData {
    public static final Codec<WayfinderIds> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("next").forGetter(d -> d.next)).apply(i, WayfinderIds::new));
    public static final SavedDataType<WayfinderIds> TYPE = new SavedDataType<>(SimpleMaps.id("wayfinder_ids"),
            WayfinderIds::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    private int next;

    public WayfinderIds() {}

    private WayfinderIds(int next) {
        this.next = next;
    }

    /** Whether this id was handed out (requests for other ids are ignored, so they never create data files). */
    public static boolean exists(MinecraftServer server, int id) {
        return id >= 0 && id < server.overworld().getDataStorage().computeIfAbsent(TYPE).next;
    }

    public static int allocate(MinecraftServer server) {
        WayfinderIds ids = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        int id = ids.next++;
        ids.setDirty();
        return id;
    }
}
