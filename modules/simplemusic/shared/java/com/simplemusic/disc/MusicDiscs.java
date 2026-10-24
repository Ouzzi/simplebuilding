package com.simplemusic.disc;

import com.simplemusic.SimpleMusic;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * The six music discs of Simple Music (W24-3, owner 23.10., docs/ai/PLAN-A-SOUND24-2026-10-23.md): instrumental pieces
 * made with ElevenLabs Music (Creator plan, the output belongs to the owner; free mod, no artist names in the prompts).
 * Like Vanilla's structure discs: plain items with {@code jukebox_playable}, found only in the chests (or the brushed
 * gravel) of one structure each, no recipe. The songs are data ({@code data/simplemusic/jukebox_song/<id>.json}, the
 * sound event {@code simplemusic:music_disc.<id>} given directly, so no loader has to register it); the music streams from
 * {@code assets/simplemusic/sounds/records/<id>.ogg}.
 */
public final class MusicDiscs {
    /**
     * One disc: song id, length in seconds, comparator level, the loot table it is found in and its chance there - for a
     * chest an extra pool (1 roll, {@code weight} against {@code 100 - weight} empty), for archaeology one more entry of
     * weight {@code weight} in the table's own pool (brushing yields exactly one item).
     */
    public record Disc(String id, float seconds, int comparator, ResourceKey<LootTable> table, int weight) {
        public ResourceKey<JukeboxSong> song() {
            return ResourceKey.create(Registries.JUKEBOX_SONG, SimpleMusic.id(id));
        }

        public Identifier sound() {
            return SimpleMusic.id("music_disc." + id);
        }

        public boolean archaeology() {
            return table.identifier().getPath().startsWith("archaeology/");
        }
    }

    public static final List<Disc> DISCS = List.of(
            new Disc("ember_forge", 90.0F, 13, table("chests/bastion_other"), 5),
            new Disc("obsidian_throne", 100.0F, 9, table("chests/nether_bridge"), 4),
            new Disc("end_departure", 100.0F, 14, table("chests/end_city_treasure"), 4),
            new Disc("fortress_march", 90.0F, 8, table("chests/stronghold_corridor"), 4),
            new Disc("deep_pulse", 90.0F, 15, table("chests/ancient_city"), 4),
            new Disc("sky_forge", 90.0F, 4, table("archaeology/trail_ruins_rare"), 1));

    /** The disc items in {@link #DISCS} order (empty until registered). */
    public static final List<Item> ITEMS = new ArrayList<>();

    private static ResourceKey<LootTable> table(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(path));
    }

    /** {@code music_disc_<id>} through the registry's item helper (called from {@code MusicRegistry.registerItems}). */
    public static void register(java.util.function.BiFunction<String, Item.Properties, Item> item) {
        if (!ITEMS.isEmpty()) return;
        for (Disc disc : DISCS) {
            ITEMS.add(item.apply("music_disc_" + disc.id(), new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(disc.song())));
        }
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
