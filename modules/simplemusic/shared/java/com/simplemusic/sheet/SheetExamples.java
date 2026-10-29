package com.simplemusic.sheet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Example sheets that ship with the mod (NA24-8): three original pieces and public domain classics, from simple to
 * complex. The note code lives in {@code data/simplemusic/examples/<id>.txt} (sources named in each file's first
 * comment line); the sheet editor's "Add example" button loads one into the open sheet (BS25-13, owner 29.10.: no single example items).
 */
public final class SheetExamples {
    /** An example: file id, title (at most {@link NoteCode#MAX_TITLE_LENGTH} characters) and author for the signature. */
    public record Example(String id, String title, String author) {}

    public static final List<Example> ALL = List.of(
            new Example("first_steps", "First Steps", "Simple Music"),
            new Example("alle_meine_entchen", "Alle meine Entchen", "Traditional"),
            new Example("ode_to_joy", "Ode to Joy", "L. v. Beethoven"),
            new Example("greensleeves", "Greensleeves", "Traditional"),
            new Example("fuer_elise", "Fuer Elise", "L. v. Beethoven"),
            new Example("redstone_walk", "Redstone Walk", "Simple Music"),
            new Example("prelude_c_major", "Prelude in C major", "J. S. Bach"),
            new Example("canon_in_d", "Canon in D", "J. Pachelbel"),
            new Example("nether_dawn", "Nether Dawn", "Simple Music"));

    private SheetExamples() {}

    /** The note code of an example; empty when the file is missing. */
    public static String code(Example example) {
        try (InputStream in = SheetExamples.class.getResourceAsStream("/data/simplemusic/examples/" + example.id() + ".txt")) {
            return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
