package com.simplemusic.sheet;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * The grid (piano roll) of the sheet music editor (owner choice WB24-4: E2 A classic grid, E3 B split with the code,
 * live both ways). Plain Java without game classes. The note code stays the truth: a grid is only ever read from a
 * compiled {@link NoteCode.Song} and written back as flat note code ({@link #toCode()}), which the editor parses again.
 *
 * <p>Per instrument a step holds up to 25 notes (note block notes 0-24) with their volume. {@link #toCode()} writes one
 * track per instrument ({@code tempo}, chords {@code [..]}, {@code @vol} where it changes, sound instruments as aliases
 * {@code $s1 = sound id}); a step with more than {@link NoteCode#MAX_CHORD} tones of one instrument (two code tracks with
 * the same instrument) gets a second voice {@code $harp_2 = harp}. Comments, aliases and repetitions are written out flat.
 */
public final class NoteGrid {
    public static final int NOTES = 25;

    public enum Toggle { ADDED, REMOVED, REFUSED }

    private final int tempo;
    private final List<NoteCode.Instrument> instruments = new ArrayList<>();
    /** Per instrument: step -> volume per note (0 = no note). */
    private final List<TreeMap<Integer, float[]>> cells = new ArrayList<>();
    private int length;

    private NoteGrid(int tempo, int length) {
        this.tempo = tempo;
        this.length = length;
    }

    public static NoteGrid empty() {
        return new NoteGrid(NoteCode.DEFAULT_TEMPO, 0);
    }

    /** The grid of valid code, or null when the code has problems (the editor then keeps its last grid). */
    public static NoteGrid fromCode(String code, NoteCode.SoundPolicy policy) {
        NoteCode.Result r = NoteCode.parse(code, policy);
        return r.ok() ? of(r.song()) : null;
    }

    public static NoteGrid of(NoteCode.Song song) {
        NoteGrid grid = new NoteGrid(song.tempo(), song.steps());
        int[] index = new int[song.instruments().size()];
        for (int i = 0; i < index.length; i++) index[i] = -1;
        for (int s = 0; s < song.steps(); s++) {
            for (NoteCode.Tone tone : song.tonesAt(s)) {
                int i = tone.instrument();
                if (index[i] < 0) index[i] = grid.index(song.instruments().get(i));
                grid.cells.get(index[i]).computeIfAbsent(s, k -> new float[NOTES])[tone.note()] = tone.volume();
            }
        }
        return grid;
    }

    public int tempo() {
        return tempo;
    }

    /** Steps of the song (its length, trailing rests included). */
    public int length() {
        return length;
    }

    /** The instruments with notes or used before, in code order. */
    public List<NoteCode.Instrument> instruments() {
        return List.copyOf(instruments);
    }

    public boolean has(NoteCode.Instrument instrument, int step, int note) {
        int i = instruments.indexOf(instrument);
        if (i < 0 || note < 0 || note >= NOTES) return false;
        float[] notes = cells.get(i).get(step);
        return notes != null && notes[fixed(instrument, note)] > 0;
    }

    /** Click on a cell: a note there goes away, otherwise one is set (refused past the chord, step or track limits). */
    public Toggle toggle(NoteCode.Instrument instrument, int step, int note) {
        if (step < 0 || note < 0 || note >= NOTES) return Toggle.REFUSED;
        if (remove(instrument, step, note)) return Toggle.REMOVED;
        int i = instruments.indexOf(instrument);
        if (i < 0 && voices() >= NoteCode.MAX_TRACKS) return Toggle.REFUSED;
        if (i >= 0 && count(cells.get(i).get(step)) >= NoteCode.MAX_CHORD) return Toggle.REFUSED;
        if (tonesAt(step) >= NoteCode.MAX_TONES_PER_STEP) return Toggle.REFUSED;
        if ((long) Math.max(length, step + 1) * tempo > NoteCode.MAX_TICKS) return Toggle.REFUSED;
        if (i < 0) i = index(instrument);
        float[] notes = cells.get(i).computeIfAbsent(step, k -> new float[NOTES]);
        float volume = 1.0F;
        for (float v : notes) if (v > 0) volume = v; // a chord keeps one volume
        notes[fixed(instrument, note)] = volume;
        length = Math.max(length, step + 1);
        return Toggle.ADDED;
    }

    /** Right click: removes the note if there is one. */
    public boolean remove(NoteCode.Instrument instrument, int step, int note) {
        if (!has(instrument, step, note)) return false;
        TreeMap<Integer, float[]> map = cells.get(instruments.indexOf(instrument));
        float[] notes = map.get(step);
        notes[fixed(instrument, note)] = 0;
        if (count(notes) == 0) map.remove(step);
        return true;
    }

    /** Flat note code of the grid; parsing it gives the same tones (instrument, step, note, volume). */
    public String toCode() {
        StringBuilder head = new StringBuilder("tempo ").append(tempo).append('\n');
        StringBuilder body = new StringBuilder();
        boolean padded = false;
        int sounds = 0;
        for (int i = 0; i < instruments.size(); i++) {
            NoteCode.Instrument instrument = instruments.get(i);
            TreeMap<Integer, float[]> map = cells.get(i);
            if (map.isEmpty()) continue;
            String label = instrument.name();
            String target = instrument.name();
            if (instrument.kind() == NoteCode.Kind.SOUND) {
                label = "$s" + ++sounds;
                target = "sound " + instrument.name();
                head.append(label).append(" = ").append(target).append('\n');
            }
            int voices = voices(map);
            for (int v = 0; v < voices; v++) {
                String name = label;
                if (v > 0) {
                    name = (label.startsWith("$") ? label : "$" + label) + "_" + (v + 1);
                    head.append(name).append(" = ").append(target).append('\n');
                }
                int last = padded ? map.lastKey() : length - 1;
                padded = true;
                body.append(name).append(':');
                float volume = 1.0F;
                for (int s = 0; s <= last; s++) {
                    List<Integer> notes = voice(map.get(s), v);
                    body.append(' ');
                    if (notes.isEmpty()) {
                        body.append('.');
                        continue;
                    }
                    float vol = map.get(s)[notes.get(0)];
                    if (vol != volume) {
                        body.append("@vol ").append(vol).append(' ');
                        volume = vol;
                    }
                    if (notes.size() > 1) body.append('[');
                    for (int n = 0; n < notes.size(); n++) {
                        if (n > 0) body.append(' ');
                        body.append(NoteCode.nameOf(notes.get(n), instrument.octave()));
                    }
                    if (notes.size() > 1) body.append(']');
                }
                body.append('\n');
            }
        }
        return head.append(body).toString().strip();
    }

    // ---------------------------------------------------------------- helpers

    private int index(NoteCode.Instrument instrument) {
        int i = instruments.indexOf(instrument);
        if (i >= 0) return i;
        instruments.add(instrument);
        cells.add(new TreeMap<>());
        return instruments.size() - 1;
    }

    /** A head plays one fixed sound: its tones always sit on note 12. */
    private static int fixed(NoteCode.Instrument instrument, int note) {
        return instrument.tunable() ? note : 12;
    }

    private static int count(float[] notes) {
        int n = 0;
        if (notes != null) for (float v : notes) if (v > 0) n++;
        return n;
    }

    private int tonesAt(int step) {
        int n = 0;
        for (TreeMap<Integer, float[]> map : cells) n += count(map.get(step));
        return n;
    }

    private static int voices(TreeMap<Integer, float[]> map) {
        int max = 0;
        for (float[] notes : map.values()) max = Math.max(max, count(notes));
        return Math.max(1, (max + NoteCode.MAX_CHORD - 1) / NoteCode.MAX_CHORD);
    }

    private int voices() {
        int n = 0;
        for (TreeMap<Integer, float[]> map : cells) if (!map.isEmpty()) n += voices(map);
        return n;
    }

    /** The notes of voice {@code v} (low to high, MAX_CHORD per voice; the voice's volume is its first note's) of a step. */
    private static List<Integer> voice(float[] notes, int v) {
        List<Integer> all = new ArrayList<>();
        if (notes != null) for (int n = 0; n < NOTES; n++) if (notes[n] > 0) all.add(n);
        int from = Math.min(all.size(), v * NoteCode.MAX_CHORD);
        return all.subList(from, Math.min(all.size(), from + NoteCode.MAX_CHORD));
    }
}
