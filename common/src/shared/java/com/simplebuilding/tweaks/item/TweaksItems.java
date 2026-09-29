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
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/** Items aus Simple Tweaks: je Block ein BlockItem, dazu Spawn-Elytra, Laserpointer, Echo-Kompass, Lohenkopf. */
public final class TweaksItems {

    private static final List<Item> BLOCK_ITEMS = new ArrayList<>();

    public static final Item SPAWN_ELYTRA = register("spawn_elytra",
            p -> new SpawnElytraItem(p.stacksTo(1).fireResistant()));
    public static final Item LASER_POINTER = register("amethyst_lens",
            // "Amethystlinse"; die Haltbarkeit ist die Ladung (nie zerbrechend, Redstone im Amboss
            // laedt auf, siehe LaserPointerItem). Rezept in ModRecipeProvider.
            p -> new LaserPointerItem(p.durability(LaserPointerItem.MAX_CHARGE)));
    public static final Item ECHO_COMPASS = register("echo_sounder",
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

    /** Endermankopf (2026-09-28), Zutat des Spawn-Teleporters I; Notenblock spielt das Enderman-Geraeusch. */
    public static final Item ENDERMAN_HEAD = register("enderman_head",
            p -> new StandingAndWallBlockItem(TweaksBlocks.ENDERMAN_HEAD, TweaksBlocks.ENDERMAN_WALL_HEAD, Direction.DOWN,
                    Waypoint.addHideAttribute(p.useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON).equippableUnswappable(EquipmentSlot.HEAD)
                            .component(DataComponents.NOTE_BLOCK_SOUND, SoundEvents.ENDERMAN_AMBIENT.location()))));

    static {
        for (Block block : TweaksBlocks.all()) {
            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            // Seltenheit nach dem Material, mit dem die Stufe gebaut wird (docs/RARITAETEN.md):
            // Netherit UNCOMMON, Enderit EPIC, sonst COMMON; beides brennt nicht (wie Netherit-Gegenstaende,
            // vgl. EnderiteMachineTests#enderiteGearInheritsEveryNetheriteTrait).
            Rarity rarity = padRarity(path);
            boolean fireproof = rarity != Rarity.COMMON;
            // Alle Pads (jede Familie, jede Stufe, auch Easter- und alte Stufen) stapeln nicht (Besitzer
            // 2026-09-28): jedes Pad traegt Besitz, Easter-Stufe, Trank oder Abklingzeit als Einzelstueck.
            boolean pad = isPad(block);
            Item item = register(path, p -> {
                Item.Properties props = p.useBlockDescriptionPrefix();
                if (pad) {
                    props = props.stacksTo(1);
                }
                if (fireproof) {
                    props = props.fireResistant();
                }
                if (rarity != Rarity.COMMON) {
                    props = props.rarity(rarity);
                }
                // Alte Stufenbloecke tauschen sich im Inventar gegen ihre neue Stufe (LegacyTierBlockItem).
                if (block instanceof com.simplebuilding.tweaks.block.LegacyTierBlock legacy) {
                    return new LegacyTierBlockItem(block, props, () -> legacy.target().asItem());
                }
                if (block instanceof com.simplebuilding.tweaks.block.PotionPadBlock) {
                    return new PotionPadItem(block, props);
                }
                return new BlockItem(block, props);
            });
            BLOCK_ITEMS.add(item);
        }
    }

    private TweaksItems() {
    }

    /**
     * Seltenheit eines Druckplatten- oder Pad-Blocks nach dem Material seiner Stufe (docs/RARITAETEN.md):
     * mit Netherit gebaut (Netherit-Platten, Spawn-Teleporter II-IV aus Netherit-Druckplatten, Trank-Pad I aus
     * der Netherit-Druckplatte) UNCOMMON, mit Enderit gebaut (Enderit-Platten, alle Flypads aus Enderit-Platte
     * und -Kern, Feines Elytra-Pad V und Trank-Pad II/III als Aufwertungen darueber) EPIC, sonst COMMON.
     */
    static Rarity padRarity(String path) {
        if (path.startsWith("enderite_") || (path.endsWith("flypad") && !path.startsWith("netherite_"))
                || path.equals("fine_elytra_pad") || path.equals("reinforced_potion_pad") || path.equals("infused_potion_pad")) {
            return Rarity.EPIC;
        }
        if (path.startsWith("netherite_") || path.startsWith("spawn_teleporter_tier_") || path.equals("potion_pad")) {
            return Rarity.UNCOMMON;
        }
        return Rarity.COMMON;
    }

    public static void init() {
    }

    /**
     * Ob der Block ein Pad ist (Elytra-Pad, Flypad, Launchpad, Chunk-Loader, Spawn-Teleporter, Trank-Pad,
     * alte Flypad-Stufen) - nicht die Druckplatten, auch nicht die Kupferplatten, die technisch
     * {@link com.simplebuilding.tweaks.block.PadBlock} sind.
     */
    public static boolean isPad(Block block) {
        return block instanceof com.simplebuilding.tweaks.block.PadBlock
                && !(block instanceof com.simplebuilding.tweaks.block.CopperPressurePlateBlock);
    }

    /** Die Items aller Pads ({@link #isPad}), in Registrierungsreihenfolge. */
    public static List<Item> padItems() {
        List<Item> pads = new ArrayList<>();
        for (Item item : BLOCK_ITEMS) {
            if (item instanceof BlockItem blockItem && isPad(blockItem.getBlock())) {
                pads.add(item);
            }
        }
        return pads;
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
        all.add(ENDERMAN_HEAD);
        return all;
    }

