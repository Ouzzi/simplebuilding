package com.simplelib.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplelib.SimpleLib;
import com.simplelib.compat.jei.InWorldUpgrade;
import com.simplelib.compat.jei.MobDrop;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

/**
 * What the Simple mods show in a recipe viewer besides their datapack recipes (JEI24, owner 24.10.: every mod shows all
 * its ways in JEI, also on its own). Free of JEI classes: a module announces its entries from its init, SimpleLib's
 * optional JEI plugin ({@code SimpleLibJeiPlugin}) turns them into the shared tabs, and game tests compare them with the
 * sources without a client.
 *
 * <ul>
 *   <li>{@link #inWorld} - tab "In-World Upgrades" ({@code simplelib:in_world_upgrade}); SimpleLib adds every entry of
 *   {@link InWorldUpgrades#table()} itself ({@link #tableEntries}), minus what the partner shows in its own tab
 *   ({@link #shownByPartner}).</li>
 *   <li>{@link #special} - tab "Special Ways" ({@code simplelib:special}): machines and hand ways without a recipe.</li>
 *   <li>{@link #mobDrops} - tab "Mob Drops" ({@code simplelib:mob_drop}); {@link #lootDrops} reads a module's entity loot
 *   tables from its own jar (so a client on a dedicated server sees them too; datapack overrides are not shown).</li>
 * </ul>
 * Every supplier runs when JEI asks; one that throws is logged and skipped, the others still show.
 */
public final class RecipeViewer {
    /** Owner of the entries SimpleLib builds from the in-world upgrade table. */
    public static final String TABLE_OWNER = "simplelib/in_world_upgrades";
    private static final String NOTE = "jei.simplelib.note.";

    private static final Map<String, List<Supplier<List<InWorldUpgrade>>>> IN_WORLD = new LinkedHashMap<>();
    private static final Map<String, List<Supplier<List<InWorldUpgrade>>>> SPECIAL = new LinkedHashMap<>();
    private static final Map<String, List<Supplier<List<MobDrop>>>> MOB_DROPS = new LinkedHashMap<>();
    private static final Map<String, List<Supplier<List<Info>>>> INFO = new LinkedHashMap<>();

    /** A JEI info page: the items that carry it and its lines (for items only loot or code gives). */
    public record Info(List<ItemStack> items, List<Component> lines) {
    }
    private static volatile Predicate<Block> partnerShows = block -> false;
    private static volatile BiPredicate<String, Item> toolGrade = (grade, tool) -> true;

    private RecipeViewer() {
    }

    // ------------------------------------------------------------ announcing

    /** In-world upgrades of {@code owner} (usually the module id) for the tab "In-World Upgrades". */
    public static synchronized void inWorld(String owner, Supplier<List<InWorldUpgrade>> entries) {
        IN_WORLD.computeIfAbsent(owner, k -> new ArrayList<>()).add(entries);
    }

    /** Machine and hand ways of {@code owner} for the tab "Special Ways". */
    public static synchronized void special(String owner, Supplier<List<InWorldUpgrade>> entries) {
        SPECIAL.computeIfAbsent(owner, k -> new ArrayList<>()).add(entries);
    }

    /** Mob drops of {@code owner} for the tab "Mob Drops". */
    public static synchronized void mobDrops(String owner, Supplier<List<MobDrop>> drops) {
        MOB_DROPS.computeIfAbsent(owner, k -> new ArrayList<>()).add(drops);
    }

    /** Info pages of {@code owner} (items no recipe, loot tab or way shows, e.g. structure loot). */
    public static synchronized void info(String owner, Supplier<List<Info>> pages) {
        INFO.computeIfAbsent(owner, k -> new ArrayList<>()).add(pages);
    }

    /** The default loot tables of every mob of {@code namespace}, read from the jar of {@code anchor} (a class of that mod). */
    public static void lootDrops(String namespace, Class<?> anchor) {
        mobDrops(namespace, () -> lootTableDrops(namespace, anchor));
    }

