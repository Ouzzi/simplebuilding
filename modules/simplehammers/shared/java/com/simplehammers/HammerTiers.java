package com.simplehammers;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;

/** The seven hammer tiers: numbers as in SimpleBuilding before the move (owner: durability unchanged until tested). */
public enum HammerTiers {
    STONE("stone", ToolMaterial.STONE, 4, -3.0F, 190, 15, 25),
    COPPER("copper", ToolMaterial.COPPER, 4, -2.8F, 190, 18, 24),
    IRON("iron", ToolMaterial.IRON, 6, -3.0F, 250, 14, 23),
    GOLD("gold", ToolMaterial.GOLD, 5, -2.8F, 32, 22, 22),
    DIAMOND("diamond", ToolMaterial.DIAMOND, 7, -2.8F, 1561, 10, 21),
    NETHERITE("netherite", ToolMaterial.NETHERITE, 8, -2.6F, 2031, 15, 20),
    ENDERITE("enderite", Holder.MATERIAL, 9, -2.4F, 2500, 18, 19),
    /** Optional tiers (ABN30-10): exist only while Simple Ores is loaded; the numbers copy its two alloys, tempo like gold/iron. */
    ROSE_BRONZE("rose_bronze", Holder.ROSE_BRONZE, 5, -2.8F, 250, 20, 23, "simpleores"),
    BLOSSOM_GOLD("blossom_gold", Holder.BLOSSOM_GOLD, 5, -2.8F, 225, 22, 22, "simpleores");

    public final String name;
    public final ToolMaterial material;
    public final int attackDamage;
    /** Before {@link com.simplehammers.item.SledgehammerItem#ATTACK_SPEED_OFFSET}. */
    public final float attackSpeed;
    /** Pickaxe durability of the tier; the hammer has {@code BASE_DURABILITY_MULTIPLIER} times it. */
    public final int baseDurability;
    public final int enchantability;
    /**
     * Ticks between two strikes of any in-world conversion (HS29-2, owner 29.10.): Netherite 20 (the tempo of the Cauldron to
     * Crucible upgrade before the change), Enderite one tick faster, every tier below one tick slower - small steps.
     */
    public final int strikeInterval;
    /** The mod this tier needs (null = always there). Never a hard requirement: without it the tier is simply not registered. */
    public final String requiredMod;

    HammerTiers(String name, ToolMaterial material, int attackDamage, float attackSpeed, int baseDurability, int enchantability, int strikeInterval) {
        this(name, material, attackDamage, attackSpeed, baseDurability, enchantability, strikeInterval, null);
    }

    HammerTiers(String name, ToolMaterial material, int attackDamage, float attackSpeed, int baseDurability, int enchantability, int strikeInterval,
                String requiredMod) {
        this.requiredMod = requiredMod;
        this.strikeInterval = strikeInterval;
        this.name = name;
        this.material = material;
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.baseDurability = baseDurability;
        this.enchantability = enchantability;
    }

    public String id() {
        return name + "_sledgehammer";
    }

    /** Whether this tier exists in the running game (the always-there tiers, or its optional mod is loaded). */
    public boolean enabled() {
        return requiredMod == null || SimpleHammers.isLoaded(requiredMod);
    }

    public boolean fireResistant() {
        return this == NETHERITE || this == ENDERITE;
    }

    /**
     * SimpleBuilding's Enderite tool material, copied (owner rule F3: the Enderite equipment moves to Simple Enderite
     * later; until then every mod with an Enderite tier carries its numbers).
     */
    public static ToolMaterial enderiteMaterial() {
        return Holder.MATERIAL;
    }

    /** Holds the Enderite material so the enum constant can use it (enum constants initialise before static fields). */
    private static final class Holder {
        static final ToolMaterial MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2530, 10.0F, 5.0F, 18,
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("simplelib", "repairs_enderite_equipment")));
        /** Copies of Simple Ores' tool materials (this mod must not import it): iron durability/tempo 11, resp. 225/gold tempo 12. */
        static final ToolMaterial ROSE_BRONZE = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 250, 11.0F, 0.0F, 20,
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("simpleores", "repairs_rose_bronze_equipment")));
        static final ToolMaterial BLOSSOM_GOLD = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 225, 12.0F, 0.0F, 22,
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("simpleores", "repairs_blossom_gold_equipment")));
    }
}
