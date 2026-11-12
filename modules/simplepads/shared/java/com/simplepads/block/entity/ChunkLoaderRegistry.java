package com.simplepads.block.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.minecraft.server.players.NameAndId;
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
 * <p><b>Limit je Spieler</b> (Besitzer 12.11.2026, Audit simplepads-P2-1, {@code tuning.chunkLoaderMaxPerPlayer},
 * Standard 9): von den Loadern eines Besitzers laufen nur die ersten N, die laufen wollen (nicht per Redstone
 * abgeschaltet, Dimension nicht gesperrt), in der Reihenfolge dieser Liste (= Reihenfolge des Setzens). Rang statt
 * "Anzahl aktiver", damit nach einem gesenkten Limit nichts hin- und herschaltet; wird ein frueherer abgebaut oder
 * abgeschaltet, rueckt der naechste nach ({@link #reconcile}). Operatoren und Loader ohne Besitzer haben kein Limit.
 *
 * <p>Die Liste wird von jeder Block-Entity bei ihrer Pruefung (alle {@value ChunkLoaderBlockEntity#CHECK_INTERVAL}
 * Ticks) nachgezogen und beim Abbau geleert; ein Eintrag ohne Loader an seiner Stelle faellt beim
 * naechsten Wecken oder beim Entfernen per Befehl heraus.
 */
public final class ChunkLoaderRegistry extends SavedData {

    /**
     * Ein Loader: Dimension als ID, Block, Besitzer (leer = vor der Besitzer-Speicherung gesetzt), Stufe 1-3, ob er gerade
     * Chunks haelt und ob ihn ein Redstone-Signal abschaltet ({@code powered}, a-chunk36).
     */
    public record Entry(String dimension, BlockPos pos, Optional<UUID> owner, int tier, boolean active, boolean powered) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("dimension").forGetter(Entry::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Entry::owner),
                Codec.INT.optionalFieldOf("tier", 1).forGetter(Entry::tier),
                Codec.BOOL.optionalFieldOf("active", false).forGetter(Entry::active),
                Codec.BOOL.optionalFieldOf("powered", false).forGetter(Entry::powered)
        ).apply(i, Entry::new));

        /** Ob der Loader laufen will und damit einen Platz des Limits belegt: kein Redstone, Dimension nicht gesperrt. */
        public boolean wantsToRun() {
            Identifier id = Identifier.tryParse(dimension);
            return !powered && (id == null || !com.simplepads.SimplePads.config().tuning.chunkLoaderBlockedIn(id));
        }

        public @Nullable ResourceKey<Level> levelKey() {
            Identifier id = Identifier.tryParse(dimension);
            return id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
        }
    }

    public static final Codec<ChunkLoaderRegistry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().optionalFieldOf("loaders", List.of()).forGetter(ChunkLoaderRegistry::entries)
    ).apply(i, ChunkLoaderRegistry::new));

    // MC 26.2: SavedDataType nimmt eine Identifier-Id (1.21.11: einen String), siehe SledgehammerProgress.
    public static final SavedDataType<ChunkLoaderRegistry> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("simplebuilding", "chunk_loaders") /* kept: the saved file of old worlds */,
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
    public static void update(ServerLevel level, BlockPos pos, @Nullable UUID owner, int tier, boolean active, boolean powered) {
        ChunkLoaderRegistry data = of(level.getServer());
        String dimension = level.dimension().identifier().toString();
        Entry entry = new Entry(dimension, pos.immutable(), Optional.ofNullable(owner), tier, active, powered);
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
        if (!ChunkLoaderBlockEntity.enabled() || com.simplepads.SimplePads.config().tuning.chunkLoaderBlockedIn(level.dimension().identifier())) {
            return false;
        }
        return owner == null || !com.simplepads.SimplePads.config().tuning.chunkLoaderRequireOwnerOnline || ownerOnline(level.getServer(), owner);
    }

    public static boolean ownerOnline(MinecraftServer server, UUID owner) {
        return server.getPlayerList().getPlayer(owner) != null;
    }

    /**
     * Operatoren sind vom Limit ausgenommen ({@code PlayerList.isOp}: die Ops-Liste ist nach UUID geschluesselt, gilt also
     * auch offline; der Einzelspieler-Host mit Cheats vergleicht den Namen, daher Name aus dem Spieler bzw. Namens-Cache).
     */
    public static boolean exempt(MinecraftServer server, @Nullable UUID owner) {
        if (owner == null) {
            return true;
        }
        net.minecraft.server.level.ServerPlayer online = server.getPlayerList().getPlayer(owner);
        NameAndId id = online != null ? online.nameAndId()
                : server.services().nameToIdCache().get(owner).orElse(new NameAndId(owner, ""));
        return server.getPlayerList().isOp(id);
    }

    /**
     * Ob der Loader an {@code pos} in {@code level} innerhalb des Limits seines Besitzers liegt: weniger als
     * {@code tuning.chunkLoaderMaxPerPlayer} laufwillige Loader desselben Besitzers stehen vor ihm in der Liste (ein noch
     * nicht eingetragener Loader steht hinter allen). Limit 0, Operatoren und Loader ohne Besitzer: immer.
     */
    public static boolean withinLimit(ServerLevel level, BlockPos pos, @Nullable UUID owner) {
        int limit = com.simplepads.SimplePads.config().tuning.chunkLoaderLimit();
        if (limit <= 0 || exempt(level.getServer(), owner)) {
            return true;
        }
        String self = key(level.dimension().identifier().toString(), pos);
        int before = 0;
        for (Map.Entry<String, Entry> e : of(level.getServer()).entries.entrySet()) {
            if (e.getKey().equals(self)) {
                break;
            }
            Entry other = e.getValue();
            if (owner.equals(other.owner().orElse(null)) && other.wantsToRun() && ++before >= limit) {
                return false;
            }
        }
        return true;
    }

    /** Ob der eingetragene Loader laufen soll: {@link #mayRun}, kein Redstone-Signal und innerhalb des Limits. */
    public static boolean shouldRun(ServerLevel level, Entry entry) {
        return mayRun(level, entry.owner().orElse(null)) && !entry.powered()
                && withinLimit(level, entry.pos(), entry.owner().orElse(null));
    }

    /** So viele nicht geladene Chunks weckt ein Abgleichlauf hoechstens. */
    public static final int MAX_WAKE_LOADS_PER_RUN = 8;

    /**
     * Gleicht alle Loader mit ihren Regeln ab (vom Server-Tick alle
     * {@value ChunkLoaderBlockEntity#CHECK_INTERVAL} Ticks): ein Loader, der laufen soll, aber keine
     * Chunks haelt (Besitzer wieder online, Schalter wieder an, Platz im Limit frei geworden), wird geweckt -
     * sein Chunk wird dazu einmal geladen. Einer, der nicht laufen soll (auch: per Redstone aus, ueber dem
     * Limit), gibt frei; sein Chunk ist dann ohnehin geladen. Abgeschaltete und Loader ueber dem Limit, die
     * nichts halten, bleiben unberuehrt (kein Laden alle 100 Ticks).
     *
     * @return wie viele Loader geweckt oder angehalten wurden
     */
    public static int reconcile(MinecraftServer server) {
        ChunkLoaderRegistry data = of(server);
        if (data.entries.isEmpty()) {
            return 0;
        }
        int changed = 0;
        int loads = 0;
        for (Entry entry : new ArrayList<>(data.entries.values())) {
            ResourceKey<Level> key = entry.levelKey();
            ServerLevel level = key == null ? null : server.getLevel(key);
            if (level == null) {
                continue;
            }
            boolean shouldRun = shouldRun(level, entry);
            if (shouldRun == entry.active()) {
                continue;
            }
            if (!shouldRun && level.getChunkSource().getChunkNow(entry.pos().getX() >> 4, entry.pos().getZ() >> 4) == null) {
                // Haelt laut Liste Chunks, ist aber nicht geladen: dann haelt er auch keine mehr.
                update(level, entry.pos(), entry.owner().orElse(null), entry.tier(), false, entry.powered());
                continue;
            }
            // getChunk laedt den Chunk, falls noetig (einmal beim Wecken). Pro Lauf nur wenige Ladungen, der Rest folgt
            // im naechsten Intervall (Join-Spitze bei vielen Loadern eines Besitzers).
            if (level.getChunkSource().getChunkNow(entry.pos().getX() >> 4, entry.pos().getZ() >> 4) == null) {
                if (loads >= MAX_WAKE_LOADS_PER_RUN) {
                    continue;
                }
                loads++;
            }
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
