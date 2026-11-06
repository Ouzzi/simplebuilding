package com.simplebuildingtools.enchantment;

import com.simplebuildingtools.SimpleBuildingTools;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The wand enchantments: Cover, Bridge and Linear are this mod's (data files, until 07.11.2026 {@code simplebuilding:}); Master
 * Builder, Color Palette and Constructor's Touch stay SimpleBuilding's (they belong to bundles, backpacks and brushes too) and are
 * asked for by key - without SimpleBuilding they do not exist and a wand simply never has them.
 */
public final class BtEnchantments {
    public static final ResourceKey<Enchantment> COVER = key(SimpleBuildingTools.MOD_ID, "cover");
    public static final ResourceKey<Enchantment> BRIDGE = key(SimpleBuildingTools.MOD_ID, "bridge");
    public static final ResourceKey<Enchantment> LINEAR = key(SimpleBuildingTools.MOD_ID, "linear");
    public static final ResourceKey<Enchantment> MASTER_BUILDER = key(SimpleBuildingTools.SB_NAMESPACE, "master_builder");
    public static final ResourceKey<Enchantment> COLOR_PALETTE = key(SimpleBuildingTools.SB_NAMESPACE, "color_palette");
    public static final ResourceKey<Enchantment> CONSTRUCTORS_TOUCH = key(SimpleBuildingTools.SB_NAMESPACE, "constructors_touch");

    private BtEnchantments() {
    }

    private static ResourceKey<Enchantment> key(String namespace, String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(namespace, path));
    }
}
