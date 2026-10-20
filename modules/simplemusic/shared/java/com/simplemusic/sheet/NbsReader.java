package com.simplemusic.sheet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reads a Note Block Studio song ({@code .nbs}, the classic format and the versioned Open Note Block Studio format
 * 1-5) and writes it as {@link NoteCode} text (concept §3.2: the import only creates text, the server only ever sees
 * the code). Plain Java, no game classes.
 *
 * <p>Mapping: NBS keys 33-57 are the 25 note block notes of every instrument (keys outside are folded by octaves);
 * one track per instrument, simultaneous notes become chords of up to {@link NoteCode#MAX_CHORD} (more open a second
 * track of the same instrument). Custom instruments, volume, panning and fine pitch are dropped. What does not fit the
 * note code limits (tracks, tones per step, 10 minutes) is left out and counted in {@link Import#dropped}.
 */
public final class NbsReader {
    /** NBS instrument ids 0-15 in order. */
    public static final List<String> INSTRUMENTS = List.of("harp", "bass", "basedrum", "snare", "hat", "guitar", "flute",
            "bell", "chime", "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit", "banjo", "pling");
    private static final int BAR = 16;

    /** The written code, the song title and how many notes had to be left out. */
    public record Import(String code, String title, int notes, int dropped) {}

    private NbsReader() {}

    public static Import read(byte[] data) {
        ByteBuffer in = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        try {
            int first = Short.toUnsignedInt(in.getShort());
            int version = 0;
            int vanillaInstruments = 10;
            if (first == 0) {
                version = Byte.toUnsignedInt(in.get());
                vanillaInstruments = Byte.toUnsignedInt(in.get());
                if (version < 1 || version > 5) throw new IllegalArgumentException("unsupported NBS version " + version);
                if (version >= 3) in.getShort(); // song length
            }
            in.getShort(); // layer count
            String name = string(in);
            String author = string(in);
            string(in); // original author
            string(in); // description
            int tempo100 = Short.toUnsignedInt(in.getShort());
            in.get(); // auto saving
            in.get(); // auto saving minutes
            in.get(); // time signature
            in.getInt(); // minutes spent
            in.getInt(); // left clicks
            in.getInt(); // right clicks
            in.getInt(); // blocks added
            in.getInt(); // blocks removed
            string(in); // imported file name
            if (version >= 4) {
                in.get(); // loop
                in.get(); // max loop count
                in.getShort(); // loop start
            }
            // tick -> instrument -> notes
            TreeMap<Integer, Map<Integer, List<Integer>>> ticks = new TreeMap<>();
            int tick = -1;
            int total = 0;
            int dropped = 0;
            while (true) {
                int jump = Short.toUnsignedInt(in.getShort());
                if (jump == 0) break;
                tick += jump;
                int layer = -1;
                while (true) {
                    int layerJump = Short.toUnsignedInt(in.getShort());
                    if (layerJump == 0) break;
                    layer += layerJump;
                    int instrument = Byte.toUnsignedInt(in.get());
                    int key = Byte.toUnsignedInt(in.get());
                    if (version >= 4) {
                        in.get(); // velocity
                        in.get(); // panning
                        in.getShort(); // fine pitch
                    }
                    total++;
                    if (instrument >= Math.min(vanillaInstruments, INSTRUMENTS.size()) && instrument >= 10) {
                        dropped++; // custom instrument
                        continue;
                    }
                    int note = key - 33;
                    while (note < 0) note += 12;
                    while (note > 24) note -= 12;
                    ticks.computeIfAbsent(tick, t -> new TreeMap<>()).computeIfAbsent(instrument, i -> new ArrayList<>()).add(note);
                }
            }
            Written written = write(name, author, tempo100, ticks, dropped);
            return new Import(written.code(), name, total, written.dropped());
        } catch (java.nio.BufferUnderflowException e) {
            throw new IllegalArgumentException("truncated NBS file", e);
        }
    }

    private static String string(ByteBuffer in) {
        int length = in.getInt();
        if (length < 0 || length > in.remaining()) throw new IllegalArgumentException("bad NBS string");
        byte[] bytes = new byte[length];
        in.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** Game ticks per note code step for an NBS tempo (song ticks per second x 100), clamped to the note code range. */
    public static int tempo(int tempo100) {
        if (tempo100 <= 0) return NoteCode.DEFAULT_TEMPO;
        return Math.max(NoteCode.MIN_TEMPO, Math.min(NoteCode.MAX_TEMPO, Math.round(2000.0F / tempo100)));
    }

    private static final class Track {
        final String instrument;
        final String alias;
        final TreeMap<Integer, List<Integer>> steps = new TreeMap<>();

        Track(String instrument, String alias) {
            this.instrument = instrument;
            this.alias = alias;
        }
    }

    private record Written(String code, int dropped) {}

    private static Written write(String name, String author, int tempo100, TreeMap<Integer, Map<Integer, List<Integer>>> ticks,
                                 int droppedBefore) {
        int tempo = tempo(tempo100);
        int maxSteps = NoteCode.MAX_TICKS / tempo;
        List<Track> tracks = new ArrayList<>();
        int dropped = droppedBefore;
        int steps = 0;
        for (Map.Entry<Integer, Map<Integer, List<Integer>>> t : ticks.entrySet()) {
            int step = t.getKey();
            if (step >= maxSteps) {
                for (List<Integer> notes : t.getValue().values()) dropped += notes.size();
                continue;
            }
            int inStep = 0;
            for (Map.Entry<Integer, List<Integer>> e : t.getValue().entrySet()) {
                String instrument = INSTRUMENTS.get(Math.min(e.getKey(), INSTRUMENTS.size() - 1));
                List<Integer> notes = e.getValue().stream().distinct().sorted().toList();
                for (int i = 0; i < notes.size(); i += NoteCode.MAX_CHORD) {
                    List<Integer> chord = new ArrayList<>(notes.subList(i, Math.min(notes.size(), i + NoteCode.MAX_CHORD)));
                    int room = NoteCode.MAX_TONES_PER_STEP - inStep;
                    if (room <= 0) {
                        dropped += chord.size();
                        continue;
                    }
                    if (chord.size() > room) {
                        dropped += chord.size() - room;
                        chord = chord.subList(0, room);
                    }
                    Track track = trackFor(tracks, instrument, step, i / NoteCode.MAX_CHORD);
                    if (track == null) {
                        dropped += chord.size();
                        continue;
                    }
                    track.steps.put(step, chord);
                    inStep += chord.size();
                    steps = Math.max(steps, step + 1);
                }
            }
        }
        StringBuilder out = new StringBuilder();
        out.append("# ").append(clean(name.isBlank() ? "Untitled" : name));
        if (!author.isBlank()) out.append(" - ").append(clean(author));
        out.append(" (imported from Note Block Studio)\n");
        out.append("tempo ").append(tempo).append('\n');
        for (Track track : tracks) out.append(track.alias).append(" = ").append(track.instrument).append('\n');
        for (int bar = 0; bar < steps; bar += BAR) {
            out.append('\n');
            for (Track track : tracks) {
                out.append(track.alias).append(':');
                for (int s = bar; s < Math.min(steps, bar + BAR); s++) {
                    List<Integer> chord = track.steps.get(s);
                    out.append(' ');
                    if (chord == null) {
                        out.append('.');
                    } else if (chord.size() == 1) {
                        out.append(note(track, chord.get(0)));
                    } else {
                        out.append('[');
                        for (int i = 0; i < chord.size(); i++) out.append(i == 0 ? "" : " ").append(note(track, chord.get(i)));
                        out.append(']');
                    }
                }
                out.append('\n');
            }
        }
        return new Written(out.toString(), dropped);
    }

    private static String note(Track track, int note) {
        return NoteCode.nameOf(note, NoteCode.NOTE_INSTRUMENTS.getOrDefault(track.instrument, 0));
    }

    /** The n-th track of an instrument that is still free at that step; null when the track limit is reached. */
    private static Track trackFor(List<Track> tracks, String instrument, int step, int index) {
        int seen = 0;
        for (Track track : tracks) {
            if (!track.instrument.equals(instrument)) continue;
            if (seen++ == index && !track.steps.containsKey(step)) return track;
        }
        if (tracks.size() >= NoteCode.MAX_TRACKS) return null;
        long same = tracks.stream().filter(t -> t.instrument.equals(instrument)).count();
        Track track = new Track(instrument, "$" + instrument + (same == 0 ? "" : String.valueOf(same + 1)));
        tracks.add(track);
        return track;
    }

    private static String clean(String text) {
        return text.replaceAll("[\\r\\n]", " ").strip();
    }
}
