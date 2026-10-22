package com.simplehammers;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;

/** The seven hammer tiers: numbers as in SimpleBuilding before the move (owner: durability unchanged until tested). */
public enum HammerTiers {
    STONE("stone", ToolMaterial.STONE, 4, -3.0F, 190, 15),
    COPPER("copper", ToolMaterial.COPPER, 4, -2.8F, 190, 18),
    IRON("iron", ToolMaterial.IRON, 6, -3.0F, 250, 14),
    GOLD("gold", ToolMaterial.GOLD, 5, -2.8F, 32, 22),
    DIAMOND("diamond", ToolMaterial.DIAMOND, 7, -2.8F, 1561, 10),
    NETHERITE("netherite", ToolMaterial.NETHERITE, 8, -2.6F, 2031, 15),
    ENDERITE("enderite", Holder.MATERIAL, 9, -2.4F, 2500, 18);

    public final String name;
    public final ToolMaterial material;
    public final int attackDamage;
    /** Before {@link com.simplehammers.item.SledgehammerItem#ATTACK_SPEED_OFFSET}. */
    public final float attackSpeed;
    /** Pickaxe durability of the tier; the hammer has {@code BASE_DURABILITY_MULTIPLIER} times it. */
    public final int baseDurability;
    public final int enchantability;

    HammerTiers(String name, ToolMaterial material, int attackDamage, float attackSpeed, int baseDurability, int enchantability) {
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
    }
}
