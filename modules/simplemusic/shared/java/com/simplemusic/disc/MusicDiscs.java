package com.simplemusic.disc;

import com.simplemusic.SimpleMusic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import org.jspecify.annotations.Nullable;

/**
 * The music discs of Simple Music (W24-3, owner 23.10., docs/ai/PLAN-A-SOUND24-2026-10-23.md; three shop discs a-shopmusic37,
 * docs/ai/PLAN-A-SHOPMUSIC37-2026-11-12.md): instrumental pieces
 * made with ElevenLabs Music (Creator plan, the output belongs to the owner; free mod, no artist names in the prompts).
 * Like Vanilla's structure discs: plain items with {@code jukebox_playable}, found only in the chests (or the brushed
 * gravel) of one structure each, no recipe. The songs are data ({@code data/simplemusic/jukebox_song/<id>.json}, the
 * sound event {@code simplemusic:music_disc.<id>} given directly, so no loader has to register it); the music streams from
 * {@code assets/simplemusic/sounds/records/<id>.ogg}.
 *
 * <p>A-/B-sides (a-shopmusic37b, owner 12.11. "alle Takes nutzen, mit B-Seite der Platte", docs/ai/PLAN-A-SHOPMUSIC37B-2026-11-12.md):
 * the four shop discs have a B-side - like SimpleBuilding's dimension discs its own item {@code music_disc_<id>_b_side} with its
 * own song {@code <id>_b_side} ("... (B-Side)"), same comparator level, label colours swapped. A right click with the disc in hand
 * turns it over ({@link SidedDiscItem}); B-sides are never loot and never sold, they come only from turning an A-side.
 */
public final class MusicDiscs {
    /**
     * One disc: song id, length in seconds, comparator level, the loot table it is found in and its chance there - for a
     * chest an extra pool (1 roll, {@code weight} against {@code 100 - weight} empty), for archaeology one more entry of
     * weight {@code weight} in the table's own pool (brushing yields exactly one item).
     */
    public record Disc(String id, float seconds, int comparator, ResourceKey<LootTable> table, int weight, float bSeconds) {
        /** A disc with only an A-side. */
        public Disc(String id, float seconds, int comparator, ResourceKey<LootTable> table, int weight) {
            this(id, seconds, comparator, table, weight, 0.0F);
        }

        public ResourceKey<JukeboxSong> song() {
            return ResourceKey.create(Registries.JUKEBOX_SONG, SimpleMusic.id(id));
        }

        public Identifier sound() {
            return SimpleMusic.id("music_disc." + id);
        }

        public boolean archaeology() {
            return table.identifier().getPath().startsWith("archaeology/");
        }

        /** Whether the disc has a B-side ({@code bSeconds} = its length). */
        public boolean sided() {
            return bSeconds > 0.0F;
        }

        /** Id of the B-side: item {@code music_disc_<bId>}, song and sound file {@code <bId>}. */
        public String bId() {
            return id + "_b_side";
        }

        public ResourceKey<JukeboxSong> bSong() {
            return ResourceKey.create(Registries.JUKEBOX_SONG, SimpleMusic.id(bId()));
        }

        public Identifier bSound() {
            return SimpleMusic.id("music_disc." + bId());
        }
    }

    public static final List<Disc> DISCS = List.of(
            new Disc("ember_forge", 90.0F, 13, table("chests/bastion_other"), 5),
            new Disc("obsidian_throne", 100.0F, 9, table("chests/nether_bridge"), 4),
            new Disc("end_departure", 100.0F, 14, table("chests/end_city_treasure"), 4),
            new Disc("fortress_march", 90.0F, 8, table("chests/stronghold_corridor"), 4),
            new Disc("deep_pulse", 90.0F, 15, table("chests/ancient_city"), 4),
            new Disc("sky_forge", 90.0F, 4, table("archaeology/trail_ruins_rare"), 1),
            // a-shopmusic37 (owner 12.11.): relaxed lounge / bossa shop discs (seamless loops, Simple Shops plays them in a shop);
            // found in village chests and sold by the wandering trader (data: villager_trade/wandering_trader).
            // a-shopmusic37b: every shop disc has a B-side (the last number: its length); all nine takes are used - the fourth
            // disc "Bargain Bin" holds the rest, its B-side a medley of two takes in the same key
            new Disc("browsing_bossa", 69.9F, 2, table("chests/village/village_savanna_house"), 5, 52.8F),
            new Disc("coin_lounge", 69.95F, 6, table("chests/village/village_cartographer"), 5, 48.0F),
            new Disc("window_shopping", 69.5F, 11, table("chests/village/village_plains_house"), 4, 53.33F),
            new Disc("bargain_bin", 48.0F, 7, table("chests/village/village_desert_house"), 4, 120.64F));