    /** The blocks whose upgrade the partner (SimpleBuilding) shows in its own tab; SimpleLib leaves them out. */
    public static void shownByPartner(Predicate<Block> blocks) {
        partnerShows = blocks == null ? block -> false : blocks;
    }

    /** The current partner rule (tests restore it). */
    public static Predicate<Block> partnerShowsNow() {
        return partnerShows;
    }

    /** Whether a tool fits a {@code min_tool} grade (the hammer mod knows its ranks); default: every tool fits. */
    public static void toolGrade(BiPredicate<String, Item> fits) {
        toolGrade = fits == null ? (grade, tool) -> true : fits;
    }

    // ------------------------------------------------------------ reading

    /** All announced in-world upgrades (without the table entries, see {@link #tableEntries}). */
    public static List<InWorldUpgrade> inWorld() {
        return collect(IN_WORLD, null);
    }

    public static List<InWorldUpgrade> inWorld(String owner) {
        return collect(IN_WORLD, owner);
    }

    public static List<InWorldUpgrade> special() {
        return collect(SPECIAL, null);
    }

    public static List<InWorldUpgrade> special(String owner) {
        return collect(SPECIAL, owner);
    }

    public static List<MobDrop> mobDrops() {
        return collect(MOB_DROPS, null);
    }

    public static List<MobDrop> mobDrops(String owner) {
        return collect(MOB_DROPS, owner);
    }

    public static List<Info> info() {
        return collect(INFO, null);
    }

    public static List<Info> info(String owner) {
        return collect(INFO, owner);
    }

    /** Owners that announced anything (tests, coverage script). */
    public static synchronized Set<String> owners() {
        Set<String> out = new LinkedHashSet<>(IN_WORLD.keySet());
        out.addAll(SPECIAL.keySet());
        out.addAll(MOB_DROPS.keySet());
        out.addAll(INFO.keySet());
        return out;
    }

    private static synchronized <T> List<T> collect(Map<String, List<Supplier<List<T>>>> map, @Nullable String owner) {
        List<T> out = new ArrayList<>();
        map.forEach((key, suppliers) -> {
            if (owner != null && !owner.equals(key)) return;
            for (Supplier<List<T>> supplier : suppliers) {
                try {
                    List<T> entries = supplier.get();
                    if (entries != null) out.addAll(entries);
                } catch (RuntimeException e) {
                    SimpleLib.LOG.warn("Recipe viewer entries of {} skipped: {}", key, e.toString());
                }
            }
        });
        return out;
    }

    // ------------------------------------------------------------ the in-world upgrade table

    /** The effective {@link InWorldUpgrades#table()} as tab entries (empty before the first load). */
    public static List<InWorldUpgrade> tableEntries() {
        return fromTable(InWorldUpgrades.table());
    }

    /**
     * One entry per upgrade: the block and its material in, the tools that may strike (the entry's tool tag, axes too
     * while the axe ways are on, each within its {@code min_tool} grade), the next block out. Left out: blocks without
     * an item, what the partner shows itself, upgrades no present tool can do.
     */
    public static List<InWorldUpgrade> fromTable(Map<Block, InWorldUpgrades.Entry> table) {
        List<InWorldUpgrades.Entry> entries = new ArrayList<>(table.values());
        entries.sort(java.util.Comparator.comparing(e -> BuiltInRegistries.BLOCK.getKey(e.from()).toString()));
        List<InWorldUpgrade> out = new ArrayList<>();
        for (InWorldUpgrades.Entry entry : entries) {
            Item from = entry.from().asItem();
            Item to = entry.to().asItem();
            if (from == Items.AIR || to == Items.AIR || partnerShows.test(entry.from())) continue;
            List<Item> tools = tools(entry);
            if (tools.isEmpty()) continue;
            HammerActions.Staged staged = HammerActions.staged(entry.from());
            int strikes = staged != null ? staged.strikes() : InWorldUpgrades.AXE_STRIKES * entry.durationFactor();
            List<Component> notes = new ArrayList<>();
            notes.add(Component.translatable(NOTE + "upgrade.strikes", strikes));
            if (!"any".equals(entry.minTool()) && !entry.minTool().isBlank()) {
                notes.add(Component.translatable(NOTE + "upgrade.min_tool", Component.translatable("jei.simplelib.grade." + entry.minTool())));
            }
            if (entry.damagePerHit() > 0) notes.add(Component.translatable(NOTE + "upgrade.damage", entry.damagePerHit()));
            Identifier key = BuiltInRegistries.BLOCK.getKey(entry.from());
            out.add(new InWorldUpgrade("table/" + key.getNamespace() + "/" + key.getPath(),
                    List.of(InWorldUpgrade.Input.of(from, 1), InWorldUpgrade.Input.of(entry.material(), entry.materialCount())),
                    tools, new ItemStack(to), notes));
        }
        return out;
    }

