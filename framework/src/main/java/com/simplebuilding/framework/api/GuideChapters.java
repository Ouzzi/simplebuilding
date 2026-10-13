package com.simplebuilding.framework.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Guide chapters a module contributes to a super mod's guide book (plan PLAN-MODULAUFTEILUNG §1.4) - pure Java, no
 * Minecraft class. Since 0.1.10.
 *
 * <p>A chapter names the book it belongs in (the super mod's book id, e.g. SimpleBuilding's {@code "machines"}), an
 * icon item id, recipe ids (must resolve when the owner is loaded) and further item ids, plus its own title and text
 * translation keys. The guide that reads this list (SimpleBuilding's mega guide, stage 9 of the split) appends the
 * chapters after its own; missing ids are skipped there.
 */
public final class GuideChapters {
    /** One chapter: owner namespace, chapter id (unique per owner), target book, icon, recipes, items, keys. */
    public record Chapter(String owner, String id, String book, String icon, List<String> recipes, List<String> items,
                          String titleKey, String textKey) {
        public Chapter {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(book, "book");
            Objects.requireNonNull(icon, "icon");
            Objects.requireNonNull(titleKey, "titleKey");
            Objects.requireNonNull(textKey, "textKey");
            recipes = List.copyOf(recipes);
            items = List.copyOf(items);
        }

        public String key() {
            return owner + ":" + id;
        }
    }

    private static final Map<String, Chapter> CHAPTERS = new LinkedHashMap<>();

    private GuideChapters() {
    }

    /** Adds (or replaces, keeping its place) the chapter {@code chapter.owner():chapter.id()}. */
    public static synchronized void register(Chapter chapter) {
        CHAPTERS.put(chapter.key(), chapter);
    }

    public static synchronized boolean unregister(String owner, String id) {
        return CHAPTERS.remove(owner + ":" + id) != null;
    }

    /** Every chapter in registration order. */
    public static synchronized List<Chapter> all() {
        return List.copyOf(CHAPTERS.values());
    }

    /** The chapters for {@code book} in registration order. */
    public static synchronized List<Chapter> forBook(String book) {
        List<Chapter> out = new ArrayList<>();
        for (Chapter chapter : CHAPTERS.values()) {
            if (chapter.book().equals(book)) {
                out.add(chapter);
            }
        }
        return List.copyOf(out);
    }
}
