package com.simplemusic.sheet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The text note code of a sheet of music (concept docs/ai/KONZEPT-NOTENBLATT-2026-10-19.md §3.1, owner decision
 * KONZ20 N2 e1). Plain Java without game classes: the same parser runs in the editor (colouring, problems, test
 * listening) and on the server (all-or-nothing check before saving, the jukebox's composition discs).
 *
 * <pre>
 * # comment
 * tempo 4                        ticks per step (1-20; 4 = 5 steps per second)
 * $h = harp                      alias: a note block instrument
 * $k = sound minecraft:entity.cat.ambient   alias: any sound (server list, blocklist)
 * $h: c4 e4 g4 - c5 . . g4       track: notes, - = hold, . = rest
 * bass: c3 . c3 . [c3 g3] .      [..] = chord (up to 4 tones); a bare instrument name is a track too
 * $h: x2 { c4 d4 } @vol 0.6 e4   inline repetition, volume of the following notes (0.1-1.0)
 * x2 {                           block repetition: every track is padded to the same length first,
 *   $h: c4 d4                    the block's lines are repeated as a section
 * }
 * </pre>
 *
 * Lines with the same label continue the same track; different labels play in parallel, step n of every track sounds
 * at the same time. Notes are written at the pitch they sound: each instrument covers its two note block octaves
 * (harp f#3-f#5, bass f#1-f#3, flute f#4-f#6, bell f#5-f#7 ...); a number 0-24 is the raw note block note.
 * Limits: {@link #MAX_TRACKS} tracks, {@link #MAX_TONES_PER_STEP} tones per step in total, {@link #MAX_TICKS}
 * ticks (10 minutes), {@link #MAX_CODE_LENGTH} characters.
 */
public final class NoteCode {
    public static final int MAX_CODE_LENGTH = 32000;
    public static final int MAX_TITLE_LENGTH = 32;
    public static final int MAX_TRACKS = 16;
    public static final int MAX_TONES_PER_STEP = 8;
    public static final int MAX_CHORD = 4;
    public static final int MAX_REPEAT = 64;
    /** 10 minutes. */
    public static final int MAX_TICKS = 12000;
    public static final int MIN_TEMPO = 1, MAX_TEMPO = 20, DEFAULT_TEMPO = 4;

    public static final byte STYLE_TEXT = 0, STYLE_COMMENT = 1, STYLE_INSTRUMENT = 2, STYLE_NOTE = 3, STYLE_KEYWORD = 4,
            STYLE_NUMBER = 5, STYLE_OPERATOR = 6, STYLE_ERROR = 7, STYLE_REST = 8;

    /** Note block instruments by their Vanilla name and their octave shift against the harp (f#3-f#5). */
    public static final Map<String, Integer> NOTE_INSTRUMENTS = new LinkedHashMap<>();
    /** Mob head instruments of the Vanilla note block: one fixed sound, the pitch is ignored. */
    public static final List<String> HEAD_INSTRUMENTS = List.of("zombie", "skeleton", "creeper", "dragon", "wither_skeleton", "piglin");

    static {
        NOTE_INSTRUMENTS.put("harp", 0);
        NOTE_INSTRUMENTS.put("basedrum", 0);
        NOTE_INSTRUMENTS.put("snare", 0);
        NOTE_INSTRUMENTS.put("hat", 0);
        NOTE_INSTRUMENTS.put("bass", -2);
        NOTE_INSTRUMENTS.put("flute", 1);
        NOTE_INSTRUMENTS.put("bell", 2);
        NOTE_INSTRUMENTS.put("guitar", -1);
        NOTE_INSTRUMENTS.put("chime", 2);
        NOTE_INSTRUMENTS.put("xylophone", 2);
        NOTE_INSTRUMENTS.put("iron_xylophone", 0);
        NOTE_INSTRUMENTS.put("cow_bell", 1);
        NOTE_INSTRUMENTS.put("didgeridoo", -2);
        NOTE_INSTRUMENTS.put("bit", 0);
        NOTE_INSTRUMENTS.put("banjo", 0);
        NOTE_INSTRUMENTS.put("pling", 0);
    }

    public enum Kind { NOTE_BLOCK, HEAD, SOUND }

    /** An instrument of the song: {@code name} is the note block instrument / head name, or the sound id for SOUND. */
    public record Instrument(Kind kind, String name, int octave) {
        public boolean tunable() {
            return kind != Kind.HEAD;
        }
    }

    /** One tone: instrument index into {@link Song#instruments}, note 0-24 (pitch 2^((note-12)/12)), volume 0.1-1. */
    public record Tone(int instrument, int note, float volume) {
        public float pitch() {
            return (float) Math.pow(2.0, (note - 12) / 12.0);
        }
    }

    /** A compiled song: {@code steps} steps of {@code tempo} ticks; the tones of step s are tones[stepStart[s]..stepStart[s+1]). */
    public record Song(int tempo, int steps, List<Instrument> instruments, int[] stepStart, Tone[] tones) {
        public static final Song EMPTY = new Song(DEFAULT_TEMPO, 0, List.of(), new int[]{0}, new Tone[0]);

        public int ticks() {
            return steps * tempo;
        }

        public List<Tone> tonesAt(int step) {
            if (step < 0 || step >= steps) return List.of();
            return Arrays.asList(tones).subList(stepStart[step], stepStart[step + 1]);
        }

        public int toneCount() {
            return tones.length;
        }
    }

    /** A problem with its line (0-based), character range and translation key {@code simplemusic.code.error.<key>}. */
    public record Problem(int line, int start, int end, String key, List<String> args) {}

    public record Result(Song song, List<Problem> problems, byte[] styles) {
        public boolean ok() {
            return problems.isEmpty();
        }
    }

    /** Which sounds the code may name ({@code sound <id>}): null = allowed, otherwise the problem key. */
    public interface SoundPolicy {
        SoundPolicy ALL = id -> null;

        String check(String soundId);
    }

    private static final Pattern NOTE = Pattern.compile("([a-g])([#b]?)(-?\\d)");
    private static final Pattern NUMBER = Pattern.compile("\\d{1,2}");
    private static final Pattern SOUND_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern LABEL = Pattern.compile("\\$?[a-z0-9_]+");
    private static final int[] SEMITONES = {9, 11, 0, 2, 4, 5, 7}; // a b c d e f g
    /** MIDI number of f#3, note 0 of the harp. */
    private static final int BASE_MIDI = 54;

    private NoteCode() {}

    public static Result parse(String code) {
        return parse(code, SoundPolicy.ALL);
    }

    public static Result parse(String code, SoundPolicy policy) {
        return new Parser(code == null ? "" : code, policy).run();
    }

    /** Note block note (0-24) of a written note for an instrument with the given octave shift, or -1 if it does not fit. */
    public static int noteOf(String token, int octave) {
        Matcher m = NOTE.matcher(token);
        if (m.matches()) {
            int semitone = SEMITONES[m.group(1).charAt(0) - 'a'];
            if (m.group(2).equals("#")) semitone++;
            if (m.group(2).equals("b")) semitone--;
            int midi = 12 * (Integer.parseInt(m.group(3)) + 1) + semitone;
            int note = midi - BASE_MIDI - 12 * octave;
            return note >= 0 && note <= 24 ? note : -1;
        }
        if (NUMBER.matcher(token).matches()) {
            int note = Integer.parseInt(token);
            return note <= 24 ? note : -1;
        }
        return -1;
    }

    /** The written name of note block note {@code note} for an instrument with the given octave shift (sharps). */
    public static String nameOf(int note, int octave) {
        String[] names = {"c", "c#", "d", "d#", "e", "f", "f#", "g", "g#", "a", "a#", "b"};
        int midi = BASE_MIDI + 12 * octave + note;
        return names[midi % 12] + (midi / 12 - 1);
    }

    private static final class ToneAt {
        final Tone tone;
        final int line, start, end;

        ToneAt(Tone tone, int line, int start, int end) {
            this.tone = tone;
            this.line = line;
            this.start = start;
            this.end = end;
        }
    }

    /** One step of one track: empty = rest. */
    private static final class Step {
        final List<ToneAt> tones = new ArrayList<>(1);
    }

    private static final class Track {
        final int instrument;
        final List<Step> steps = new ArrayList<>();
        float volume = 1.0F;

        Track(int instrument) {
            this.instrument = instrument;
        }
    }

    /** A block repetition {@code xN { ... }} in progress: its own tracks, merged into the parent at '}'. */
    private static final class Frame {
        final int count, line, start;
        final Map<String, Track> tracks = new LinkedHashMap<>();

        Frame(int count, int line, int start) {
            this.count = count;
            this.line = line;
            this.start = start;
        }
    }

    private static final class Parser {
        final String code;
        final SoundPolicy policy;
        final byte[] styles;
        final List<Problem> problems = new ArrayList<>();
        final List<Instrument> instruments = new ArrayList<>();
        final Map<String, Integer> aliases = new LinkedHashMap<>();
        final Map<String, Track> tracks = new LinkedHashMap<>();
        final List<Frame> frames = new ArrayList<>();
        final Map<String, Integer> labelInstrument = new LinkedHashMap<>();
        int tempo = DEFAULT_TEMPO;
        boolean tooLong;
        int line;

        Parser(String code, SoundPolicy policy) {
            this.code = code;
            this.policy = policy;
            this.styles = new byte[code.length()];
        }

        Result run() {
            if (code.length() > MAX_CODE_LENGTH) {
                problem(0, MAX_CODE_LENGTH, code.length(), "too_long_code", String.valueOf(MAX_CODE_LENGTH));
                return new Result(Song.EMPTY, problems, styles);
            }
            int pos = 0;
            line = 0;
            while (pos <= code.length()) {
                int end = code.indexOf('\n', pos);
                if (end < 0) end = code.length();
                parseLine(pos, end);
                line++;
                pos = end + 1;
            }
            for (Frame frame : frames) {
                problem(frame.line, frame.start, frame.start + 1, "unclosed_block");
            }
            Song song = build();
            return new Result(problems.isEmpty() ? song : Song.EMPTY, problems, styles);
        }

        // ---------------------------------------------------------------- lines

        void parseLine(int from, int to) {
            List<int[]> tokens = tokens(from, to);
            if (tokens.isEmpty()) return;
            String first = text(tokens.get(0));
            if (first.equals("tempo")) {
                style(tokens.get(0), STYLE_KEYWORD);
                if (tokens.size() != 2 || !NUMBER.matcher(text(tokens.get(1))).matches()) {
                    problemAt(tokens.size() > 1 ? tokens.get(1) : tokens.get(0), "bad_tempo", String.valueOf(MIN_TEMPO), String.valueOf(MAX_TEMPO));
                    return;
                }
                int value = Integer.parseInt(text(tokens.get(1)));
                if (value < MIN_TEMPO || value > MAX_TEMPO) {
                    problemAt(tokens.get(1), "bad_tempo", String.valueOf(MIN_TEMPO), String.valueOf(MAX_TEMPO));
                    return;
                }
                style(tokens.get(1), STYLE_NUMBER);
                tempo = value;
                return;
            }
            if (first.equals("}")) {
                style(tokens.get(0), STYLE_OPERATOR);
                closeBlock(tokens.get(0));
                for (int i = 1; i < tokens.size(); i++) problemAt(tokens.get(i), "unexpected");
                return;
            }
            if (first.matches("x\\d+") && tokens.size() >= 2 && text(tokens.get(1)).equals("{")) {
                int count = repeatCount(tokens.get(0));
                style(tokens.get(1), STYLE_OPERATOR);
                frames.add(new Frame(Math.max(1, count), line, tokens.get(1)[0]));
                if (tokens.size() == 2) return;
                // one-line block: x2 { $h: c4 d4 }
                int[] last = tokens.get(tokens.size() - 1);
                if (!text(last).equals("}") || tokens.size() < 4 || !text(tokens.get(2)).endsWith(":")) {
                    problemAt(tokens.get(2), "unknown_line");
                    frames.remove(frames.size() - 1);
                    return;
                }
                trackLine(tokens.subList(2, tokens.size() - 1));
                style(last, STYLE_OPERATOR);
                closeBlock(last);
                return;
            }
            if (first.startsWith("$") && tokens.size() >= 2 && text(tokens.get(1)).equals("=")) {
                defineAlias(tokens);
                return;
            }
            if (first.endsWith(":") && first.length() > 1) {
                trackLine(tokens);
                return;
            }
            problemAt(tokens.get(0), "unknown_line");
        }

        int repeatCount(int[] token) {
            style(token, STYLE_KEYWORD);
            int count;
            try {
                count = Integer.parseInt(text(token).substring(1));
            } catch (NumberFormatException e) {
                count = 0;
            }
            if (count < 1 || count > MAX_REPEAT) {
                problemAt(token, "bad_repeat", String.valueOf(MAX_REPEAT));
                return 1;
            }
            return count;
        }

        void defineAlias(List<int[]> tokens) {
            int[] name = tokens.get(0);
            String alias = text(name);
            if (!LABEL.matcher(alias).matches() || alias.length() < 2) {
                problemAt(name, "bad_alias");
                return;
            }
            style(name, STYLE_INSTRUMENT);
            style(tokens.get(1), STYLE_OPERATOR);
            if (tokens.size() < 3) {
                problemAt(tokens.get(1), "missing_instrument");
                return;
            }
            int[] what = tokens.get(2);
            String word = text(what);
            Integer index;
            if (word.equals("sound")) {
                style(what, STYLE_KEYWORD);
                if (tokens.size() != 4) {
                    problemAt(tokens.size() > 4 ? tokens.get(4) : what, tokens.size() > 4 ? "unexpected" : "missing_sound");
                    return;
                }
                int[] idToken = tokens.get(3);
                String id = text(idToken);
                if (!SOUND_ID.matcher(id).matches()) {
                    problemAt(idToken, "bad_sound", id);
                    return;
                }
                String refused = policy.check(id);
                if (refused != null) {
                    problemAt(idToken, refused, id);
                    return;
                }
                style(idToken, STYLE_INSTRUMENT);
                index = instrument(new Instrument(Kind.SOUND, id, 0));
            } else {
                if (tokens.size() != 3) {
                    problemAt(tokens.get(3), "unexpected");
                    return;
                }
                index = namedInstrument(word);
                if (index == null) {
                    problemAt(what, "unknown_instrument", word);
                    return;
                }
                style(what, STYLE_INSTRUMENT);
            }
            if (aliases.containsKey(alias)) {
                problemAt(name, "duplicate_alias", alias);
                return;
            }
            aliases.put(alias, index);
        }

        Integer namedInstrument(String word) {
            if (NOTE_INSTRUMENTS.containsKey(word)) return instrument(new Instrument(Kind.NOTE_BLOCK, word, NOTE_INSTRUMENTS.get(word)));
            if (HEAD_INSTRUMENTS.contains(word)) return instrument(new Instrument(Kind.HEAD, word, 0));
            return null;
        }

        int instrument(Instrument instrument) {
            int index = instruments.indexOf(instrument);
            if (index >= 0) return index;
            instruments.add(instrument);
            return instruments.size() - 1;
        }

        void trackLine(List<int[]> tokens) {
            int[] head = tokens.get(0);
            String label = text(head);
            label = label.substring(0, label.length() - 1);
            Integer index = label.startsWith("$") ? aliases.get(label) : namedInstrument(label);
            if (index == null) {
                problem(line, head[0], head[1] - 1, label.startsWith("$") ? "unknown_alias" : "unknown_instrument", label);
                return;
            }
            Integer known = labelInstrument.putIfAbsent(label, index);
            style(new int[]{head[0], head[1] - 1}, STYLE_INSTRUMENT);
            style(new int[]{head[1] - 1, head[1]}, STYLE_OPERATOR);
            Map<String, Track> scope = frames.isEmpty() ? tracks : frames.get(frames.size() - 1).tracks;
            Track track = scope.get(label);
            if (track == null) {
                if (!trackKnown(label) && trackCount() >= MAX_TRACKS) {
                    problemAt(head, "too_many_tracks", String.valueOf(MAX_TRACKS));
                    return;
                }
                track = new Track(known != null ? known : index);
                Track outer = findTrack(label);
                if (outer != null) track.volume = outer.volume;
                scope.put(label, track);
            }
            List<Step> steps = new ArrayList<>();
            int[] at = {1};
            float[] volume = {track.volume};
            sequence(tokens, at, track.instrument, volume, steps, false);
            track.volume = volume[0];
            append(track.steps, steps);
        }

        boolean trackKnown(String label) {
            return labelInstrument.containsKey(label) && (tracks.containsKey(label) || frames.stream().anyMatch(f -> f.tracks.containsKey(label)));
        }

        Track findTrack(String label) {
            for (int i = frames.size() - 2; i >= 0; i--) {
                Track t = frames.get(i).tracks.get(label);
                if (t != null) return t;
            }
            return tracks.get(label);
        }

        int trackCount() {
            java.util.Set<String> labels = new java.util.HashSet<>(tracks.keySet());
            frames.forEach(f -> labels.addAll(f.tracks.keySet()));
            return labels.size();
        }

        /** Parses tokens from at[0] into steps until the end or a closing '}' (when nested); true when closed by '}'. */
        boolean sequence(List<int[]> tokens, int[] at, int instrumentIndex, float[] volume, List<Step> out, boolean nested) {
            Instrument instrument = instruments.get(instrumentIndex);
            while (at[0] < tokens.size()) {
                int[] token = tokens.get(at[0]);
                String word = text(token);
                at[0]++;
                if (word.equals("}")) {
                    style(token, STYLE_OPERATOR);
                    if (nested) return true;
                    problemAt(token, "unexpected");
                    continue;
                }
                if (word.equals(".") || word.equals("-")) {
                    style(token, STYLE_REST);
                    addStep(out, new Step());
                    continue;
                }
                if (word.equals("@vol")) {
                    style(token, STYLE_KEYWORD);
                    if (at[0] >= tokens.size()) {
                        problemAt(token, "bad_volume");
                        continue;
                    }
                    int[] value = tokens.get(at[0]++);
                    try {
                        float v = Float.parseFloat(text(value));
                        if (!(v >= 0.1F && v <= 1.0F)) throw new NumberFormatException();
                        volume[0] = v;
                        style(value, STYLE_NUMBER);
                    } catch (NumberFormatException e) {
                        problemAt(value, "bad_volume");
                    }
                    continue;
                }
                if (word.matches("x\\d+")) {
                    int count = repeatCount(token);
                    if (at[0] >= tokens.size() || !text(tokens.get(at[0])).equals("{")) {
                        problemAt(token, "missing_brace");
                        continue;
                    }
                    style(tokens.get(at[0]), STYLE_OPERATOR);
                    at[0]++;
                    List<Step> inner = new ArrayList<>();
                    int open = at[0] - 1;
                    if (!sequence(tokens, at, instrumentIndex, volume, inner, true)) problemAt(tokens.get(open), "unclosed_block");
                    for (int i = 0; i < count && !tooLong; i++) {
                        for (Step s : inner) addStep(out, s);
                    }
                    continue;
                }
                if (word.startsWith("[")) {
                    // chord: tokens up to the one ending with ']'
                    Step step = new Step();
                    List<int[]> parts = new ArrayList<>();
                    int[] cur = token;
                    while (true) {
                        parts.add(cur);
                        if (text(cur).endsWith("]")) break;
                        if (at[0] >= tokens.size()) {
                            problemAt(token, "unclosed_chord");
                            parts = null;
                            break;
                        }
                        cur = tokens.get(at[0]++);
                    }
                    if (parts == null) continue;
                    for (int[] part : parts) {
                        int s = part[0], e = part[1];
                        if (text(part).startsWith("[")) {
                            styles[s] = STYLE_OPERATOR;
                            s++;
                        }
                        if (e > s && code.charAt(e - 1) == ']') {
                            styles[e - 1] = STYLE_OPERATOR;
                            e--;
                        }
                        if (e <= s) continue;
                        ToneAt tone = tone(new int[]{s, e}, instrument, instrumentIndex, volume[0]);
                        if (tone != null) step.tones.add(tone);
                    }
                    if (step.tones.size() > MAX_CHORD) {
                        problemAt(token, "chord_too_big", String.valueOf(MAX_CHORD));
                    }
                    addStep(out, step);
                    continue;
                }
                ToneAt tone = tone(token, instrument, instrumentIndex, volume[0]);
                Step step = new Step();
                if (tone != null) step.tones.add(tone);
                addStep(out, step);
            }
            return false;
        }

        void addStep(List<Step> out, Step step) {
            if (out.size() >= MAX_TICKS / MIN_TEMPO) {
                if (!tooLong) problem(line, 0, 0, "too_long", "10");
                tooLong = true;
                return;
            }
            out.add(step);
        }

        ToneAt tone(int[] token, Instrument instrument, int instrumentIndex, float volume) {
            String word = text(token);
            int note = noteOf(word, instrument.octave());
            if (note < 0) {
                if (NOTE.matcher(word).matches() || NUMBER.matcher(word).matches()) {
                    problemAt(token, "out_of_range", word, nameOf(0, instrument.octave()), nameOf(24, instrument.octave()));
                } else {
                    problemAt(token, "bad_note", word);
                }
                return null;
            }
            style(token, NUMBER.matcher(word).matches() ? STYLE_NUMBER : STYLE_NOTE);
            return new ToneAt(new Tone(instrumentIndex, instrument.tunable() ? note : 12, volume), line, token[0], token[1]);
        }

        void closeBlock(int[] brace) {
            if (frames.isEmpty()) {
                problemAt(brace, "unexpected");
                return;
            }
            Frame frame = frames.remove(frames.size() - 1);
            Map<String, Track> parent = frames.isEmpty() ? tracks : frames.get(frames.size() - 1).tracks;
            int length = 0;
            for (Track t : frame.tracks.values()) length = Math.max(length, t.steps.size());
            int sync = 0;
            for (Track t : parent.values()) sync = Math.max(sync, t.steps.size());
            for (Map.Entry<String, Track> e : frame.tracks.entrySet()) {
                parent.computeIfAbsent(e.getKey(), k -> {
                    Track t = new Track(e.getValue().instrument);
                    t.volume = e.getValue().volume;
                    return t;
                });
            }
            for (Track t : parent.values()) pad(t.steps, sync);
            for (Map.Entry<String, Track> e : frame.tracks.entrySet()) {
                Track target = parent.get(e.getKey());
                pad(e.getValue().steps, length);
                for (int i = 0; i < frame.count && !tooLong; i++) append(target.steps, e.getValue().steps);
                target.volume = e.getValue().volume;
            }
            int end = sync + length * frame.count;
            for (Track t : parent.values()) pad(t.steps, Math.min(end, MAX_TICKS / MIN_TEMPO));
        }

        void pad(List<Step> steps, int length) {
            while (steps.size() < length && !tooLong) addStep(steps, new Step());
        }

        void append(List<Step> target, List<Step> steps) {
            for (Step s : steps) {
                if (tooLong) return;
                addStep(target, s);
            }
        }

        Song build() {
            int steps = 0;
            for (Track t : tracks.values()) steps = Math.max(steps, t.steps.size());
            if ((long) steps * tempo > MAX_TICKS && !tooLong) {
                problem(0, 0, 0, "too_long", "10");
            }
            int[] start = new int[steps + 1];
            List<Tone> tones = new ArrayList<>();
            for (int s = 0; s < steps; s++) {
                start[s] = tones.size();
                int count = 0;
                for (Track t : tracks.values()) {
                    if (s >= t.steps.size()) continue;
                    for (ToneAt tone : t.steps.get(s).tones) {
                        if (++count > MAX_TONES_PER_STEP) {
                            problem(tone.line, tone.start, tone.end, "too_many_tones", String.valueOf(MAX_TONES_PER_STEP), String.valueOf(s + 1));
                            continue;
                        }
                        tones.add(tone.tone);
                    }
                }
            }
            start[steps] = tones.size();
            return new Song(tempo, steps, List.copyOf(instruments), start, tones.toArray(new Tone[0]));
        }

        // ---------------------------------------------------------------- tokens

        /** Whitespace separated tokens of [from, to) as {start, end}; a comment ('#' at a token start) ends the line. */
        List<int[]> tokens(int from, int to) {
            List<int[]> out = new ArrayList<>();
            int i = from;
            while (i < to) {
                char c = code.charAt(i);
                if (Character.isWhitespace(c)) {
                    i++;
                    continue;
                }
                if (c == '#') {
                    Arrays.fill(styles, i, to, STYLE_COMMENT);
                    break;
                }
                int s = i;
                // '{' and '}' and '=' standing alone are their own tokens even without spaces around them
                if (c == '{' || c == '}' || c == '=') {
                    out.add(new int[]{s, s + 1});
                    i++;
                    continue;
                }
                while (i < to && !Character.isWhitespace(code.charAt(i)) && code.charAt(i) != '{' && code.charAt(i) != '}'
                        && code.charAt(i) != '=') {
                    i++;
                }
                out.add(new int[]{s, i});
            }
            return out;
        }

        String text(int[] token) {
            return code.substring(token[0], token[1]).toLowerCase(Locale.ROOT);
        }

        void style(int[] token, byte style) {
            Arrays.fill(styles, token[0], token[1], style);
        }

        void problemAt(int[] token, String key, String... args) {
            problem(line, token[0], token[1], key, args);
        }

        void problem(int atLine, int start, int end, String key, String... args) {
            if (end > start && end <= styles.length) Arrays.fill(styles, start, end, STYLE_ERROR);
            problems.add(new Problem(atLine, start, end, key, List.of(args)));
        }
    }
}