    /** The tools that may run {@code entry}, hammers (or the tag's items) first, then the axes of the axe ways. */
    public static List<Item> tools(InWorldUpgrades.Entry entry) {
        Set<Item> out = new LinkedHashSet<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(entry.tool())) {
            if (toolGrade.test(entry.minTool(), holder.value())) out.add(holder.value());
        }
        if (entry.tool().equals(InWorldUpgrades.UPGRADE_TOOLS) && SimpleLibApi.axeWaysEnabled()) {
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.AXES)) {
                if (InWorldUpgrades.axeGradeFits(entry.minTool(), new ItemStack(holder.value()))) out.add(holder.value());
            }
        }
        return new ArrayList<>(out);
    }

    /** The items of an item tag, in registry order. */
    public static List<Item> tagged(TagKey<Item> tag) {
        List<Item> out = new ArrayList<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) out.add(holder.value());
        return out;
    }

    /** The upgrade tools of the current mode: hammers, plus axes while the axe ways are on (for module entries). */
    public static List<Item> upgradeTools() {
        Set<Item> out = new LinkedHashSet<>(tagged(InWorldUpgrades.HAMMERS));
        out.addAll(tagged(InWorldUpgrades.UPGRADE_TOOLS));
        if (SimpleLibApi.axeWaysEnabled()) out.addAll(tagged(ItemTags.AXES));
        return new ArrayList<>(out);
    }

    // ------------------------------------------------------------ mob drops from loot tables

    /** A mob's spawn egg, or empty. */
    public static ItemStack egg(EntityType<?> type) {
        return SpawnEggItem.byId(type).map(holder -> new ItemStack(holder.value())).orElse(ItemStack.EMPTY);
    }

    /** The spawn egg of entity {@code id}, or empty (mob not registered). */
    public static ItemStack egg(String id) {
        Identifier key = Identifier.tryParse(id);
        if (key == null) return ItemStack.EMPTY;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(key).map(RecipeViewer::egg).orElse(ItemStack.EMPTY);
    }

    /** The mobs of {@code namespace} that have a default loot table, by id. */
    public static List<EntityType<?>> mobsWithLoot(String namespace) {
        List<EntityType<?>> out = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (namespace.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace()) && type.getDefaultLootTable().isPresent()) {
                out.add(type);
            }
        }
        out.sort(java.util.Comparator.comparing(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()));
        return out;
    }

    /** One entry per mob of {@code namespace} whose shipped loot table names at least one item. */
    public static List<MobDrop> lootTableDrops(String namespace, Class<?> anchor) {
        List<MobDrop> out = new ArrayList<>();
        for (EntityType<?> type : mobsWithLoot(namespace)) {
            Identifier table = type.getDefaultLootTable().get().identifier();
            JsonElement json = readLootTable(table, anchor);
            if (json == null) continue;
            List<Item> items = lootItems(json, table.getNamespace(), anchor, 0);
            if (items.isEmpty()) continue;
            List<ItemStack> results = new ArrayList<>();
            items.forEach(item -> results.add(new ItemStack(item)));
            out.add(new MobDrop(namespace + "/loot/" + BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath(), type, egg(type),
                    ItemStack.EMPTY, results, List.of(Component.translatable(NOTE + "mob_drop.loot", type.getDescription()))));
        }
        return out;
    }

    /** {@code data/<ns>/loot_table/<path>.json} from the jar of {@code anchor}, or null. */
    public static @Nullable JsonElement readLootTable(Identifier table, Class<?> anchor) {
        String path = "/data/" + table.getNamespace() + "/loot_table/" + table.getPath() + ".json";
        try (InputStream in = anchor.getResourceAsStream(path)) {
            if (in == null) return null;
            return com.google.gson.JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            SimpleLib.LOG.warn("Could not read loot table {}: {}", path, e.toString());
            return null;
        }
    }

    /**
     * Every item a loot table can name, in order, each once: item entries, item tags (as bound now), nested
     * alternatives/groups/sequences and referenced tables of the same namespace. Conditions and counts are not read.
     */
    public static List<Item> lootItems(JsonElement table, String namespace, @Nullable Class<?> anchor, int depth) {
        Set<Item> out = new LinkedHashSet<>();
        if (table != null && table.isJsonObject() && table.getAsJsonObject().has("pools")) {
            for (JsonElement pool : table.getAsJsonObject().getAsJsonArray("pools")) {
                if (pool.isJsonObject() && pool.getAsJsonObject().has("entries")) {
                    entries(pool.getAsJsonObject().getAsJsonArray("entries"), namespace, anchor, depth, out);
                }
            }
        }
        return new ArrayList<>(out);
    }

    private static void entries(JsonArray entries, String namespace, @Nullable Class<?> anchor, int depth, Set<Item> out) {
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) continue;
            JsonObject entry = element.getAsJsonObject();
            String type = entry.has("type") ? entry.get("type").getAsString() : "";
            switch (type.startsWith("minecraft:") ? type.substring(10) : type) {
                case "item" -> item(string(entry, "name"), out);
                case "tag" -> {
                    String tag = string(entry, entry.has("items") ? "items" : "name");
                    Identifier key = tag == null ? null : Identifier.tryParse(tag.startsWith("#") ? tag.substring(1) : tag);
                    if (key != null) out.addAll(tagged(TagKey.create(Registries.ITEM, key)));
                }
                case "loot_table" -> {
                    String ref = string(entry, "value");
                    Identifier key = ref == null ? null : Identifier.tryParse(ref);
                    if (key != null && anchor != null && depth < 4) {
                        JsonElement nested = readLootTable(key, anchor);
                        if (nested != null) out.addAll(lootItems(nested, namespace, anchor, depth + 1));
                    }
                }
                default -> {
                    if (entry.has("children")) entries(entry.getAsJsonArray("children"), namespace, anchor, depth, out);
                }
            }
        }
    }

    private static @Nullable String string(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : null;
    }

    private static void item(@Nullable String id, Set<Item> out) {
        Identifier key = id == null ? null : Identifier.tryParse(id);
        if (key == null) return;
        BuiltInRegistries.ITEM.getOptional(key).filter(item -> item != Items.AIR).ifPresent(out::add);
    }

    // ------------------------------------------------------------ checks (game tests)

    /**
     * Problems of {@code owner}'s mob drops against the server (empty = fine): every mob of {@code namespace} whose loot
     * table the server has must have an entry, and every entry's mob must be of that namespace and drop something.
     */
    public static List<String> lootDropProblems(MinecraftServer server, String namespace, String owner) {
        List<String> problems = new ArrayList<>();
        List<MobDrop> drops = mobDrops(owner);
        Set<EntityType<?>> covered = new LinkedHashSet<>();
        for (MobDrop drop : drops) {
            covered.add(drop.victim());
            if (drop.results().isEmpty()) problems.add(drop.id() + " drops nothing");
        }
        for (EntityType<?> type : mobsWithLoot(namespace)) {
            ResourceKey<LootTable> key = type.getDefaultLootTable().get();
            LootTable table = server.reloadableRegistries().getLootTable(key);
            if (table != LootTable.EMPTY && !covered.contains(type)) {
                problems.add(BuiltInRegistries.ENTITY_TYPE.getKey(type) + " has loot table " + key.identifier() + " but no JEI entry");
            }
        }
        return problems;
    }
}
