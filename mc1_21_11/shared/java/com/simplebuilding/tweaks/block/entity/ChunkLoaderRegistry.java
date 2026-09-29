package com.simplebuilding.tweaks.block.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.config.ServerTuning;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

/**
 * Alle gesetzten Chunk-Loader des Servers (Besitzer-Entscheidung 2026-09-28), gespeichert mit der
 * Oberwelt ({@link SavedData}): Dimension, Position, Besitzer, Stufe und ob er gerade Chunks haelt.
 *
 * <p>Wozu: ein Loader, dessen Besitzer offline ist, gibt seine Chunks frei
 * ({@code server.chunkLoaders.requireOwnerOnline}); danach ist sein Chunk nicht mehr geladen und die
 * Block-Entity tickt nicht. Kommt der Besitzer zurueck, weckt {@link #reconcile} den Loader ueber
 * diese Liste (Chunk laden, {@link ChunkLoaderBlockEntity#update}). Dieselbe Liste zeigt der
 * Admin-Befehl {@code /simplebuilding chunkloaders list} und raeumt {@code ... remove} auf.
 *
 * <p>Die Liste wird von jeder Block-Entity bei ihrer Pruefung (alle {@value ChunkLoaderBlockEntity#CHECK_INTERVAL}
 * Ticks) nachgezogen und beim Abbau geleert; ein Eintrag ohne Loader an seiner Stelle faellt beim
 * naechsten Wecken oder beim Entfernen per Befehl heraus.
 */
public final class ChunkLoaderRegistry extends SavedData {

    /** Ein Loader: Dimension als ID, Block, Besitzer (leer = vor der Besitzer-Speicherung gesetzt), Stufe 1-3. */
    public record Entry(String dimension, BlockPos pos, Optional<UUID> owner, int tier, boolean active) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("dimension").forGetter(Entry::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Entry::owner),
                Codec.INT.optionalFieldOf("tier", 1).forGetter(Entry::tier),
                Codec.BOOL.optionalFieldOf("active", false).forGetter(Entry::active)
        ).apply(i, Entry::new));

        public @Nullable ResourceKey<Level> levelKey() {
            Identifier id = Identifier.tryParse(dimension);
            return id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
        }
    }

    public static final Codec<ChunkLoaderRegistry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().optionalFieldOf("loaders", List.of()).forGetter(ChunkLoaderRegistry::entries)
    ).apply(i, ChunkLoaderRegistry::new));

    // MC 1.21.11: SavedDataType nimmt einen String als Dateinamen (26.2: eine Identifier-Id).
    public static final SavedDataType<ChunkLoaderRegistry> TYPE = new SavedDataType<>(
            "simplebuilding_chunk_loaders",
            ChunkLoaderRegistry::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public ChunkLoaderRegistry() {
    }

    private ChunkLoaderRegistry(List<Entry> loaded) {
        for (Entry entry : loaded) {
            entries.put(key(entry.dimension(), entry.pos()), entry);
        }
    }

    public List<Entry> entries() {
        return new ArrayList<>(entries.values());
    }

    private static String key(String dimension, BlockPos pos) {
        return dimension + "@" + pos.asLong();
    }

    private static ChunkLoaderRegistry of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    // =====================================================================================
    // Pflege (aus der Block-Entity)
    // =====================================================================================

    /** Traegt den Loader ein bzw. zieht ihn nach; speichert nur bei einer Aenderung. */
    public static void update(ServerLevel level, BlockPos pos, @Nullable UUID owner, int tier, boolean active) {
        ChunkLoaderRegistry data = of(level.getServer());
        String dimension = level.dimension().identifier().toString();
        Entry entry = new Entry(dimension, pos.immutable(), Optional.ofNullable(owner), tier, active);
        Entry old = data.entries.put(key(dimension, pos), entry);
        if (!entry.equals(old)) {
            data.setDirty();
        }
    }

    /** Streicht den Loader (abgebaut, ersetzt). */
    public static void remove(ServerLevel level, BlockPos pos) {
        ChunkLoaderRegistry data = of(level.getServer());
        if (data.entries.remove(key(level.dimension().identifier().toString(), pos)) != null) {
            data.setDirty();
        }
    }

    public static List<Entry> all(MinecraftServer server) {
        return of(server).entries();
    }

    public static @Nullable Entry find(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos) {
        return of(server).entries.get(key(dimension.identifier().toString(), pos));
    }

    // =====================================================================================
    // Regeln
    // =====================================================================================

    /**
     * Ob ein Loader in {@code level} mit diesem Besitzer Chunks halten darf: Pads-Schalter an, Dimension
     * nicht gesperrt ({@code server.dimensionLocks.chunkLoaderBlockedDimensions}) und - wenn verlangt -
     * der Besitzer online. Loader ohne Besitzer laufen immer.
     */
    public static boolean mayRun(ServerLevel level, @Nullable UUID owner) {
        if (!ChunkLoaderBlockEntity.enabled() || ServerTuning.chunkLoaderBlockedIn(level.dimension().identifier())) {
            return false;
        }
        return owner == null || !ServerTuning.get().chunkLoaders.requireOwnerOnline || ownerOnline(level.getServer(), owner);
    }

    public static boolean ownerOnline(MinecraftServer server, UUID owner) {
        return server.getPlayerList().getPlayer(owner) != null;
    }

    /**
     * Gleicht alle Loader mit ihren Regeln ab (vom Server-Tick alle
     * {@value ChunkLoaderBlockEntity#CHECK_INTERVAL} Ticks): ein Loader, der laufen soll, aber keine
     * Chunks haelt (Besitzer wieder online, Schalter wieder an), wird geweckt - sein Chunk wird dazu
     * einmal geladen. Einer, der nicht laufen soll, gibt frei; sein Chunk ist dann ohnehin geladen.
     *
     * @return wie viele Loader geweckt oder angehalten wurden
     */
    public static int reconcile(MinecraftServer server) {
        ChunkLoaderRegistry data = of(server);
        if (data.entries.isEmpty()) {
            return 0;
        }
        int changed = 0;
        for (Entry entry : new ArrayList<>(data.entries.values())) {
            ResourceKey<Level> key = entry.levelKey();
            ServerLevel level = key == null ? null : server.getLevel(key);
            if (level == null) {
                continue;
            }
            boolean shouldRun = mayRun(level, entry.owner().orElse(null));
            if (shouldRun == entry.active()) {
                continue;
            }
            if (!shouldRun && level.getChunkSource().getChunkNow(entry.pos().getX() >> 4, entry.pos().getZ() >> 4) == null) {
                // Haelt laut Liste Chunks, ist aber nicht geladen: dann haelt er auch keine mehr.
                update(level, entry.pos(), entry.owner().orElse(null), entry.tier(), false);
                continue;
            }
            // getChunk laedt den Chunk, falls noetig (einmal beim Wecken).
            level.getChunk(entry.pos());
            if (level.getBlockEntity(entry.pos()) instanceof ChunkLoaderBlockEntity loader) {
                loader.update(level);
                changed++;
            } else {
                data.entries.remove(key(entry.dimension(), entry.pos()));
                data.setDirty();
            }
        }
        return changed;
    }
}
