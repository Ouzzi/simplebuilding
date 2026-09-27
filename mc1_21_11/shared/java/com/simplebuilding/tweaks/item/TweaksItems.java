package com.simplebuilding.tweaks.item;

import com.simplebuilding.items.CreativeTabLayout;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/** Items aus Simple Tweaks: je Block ein BlockItem, dazu Spawn-Elytra, Laserpointer, Echo-Kompass. */
public final class TweaksItems {

    private static final List<Item> BLOCK_ITEMS = new ArrayList<>();

    public static final Item SPAWN_ELYTRA = register("spawn_elytra",
            p -> new SpawnElytraItem(p.stacksTo(1).fireResistant()));
    public static final Item LASER_POINTER = register("laser_pointer",
            // "Amethystlinse"; die Haltbarkeit ist die Ladung (nie zerbrechend, Redstone im Amboss
            // laedt auf, siehe LaserPointerItem). Rezept in ModRecipeProvider.
            p -> new LaserPointerItem(p.durability(LaserPointerItem.MAX_CHARGE).rarity(Rarity.EPIC)));
    public static final Item ECHO_COMPASS = register("echo_compass",
            // 1500 Reparaturpunkte, ein Sprung leert ihn; Echoscherben reparieren am Amboss je ein Viertel.
            p -> new EchoCompassItem(p.stacksTo(1).durability(EchoCompassItem.MAX_DAMAGE).enchantable(15)
                    .repairable(Items.ECHO_SHARD).rarity(Rarity.EPIC).fireResistant()));

    static {
        for (Block block : TweaksBlocks.all()) {
            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            // Enderit und die Netherstern-Stufen: episch; alles aus Netherit/Enderit brennt nicht
            // (wie Netherit-Gegenstaende, vgl. EnderiteMachineTests#enderiteGearInheritsEveryNetheriteTrait).
            boolean epic = path.startsWith("enderite_") || path.equals("fine_elytra_pad") || path.equals("stellar_flypad");
            boolean fireproof = epic || path.startsWith("netherite_");
            Item item = register(path, p -> {
                Item.Properties props = p.useBlockDescriptionPrefix();
                if (fireproof) {
                    props = props.fireResistant();
                }
                if (epic) {
                    props = props.rarity(Rarity.EPIC);
                }
                // Alte Stufenbloecke tauschen sich im Inventar gegen ihre neue Stufe (LegacyTierBlockItem).
                if (block instanceof com.simplebuilding.tweaks.block.LegacyFlypadBlock legacy) {
                    return new LegacyTierBlockItem(block, props, () -> legacy.target().asItem());
                }
                return new BlockItem(block, props);
            });
            BLOCK_ITEMS.add(item);
        }
    }

    private TweaksItems() {
    }

    public static void init() {
    }

    public static List<Item> blockItems() {
        return Collections.unmodifiableList(BLOCK_ITEMS);
    }

    /** Alle Items dieses Teils (Bloecke + Einzelitems). */
    public static List<Item> all() {
        List<Item> all = new ArrayList<>(BLOCK_ITEMS);
        all.add(SPAWN_ELYTRA);
        all.add(LASER_POINTER);
        all.add(ECHO_COMPASS);
        return all;
    }

    /**
     * Zeilen fuer den Tab "Maschinen & Lager" (ModItemGroupsContent#functionalRows). Druckplatten nach
     * Material in aufsteigender Stufe, Vanilla vor den Mod-Platten (Besitzer 2026-09-27): Holz, Stein,
     * Kupfer (vier Oxidationsstufen, dann gewachst), dann Eisen (schwer), Gold (leicht), Diamant, Netherit,
     * Enderit. Danach je Familie eine Zeile, Stufen aufsteigend.
     */
    public static List<CreativeTabLayout.Row> functionalRows() {
        return List.of(
                CreativeTabLayout.Row.of("wooden_pressure_plates", woodenPressurePlates()),
                CreativeTabLayout.Row.of("stone_pressure_plates",
                        Items.STONE_PRESSURE_PLATE, Items.POLISHED_BLACKSTONE_PRESSURE_PLATE),
                CreativeTabLayout.Row.of("copper_pressure_plates",
                        TweaksBlocks.COPPER_PRESSURE_PLATE, TweaksBlocks.EXPOSED_COPPER_PRESSURE_PLATE,
                        TweaksBlocks.WEATHERED_COPPER_PRESSURE_PLATE, TweaksBlocks.OXIDIZED_COPPER_PRESSURE_PLATE,
                        TweaksBlocks.WAXED_COPPER_PRESSURE_PLATE, TweaksBlocks.WAXED_EXPOSED_COPPER_PRESSURE_PLATE,
                        TweaksBlocks.WAXED_WEATHERED_COPPER_PRESSURE_PLATE, TweaksBlocks.WAXED_OXIDIZED_COPPER_PRESSURE_PLATE),
                CreativeTabLayout.Row.of("pressure_plates",
                        Items.HEAVY_WEIGHTED_PRESSURE_PLATE, Items.LIGHT_WEIGHTED_PRESSURE_PLATE,
                        TweaksBlocks.DIAMOND_PRESSURE_PLATE, TweaksBlocks.NETHERITE_PRESSURE_PLATE, TweaksBlocks.ENDERITE_PRESSURE_PLATE),
                CreativeTabLayout.Row.of("elytra_pads",
                        TweaksBlocks.ELYTRA_PAD, TweaksBlocks.REINFORCED_ELYTRA_PAD, TweaksBlocks.NETHERITE_ELYTRA_PAD,
                        TweaksBlocks.ENDERITE_ELYTRA_PAD, TweaksBlocks.FINE_ELYTRA_PAD, SPAWN_ELYTRA),
                CreativeTabLayout.Row.of("flypads",
                        TweaksBlocks.FLYPAD, TweaksBlocks.REINFORCED_FLYPAD, TweaksBlocks.STELLAR_FLYPAD),
                CreativeTabLayout.Row.of("spawn_teleporters",
                        TweaksBlocks.SPAWN_TELEPORTER, TweaksBlocks.SPAWN_TELEPORTER_TIER_2, TweaksBlocks.SPAWN_TELEPORTER_TIER_3,
                        TweaksBlocks.SPAWN_TELEPORTER_TIER_4, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER),
                CreativeTabLayout.Row.of("launchpads",
                        TweaksBlocks.LAUNCHPAD, TweaksBlocks.NETHERITE_LAUNCHPAD, TweaksBlocks.ENDERITE_LAUNCHPAD),
                CreativeTabLayout.Row.of("chunk_loaders",
                        TweaksBlocks.CHUNK_LOADER, TweaksBlocks.NETHERITE_CHUNK_LOADER, TweaksBlocks.ENDERITE_CHUNK_LOADER));
    }

    /**
     * Holzarten in Vanilla-Reihenfolge (Baeume, dann Bambus, dann Nether). Pappel gibt es erst ab MC 26.3;
     * was eine Linie nicht kennt, faellt weg.
     */
    public static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "pale_oak", "poplar", "bamboo", "crimson", "warped");

    /** Alle Holzdruckplatten dieser Minecraft-Version in {@link #WOODS}-Reihenfolge. */
    private static ItemLike[] woodenPressurePlates() {
        List<ItemLike> plates = new ArrayList<>();
        for (String wood : WOODS) {
            BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(wood + "_pressure_plate")).ifPresent(plates::add);
        }
        return plates.toArray(ItemLike[]::new);
    }

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleTweaks.id(name));
        return Registry.register(BuiltInRegistries.ITEM, SimpleTweaks.id(name), factory.apply(new Item.Properties().setId(key)));
    }
}
