package com.simplebuilding.datagen;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public ModItemTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        valueLookupBuilder(ModTags.Items.CHISEL_TOOLS)
                .add(ModItems.STONE_CHISEL)
                .add(ModItems.COPPER_CHISEL)
                .add(ModItems.IRON_CHISEL)
                .add(ModItems.GOLD_CHISEL)
                .add(ModItems.DIAMOND_CHISEL)
                .add(ModItems.NETHERITE_CHISEL)
                .add(ModItems.STONE_SPATULA)
                .add(ModItems.COPPER_SPATULA)
                .add(ModItems.IRON_SPATULA)
                .add(ModItems.GOLD_SPATULA)
                .add(ModItems.DIAMOND_SPATULA)
                .add(ModItems.NETHERITE_SPATULA);

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

        TagKey<Item> TRIM_TEMPLATES = TagKey.of(RegistryKeys.ITEM, Identifier.ofVanilla("trim_templates"));

        valueLookupBuilder(TRIM_TEMPLATES)
                .add(ModItems.GLOWING_TRIM_TEMPLATE)
                .add(ModItems.EMITTING_TRIM_TEMPLATE);

        // Optional: Damit der Leuchtbeutel generell als "Trim Material" erkannt wird (hilft bei der GUI-Validierung)
        valueLookupBuilder(ItemTags.TRIM_MATERIALS)
                .add(Items.GLOW_INK_SAC)
                .add(Items.GLOWSTONE_DUST);


    }
}
