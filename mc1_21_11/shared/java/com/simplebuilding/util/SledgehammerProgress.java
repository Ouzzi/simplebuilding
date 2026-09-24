package com.simplebuilding.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

/**
 * Wie weit eine abgebrochene Aufwertung mit dem Vorschlaghammer schon war, je Dimension und
 * Blockposition gespeichert ({@link SavedData}, ueberlebt Neustarts).
 *
 * <p><b>Was gespeichert wird.</b> Jeder Schlag 1..4 einer Aufwertung schreibt die Zahl der Schlaege
 * und den Ausgangsblock (etwa den verstaerkten Ofen). Wer spaeter - egal welcher Spieler - mit Hammer
 * und passendem Nugget weiterhaemmert, setzt dort fort: die Benutzung dauert nur noch die fehlenden
 * Schlaege. Der fuenfte Schlag (der Umbau) loescht den Eintrag.
 *
 * <p><b>Verfall.</b> Der Fortschritt bleibt, bis der Block abgebaut, ersetzt oder zu einem anderen
 * Block wird; Zustandswechsel desselben Blocks (Ofen brennt, Trichter gesperrt, Blickrichtung)
 * behalten ihn. Kein Zeitablauf: die Schlaege haben Haltbarkeit gekostet, die soll nicht verfallen.
 * Geprueft wird einmal pro Sekunde fuer geladene Positionen und bei jedem neuen Anlauf; ungeladene
 * Positionen behalten ihren Eintrag, bis sie wieder geladen sind.
 *
 * <p><b>Anzeige.</b> Alle Spieler in 32 Bloecken Umkreis sehen den Fortschritt als Risse im Block -
 * dieselben Stufen wie beim Abbauen: 1, 3, 5 und 7 von 0..9 nach Schlag 1..4. Der Client wirft
 * solche Risse nach 400 Ticks ohne Auffrischung weg, und wer spaeter in die Naehe kommt, hat nie
 * ein Paket bekommen; deshalb sendet {@link #tick} sie alle {@value #REBROADCAST_TICKS} Ticks neu.
 * Jede Position hat eine eigene, negative Riss-Kennung, damit sie nie mit den Abbaurissen eines
 * Spielers (dessen Entity-Id, immer positiv) zusammenfaellt.
 */
public final class SledgehammerProgress extends SavedData {

    /** Alle so viele Ticks gehen die Risse erneut an die Spieler in der Naehe. */
    public static final int REBROADCAST_TICKS = 100;
    /** Alle so viele Ticks werden geladene Eintraege gegen den Block an ihrer Position geprueft. */
    public static final int VALIDATE_TICKS = 20;

