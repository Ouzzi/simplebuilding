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
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

/** Items aus Simple Tweaks: je Block ein BlockItem, dazu Spawn-Elytra, Laserpointer, Echo-Kompass. */
public final class TweaksItems {

    private static final List<Item> BLOCK_ITEMS = new ArrayList<>();

    public static final Item SPAWN_ELYTRA = register("spawn_elytra",
            p -> new SpawnElytraItem(p.stacksTo(1).fireResistant()));
    public static final Item LASER_POINTER = register("laser_pointer",
            p -> new LaserPointerItem(p.stacksTo(1).durability(500).rarity(Rarity.EPIC)));
    public static final Item ECHO_COMPASS = register("echo_compass",
            p -> new EchoCompassItem(p.stacksTo(1).durability(EchoCompassItem.DURABILITY).enchantable(15)
                    .rarity(Rarity.EPIC).fireResistant()));

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

    /** Zeilen fuer den Tab "Maschinen & Lager" (ModItemGroupsContent#functionalRows). */
    public static List<CreativeTabLayout.Row> functionalRows() {
        return List.of(
                CreativeTabLayout.Row.of("pressure_plates",
                        Items.LIGHT_WEIGHTED_PRESSURE_PLATE, Items.HEAVY_WEIGHTED_PRESSURE_PLATE,
                        TweaksBlocks.DIAMOND_PRESSURE_PLATE, TweaksBlocks.NETHERITE_PRESSURE_PLATE, TweaksBlocks.ENDERITE_PRESSURE_PLATE,
                        TweaksBlocks.COPPER_PRESSURE_PLATE, TweaksBlocks.EXPOSED_COPPER_PRESSURE_PLATE,
                        TweaksBlocks.WEATHERED_COPPER_PRESSURE_PLATE, TweaksBlocks.OXIDIZED_COPPER_PRESSURE_PLATE),
                CreativeTabLayout.Row.of("elytra_pads",
                        SPAWN_ELYTRA, TweaksBlocks.ELYTRA_PAD, TweaksBlocks.REINFORCED_ELYTRA_PAD, TweaksBlocks.NETHERITE_ELYTRA_PAD,
                        TweaksBlocks.ENDERITE_ELYTRA_PAD, TweaksBlocks.FINE_ELYTRA_PAD),
                CreativeTabLayout.Row.of("flypads",
                        TweaksBlocks.FLYPAD, TweaksBlocks.REINFORCED_FLYPAD, TweaksBlocks.NETHERITE_FLYPAD,
                        TweaksBlocks.ENDERITE_FLYPAD, TweaksBlocks.STELLAR_FLYPAD),
                CreativeTabLayout.Row.of("spawn_teleporters",
                        TweaksBlocks.SPAWN_TELEPORTER, TweaksBlocks.SPAWN_TELEPORTER_TIER_2, TweaksBlocks.SPAWN_TELEPORTER_TIER_3,
                        TweaksBlocks.SPAWN_TELEPORTER_TIER_4, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER, ECHO_COMPASS),
                CreativeTabLayout.Row.of("travel_and_loading",
                        TweaksBlocks.LAUNCHPAD, TweaksBlocks.ENDERITE_LAUNCHPAD, TweaksBlocks.CHUNK_LOADER,
                        TweaksBlocks.ENDERITE_CHUNK_LOADER, LASER_POINTER));
    }

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleTweaks.id(name));
        return Registry.register(BuiltInRegistries.ITEM, SimpleTweaks.id(name), factory.apply(new Item.Properties().setId(key)));
    }
}
