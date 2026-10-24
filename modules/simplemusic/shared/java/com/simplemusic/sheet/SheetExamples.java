package com.simplemusic.sheet;

import com.simplemusic.registry.MusicComponents;
import com.simplemusic.registry.MusicRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * Example sheets that ship with the mod (NA24-8): three original pieces and public domain classics, from simple to
 * complex. The note code lives in {@code data/simplemusic/examples/<id>.txt} (sources named in each file's first
 * comment line); the creative tab hands them out as signed sheets.
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

    /** One signed sheet per example (creative tab). */
    public static List<ItemStack> stacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Example e : ALL) {
            ItemStack stack = new ItemStack(MusicRegistry.SHEET_MUSIC);
            stack.set(MusicComponents.SHEET, new SheetContent(code(e), e.title(), e.author(), true));
            out.add(stack);
        }
        return out;
    }
}
