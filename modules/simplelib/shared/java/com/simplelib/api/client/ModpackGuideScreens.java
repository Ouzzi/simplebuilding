package com.simplelib.api.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplelib.SimpleLib;
import com.simplelib.api.ModpackGuide;
import com.simplelib.client.guide.ModpackGuideScreen;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import org.jspecify.annotations.Nullable;

/**
 * Client side of the Simple Modpack guide ({@link ModpackGuide}, b-guide01): reads every mod's contribution from the
 * resource packs, opens a section, remembers the last one and keeps which chapter advancements the server reported done.
 *
 * <p>A mod with a book screen of its own registers it for its section ({@link #register}); that screen draws the top
 * bookmarks itself through {@link ModpackGuideBar} (SimpleBuilding's guide does). All other
 * sections open in SimpleLib's {@link ModpackGuideScreen}.
 */
public final class ModpackGuideScreens {
    /** Builds a section's own book screen; {@code hand} holds the guide (null: opened without one, e.g. by a test probe). */
    public interface Factory {
        Screen create(@Nullable InteractionHand hand);
    }

    private static final Map<String, Factory> SCREENS = new ConcurrentHashMap<>();
    private static volatile Set<Identifier> done = Set.of();
    private static volatile int version;
    private static volatile @Nullable String last;
    private static volatile @Nullable Set<Identifier> override;
    private static volatile @Nullable List<ModpackGuide.Section> cache;
    private static volatile Consumer<List<Identifier>> sender = ids -> {
    };

    private ModpackGuideScreens() {}

    public static void register(String section, Factory factory) {
        SCREENS.put(section, factory);
    }

    public static boolean hasScreen(String section) {
        return SCREENS.containsKey(section);
    }

    /** The loader's client entry hands in how a query reaches the server. */
    public static void setSender(Consumer<List<Identifier>> send) {
        sender = send != null ? send : ids -> {
        };
    }

    /** Every contribution in the loaded resource packs, grouped into sections (read when a guide opens, then cached). */
    public static List<ModpackGuide.Section> sections() {
        List<ModpackGuide.Section> cached = cache;
        return cached != null ? cached : reload();
    }

    /** Reads the contributions again (resource packs may have changed since the last book). */
    public static List<ModpackGuide.Section> reload() {
        List<ModpackGuide.Contribution> contributions = new ArrayList<>();
        var resources = Minecraft.getInstance().getResourceManager()
                .listResources("simplelib", id -> id.getPath().equals(ModpackGuide.CONTRIBUTION));
        resources.forEach((id, resource) -> {
            try (Reader reader = resource.openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                contributions.add(ModpackGuide.parse(id.getNamespace(), json));
            } catch (Exception e) {
                SimpleLib.LOG.warn("Simple Modpack guide: skipping {} ({})", id, e.toString());
            }
        });
        List<ModpackGuide.Section> sections = ModpackGuide.sections(contributions);
        cache = sections;
        return sections;
    }

    public static ModpackGuide.@Nullable Section section(List<ModpackGuide.Section> sections, @Nullable String id) {
        for (ModpackGuide.Section section : sections) if (section.id().equals(id)) return section;
        return null;
    }

    /** Opens the book at {@code section} (null: the last one read, else the first). */
    public static void open(@Nullable String section, @Nullable InteractionHand hand) {
        List<ModpackGuide.Section> sections = reload();
        ModpackGuide.Section target = section(sections, section != null ? section : last);
        if (target == null && !sections.isEmpty()) target = sections.getFirst();
        Minecraft minecraft = Minecraft.getInstance();
        if (target != null && target.screen() && SCREENS.containsKey(target.id())) {
            last = target.id();
            minecraft.gui.setScreen(SCREENS.get(target.id()).create(hand));
            return;
        }
        openChapters(target == null ? null : target.id(), hand);
    }

    /** Opens SimpleLib's own screen at the data chapters of {@code section}, also for a section with its own screen. */
    public static void openChapters(@Nullable String section, @Nullable InteractionHand hand) {
        if (section != null) last = section;
        Minecraft.getInstance().gui.setScreen(new ModpackGuideScreen(section, hand));
    }

    /** Whether {@code section} has data chapters (SimpleBuilding shows its "More Simple mods" bookmark only then). */
    public static boolean hasChapters(String section) {
        ModpackGuide.Section found = section(sections(), section);
        return found != null && !found.chapters().isEmpty();
    }

    // =====================================================================================
    // Unlocks
    // =====================================================================================

    /** Asks the server about the chapter advancements of these sections. */
    public static void query(List<ModpackGuide.Section> sections) {
        List<Identifier> ids = new ArrayList<>();
        for (ModpackGuide.Section section : sections) {
            for (ModpackGuide.Chapter chapter : section.chapters()) {
                if (chapter.advancement() != null && !ids.contains(chapter.advancement()) && ids.size() < ModpackGuide.MAX_QUERY) {
                    ids.add(chapter.advancement());
                }
            }
        }
        if (!ids.isEmpty() && Minecraft.getInstance().getConnection() != null) sender.accept(ids);
    }

    /** Client thread: the server's answer. */
    public static void receive(List<Identifier> completed) {
        done = Set.copyOf(completed);
        version++;
    }

    /** Test probe only: pretend these advancements are done (null: back to what the server said). */
    public static void override(@Nullable Set<Identifier> completed) {
        override = completed == null ? null : Set.copyOf(completed);
        version++;
    }

    /** Whether a chapter is open for the reader: no advancement, the advancement done, or creative mode. */
    public static boolean open(ModpackGuide.Chapter chapter) {
        if (chapter.advancement() == null) return true;
        Set<Identifier> forced = override;
        if (forced != null) return forced.contains(chapter.advancement());
        var player = Minecraft.getInstance().player;
        return (player != null && player.isCreative()) || done.contains(chapter.advancement());
    }

    /** Changes whenever the unlock state changes (screens relayout then). */
    public static int version() {
        return version;
    }
}
