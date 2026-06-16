package com.simplebuilding.datagen;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
    public ModItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        valueLookupBuilder(ModTags.Items.CHISEL_TOOLS)
                .add(ModItems.STONE_CHISEL)
                .add(ModItems.COPPER_CHISEL)
                .add(ModItems.IRON_CHISEL)
                .add(ModItems.GOLD_CHISEL)
                .add(ModItems.DIAMOND_CHISEL)
                .add(ModItems.NETHERITE_CHISEL);

        var octantBuilder = valueLookupBuilder(ModTags.Items.OCTANTS_ENCHANTABLE)
                .add(ModItems.OCTANT);

        for (Item coloredRangefinder : ModItems.COLORED_OCTANT_ITEMS.values()) {
            octantBuilder.add(coloredRangefinder);
        }

        valueLookupBuilder(ModTags.Items.CHISEL_AND_MINING_TOOLS)
                .addTag(ModTags.Items.CHISEL_TOOLS)
                .forceAddTag(ItemTags.MINING_ENCHANTABLE)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE)
                .forceAddTag(ModTags.Items.OCTANTS_ENCHANTABLE);

        valueLookupBuilder(ItemTags.DURABILITY_ENCHANTABLE)
                .addTag(ModTags.Items.CHISEL_TOOLS)
                .addTag(ModTags.Items.OCTANTS_ENCHANTABLE)
                .add(ModItems.ORE_DETECTOR)
                .add(ModItems.ROTATOR)
                .add(ModItems.ENDERITE_SPEAR)
                .addTag(ModTags.Items.BUILDING_WAND_ENCHANTABLE)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE);

        valueLookupBuilder(ItemTags.MINING_ENCHANTABLE)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE);

        valueLookupBuilder(ItemTags.MINING_LOOT_ENCHANTABLE)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE);

        valueLookupBuilder(ItemTags.VANISHING_ENCHANTABLE)
                .addTag(ModTags.Items.CHISEL_TOOLS);

        valueLookupBuilder(ModTags.Items.BUNDLE_ENCHANTABLE)
                .add(ModItems.REINFORCED_BUNDLE)
                .add(ModItems.NETHERITE_BUNDLE)
                .add(ModItems.QUIVER)
                .add(ModItems.NETHERITE_QUIVER);

        valueLookupBuilder(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE)
                .addTag(ModTags.Items.BUILDING_WAND_ENCHANTABLE)
                .add(ModItems.REINFORCED_BUNDLE)
                .add(ModItems.NETHERITE_BUNDLE)
                .add(ModItems.QUIVER)
                .add(ModItems.NETHERITE_QUIVER);

        valueLookupBuilder(ModTags.Items.CONSTRUCTORS_TOUCH_ENCHANTABLE)
                .add(ModItems.REINFORCED_BUNDLE)
                .add(ModItems.NETHERITE_BUNDLE)
                .add(ModItems.QUIVER)
                .add(ModItems.NETHERITE_QUIVER)
                .add(Items.SHULKER_BOX)
                .addTag(ModTags.Items.CHISEL_TOOLS)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE)
                .addTag(ModTags.Items.BUILDING_WAND_ENCHANTABLE)
                .add(ModItems.VELOCITY_GAUGE)
                .add(ModItems.ORE_DETECTOR)
                .add(ModItems.MAGNET)
                .forceAddTag(ModTags.Items.OCTANTS_ENCHANTABLE)
                .add(Items.STICK);

        valueLookupBuilder(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE)
                .add(ModItems.STONE_SLEDGEHAMMER)
                .add(ModItems.COPPER_SLEDGEHAMMER)
                .add(ModItems.IRON_SLEDGEHAMMER)
                .add(ModItems.GOLD_SLEDGEHAMMER)
                .add(ModItems.DIAMOND_SLEDGEHAMMER)
                .add(ModItems.NETHERITE_SLEDGEHAMMER);

        valueLookupBuilder(ModTags.Items.BUILDING_WAND_ENCHANTABLE)
                .add(ModItems.COPPER_BUILDING_WAND)
                .add(ModItems.IRON_BUILDING_WAND)
                .add(ModItems.GOLD_BUILDING_WAND)
                .add(ModItems.DIAMOND_BUILDING_WAND)
                .add(ModItems.NETHERITE_BUILDING_WAND);

        valueLookupBuilder(ModTags.Items.VEINMINE_ENCHANTABLE)
                .forceAddTag(ItemTags.PICKAXES)
                .forceAddTag(ItemTags.AXES);

        TagKey<Item> TRIM_TEMPLATES = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("trim_templates"));

        valueLookupBuilder(TRIM_TEMPLATES)
                .add(ModItems.GLOWING_TRIM_TEMPLATE)
                .add(ModItems.EMITTING_TRIM_TEMPLATE);

        // Optional: Damit der Leuchtbeutel generell als "Trim Material" erkannt wird (hilft bei der GUI-Validierung)
        valueLookupBuilder(ItemTags.TRIM_MATERIALS)
                .add(ModItems.ASTRALIT_DUST)
                .add(ModItems.NIHILITH_SHARD)
                .add(ModItems.ENDERITE_INGOT)
                .add(Items.GLOW_INK_SAC)
                .add(Items.GLOWSTONE_DUST);

        valueLookupBuilder(ItemTags.TRIMMABLE_ARMOR)
                .add(ModItems.ENDERITE_HELMET)
                .add(ModItems.ENDERITE_CHESTPLATE)
                .add(ModItems.ENDERITE_LEGGINGS)
                .add(ModItems.ENDERITE_BOOTS);
    }
}
