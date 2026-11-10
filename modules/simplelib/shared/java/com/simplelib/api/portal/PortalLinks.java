package com.simplelib.api.portal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplelib.SimpleLib;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * Stored portal ends (BACK22 core §5.1): for doors whose other end cannot be computed from the position alone - an Overworld house
 * whose height is only known when it is built, later the built Overworld pairs (S3). Saved with the Overworld; link id → end.
 */
public final class PortalLinks extends SavedData {
    /** One stored end: dimension, anchor (left leaf, lower half), facing (walking direction), width. */
    public record End(ResourceKey<Level> dimension, BlockPos anchor, Direction facing, int width) {
        static final Codec<End> CODEC = RecordCodecBuilder.create(i -> i.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(End::dimension),
                BlockPos.CODEC.fieldOf("anchor").forGetter(End::anchor),
                Direction.CODEC.fieldOf("facing").forGetter(End::facing),
                Codec.INT.fieldOf("width").forGetter(End::width)).apply(i, End::new));
    }

    record Entry(long link, End end) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("link").forGetter(Entry::link),
                End.CODEC.fieldOf("end").forGetter(Entry::end)).apply(i, Entry::new));
    }

    public static final Codec<PortalLinks> CODEC = Entry.CODEC.listOf().optionalFieldOf("ends", List.of())
            .xmap(PortalLinks::new, d -> d.ends.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList()).codec();
    public static final SavedDataType<PortalLinks> TYPE = new SavedDataType<>(SimpleLib.id("portal_links"), PortalLinks::new, CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<Long, End> ends = new HashMap<>();

    public PortalLinks() {
        this(List.of());
    }

    private PortalLinks(List<Entry> list) {
        list.forEach(e -> ends.put(e.link(), e.end()));
    }

    public static PortalLinks of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static void put(MinecraftServer server, long link, End end) {
        PortalLinks data = of(server);
        if (!end.equals(data.ends.put(link, end))) data.setDirty();
    }

    public static @Nullable End get(MinecraftServer server, long link) {
        return of(server).ends.get(link);
    }

    public static void remove(MinecraftServer server, long link) {
        PortalLinks data = of(server);
        if (data.ends.remove(link) != null) data.setDirty();
    }
}
