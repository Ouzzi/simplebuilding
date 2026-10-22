package com.simplebuilding.items;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public class ModToolMaterials {

    /** Enderit-Ausruestung wird nur mit Enderit-Barren repariert (Besitzer 21.10.); der Tag liegt in SimpleLib. */
    public static final TagKey<Item> REPAIRS_ENDERITE =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("simplelib", "repairs_enderite_equipment"));

    // Wir erstellen eine Instanz des Records ToolMaterial
    public static final ToolMaterial ENDERITE = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, // Incorrect blocks tag
            2530,                                    // Durability
            10.0F,                                   // Speed
            5.0F,                                    // Attack Damage Bonus
            18,                                      // Enchantability
            REPAIRS_ENDERITE                         // Repair Items: nur Enderit
    );
}