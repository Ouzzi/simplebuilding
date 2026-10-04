package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import org.jetbrains.annotations.Nullable;

/**
 * Die Schallplatten der Dimensionen (Besitzer 2026-10-03, {@code McVersion.MUSIC_DISCS}): je Dimension eine Platte
 * mit bis zu vier Tracks. Track 1 ist die A-Seite, Track 2 die B-Seite ({@code _b_side}), Track 3/4
 * ({@code _track_3}, {@code _track_4}) gibt es nur, wenn der Besitzer Musik dafuer importiert hat. Ein Vorschlaghammer
 * an der abgelegten Platte schaltet zum naechsten vorhandenen Track ({@link DiscFlips}).
 *
 * <p>Die Songs ({@code data/simplebuilding/jukebox_song/<song>.json}: Laenge, Komparator-Stufe, Sound-Event
 * {@code simplebuilding:music_disc.<song>}) erzeugt die Datagen aus {@link #SONGS} ({@link #bootstrap}); der Klang liegt
 * in {@code assets/simplebuilding/sounds/records/<song>.ogg} ({@code "stream": true}). Die echten Stuecke legt der
 * Besitzer nach {@code musik/<datei>.mp3}; {@code tools/audio/import_discs.py} wandelt sie und traegt die Laengen hier in
 * {@link #SONGS} ein - fuer Track 3/4 entsteht der Track erst dadurch ({@code docs/ai/PLAN-SCHALLPLATTEN-2026-10-03.md}).
 */
public final class MusicDiscs {
    /** Hoechstens so viele Tracks je Platte. */
    public static final int MAX_TRACKS = 4;

    /**
     * Eine Platte: Song-Name (Track 1), Datei des Besitzers ohne Endung, Komparator-Stufe und die Laengen der Tracks in
     * Sekunden (Track 1 und 2 immer; Track 3/4 nur mit Laenge &gt; 0, eine {@code 0.0F} heisst "kein Track"). Die Zeilen in
     * {@link #SONGS} schreibt {@code tools/audio/import_discs.py} um.
     */
    public record Song(String name, String ownerFile, int comparator, float... lengths) {
        /** Song-Name von Track {@code track} (1..4). */
        public String trackName(int track) {
            return switch (track) {
                case 1 -> name;
                case 2 -> name + "_b_side";
                default -> name + "_track_" + track;
            };
        }

        /** Datei des Besitzers fuer Track {@code track} (ohne Endung). */
        public String trackFile(int track) {
            return switch (track) {
                case 1 -> ownerFile;
                case 2 -> ownerFile + "_alt";
                default -> ownerFile + "_" + track;
            };
        }

        /** Ob Track {@code track} (1..4) existiert. */
        public boolean hasTrack(int track) {
            return track >= 1 && track <= lengths.length && track <= MAX_TRACKS && lengths[track - 1] > 0.0F;
        }

        /** Laenge von Track {@code track} in Sekunden. */
        public float length(int track) {
            return lengths[track - 1];
        }

        /** Die vorhandenen Tracks (1..4), aufsteigend. */
        public List<Integer> tracks() {
            List<Integer> out = new ArrayList<>();
            for (int track = 1; track <= MAX_TRACKS; track++) {
                if (hasTrack(track)) {
                    out.add(track);
                }
            }
            return out;
        }

        /** Song-Name der B-Seite (Track 2). */
        public String bSide() {
            return trackName(2);
        }
    }

    /** End, Oberwelt 1, Oberwelt 2, Nether - in dieser Reihenfolge (Tabs, Testzentrale, Import-Skript). */
    public static final List<Song> SONGS = List.of(
            new Song("voidline", "end", 14, 120.0F, 120.0F, 140.0F),
            new Song("driftwood", "overworld1", 6, 180.0F, 180.0F),
            new Song("daybreak", "overworld2", 12, 120.0F, 120.0F),
            new Song("brimstone", "nether", 13, 150.0F, 150.0F));

    private MusicDiscs() {
    }

    /** Alle Song-Namen der vorhandenen Tracks (fuer Sound-Events und Songs), auch ohne das Feature. */
    public static List<String> songNames() {
        List<String> out = new ArrayList<>();
        for (Song song : SONGS) {
            for (int track : song.tracks()) {
                out.add(song.trackName(track));
            }
        }
        return out;
    }

