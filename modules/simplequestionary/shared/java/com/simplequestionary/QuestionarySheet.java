package com.simplequestionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * What the open book shows besides the plain text (QT23, owner 23.10.), as pure logic without client classes so the
 * server tests can check it: the text page's keywords (bold spans), the colours of the text hierarchy, the items next
 * to the picture plate (family) and where an item comes from (the "Use" page's find hint).
 */
public final class QuestionarySheet {
    /** Paper and inks of the book's pages; every ink keeps 4.5:1 on {@link #PAPER} and on {@link #PAPER_DARK}. */
    public static final int PAPER = 0xFFF0E4C6, PAPER_DARK = 0xFFE4D4AE;
    /** Page headings (the brown of the guide books), body text (warm grey), keywords (dark ink, bold), captions. */
    public static final int HEADING = 0xFF6B4423, BODY = 0xFF57524B, KEYWORD = 0xFF2E241A, CAPTION = 0xFF5E5545;
    public static final int[] INKS = {HEADING, BODY, KEYWORD, CAPTION};
    /** Placeholders, {@code **marked**} words and numbers. */
    private static final Pattern MARKS = Pattern.compile("\\{(name|mod|item:([a-z0-9_.-]+:[a-z0-9_./-]+))}|\\*\\*([^*]+)\\*\\*|\\d+(?:[.,]\\d+)?");

    /** A piece of a paragraph; keywords are bold. */
    public record Span(String text, boolean bold) {}

    /** Where an item comes from: a recipe makes it, a block found in the world, or something found otherwise. */
    public enum Origin {
        CRAFT, MINE, FIND;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private QuestionarySheet() {}

    /**
     * One paragraph as spans with the placeholders filled in. Keywords (bold): what the placeholders name, words
     * marked {@code **so**} in the data, the item's own name where the text spells it out, and numbers.
     */
    public static List<Span> spans(String paragraph, String name, String mod, Function<Identifier, String> itemNames) {
        Pattern pattern = name.isBlank() ? MARKS : Pattern.compile(MARKS.pattern() + "|" + Pattern.quote(name));
        List<Span> out = new ArrayList<>();
        Matcher m = pattern.matcher(paragraph);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) add(out, paragraph.substring(last, m.start()), false);
            String word;
            if ("name".equals(m.group(1))) word = name;
            else if ("mod".equals(m.group(1))) word = mod;
            else if (m.group(2) != null) {
                Identifier id = Identifier.tryParse(m.group(2));
                word = id == null ? m.group(2) : itemNames.apply(id);
            } else if (m.group(3) != null) word = m.group(3);
            else word = m.group();
            add(out, word, true);
            last = m.end();
        }
        if (last < paragraph.length()) add(out, paragraph.substring(last), false);
        return out;
    }

    private static void add(List<Span> out, String text, boolean bold) {
        if (text.isEmpty()) return;
        if (!out.isEmpty() && out.get(out.size() - 1).bold() == bold) {
            out.set(out.size() - 1, new Span(out.get(out.size() - 1).text() + text, bold));
        } else {
            out.add(new Span(text, bold));
        }
    }

    /** The find hint: made by a recipe the page can show, else a block is mined, anything else is found (chests, mobs, trades). */
    public static Origin origin(QuestionaryFacts.Facts facts, Item item) {
        if (!facts.madeFrom().isEmpty()) return Origin.CRAFT;
        return item instanceof BlockItem ? Origin.MINE : Origin.FIND;
    }

    /** The other registered members of {@code family} in registry order (empty without a family). */
    public static List<Identifier> family(QuestionaryData.Family family, Identifier self, Iterable<Identifier> registered,
            Function<Identifier, Predicate<Identifier>> tagsOf) {
        List<Identifier> out = new ArrayList<>();
        if (family == null) return out;
        for (Identifier id : registered) {
            if (!id.equals(self) && family.matches(id, tagsOf.apply(id))) out.add(id);
        }
        return out;
    }
}