    /** The disc items (A-sides) in {@link #DISCS} order (empty until registered). */
    public static final List<Item> ITEMS = new ArrayList<>();
    /** The B-side items by disc id (empty until registered). */
    private static final Map<String, Item> B_SIDES = new HashMap<>();
    /** A-side to B-side and back. */
    private static final Map<Item, Item> OTHER_SIDE = new HashMap<>();

    /** How the registry creates an item: {@code (name, factory, properties)}. */
    @FunctionalInterface
    public interface ItemRegistrar {
        Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties);
    }

    private static ResourceKey<LootTable> table(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(path));
    }

    /**
     * {@code music_disc_<id>} (and {@code music_disc_<id>_b_side} for a sided disc) through the registry's item helper (called from
     * {@code MusicRegistry.registerItems}).
     */
    public static void register(ItemRegistrar registrar) {
        if (!ITEMS.isEmpty()) return;
        for (Disc disc : DISCS) {
            Function<Item.Properties, Item> factory = disc.sided() ? SidedDiscItem::new : Item::new;
            Item a = registrar.item("music_disc_" + disc.id(), factory, properties(disc.song()));
            ITEMS.add(a);
            if (disc.sided()) {
                Item b = registrar.item("music_disc_" + disc.bId(), SidedDiscItem::new, properties(disc.bSong()));
                B_SIDES.put(disc.id(), b);
                OTHER_SIDE.put(a, b);
                OTHER_SIDE.put(b, a);
            }
        }
    }

    private static Item.Properties properties(ResourceKey<JukeboxSong> song) {
        return new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(song);
    }

    /** The B-side item of {@code disc}, or {@code null} (no B-side or not registered). */
    public static @Nullable Item bSide(Disc disc) {
        return B_SIDES.get(disc.id());
    }

    /** The other side of a sided disc item, or {@code null} for any other item. */
    public static @Nullable Item otherSide(Item item) {
        return OTHER_SIDE.get(item);
    }

    /**
     * The disc turned over: the same stack (name and other changes kept) as the other side's item; empty when {@code stack} has no
     * other side or the module is switched off.
     */
    public static ItemStack turnOver(ItemStack stack) {
        Item other = SimpleMusic.on() ? otherSide(stack.getItem()) : null;
        return other == null ? ItemStack.EMPTY : stack.transmuteCopy(other, stack.getCount());
    }

    /** All disc items for tabs and lists: each A-side followed by its B-side (empty until registered). */
    public static List<Item> allItems() {
        List<Item> out = new ArrayList<>();
        for (int i = 0; i < Math.min(DISCS.size(), ITEMS.size()); i++) {
            out.add(ITEMS.get(i));
            Item b = bSide(DISCS.get(i));
            if (b != null) out.add(b);
        }
        return out;
    }

    /**
     * Loader loot hook: adds the disc of {@code key} - a new pool for a chest ({@code addPool}), one more entry in the
     * existing pool for archaeology ({@code modifyPools}). Nothing while the module is switched off.
     */
    public static void loot(ResourceKey<LootTable> key, Consumer<LootPool.Builder> addPool, Consumer<Consumer<LootPool.Builder>> modifyPools) {
        if (ITEMS.isEmpty() || !SimpleMusic.on()) return;
        for (int i = 0; i < DISCS.size(); i++) {
            Disc disc = DISCS.get(i);
            if (!disc.table().equals(key)) continue;
            Item item = ITEMS.get(i);
            if (disc.archaeology()) {
                modifyPools.accept(pool -> pool.add(LootItem.lootTableItem(item).setWeight(disc.weight())));
            } else {
                addPool.accept(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                        .add(LootItem.lootTableItem(item).setWeight(disc.weight()))
                        .add(EmptyLootItem.emptyItem().setWeight(100 - disc.weight())));
            }
        }
    }

    private MusicDiscs() {}
}