    /** Ein gespeicherter Stand: so viele Schlaege auf diesen Ausgangsblock an dieser Position. */
    public record Entry(BlockPos pos, Block block, int hits) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(Entry::block),
                Codec.INT.fieldOf("hits").forGetter(Entry::hits)
        ).apply(i, Entry::new));
    }

    public static final Codec<SledgehammerProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(SledgehammerProgress::entries)
    ).apply(i, SledgehammerProgress::new));

    // MC 1.21.11: SavedDataType nimmt einen String als Dateinamen (26.2: eine Identifier-Id). Der
    // Datenfix-Typ darf nicht null sein, sonst verwirft das Laden die Datei; Befehlsspeicher-Daten
    // fasst kein Fix an.
    public static final SavedDataType<SledgehammerProgress> TYPE = new SavedDataType<>(
            "simplebuilding_sledgehammer_progress",
            SledgehammerProgress::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    /** Naechste freie Riss-Kennung; negativ, siehe Klassenkommentar. Nicht gespeichert. */
    private static final AtomicInteger NEXT_CRACK_ID = new AtomicInteger(-1_000_000);

    private final Map<BlockPos, Entry> entries = new LinkedHashMap<>();
    private final Map<BlockPos, Integer> crackIds = new HashMap<>();

    public SledgehammerProgress() {
    }

    private SledgehammerProgress(List<Entry> loaded) {
        for (Entry entry : loaded) {
            if (entry.hits() > 0) {
                entries.put(entry.pos().immutable(), entry);
            }
        }
    }

    public List<Entry> entries() {
        return new ArrayList<>(entries.values());
    }

    // =====================================================================================
    // ZUGRIFF (nur Server)
    // =====================================================================================

    private static @Nullable SledgehammerProgress existing(ServerLevel level) {
        return level.getDataStorage().get(TYPE);
    }

    /**
     * Die Schlaege, die an {@code pos} auf den Ausgangsblock {@code from} schon gefallen sind, oder 0.
     * Ein Eintrag fuer einen anderen Block ist verfallen und wird dabei entfernt.
     */
    public static int hits(ServerLevel level, BlockPos pos, Block from) {
        SledgehammerProgress data = existing(level);
        if (data == null) {
            return 0;
        }
        Entry entry = data.entries.get(pos);
        if (entry == null) {
            return 0;
        }
        if (entry.block() != from || !level.getBlockState(pos).is(from)) {
            data.remove(level, pos);
            return 0;
        }
        return entry.hits();
    }

    /** Merkt sich {@code hits} Schlaege auf {@code from} an {@code pos} und zeigt die Risse. */
    public static void record(ServerLevel level, BlockPos pos, Block from, int hits) {
        if (hits <= 0) {
            clear(level, pos);
            return;
        }
        SledgehammerProgress data = level.getDataStorage().computeIfAbsent(TYPE);
        BlockPos key = pos.immutable();
        data.entries.put(key, new Entry(key, from, hits));
        data.setDirty();
        data.broadcast(level, key, hits);
    }

    /** Vergisst den Stand an {@code pos} und nimmt die Risse weg. */
    public static void clear(ServerLevel level, BlockPos pos) {
        SledgehammerProgress data = existing(level);
        if (data != null) {
            data.remove(level, pos);
        }
    }

    /** Die Riss-Stufe (0..9) nach so vielen Schlaegen, oder -1 fuer "keine Risse". */
    public static int crackStage(int hits) {
        return hits <= 0 ? -1 : Math.min(9, hits * 2 - 1);
    }

    /** Jeder Server-Tick, von allen Loadern: Eintraege pruefen und Risse auffrischen. */
    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        boolean validate = now % VALIDATE_TICKS == 0;
        boolean rebroadcast = now % REBROADCAST_TICKS == 0;
        if (!validate && !rebroadcast) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            SledgehammerProgress data = existing(level);
            if (data == null || data.entries.isEmpty()) {
                continue;
            }
            Iterator<Map.Entry<BlockPos, Entry>> it = data.entries.entrySet().iterator();
            while (it.hasNext()) {
                Entry entry = it.next().getValue();
                if (!level.isLoaded(entry.pos())) {
                    continue;
                }
                if (!level.getBlockState(entry.pos()).is(entry.block())) {
                    it.remove();
                    data.setDirty();
                    Integer id = data.crackIds.remove(entry.pos());
                    if (id != null) {
                        level.destroyBlockProgress(id, entry.pos(), -1);
                    }
                } else if (rebroadcast) {
                    data.broadcast(level, entry.pos(), entry.hits());
                }
            }
        }
    }

    private void remove(ServerLevel level, BlockPos pos) {
        if (entries.remove(pos) != null) {
            setDirty();
        }
        Integer id = crackIds.remove(pos);
        if (id != null) {
            level.destroyBlockProgress(id, pos, -1);
        }
    }

    private void broadcast(ServerLevel level, BlockPos pos, int hits) {
        int id = crackIds.computeIfAbsent(pos, p -> NEXT_CRACK_ID.getAndDecrement());
        level.destroyBlockProgress(id, pos, crackStage(hits));
    }
}
