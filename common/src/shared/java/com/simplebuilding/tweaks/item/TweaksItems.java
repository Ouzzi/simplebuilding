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
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.level.block.Block;

/** Items aus Simple Tweaks: je Block ein BlockItem, dazu Spawn-Elytra, Laserpointer, Echo-Kompass, Lohenkopf. */
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

    /**
     * Lohenkopf: wie Vanillas Mob-Koepfe (stehend und an der Wand, auf dem Kopf tragbar), Notenblock
     * spielt das Lohen-Geraeusch (Instrument CUSTOM_HEAD liest {@code note_block_sound}).
     */
    public static final Item BLAZE_HEAD = register("blaze_head",
            p -> new StandingAndWallBlockItem(TweaksBlocks.BLAZE_HEAD, TweaksBlocks.BLAZE_WALL_HEAD, Direction.DOWN,
                    Waypoint.addHideAttribute(p.useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON).equippableUnswappable(EquipmentSlot.HEAD)
                            .component(DataComponents.NOTE_BLOCK_SOUND, SoundEvents.BLAZE_AMBIENT.location()))));

    static {
        for (Block block : TweaksBlocks.all()) {
            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            // Enderit und die Netherstern-Stufen: episch; alles aus Netherit/Enderit brennt nicht
            // (wie Netherit-Gegenstaende, vgl. EnderiteMachineTests#enderiteGearInheritsEveryNetheriteTrait).
            // Trank-Pads: alle aus Netherit (brennen nicht), II und III aus Enderit-Aufwertungen (episch).
            boolean potionPad = path.endsWith("potion_pad");
            boolean epic = path.startsWith("enderite_") || path.equals("fine_elytra_pad") || path.equals("stellar_flypad")
                    || (potionPad && !path.equals("potion_pad"));
            boolean fireproof = epic || potionPad || path.startsWith("netherite_");
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
        all.add(BLAZE_HEAD);
        return all;
    }

    /** Zeilen fuer den Tab "Maschinen & Lager" (ModItemGroupsContent#functionalRows). */
    public static List<CreativeTabLayout.Row> functionalRows() {
        return List.of(
                CreativeTabLayout.Row.of("pressure_plates",
                        TweaksBlocks.DIAMOND_PRESSURE_PLATE, TweaksBlocks.NETHERITE_PRESSURE_PLATE, TweaksBlocks.ENDERITE_PRESSURE_PLATE,
                        TweaksBlocks.COPPER_PRESSURE_PLATE, TweaksBlocks.EXPOSED_COPPER_PRESSURE_PLATE,
                        TweaksBlocks.WEATHERED_COPPER_PRESSURE_PLATE, TweaksBlocks.OXIDIZED_COPPER_PRESSURE_PLATE),
                CreativeTabLayout.Row.of("elytra_pads",
                        SPAWN_ELYTRA, TweaksBlocks.ELYTRA_PAD, TweaksBlocks.REINFORCED_ELYTRA_PAD, TweaksBlocks.NETHERITE_ELYTRA_PAD,
                        TweaksBlocks.ENDERITE_ELYTRA_PAD, TweaksBlocks.FINE_ELYTRA_PAD),
                CreativeTabLayout.Row.of("flypads",
                        TweaksBlocks.FLYPAD, TweaksBlocks.REINFORCED_FLYPAD, TweaksBlocks.STELLAR_FLYPAD),
                CreativeTabLayout.Row.of("spawn_teleporters",
                        TweaksBlocks.SPAWN_TELEPORTER, TweaksBlocks.SPAWN_TELEPORTER_TIER_2, TweaksBlocks.SPAWN_TELEPORTER_TIER_3,
                        TweaksBlocks.SPAWN_TELEPORTER_TIER_4, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER),
                CreativeTabLayout.Row.of("travel_and_loading",
                        TweaksBlocks.LAUNCHPAD, TweaksBlocks.NETHERITE_LAUNCHPAD, TweaksBlocks.ENDERITE_LAUNCHPAD,
                        TweaksBlocks.CHUNK_LOADER, TweaksBlocks.NETHERITE_CHUNK_LOADER, TweaksBlocks.ENDERITE_CHUNK_LOADER),
                CreativeTabLayout.Row.of("potion_pads",
                        BLAZE_HEAD, TweaksBlocks.POTION_PAD, TweaksBlocks.REINFORCED_POTION_PAD, TweaksBlocks.INFUSED_POTION_PAD));
    }

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleTweaks.id(name));
        return Registry.register(BuiltInRegistries.ITEM, SimpleTweaks.id(name), factory.apply(new Item.Properties().setId(key)));
    }
}
