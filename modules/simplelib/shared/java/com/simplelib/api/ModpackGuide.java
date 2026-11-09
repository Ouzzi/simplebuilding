package com.simplelib.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplelib.SimpleLib;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The "Simple Modpack" guide (owner 03.11., docs/ai/PLAN-B-GUIDE01-2026-11-03.md): one book that exists whatever Simple
 * mod is installed. Every mod hands in its part as data, never as code: a file {@code assets/<ns>/simplelib/modpack_guide.json}
 * ({@link #CONTRIBUTION}) with its section (the super-mod it belongs to, a top bookmark of the book) and its chapters (side
 * bookmarks; pages are lang keys {@code <page_key>.<n>} and {@code .title}). A chapter with an {@code advancement} opens once
 * the reader has that advancement - the server answers which ones ({@link #done}); without one it is always open.
 *
 * <p>Sections ({@link #sections}): contributions group under their {@code section} when the super-mod of that name hands in
 * a file of its own (it is installed); otherwise a contribution forms its own section, so Simple Fun alone shows "Simple
 * Fun". A section may name a {@code screen}: its owner then registers its own book screen on the client
 * ({@code com.simplelib.api.client.ModpackGuideScreens}), and the section's data chapters stay reachable beside it.
 *
 * <p>Retired single guides ({@link #isRetiredGuide}): the old per-mod books stay registered (worlds keep them) but leave
 * every creative tab, and they turn into this guide when used (modules) or at login (SimpleBuilding).
 */
public final class ModpackGuide {
    public static final Identifier ITEM_ID = SimpleLib.id("modpack_guide");
    /** Path of a mod's contribution below {@code assets/<namespace>/}. */
    public static final String CONTRIBUTION = "simplelib/modpack_guide.json";
    /**
     * b-guidechap14: a retired single guide writes its chapter id ({@code <ns>:guide}) under this key into the
     * {@code custom_data} of the guide it turns into; with the client switch {@code guideOpensChapter} that guide opens there.
     */
    public static final String CHAPTER_TAG = "simplelib_guide_chapter";
    /**
     * b-guidechap14 client switch {@code guideOpensChapter} (SimpleLib client file): a guide that came from a retired single
     * guide opens at that mod's chapter ({@link #chapterOf}). Default off = the start page as before (owner chooses, GUIDE14).
     * Kept here, not in the client classes, because the client file is also read on a dedicated server.
     */
    public static volatile boolean opensChapter = false;
    /** Most advancement ids one query may ask about. */
    public static final int MAX_QUERY = 256;

    /** A side bookmark: {@code pages} pages {@code <pageKey>.<n>} (body) and {@code <pageKey>.<n>.title}. */
    public record Chapter(Identifier id, String title, Identifier icon, String pageKey, int pages,
                          @Nullable Identifier advancement, @Nullable Identifier hint) {
    }

    /** One mod's file. {@code name}/{@code icon} label the section when this file owns it (or stands alone). */
    public record Contribution(String namespace, String section, String name, Identifier icon, int order, boolean screen,
                               List<Chapter> chapters) {
    }

    /** A top bookmark: {@code screen} = the owner registered its own book screen for it. */
    public record Section(String id, String name, Identifier icon, int order, boolean screen, List<Chapter> chapters) {
    }

    private static final List<Consumer<ServerPlayer>> USE_HOOKS = new CopyOnWriteArrayList<>();

    private ModpackGuide() {}

    /** The registered guide item, or null before SimpleLib registered it. */
    public static @Nullable Item item() {
        return BuiltInRegistries.ITEM.getOptional(ITEM_ID).orElse(null);
    }

    public static ItemStack stack() {
        Item item = item();
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** The chapter a guide came from ({@link #CHAPTER_TAG}), or null (crafted, or turned before b-guidechap14). */
    public static @Nullable Identifier chapterOf(ItemStack stack) {
        String raw = markOf(stack);
        return raw == null || raw.indexOf(':') < 0 ? null : Identifier.tryParse(raw);
    }

    /**
     * The raw mark ({@link #CHAPTER_TAG}): a chapter id {@code <ns>:guide}, or a section id without a colon (b-guide15:
     * SimpleBuilding's own book, turned at login, marks {@code simplebuilding}); null when unmarked.
     */
    public static @Nullable String markOf(ItemStack stack) {
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        String raw = data.copyTag().getStringOr(CHAPTER_TAG, "");
        return raw.isEmpty() ? null : raw;
    }

    /** The section holding {@code chapter}, or null. */
    public static @Nullable Section sectionOf(List<Section> sections, @Nullable Identifier chapter) {
        if (chapter == null) return null;
        for (Section section : sections) {
            for (Chapter c : section.chapters()) if (c.id().equals(chapter)) return section;
        }
        return null;
    }

    /** Whether the id is one of the retired single guides ({@code <mod>:guide_book}, SimpleBuilding's {@code guide_book_*}). */
    public static boolean isRetiredGuide(Identifier id) {
        return !"minecraft".equals(id.getNamespace()) && !ITEM_ID.equals(id)
                && (id.getPath().equals("guide_book") || id.getPath().startsWith("guide_book_"));
    }

    /** A mod runs this on the server whenever a player opens the guide (SimpleBuilding refreshes its own bookmarks). */
    public static void addUseHook(Consumer<ServerPlayer> hook) {
        USE_HOOKS.add(hook);
    }

    public static void onUse(ServerPlayer player) {
        for (Consumer<ServerPlayer> hook : USE_HOOKS) {
            try {
                hook.accept(player);
            } catch (RuntimeException e) {
                SimpleLib.LOG.warn("Simple Modpack guide: a use hook failed: {}", e.toString());
            }
        }
    }

    /** Which of these advancements the player has completed (unknown ids count as not done; at most {@link #MAX_QUERY}). */
    public static List<Identifier> done(ServerPlayer player, List<Identifier> advancements) {
        List<Identifier> out = new ArrayList<>();
        var server = player.level().getServer();
        if (server == null) return out;
        for (Identifier id : advancements.subList(0, Math.min(advancements.size(), MAX_QUERY))) {
            var holder = server.getAdvancements().get(id);
            if (holder != null && player.getAdvancements().getOrStartProgress(holder).isDone()) out.add(id);
        }
        return out;
    }

    // =====================================================================================
    // Data
    // =====================================================================================

    /** Reads one contribution file of {@code namespace}; throws on a malformed file (the caller logs and skips it). */
    public static Contribution parse(String namespace, JsonObject json) {
        String section = json.has("section") ? json.get("section").getAsString() : namespace;
        List<Chapter> chapters = new ArrayList<>();
        JsonArray array = json.has("chapters") ? json.getAsJsonArray("chapters") : new JsonArray();
        for (JsonElement element : array) {
            JsonObject c = element.getAsJsonObject();
            int pages = c.get("pages").getAsInt();
            if (pages < 0 || pages > 200) throw new IllegalArgumentException("chapter with " + pages + " pages");
            chapters.add(new Chapter(Identifier.parse(c.get("id").getAsString()), c.get("title").getAsString(),
                    Identifier.parse(c.get("icon").getAsString()), c.get("page_key").getAsString(), pages,
                    optionalId(c, "advancement"), optionalId(c, "hint")));
        }
        return new Contribution(namespace, section, json.get("name").getAsString(), Identifier.parse(json.get("icon").getAsString()),
                json.has("order") ? json.get("order").getAsInt() : 100, json.has("screen") && json.get("screen").getAsBoolean(),
                List.copyOf(chapters));
    }

    private static @Nullable Identifier optionalId(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? Identifier.parse(json.get(key).getAsString()) : null;
    }

    /**
     * The top bookmarks: a contribution joins its {@code section} when that super-mod has a file of its own, else it is a
     * section by itself. The owner's chapters come first, then the others by order and namespace; sections by order and id.
     */
    public static List<Section> sections(List<Contribution> contributions) {
        Map<String, Contribution> byNamespace = new LinkedHashMap<>();
        for (Contribution c : contributions) byNamespace.putIfAbsent(c.namespace(), c);
        Map<String, List<Contribution>> groups = new LinkedHashMap<>();
        for (Contribution c : byNamespace.values()) {
            String key = byNamespace.containsKey(c.section()) ? c.section() : c.namespace();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }
        List<Section> out = new ArrayList<>();
        groups.forEach((key, members) -> {
            Contribution owner = byNamespace.get(key);
            members.sort(Comparator.comparing((Contribution c) -> c != owner).thenComparingInt(Contribution::order)
                    .thenComparing(Contribution::namespace));
            List<Chapter> chapters = new ArrayList<>();
            for (Contribution c : members) chapters.addAll(c.chapters());
            if (!chapters.isEmpty() || owner.screen()) {
                out.add(new Section(key, owner.name(), owner.icon(), owner.order(), owner.screen(), List.copyOf(chapters)));
            }
        });
        out.sort(Comparator.comparingInt(Section::order).thenComparing(Section::id));
        return List.copyOf(out);
    }
}
