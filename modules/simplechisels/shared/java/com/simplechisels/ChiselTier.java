package com.simplechisels;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;

/**
 * The nine chisels (N89A, 17.11.2026: the two Simple Ores rose alloys after their parent metal): numbers as in SimpleBuilding before the move (durability, cooldown before Fast Chiseling, enchantability)
 * and the transformation tier each one uses ({@link com.simplechisels.data.ChiselTables#TIERS}: copper shares iron's table,
 * gold shares diamond's).
 */
public enum ChiselTier {
    STONE("stone", ToolMaterial.STONE, "stone", 48 * 4, 30, 15),
    COPPER("copper", ToolMaterial.COPPER, "iron", 48 * 4, 25, 18),
    /** N89A: Rose Copper - copper's table and durability, cooldown 23 (tool speed 8 between copper 5 -> 25 and gold 12 -> 20), enchantability 15. */
    ROSE_BRONZE("rose_bronze", Holder.ROSE_BRONZE, "iron", 48 * 4, 23, 15),
    IRON("iron", ToolMaterial.IRON, "iron", 64 * 4, 25, 14),
    GOLD("gold", ToolMaterial.GOLD, "diamond", 32 * 4, 20, 22),
    /** N89A: Rose Gold - gold's table and tempo, 90 % of iron's durability (58 x 4, like its tools 225 against 250), enchantability 22. */
    ROSE_GOLD("rose_gold", Holder.ROSE_GOLD, "diamond", 58 * 4, 20, 22),
    DIAMOND("diamond", ToolMaterial.DIAMOND, "diamond", 98 * 4, 10, 10),
    NETHERITE("netherite", ToolMaterial.NETHERITE, "netherite", 128 * 4, 5, 15),
    ENDERITE("enderite", Holder.ENDERITE, "enderite", 150 * 4, 5, 18);

    public final String name;
    public final ToolMaterial material;
    /** Transformation tier ({@code stone | iron | diamond | netherite | enderite}). */
    public final String table;
    public final int durability;
    /** Default cooldown in ticks ({@code cooldownTicks.<name>} in the config). */
    public final int cooldownTicks;
    public final int enchantability;

    ChiselTier(String name, ToolMaterial material, String table, int durability, int cooldownTicks, int enchantability) {
        this.name = name;
        this.material = material;
        this.table = table;
        this.durability = durability;
        this.cooldownTicks = cooldownTicks;
        this.enchantability = enchantability;
    }

    public String id() {
        return name + "_chisel";
    }

    public boolean fireResistant() {
        return this == NETHERITE || this == ENDERITE;
    }

    /** SimpleBuilding's Enderite tool material, copied (the same numbers as Simple Hammers' copy; this mod imports neither). */
    private static final class Holder {
        static final ToolMaterial ENDERITE = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2530, 10.0F, 5.0F, 18,
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("simplelib", "repairs_enderite_equipment")));
        /** N89A: copies of Simple Ores' alloy tool materials (OresMaterials; this mod must not import it), repaired by the alloy's own tag. */
        static final ToolMaterial ROSE_BRONZE = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 190, 8.0F, 1.0F, 15,
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("simpleores", "repairs_rose_bronze_equipment")));
        static final ToolMaterial ROSE_GOLD = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 225, 12.0F, 2.0F, 22,
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("simpleores", "repairs_rose_gold_equipment")));
    }
}
