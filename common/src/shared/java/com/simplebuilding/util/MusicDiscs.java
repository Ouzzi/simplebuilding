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
 * und zu jeder eine B-Seite, die ein Vorschlaghammer an der abgelegten Platte hervorholt ({@link DiscFlips}).
 *
 * <p>Die Songs ({@code data/simplebuilding/jukebox_song/<song>.json}: Laenge, Komparator-Stufe, Sound-Event
 * {@code simplebuilding:music_disc.<song>}) erzeugt die Datagen aus {@link #SONGS} ({@link #bootstrap}); der Klang liegt
 * in {@code assets/simplebuilding/sounds/records/<song>.ogg} ({@code "stream": true}). Die echten Stuecke legt der
 * Besitzer spaeter nach {@code musik/<datei>.mp3}; {@code tools/audio/import_discs.py} wandelt sie und traegt die
 * Laengen hier in {@link #SONGS} und in die erzeugten Song-Dateien ein ({@code docs/ai/PLAN-SCHALLPLATTEN-2026-10-03.md}).
 */
public final class MusicDiscs {
    /**
     * Eine Platte: Song-Name (A-Seite), Datei des Besitzers ohne Endung, Komparator-Stufe, Laenge der A- und der
     * B-Seite in Sekunden (die Zeilen in {@link #SONGS} schreibt {@code tools/audio/import_discs.py} um).
     */
    public record Song(String name, String ownerFile, int comparator, float aLength, float bLength) {
        /** Song-Name der B-Seite. */
        public String bSide() {
            return name + "_b_side";
        }
    }

    /** End, Oberwelt 1, Oberwelt 2, Nether - in dieser Reihenfolge (Tabs, Testzentrale, Import-Skript). */
    public static final List<Song> SONGS = List.of(
            new Song("voidline", "end", 14, 3.0F, 3.0F),
            new Song("driftwood", "overworld1", 6, 3.0F, 3.0F),
            new Song("daybreak", "overworld2", 12, 3.0F, 3.0F),
            new Song("brimstone", "nether", 13, 3.0F, 3.0F));

    private MusicDiscs() {
    }

    /** Alle Song-Namen, A- und B-Seiten (fuer Sound-Events und Songs), auch ohne das Feature. */
    public static List<String> songNames() {
        List<String> out = new ArrayList<>();
        for (Song song : SONGS) {
            out.add(song.name());
            out.add(song.bSide());
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

    /** Datagen: die acht Songs (A- und B-Seite je Platte) mit ihren registrierten Sound-Events. */
    public static void bootstrap(net.minecraft.data.worldgen.BootstrapContext<JukeboxSong> context) {
        for (Song song : SONGS) {
            register(context, song.name(), song.comparator(), song.aLength());
            register(context, song.bSide(), song.comparator(), song.bLength());
        }
    }

    private static void register(net.minecraft.data.worldgen.BootstrapContext<JukeboxSong> context, String song, int comparator, float length) {
        net.minecraft.sounds.SoundEvent sound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(soundId(song));
        context.register(songKey(song), new JukeboxSong(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                net.minecraft.network.chat.Component.translatable("jukebox_song.simplebuilding." + song), length, comparator));
    }

    /** Eine Platte mit ihrer B-Seite. */
    public record Disc(Song song, Item aSide, Item bSide) {
    }

    /** Die vier Platten mit B-Seite (leer ohne das Feature). */
    public static List<Disc> discs() {
        if (!com.simplebuilding.version.McVersion.MUSIC_DISCS || ModItems.MUSIC_DISC_VOIDLINE == null) {
            return List.of();
        }
        return List.of(
                new Disc(SONGS.get(0), ModItems.MUSIC_DISC_VOIDLINE, ModItems.MUSIC_DISC_VOIDLINE_B_SIDE),
                new Disc(SONGS.get(1), ModItems.MUSIC_DISC_DRIFTWOOD, ModItems.MUSIC_DISC_DRIFTWOOD_B_SIDE),
                new Disc(SONGS.get(2), ModItems.MUSIC_DISC_DAYBREAK, ModItems.MUSIC_DISC_DAYBREAK_B_SIDE),
                new Disc(SONGS.get(3), ModItems.MUSIC_DISC_BRIMSTONE, ModItems.MUSIC_DISC_BRIMSTONE_B_SIDE));
    }

    /** Alle acht Platten, je A-Seite gefolgt von ihrer B-Seite (leer ohne das Feature). */
    public static List<Item> items() {
        List<Item> out = new ArrayList<>();
        for (Disc disc : discs()) {
            out.add(disc.aSide());
            out.add(disc.bSide());
        }
        return out;
    }

    /** Die andere Seite von {@code item}, oder null, wenn es keine Platte der Mod ist. */
    public static @Nullable Item otherSide(Item item) {
        for (Disc disc : discs()) {
            if (item == disc.aSide()) {
                return disc.bSide();
            }
            if (item == disc.bSide()) {
                return disc.aSide();
            }
        }
        return null;
    }
}