    /**
     * Zeilen des Tabs "SimplePads" (ModItemGroupsContent#padsRows; bis 2026-09-29 in SimpleMachines), Besitzer 2026-09-28/29:
     * erst die Druckplatten - Eiche (als einzige Holzplatte, die anderen Holzarten stehen nur noch im
     * Vanilla-Tab), Stein und polierter Schwarzstein mit den Metall-/Materialplatten (schwer = Eisen,
     * leicht = Gold, Diamant, Netherit, Enderit) in einer Zeile, dann Kupfer (vier Stufen, dann gewachst).
     * Danach die Pads in Erz-Reihenfolge Kupfer, Eisen, Gold, Diamant, Netherit, Enderit: jede Familie mit
     * drei Stufen als "drei Stufen + ihre Freischalt-Zutat im vierten Feld", eine Luecke, dann die naechste
     * Familie in derselben Zeile. Zuletzt die weiteren Mobkoepfe ({@link #extraMobHeads()}).
     */
    public static List<CreativeTabLayout.Row> padsRows() {
        List<CreativeTabLayout.Row> rows = new ArrayList<>(List.of(
                CreativeTabLayout.Row.of("pressure_plates",
                        Items.OAK_PRESSURE_PLATE, Items.STONE_PRESSURE_PLATE, Items.POLISHED_BLACKSTONE_PRESSURE_PLATE,
                        Items.HEAVY_WEIGHTED_PRESSURE_PLATE, Items.LIGHT_WEIGHTED_PRESSURE_PLATE,
                        TweaksBlocks.DIAMOND_PRESSURE_PLATE, TweaksBlocks.NETHERITE_PRESSURE_PLATE, TweaksBlocks.ENDERITE_PRESSURE_PLATE),
                CreativeTabLayout.Row.of("copper_pressure_plates",
                        TweaksBlocks.COPPER_PRESSURE_PLATE, TweaksBlocks.EXPOSED_COPPER_PRESSURE_PLATE,
                        TweaksBlocks.WEATHERED_COPPER_PRESSURE_PLATE, TweaksBlocks.OXIDIZED_COPPER_PRESSURE_PLATE,
                        TweaksBlocks.WAXED_COPPER_PRESSURE_PLATE, TweaksBlocks.WAXED_EXPOSED_COPPER_PRESSURE_PLATE,
                        TweaksBlocks.WAXED_WEATHERED_COPPER_PRESSURE_PLATE, TweaksBlocks.WAXED_OXIDIZED_COPPER_PRESSURE_PLATE),
                // Kupfer (Chunk-Loader + Kupferkern), Luecke, Eisen (Launchpad + Eisenkern)
                CreativeTabLayout.Row.of("chunk_loaders_and_launchpads",
                        TweaksBlocks.CHUNK_LOADER, TweaksBlocks.NETHERITE_CHUNK_LOADER, TweaksBlocks.ENDERITE_CHUNK_LOADER,
                        com.simplebuilding.items.ModItems.COPPER_CORE, CreativeTabLayout.GAP,
                        TweaksBlocks.LAUNCHPAD, TweaksBlocks.NETHERITE_LAUNCHPAD, TweaksBlocks.ENDERITE_LAUNCHPAD,
                        com.simplebuilding.items.ModItems.IRON_CORE),
                // Gold: Spawn-Teleporter I-III + Endermankopf
                CreativeTabLayout.Row.of("spawn_teleporters",
                        TweaksBlocks.SPAWN_TELEPORTER, TweaksBlocks.SPAWN_TELEPORTER_TIER_2, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER,
                        ENDERMAN_HEAD),
                // Diamant: Elytra-Pads I-V, dann die Spawn-Elytra
                CreativeTabLayout.Row.of("elytra_pads",
                        TweaksBlocks.ELYTRA_PAD, TweaksBlocks.REINFORCED_ELYTRA_PAD, TweaksBlocks.NETHERITE_ELYTRA_PAD,
                        TweaksBlocks.ENDERITE_ELYTRA_PAD, TweaksBlocks.FINE_ELYTRA_PAD, SPAWN_ELYTRA),
                // Netherit (Trank-Pad + Lohenkopf), Luecke, Enderit (Flypad + Enderit-Kern)
                CreativeTabLayout.Row.of("potion_pads_and_flypads",
                        TweaksBlocks.POTION_PAD, TweaksBlocks.REINFORCED_POTION_PAD, TweaksBlocks.INFUSED_POTION_PAD, BLAZE_HEAD,
                        CreativeTabLayout.GAP,
                        TweaksBlocks.FLYPAD, TweaksBlocks.REINFORCED_FLYPAD, TweaksBlocks.STELLAR_FLYPAD,
                        com.simplebuilding.items.ModItems.ENDERITE_CORE)));
        // Weitere Mobkoepfe: eigene Zeile direkt unter Lohen- und Endermankopf, nur wenn es welche gibt.
        List<ItemLike> heads = extraMobHeads();
        if (!heads.isEmpty()) {
            rows.add(CreativeTabLayout.Row.of("mob_heads", heads.toArray(ItemLike[]::new)));
        }
        return List.copyOf(rows);
    }

    /**
     * Platz fuer weitere Mobkoepfe (etwa die der Pruefkammer-Mobs): hier eintragen, dann erscheinen sie in
     * SimplePads als Zeile "mob_heads" direkt unter der Zeile mit dem Lohenkopf (Lohen- und Endermankopf
     * bleiben als Freischalt-Zutat bei ihren Pads). Leer erscheint die Zeile nicht.
     */
    public static List<ItemLike> extraMobHeads() {
        return List.of();
    }

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleTweaks.id(name));
        return Registry.register(BuiltInRegistries.ITEM, SimpleTweaks.id(name), factory.apply(new Item.Properties().setId(key)));
    }
}
