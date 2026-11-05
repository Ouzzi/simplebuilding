package com.simplepads.item;

import com.simplepads.SimplePads;
import com.simplepads.block.PadsBlocks;
import com.simplepads.util.PadRefs;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/**
 * Items of Simple Pads (until 04.11.2026 SimpleBuilding {@code TweaksItems}): one block item per pad and plate, and the Spawn Elytra.
 * Mob heads, Resonance Rod and Echo Sounder stay with their own mods.
 */
public final class PadsItems {

    private static final List<Item> BLOCK_ITEMS = new ArrayList<>();

    public static final Item SPAWN_ELYTRA = register("spawn_elytra",
            p -> new SpawnElytraItem(p.stacksTo(1).fireResistant()));

    static {
        for (Block block : PadsBlocks.all()) {
            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            // Seltenheit nach dem Material, mit dem die Stufe gebaut wird (docs/RARITAETEN.md):
            // Netherit UNCOMMON, Enderit EPIC, sonst COMMON; beides brennt nicht (wie Netherit-Gegenstaende).
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
                if (block instanceof com.simplepads.block.LegacyTierBlock legacy) {
                    return new LegacyTierBlockItem(block, props, () -> legacy.target().asItem());
                }
                if (block instanceof com.simplepads.block.PotionPadBlock) {
                    return new PotionPadItem(block, props);
                }
                return new BlockItem(block, props);
            });
            BLOCK_ITEMS.add(item);
        }
    }

    private PadsItems() {
    }

    /**
     * Seltenheit eines Druckplatten- oder Pad-Blocks nach dem Material seiner Stufe (docs/RARITAETEN.md):
     * mit Netherit gebaut UNCOMMON, mit Enderit gebaut EPIC, sonst COMMON (wie bisher in SimpleBuilding).
     */
    static Rarity padRarity(String path) {
        if (path.startsWith("enderite_") || (path.endsWith("flypad") && !path.startsWith("netherite_"))
                || path.equals("fine_elytra_pad") || path.equals("infused_potion_pad")) {
            return Rarity.EPIC;
        }
        if (path.startsWith("netherite_") || path.startsWith("spawn_teleporter_tier_") || path.equals("potion_pad")
                || path.equals("reinforced_potion_pad")) {
            return Rarity.UNCOMMON;
        }
        return Rarity.COMMON;
    }

    public static void init() {
    }

    /**
     * Ob der Block ein Pad ist (Elytra-Pad, Flypad, Launchpad, Chunk-Loader, Spawn-Teleporter, Trank-Pad,
     * alte Flypad-Stufen) - nicht die Druckplatten, auch nicht die Kupferplatten, die technisch
     * {@link com.simplepads.block.PadBlock} sind.
     */
    public static boolean isPad(Block block) {
        return block instanceof com.simplepads.block.PadBlock
                && !(block instanceof com.simplepads.block.CopperPressurePlateBlock);
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

    /** Alle Items dieses Mods (Bloecke + Spawn-Elytra + Funny Stick). */
    public static List<Item> all() {
        List<Item> all = new ArrayList<>(BLOCK_ITEMS);
        all.add(SPAWN_ELYTRA);
        return all;
    }

    /**
     * Die Zeilen des Tabs "Simple Pads" wie bisher in SimpleBuilding (Besitzer 2026-09-28/29, B23-M6): erst die Druckplatten
     * (Eiche, Stein, polierter Schwarzstein, schwer, leicht, Diamant, Netherit, Enderit), dann Kupfer (vier Stufen, dann gewachst),
     * die Kerne (SimpleLib), die Pads je Stufe, zuletzt die Spawn-Elytra.
     */
    /** One row of the tab: name (as in SimpleBuilding's tab layout) and its stacks. */
    public record Row(String name, List<ItemStack> stacks) {
        static Row of(String name, ItemLike... items) {
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemLike item : items) stacks.add(new ItemStack(item));
            return new Row(name, List.copyOf(stacks));
        }
    }

    public static List<Row> padsRows() {
        return List.of(
                Row.of("pressure_plates", Items.OAK_PRESSURE_PLATE, Items.STONE_PRESSURE_PLATE, Items.POLISHED_BLACKSTONE_PRESSURE_PLATE,
                        Items.HEAVY_WEIGHTED_PRESSURE_PLATE, Items.LIGHT_WEIGHTED_PRESSURE_PLATE,
                        PadsBlocks.DIAMOND_PRESSURE_PLATE, PadsBlocks.NETHERITE_PRESSURE_PLATE, PadsBlocks.ENDERITE_PRESSURE_PLATE),
                Row.of("copper_pressure_plates", PadsBlocks.COPPER_PRESSURE_PLATE, PadsBlocks.EXPOSED_COPPER_PRESSURE_PLATE,
                        PadsBlocks.WEATHERED_COPPER_PRESSURE_PLATE, PadsBlocks.OXIDIZED_COPPER_PRESSURE_PLATE,
                        PadsBlocks.WAXED_COPPER_PRESSURE_PLATE, PadsBlocks.WAXED_EXPOSED_COPPER_PRESSURE_PLATE,
                        PadsBlocks.WAXED_WEATHERED_COPPER_PRESSURE_PLATE, PadsBlocks.WAXED_OXIDIZED_COPPER_PRESSURE_PLATE),
                Row.of("pad_cores", PadRefs.core("copper"), PadRefs.core("iron"), PadRefs.core("gold"), PadRefs.core("diamond"),
                        PadRefs.core("netherite"), PadRefs.core("enderite")),
                Row.of("pad_tier_1", PadsBlocks.CHUNK_LOADER, PadsBlocks.LAUNCHPAD, PadsBlocks.SPAWN_TELEPORTER,
                        PadsBlocks.ELYTRA_PAD, PadsBlocks.POTION_PAD, PadsBlocks.FLYPAD),
                Row.of("pad_tier_2", PadsBlocks.NETHERITE_CHUNK_LOADER, PadsBlocks.NETHERITE_LAUNCHPAD, PadsBlocks.SPAWN_TELEPORTER_TIER_2,
                        PadsBlocks.NETHERITE_ELYTRA_PAD, PadsBlocks.REINFORCED_POTION_PAD, PadsBlocks.REINFORCED_FLYPAD),
                Row.of("pad_tier_3", PadsBlocks.ENDERITE_CHUNK_LOADER, PadsBlocks.ENDERITE_LAUNCHPAD, PadsBlocks.ENDERITE_SPAWN_TELEPORTER,
                        PadsBlocks.ENDERITE_ELYTRA_PAD, PadsBlocks.INFUSED_POTION_PAD, PadsBlocks.STELLAR_FLYPAD),
                Row.of("pad_spawn_elytra", SPAWN_ELYTRA));
    }

    /** The tab's stacks in order (cores missing in this installation are left out). */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Row row : padsRows()) {
            for (ItemStack stack : row.stacks()) {
                if (!stack.isEmpty()) out.add(stack.copy());
            }
        }
        return out;
    }

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimplePads.id(name));
        return Registry.register(BuiltInRegistries.ITEM, SimplePads.id(name), factory.apply(new Item.Properties().setId(key)));
    }
}
