package com.simplebuilding.items;

import net.minecraft.item.ToolMaterial;
import net.minecraft.util.registry.tag.BlockTags;
import net.minecraft.util.registry.tag.ItemTags;

public class ModToolMaterials {

    // Wir erstellen eine Instanz des Records ToolMaterial
    public static final ToolMaterial ENDERITE = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, // Incorrect blocks tag
            2530,                                    // Durability
            10.0F,                                   // Speed
            5.0F,                                    // Attack Damage Bonus
            18,                                      // Enchantability
            ItemTags.NETHERITE_TOOL_MATERIALS        // Repair Items (Platzhalter, idealerweise dein eigener Tag)
    );

    // Hinweis: Wenn du dein eigenes Reparatur-Item (Enderite Ingot) nutzen willst,
    // musst du erst einen Tag dafÃ¼r erstellen oder die ToolMaterial Klasse ignorieren und die Werte manuell setzen.
    // Aber fÃ¼r den Anfang nutzen wir Netherite Repair Items oder erstellen spÃ¤ter einen Tag.
}
