package com.simplebuilding.blueprint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

/**
 * Die laufenden Bauauftraege der Blaupause, je Dimension gespeichert ({@link SavedData}), damit ein
 * Auftrag Logout und Serverneustart ueberlebt.
 *
 * <p><b>Was gespeichert wird.</b> Je Spieler ein Eintrag: welcher Code gebaut wird (als Hash, nicht
 * der ganze Text), wo ({@code target}), in welcher Drehung, wie weit der Auftrag in seiner
 * Bau-Reihenfolge schon war ({@code index}) und wie viele Bloecke er dabei gesetzt hat. Der Titel
 * steht fuer den Hinweis dabei. Geschrieben wird nach jeder Scheibe; ein fertiger, abgebrochener
 * oder durch einen neuen Bau ersetzter Auftrag loescht seinen Eintrag.
 *
 * <p><b>Fortsetzen.</b> {@link BlueprintBuilder#tick} setzt einen gespeicherten Auftrag fort, sobald
 * der Spieler wieder den Baustab in der Haupthand und eine signierte Blaupause mit <b>demselben</b>
 * Code in der Nebenhand haelt - die Blaupause darf eine Kopie sein, es zaehlt der Code. Die
 * Reihenfolge ist je Modell fest, also setzt {@code index} genau dort an, wo der Auftrag stand;
 * was inzwischen schon steht, zaehlt ohnehin als "schon richtig".
 */
public final class BlueprintJobs extends SavedData {

    /** Ein unterbrochener Auftrag. {@code rotation} ist {@code Rotation#ordinal()}. */
    public record Pending(UUID player, String codeHash, String title, BlockPos target, int rotation, int index, int placed) {
        public static final Codec<Pending> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(Pending::player),
                Codec.STRING.fieldOf("code_hash").forGetter(Pending::codeHash),
                Codec.STRING.optionalFieldOf("title", "").forGetter(Pending::title),
                BlockPos.CODEC.fieldOf("target").forGetter(Pending::target),
                Codec.INT.fieldOf("rotation").forGetter(Pending::rotation),
                Codec.INT.fieldOf("index").forGetter(Pending::index),
                Codec.INT.optionalFieldOf("placed", 0).forGetter(Pending::placed)
        ).apply(i, Pending::new));
    }

    public static final Codec<BlueprintJobs> CODEC = RecordCodecBuilder.create(i -> i.group(
            Pending.CODEC.listOf().optionalFieldOf("jobs", List.of()).forGetter(BlueprintJobs::pending)
    ).apply(i, BlueprintJobs::new));

    // MC 26.2: SavedDataType nimmt eine Identifier-Id (1.21.11: einen String). Der Datenfix-Typ darf
    // nicht null sein, sonst verwirft das Laden die Datei; Befehlsspeicher-Daten fasst kein Fix an.
    public static final SavedDataType<BlueprintJobs> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("simplebuilding", "blueprint_jobs"),
            BlueprintJobs::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, Pending> jobs = new LinkedHashMap<>();

    public BlueprintJobs() {
    }

    private BlueprintJobs(List<Pending> loaded) {
        for (Pending pending : loaded) {
            jobs.put(pending.player(), pending);
        }
    }

    public List<Pending> pending() {
        return new ArrayList<>(jobs.values());
    }

    /** Der gespeicherte Auftrag des Spielers in dieser Dimension, oder {@code null}. */
    public static @Nullable Pending get(ServerLevel level, UUID player) {
        BlueprintJobs data = level.getDataStorage().get(TYPE);
        return data == null ? null : data.jobs.get(player);
    }

    /** Merkt sich (oder aktualisiert) den Stand eines laufenden Auftrags. */
    public static void save(ServerLevel level, Pending pending) {
        BlueprintJobs data = level.getDataStorage().computeIfAbsent(TYPE);
        if (!pending.equals(data.jobs.put(pending.player(), pending))) {
            data.setDirty();
        }
    }

    /** Vergisst den Auftrag des Spielers in dieser Dimension. */
    public static void clear(ServerLevel level, UUID player) {
        BlueprintJobs data = level.getDataStorage().get(TYPE);
        if (data != null && data.jobs.remove(player) != null) {
            data.setDirty();
        }
    }

    private static final Map<String, String> HASHES = new LinkedHashMap<>(8, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 8;
        }
    };

    /**
     * Fingerabdruck eines Codes (SHA-256, hex). Gemerkt fuer die letzten acht Codes, weil der
     * Baustab jeden Tick fragt, ob die Blaupause in der Nebenhand zum gespeicherten Auftrag passt.
     */
    public static String hash(String code) {
        synchronized (HASHES) {
            String known = HASHES.get(code);
            if (known != null) {
                return known;
            }
        }
        String hex;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
            hex = HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            hex = Integer.toHexString(code.hashCode()) + ":" + code.length();
        }
        synchronized (HASHES) {
            HASHES.put(code, hex);
        }
        return hex;
    }
}
