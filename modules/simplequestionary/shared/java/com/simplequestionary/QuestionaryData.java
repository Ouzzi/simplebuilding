package com.simplequestionary;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * The questionary texts: one JSON per namespace and language, {@code assets/<ns>/questionary/<lang>.json} (client
 * resources, so resource packs can add or replace entries; later packs win per key). Each mod describes its own items
 * there without knowing this module (pure data, inert without the table).
 *
 * <pre>
 * { "enderite_ingot": { "lore": ["..."], "use": ["..."], "secret": ["..."] },
 *   "family:stairs":  { "items": ["#minecraft:stairs", "regex:.*_stairs"], "lore": ["{name}: ..."], ... },
 *   "fallback": { "block": { "lore": [8 variants], "use": [...], "secret": [...] }, "tool": ..., ... } }
 * </pre>
 * A page is a list of paragraphs. Keys starting with {@code _} are notes (e.g. generator facts) and ignored. A family
 * names its items by item tag ({@code #ns:tag}) or by a regex over the item path in the file's namespace (a regex with
 * a {@code :} is matched against the full id). Per page the order is: own entry, family, fallback; each first in the
 * selected language, then in {@code en_us}. The fallback (only in this module's file) picks one of
 * {@value #VARIANTS} variants per item class and page with a stable FNV-1a hash, so a foreign item reads the same in
 * every world. Placeholders: {@code {name}}, {@code {mod}}, {@code {item:ns:id}}.
 */
public final class QuestionaryData {
    public static final String DIRECTORY = "questionary", ENGLISH = "en_us", GERMAN = "de_de";
    public static final int VARIANTS = 8;
    /** Hard length limit of one page (characters after placeholders; the screen fits about this much). */
    public static final int MAX_PAGE_CHARS = 350;
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(name|mod|item:([a-z0-9_.-]+:[a-z0-9_./-]+))}");

    public enum Page {
        LORE("lore"), USE("use"), SECRET("secret");

        public final String key;

        Page(String key) {
            this.key = key;
        }
    }

    /** Item classes of the fallback texts. */
    public enum Kind {
        BLOCK, TOOL, ARMOR, FOOD, MISC;

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public enum Source { ENTRY, FAMILY, FALLBACK, NONE }

    public record Entry(Map<Page, List<String>> pages) {
        List<String> page(Page page) {
            return pages.getOrDefault(page, List.of());
        }
    }

    public record Family(String namespace, String name, List<Identifier> tags, List<Pattern> patterns, Entry entry) {
        public boolean matches(Identifier item, Predicate<Identifier> inTag) {
            for (Identifier tag : tags) if (inTag.test(tag)) return true;
            for (Pattern pattern : patterns) {
                boolean full = pattern.pattern().contains(":");
                if (full ? pattern.matcher(item.toString()).matches()
                        : item.getNamespace().equals(namespace) && pattern.matcher(item.getPath()).matches()) return true;
            }
            return false;
        }
    }

    /** Everything loaded for one language. */
    public static final class Language {
        final Map<Identifier, Entry> entries = new LinkedHashMap<>();
        final List<Family> families = new ArrayList<>();
        final Map<Kind, Map<Page, List<String>>> fallback = new EnumMap<>(Kind.class);

        public Map<Identifier, Entry> entries() {
            return entries;
        }

        public List<Family> families() {
            return families;
        }

        public List<String> fallback(Kind kind, Page page) {
            return fallback.getOrDefault(kind, Map.of()).getOrDefault(page, List.of());
        }
    }

    /** The resolved pages of one item: paragraphs (placeholders still in) and where each page came from. */
    public record Resolved(Map<Page, List<String>> pages, Map<Page, Source> sources) {
        public List<String> page(Page page) {
            return pages.getOrDefault(page, List.of());
        }

        public Source source(Page page) {
            return sources.getOrDefault(page, Source.NONE);
        }
    }

    public static final class Library {
        public static final Library EMPTY = new Library(Map.of(), Set.of());
        private final Map<String, Language> languages;
        private final Set<String> namespaces;

        public Library(Map<String, Language> languages, Set<String> namespaces) {
            this.languages = languages;
            this.namespaces = namespaces;
        }

        /** Namespaces that ship at least one questionary file. */
        public Set<String> namespaces() {
            return namespaces;
        }

        public Language language(String lang) {
            return languages.get(lang);
        }

        public Set<String> languages() {
            return languages.keySet();
        }

        /** Resolution for display: selected language, then English, per page. */
        public Resolved resolve(Identifier item, Kind kind, Predicate<Identifier> inTag, String lang) {
            List<String> chain = lang.equals(ENGLISH) ? List.of(ENGLISH) : List.of(lang, ENGLISH);
            return resolve(item, kind, inTag, chain, true);
        }

        /** The first family {@code item} belongs to (selected language, then English), or null. */
        public Family family(Identifier item, Predicate<Identifier> inTag, String lang) {
            for (String l : lang.equals(ENGLISH) ? List.of(ENGLISH) : List.of(lang, ENGLISH)) {
                Language language = languages.get(l);
                if (language == null) continue;
                for (Family family : language.families) {
                    if (family.matches(item, inTag)) return family;
                }
            }
            return null;
        }

        /** Resolution in exactly one language, own entries and families only (coverage check). */
        public Resolved resolveStrict(Identifier item, Predicate<Identifier> inTag, String lang) {
            return resolve(item, Kind.MISC, inTag, List.of(lang), false);
        }

        private Resolved resolve(Identifier item, Kind kind, Predicate<Identifier> inTag, List<String> chain, boolean withFallback) {
            Map<Page, List<String>> pages = new EnumMap<>(Page.class);
            Map<Page, Source> sources = new EnumMap<>(Page.class);
            for (Page page : Page.values()) {
                List<String> text = List.of();
                Source source = Source.NONE;
                for (String lang : chain) {
                    Language l = languages.get(lang);
                    Entry entry = l == null ? null : l.entries.get(item);
                    if (entry != null && !entry.page(page).isEmpty()) {
                        text = entry.page(page);
                        source = Source.ENTRY;
                        break;
                    }
                }
                if (source == Source.NONE) {
                    outer:
                    for (String lang : chain) {
                        Language l = languages.get(lang);
                        if (l == null) continue;
                        for (Family family : l.families) {
                            if (!family.entry.page(page).isEmpty() && family.matches(item, inTag)) {
                                text = family.entry.page(page);
                                source = Source.FAMILY;
                                break outer;
                            }
                        }
                    }
                }
                if (source == Source.NONE && withFallback) {
                    for (String lang : chain) {
                        Language l = languages.get(lang);
                        List<String> variants = l == null ? List.of() : l.fallback(kind, page);
                        if (!variants.isEmpty()) {
                            text = List.of(variants.get(Math.floorMod(fnv1a(item + "#" + page.key), variants.size())));
                            source = Source.FALLBACK;
                            break;
                        }
                    }
                }
                pages.put(page, text);
                sources.put(page, source);
            }
            return new Resolved(pages, sources);
        }
    }

    /** What the client shows; replaced by the reload listener. */
    public static volatile Library current = Library.EMPTY;

    /** A fresh client reload listener for the loader to register. */
    public static SimplePreparableReloadListener<Library> reloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected Library prepare(ResourceManager manager, ProfilerFiller profiler) {
                return load(manager);
            }

            @Override
            protected void apply(Library library, ResourceManager manager, ProfilerFiller profiler) {
                current = library;
            }
        };
    }

    /** Reads every {@code assets/<ns>/questionary/<lang>.json} of all packs (bottom pack first, top pack wins per key). */
    public static Library load(ResourceManager manager) {
        FileToIdConverter lister = FileToIdConverter.json(DIRECTORY);
        Map<Identifier, List<JsonObject>> files = new TreeMap<>();
        for (Map.Entry<Identifier, List<Resource>> e : lister.listMatchingResourceStacks(manager).entrySet()) {
            Identifier id = lister.fileToId(e.getKey());
            for (Resource resource : e.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonElement json = JsonParser.parseReader(reader);
                    if (json.isJsonObject()) files.computeIfAbsent(id, k -> new ArrayList<>()).add(json.getAsJsonObject());
                } catch (Exception ex) {
                    SimpleQuestionary.LOG.warn("Could not read questionary {}: {}", id, ex.toString());
                }
            }
        }
        return parse(files);
    }

    /** Builds the library from parsed files keyed {@code <ns>:<lang>} (several layers per file, bottom first). */
    public static Library parse(Map<Identifier, List<JsonObject>> files) {
        Map<String, Language> languages = new TreeMap<>();
        Set<String> namespaces = new java.util.TreeSet<>();
        for (Map.Entry<Identifier, List<JsonObject>> file : files.entrySet()) {
            String ns = file.getKey().getNamespace(), lang = file.getKey().getPath();
            namespaces.add(ns);
            Language language = languages.computeIfAbsent(lang, k -> new Language());
            Map<String, Family> families = new LinkedHashMap<>();
            for (JsonObject layer : file.getValue()) {
                for (Map.Entry<String, JsonElement> e : layer.entrySet()) {
                    String key = e.getKey();
                    if (key.startsWith("_") || !e.getValue().isJsonObject()) continue;
                    JsonObject value = e.getValue().getAsJsonObject();
                    if (key.equals("fallback")) {
                        for (Kind kind : Kind.values()) {
                            if (!value.has(kind.key()) || !value.get(kind.key()).isJsonObject()) continue;
                            Entry pages = entry(value.getAsJsonObject(kind.key()));
                            pages.pages().forEach((page, list) -> language.fallback.computeIfAbsent(kind, k -> new EnumMap<>(Page.class)).put(page, list));
                        }
                    } else if (key.startsWith("family:")) {
                        String name = key.substring("family:".length());
                        families.put(name, family(ns, name, value));
                    } else {
                        Identifier id = Identifier.tryParse(ns + ":" + key);
                        if (id != null) language.entries.put(id, entry(value));
                    }
                }
            }
            language.families.addAll(families.values());
        }
        return new Library(languages, namespaces);
    }

    private static Entry entry(JsonObject value) {
        Map<Page, List<String>> pages = new EnumMap<>(Page.class);
        for (Page page : Page.values()) {
            JsonElement element = value.get(page.key);
            if (element == null) continue;
            List<String> list = new ArrayList<>();
            if (element.isJsonArray()) {
                for (JsonElement p : element.getAsJsonArray()) if (p.isJsonPrimitive() && !p.getAsString().isBlank()) list.add(p.getAsString());
            } else if (element.isJsonPrimitive() && !element.getAsString().isBlank()) {
                list.add(element.getAsString());
            }
            if (!list.isEmpty()) pages.put(page, List.copyOf(list));
        }
        return new Entry(pages);
    }

    private static Family family(String ns, String name, JsonObject value) {
        List<Identifier> tags = new ArrayList<>();
        List<Pattern> patterns = new ArrayList<>();
        JsonArray items = value.has("items") && value.get("items").isJsonArray() ? value.getAsJsonArray("items") : new JsonArray();
        for (JsonElement element : items) {
            if (!element.isJsonPrimitive()) continue;
            String s = element.getAsString();
            if (s.startsWith("#")) {
                Identifier tag = Identifier.tryParse(s.substring(1));
                if (tag != null) tags.add(tag);
            } else if (s.startsWith("regex:")) {
                try {
                    patterns.add(Pattern.compile(s.substring("regex:".length())));
                } catch (PatternSyntaxException e) {
                    SimpleQuestionary.LOG.warn("Questionary family {}:{} has a bad pattern {}", ns, name, s);
                }
            }
        }
        return new Family(ns, name, List.copyOf(tags), List.copyOf(patterns), entry(value));
    }

    /** 32-bit FNV-1a over UTF-8 - stable across JVMs and runs (unlike relying on {@code String.hashCode}). */
    public static int fnv1a(String text) {
        int hash = 0x811C9DC5;
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
            hash ^= b & 0xFF;
            hash *= 0x01000193;
        }
        return hash;
    }

    /** Fills the placeholders; an unknown {@code {item:...}} id keeps its id as text. */
    public static String format(String text, String name, String mod, Function<Identifier, String> itemNames) {
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String replacement = switch (m.group(1)) {
                case "name" -> name;
                case "mod" -> mod;
                default -> {
                    Identifier id = Identifier.tryParse(m.group(2));
                    yield id == null ? m.group(2) : itemNames.apply(id);
                }
            };
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** Text problems of one page: leftover braces, TODO marks, over-long pages. Empty when the page is fine. */
    public static List<String> problems(List<String> paragraphs) {
        List<String> out = new ArrayList<>();
        String joined = String.join("\n", paragraphs);
        String filled = format(joined, "Name", "Mod", Identifier::getPath);
        if (filled.indexOf('{') >= 0 || filled.indexOf('}') >= 0) out.add("unresolved placeholder");
        if (joined.contains("TODO")) out.add("TODO left");
        if (filled.length() > MAX_PAGE_CHARS) out.add("longer than " + MAX_PAGE_CHARS + " characters (" + filled.length() + ")");
        return out;
    }

    /** Fallback class of an item from its default components. */
    public static Kind kindOf(Item item) {
        var components = item.components();
        var equippable = components.get(DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.slot().isArmor()) return Kind.ARMOR;
        if (components.has(DataComponents.TOOL) || components.has(DataComponents.WEAPON)) return Kind.TOOL;
        if (components.has(DataComponents.FOOD) || components.has(DataComponents.POTION_CONTENTS) || components.has(DataComponents.CONSUMABLE)) return Kind.FOOD;
        if (item instanceof BlockItem) return Kind.BLOCK;
        return Kind.MISC;
    }

    /** Coverage of the given namespaces: per namespace, items with a missing page in English or German, and text problems. */
    public record Coverage(Map<String, Integer> items, Map<String, Integer> missing, List<String> problems, List<Identifier> missingIds) {}

    public static Coverage coverage(Library library, Iterable<Identifier> registered, Set<String> namespaces, Function<Identifier, Predicate<Identifier>> tagsOf) {
        Map<String, Integer> items = new TreeMap<>(), missing = new TreeMap<>();
        List<String> problems = new ArrayList<>();
        List<Identifier> missingIds = new ArrayList<>();
        for (Identifier id : registered) {
            if (!namespaces.contains(id.getNamespace())) continue;
            items.merge(id.getNamespace(), 1, Integer::sum);
            boolean complete = true;
            for (String lang : List.of(ENGLISH, GERMAN)) {
                Resolved resolved = library.resolveStrict(id, tagsOf.apply(id), lang);
                for (Page page : Page.values()) {
                    if (resolved.source(page) == Source.NONE) complete = false;
                    else for (String p : problems(resolved.page(page))) problems.add(id + " " + lang + " " + page.key + ": " + p);
                }
            }
            if (!complete) {
                missing.merge(id.getNamespace(), 1, Integer::sum);
                missingIds.add(id);
            }
        }
        return new Coverage(items, missing, problems, missingIds);
    }

    /** Namespaces that ship at least one questionary file, plus {@code minecraft} (V1: Simple mods + Vanilla). */
    public static Set<String> describedNamespaces(Set<String> fileNamespaces) {
        Set<String> out = new LinkedHashSet<>(fileNamespaces);
        out.add("minecraft");
        return out;
    }

    private QuestionaryData() {}
}
