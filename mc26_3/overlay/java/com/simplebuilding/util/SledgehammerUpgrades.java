package com.simplebuilding.util;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * 26.3 (split stage 7): the in-world upgrade engine is Simple Hammers' ({@link com.simplehammers.engine.SledgehammerUpgrades});
 * SimpleBuilding's code calls it through the inherited statics of this class. SimpleBuilding keeps what only it knows:
 * the built-in table its datapack files are generated from (also the engine's table before the first datapack load,
 * {@code LibServices.init}); the upgrade hints in the tooltips are the module's since Queue 738. The 26.2 twin (common/src/mc26_2/java)
 * keeps SimpleBuilding's own engine.
 */
public final class SledgehammerUpgrades extends com.simplehammers.engine.SledgehammerUpgrades {
    private static volatile @Nullable Map<Block, Upgrade> builtIn;

    private SledgehammerUpgrades() {
    }

    /** Whether this step upgrades a shulker box (contents and color move, see {@link TieredShulkerBoxes}). */
    public static boolean isShulkerBox(Upgrade upgrade) {
        // Split stage 8b: the tier shulker boxes are Simple Storage's; they are Vanilla shulker boxes to everyone else.
        return upgrade.to() instanceof net.minecraft.world.level.block.ShulkerBoxBlock;
    }

    /** The built-in table - the default the shipped datapack files are generated from. */
    public static Map<Block, Upgrade> builtInTable() {
        Map<Block, Upgrade> table = builtIn;
        if (table == null) {
            Map<Block, Upgrade> map = new HashMap<>();
            toNetherite(map, ModBlocks.REINFORCED_HOPPER, ModBlocks.NETHERITE_HOPPER);
            if (com.simplebuilding.version.McVersion.OVENS_IN_SB) { // 26.3: data/simpleovens/in_world_upgrades
                toNetherite(map, ModBlocks.REINFORCED_FURNACE, ModBlocks.NETHERITE_FURNACE);
                toNetherite(map, ModBlocks.REINFORCED_SMOKER, ModBlocks.NETHERITE_SMOKER);
                toNetherite(map, ModBlocks.REINFORCED_BLAST_FURNACE, ModBlocks.NETHERITE_BLAST_FURNACE);
            }
            toNetherite(map, ModBlocks.REINFORCED_PISTON, ModBlocks.NETHERITE_PISTON);
            // Besitzer 2026-09-29: auch der verstaerkte klebrige Kolben wird zum Netherit-Kolben (Brecher, nicht klebrig).
            toNetherite(map, ModBlocks.REINFORCED_STICKY_PISTON, ModBlocks.NETHERITE_PISTON);
            toEnderite(map, ModBlocks.NETHERITE_HOPPER, ModBlocks.ENDERITE_HOPPER);
            if (com.simplebuilding.version.McVersion.OVENS_IN_SB) { // 26.3: data/simpleovens/in_world_upgrades
                toEnderite(map, ModBlocks.NETHERITE_FURNACE, ModBlocks.ENDERITE_FURNACE);
                toEnderite(map, ModBlocks.NETHERITE_SMOKER, ModBlocks.ENDERITE_SMOKER);
                toEnderite(map, ModBlocks.NETHERITE_BLAST_FURNACE, ModBlocks.ENDERITE_BLAST_FURNACE);
            }
            toEnderite(map, ModBlocks.NETHERITE_PISTON, ModBlocks.ENDERITE_PISTON);
            if (McVersion.STORAGE_IN_SB) { // 26.3: data/simplestorage/in_world_upgrades (split stage 8b)
                // Truhen: jede der acht Vanilla-Kupfertruhen (vier Oxidationsstufen, gewachst oder nicht)
                // -> Verstaerkt -> Netherit -> Enderit. Der Umbau behaelt den Inhalt (TieredChests).
                for (Block copperChest : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
                    if (copperChest instanceof net.minecraft.world.level.block.CopperChestBlock) {
                        map.put(copperChest, new Upgrade(copperChest, ModBlocks.REINFORCED_CHEST, ModItems.CRACKED_DIAMOND,
                                0, REINFORCED_DAMAGE_PER_HIT, false, true));
                    }
                }
                toNetherite(map, ModBlocks.REINFORCED_CHEST, ModBlocks.NETHERITE_CHEST);
                toEnderite(map, ModBlocks.NETHERITE_CHEST, ModBlocks.ENDERITE_CHEST);
                if (com.simplebuilding.version.McVersion.TRAPPED_TIERED_CHESTS) {
                    toNetherite(map, ModBlocks.REINFORCED_TRAPPED_CHEST, ModBlocks.NETHERITE_TRAPPED_CHEST);
                    toEnderite(map, ModBlocks.NETHERITE_TRAPPED_CHEST, ModBlocks.ENDERITE_TRAPPED_CHEST);
                }
                // Shulkerkisten: jede der 17 Vanilla-Shulkerkisten (ungefaerbt und 16 Farben) -> Verstaerkt ->
                // Netherit -> Enderit, teurer als die Truhen (TieredShulkerBoxes). Farbe und Inhalt bleiben.
                int factor = TieredShulkerBoxes.SHULKER_UPGRADE_DURATION_FACTOR;
                int cost = TieredShulkerBoxes.SHULKER_UPGRADE_MATERIAL_COST;
                for (Block vanillaBox : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
                    if (TieredShulkerBoxes.isVanillaShulkerBox(vanillaBox)) {
                        map.put(vanillaBox, new Upgrade(vanillaBox, ModBlocks.REINFORCED_SHULKER_BOX, ModItems.CRACKED_DIAMOND,
                                0, REINFORCED_DAMAGE_PER_HIT, false, true, factor, cost));
                    }
                }
                map.put(ModBlocks.REINFORCED_SHULKER_BOX, new Upgrade(ModBlocks.REINFORCED_SHULKER_BOX, ModBlocks.NETHERITE_SHULKER_BOX,
                        ModItems.NETHERITE_NUGGET, RANK_DIAMOND, NETHERITE_DAMAGE_PER_HIT, false, false, factor, cost));
                map.put(ModBlocks.NETHERITE_SHULKER_BOX, new Upgrade(ModBlocks.NETHERITE_SHULKER_BOX, ModBlocks.ENDERITE_SHULKER_BOX,
                        ModItems.ENDERITE_NUGGET, RANK_NETHERITE, ENDERITE_DAMAGE_PER_HIT, true, false, factor, cost));
            }
            // Crucibles, barrels and the cauldron: Simple Crucibles ships them (data/simplecrucibles/in_world_upgrades, a-small22),
            // so the hammer ways also work without SimpleBuilding.
            // Astral-Verzauberungstisch (Queue N27, BS25-16 29.10.): Verzauberungstisch + 8 Enderit-Nuggets (alle 2 Schlaege
            // eins = 16 Schlaege), ab Netherit-Hammer, 35 Haltbarkeit je Schlag; verbraucht werden die Nuggets beim letzten Schlag.
            if (McVersion.ASTRAL_ENCHANTING) {
                map.put(net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE, new Upgrade(net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE,
                        ModBlocks.ASTRAL_ENCHANTING_TABLE, ModItems.ENDERITE_NUGGET, RANK_NETHERITE, ASTRAL_TABLE_DAMAGE_PER_HIT, true, false,
                        1, ASTRAL_TABLE_NUGGETS, ASTRAL_TABLE_STRIKES_PER_NUGGET));
            }
            table = Map.copyOf(map);
            builtIn = table;
        }
        return table;
    }