    /** Der Song {@code simplebuilding:<song>} der Registry {@code jukebox_song}. */
    public static ResourceKey<JukeboxSong> songKey(String song) {
        return ResourceKey.create(Registries.JUKEBOX_SONG, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, song));
    }

    /** Das Sound-Event {@code simplebuilding:music_disc.<song>}. */
    public static Identifier soundId(String song) {
        return Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "music_disc." + song);
    }

    /** Datagen: je vorhandenem Track ein Song mit seinem registrierten Sound-Event. */
    public static void bootstrap(net.minecraft.data.worldgen.BootstrapContext<JukeboxSong> context) {
        for (Song song : SONGS) {
            for (int track : song.tracks()) {
                register(context, song.trackName(track), song.comparator(), song.length(track));
            }
        }
    }

    private static void register(net.minecraft.data.worldgen.BootstrapContext<JukeboxSong> context, String song, int comparator, float length) {
        net.minecraft.sounds.SoundEvent sound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(soundId(song));
        context.register(songKey(song), new JukeboxSong(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                net.minecraft.network.chat.Component.translatable("jukebox_song.simplebuilding." + song), length, comparator));
    }

    /** Eine Platte mit ihren vorhandenen Tracks (Item je Track, in Track-Reihenfolge). */
    public record Disc(Song song, List<Item> tracks) {
        /** Track 1 (A-Seite). */
        public Item aSide() {
            return tracks.get(0);
        }

        /** Track 2 (B-Seite). */
        public Item bSide() {
            return tracks.get(1);
        }
    }

    /** Die vier Platten mit ihren Tracks (leer ohne das Feature). */
    public static List<Disc> discs() {
        if (!com.simplebuilding.version.McVersion.MUSIC_DISCS || ModItems.MUSIC_DISC_VOIDLINE == null) {
            return List.of();
        }
        Item[][] fixed = {
                {ModItems.MUSIC_DISC_VOIDLINE, ModItems.MUSIC_DISC_VOIDLINE_B_SIDE},
                {ModItems.MUSIC_DISC_DRIFTWOOD, ModItems.MUSIC_DISC_DRIFTWOOD_B_SIDE},
                {ModItems.MUSIC_DISC_DAYBREAK, ModItems.MUSIC_DISC_DAYBREAK_B_SIDE},
                {ModItems.MUSIC_DISC_BRIMSTONE, ModItems.MUSIC_DISC_BRIMSTONE_B_SIDE}};
        List<Disc> out = new ArrayList<>();
        for (int i = 0; i < SONGS.size(); i++) {
            Song song = SONGS.get(i);
            List<Item> tracks = new ArrayList<>(List.of(fixed[i]));
            for (int track = 3; track <= MAX_TRACKS; track++) {
                Item extra = song.hasTrack(track) ? ModItems.MUSIC_DISC_EXTRA_TRACKS.get(song.trackName(track)) : null;
                if (extra != null) {
                    tracks.add(extra);
                }
            }
            out.add(new Disc(song, List.copyOf(tracks)));
        }
        return out;
    }

    /** Alle Platten, je Platte ihre Tracks hintereinander (leer ohne das Feature). */
    public static List<Item> items() {
        List<Item> out = new ArrayList<>();
        for (Disc disc : discs()) {
            out.addAll(disc.tracks());
        }
        return out;
    }

    /** Der naechste vorhandene Track nach {@code item} (nach dem letzten wieder Track 1), oder null fuer fremde Items. */
    public static @Nullable Item nextTrack(Item item) {
        for (Disc disc : discs()) {
            Item next = next(disc.tracks(), item);
            if (next != null) {
                return next;
            }
        }
        return null;
    }

    /** Der Nachfolger von {@code current} im Kreis {@code cycle}, oder null, wenn es nicht darin steht. */
    public static <T> @Nullable T next(List<T> cycle, T current) {
        int index = cycle.indexOf(current);
        return index < 0 || cycle.size() < 2 ? null : cycle.get((index + 1) % cycle.size());
    }
}