    /** Astral Enchanting Table (BS25-16, owner 25.10./29.10.): 8 Enderite nuggets, one per 2 strikes, 35 durability a strike (30-40). */
    public static final int ASTRAL_TABLE_NUGGETS = 8, ASTRAL_TABLE_STRIKES_PER_NUGGET = 2, ASTRAL_TABLE_DAMAGE_PER_HIT = 35;

    private static void toNetherite(Map<Block, Upgrade> map, Block from, Block to) {
        map.put(from, new Upgrade(from, to, ModItems.NETHERITE_NUGGET, RANK_DIAMOND, NETHERITE_DAMAGE_PER_HIT, false, false));
    }

    private static void toEnderite(Map<Block, Upgrade> map, Block from, Block to) {
        map.put(from, new Upgrade(from, to, ModItems.ENDERITE_NUGGET, RANK_NETHERITE, ENDERITE_DAMAGE_PER_HIT, true, false));
    }

    /**
     * 26.3: no lines from SimpleBuilding - Simple Hammers puts the upgrade hint under every upgradable block item itself
     * ({@code com.simplehammers.engine.UpgradeHints}, Queue 738), so it shows once and also without SimpleBuilding. The
     * 26.2 twin keeps SimpleBuilding's hints.
     */
    public static java.util.List<Component> tooltipHints(ItemStack stack) {
        return java.util.List.of();
    }
}
