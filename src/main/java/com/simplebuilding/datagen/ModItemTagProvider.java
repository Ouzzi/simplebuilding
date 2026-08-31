package com.simplebuilding.datagen;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
    public ModItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture);
    }

    /**
     * MC 26.2: {@code valueLookupBuilder(...)} ist entfallen (Ersatz: {@code builder(...)}) und
     * {@link net.minecraft.data.tags.TagAppender} nimmt nur noch {@link ResourceKey}s statt Item-Instanzen
     * entgegen (Vanilla nutzt dafuer die Konstanten aus {@code net.minecraft.references.ItemIds}).
     * Dieser Helfer liefert den Registry-Key zu einer Item-Instanz, damit die Tag-Inhalte
     * unveraendert bleiben.
     */
    private static ResourceKey<Item> key(Item item) {
        return BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow();
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        builder(ModTags.Items.CHISEL_TOOLS)
                .add(key(ModItems.STONE_CHISEL))
                .add(key(ModItems.COPPER_CHISEL))
                .add(key(ModItems.IRON_CHISEL))
                .add(key(ModItems.GOLD_CHISEL))
                .add(key(ModItems.DIAMOND_CHISEL))
                .add(key(ModItems.NETHERITE_CHISEL));

        var octantBuilder = builder(ModTags.Items.OCTANTS_ENCHANTABLE)
                .add(key(ModItems.OCTANT));

        for (Item coloredRangefinder : ModItems.COLORED_OCTANT_ITEMS.values()) {
            octantBuilder.add(key(coloredRangefinder));
        }

        builder(ModTags.Items.CHISEL_AND_MINING_TOOLS)
                .addTag(ModTags.Items.CHISEL_TOOLS)
                .forceAddTag(ItemTags.MINING_ENCHANTABLE)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE)
                .forceAddTag(ModTags.Items.OCTANTS_ENCHANTABLE);

        builder(ItemTags.DURABILITY_ENCHANTABLE)
                .addTag(ModTags.Items.CHISEL_TOOLS)
                .addTag(ModTags.Items.OCTANTS_ENCHANTABLE)
                .add(key(ModItems.ORE_DETECTOR))
                .add(key(ModItems.ROTATOR))
                .add(key(ModItems.ENDERITE_SPEAR))
                .addTag(ModTags.Items.BUILDING_WAND_ENCHANTABLE)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE);

        builder(ItemTags.MINING_ENCHANTABLE)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE);

        builder(ItemTags.MINING_LOOT_ENCHANTABLE)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE);

        builder(ItemTags.VANISHING_ENCHANTABLE)
                .addTag(ModTags.Items.CHISEL_TOOLS);

        builder(ModTags.Items.BUNDLE_ENCHANTABLE)
                .add(key(ModItems.REINFORCED_BUNDLE))
                .add(key(ModItems.NETHERITE_BUNDLE))
                .add(key(ModItems.QUIVER))
                .add(key(ModItems.NETHERITE_QUIVER));

        builder(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE)
                .addTag(ModTags.Items.BUILDING_WAND_ENCHANTABLE)
                .add(key(ModItems.REINFORCED_BUNDLE))
                .add(key(ModItems.NETHERITE_BUNDLE))
                .add(key(ModItems.QUIVER))
                .add(key(ModItems.NETHERITE_QUIVER));

        builder(ModTags.Items.CONSTRUCTORS_TOUCH_ENCHANTABLE)
                .add(key(ModItems.REINFORCED_BUNDLE))
                .add(key(ModItems.NETHERITE_BUNDLE))
                .add(key(ModItems.QUIVER))
                .add(key(ModItems.NETHERITE_QUIVER))
                .add(key(Items.SHULKER_BOX))
                .addTag(ModTags.Items.CHISEL_TOOLS)
                .addTag(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE)
                .addTag(ModTags.Items.BUILDING_WAND_ENCHANTABLE)
                .add(key(ModItems.VELOCITY_GAUGE))
                .add(key(ModItems.ORE_DETECTOR))
                .add(key(ModItems.MAGNET))
                .forceAddTag(ModTags.Items.OCTANTS_ENCHANTABLE)
                .add(key(Items.STICK));

        builder(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE)
                .add(key(ModItems.STONE_SLEDGEHAMMER))
                .add(key(ModItems.COPPER_SLEDGEHAMMER))
                .add(key(ModItems.IRON_SLEDGEHAMMER))
                .add(key(ModItems.GOLD_SLEDGEHAMMER))
                .add(key(ModItems.DIAMOND_SLEDGEHAMMER))
                .add(key(ModItems.NETHERITE_SLEDGEHAMMER));

        builder(ModTags.Items.BUILDING_WAND_ENCHANTABLE)
                .add(key(ModItems.COPPER_BUILDING_WAND))
                .add(key(ModItems.IRON_BUILDING_WAND))
                .add(key(ModItems.GOLD_BUILDING_WAND))
                .add(key(ModItems.DIAMOND_BUILDING_WAND))
                .add(key(ModItems.NETHERITE_BUILDING_WAND));

        builder(ModTags.Items.VEINMINE_ENCHANTABLE)
                .forceAddTag(ItemTags.PICKAXES)
                .forceAddTag(ItemTags.AXES);

        TagKey<Item> TRIM_TEMPLATES = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("trim_templates"));

        builder(TRIM_TEMPLATES)
                .add(key(ModItems.GLOWING_TRIM_TEMPLATE))
                .add(key(ModItems.EMITTING_TRIM_TEMPLATE));

        // Optional: Damit der Leuchtbeutel generell als "Trim Material" erkannt wird (hilft bei der GUI-Validierung)
        builder(ItemTags.TRIM_MATERIALS)
                .add(key(ModItems.ASTRALIT_DUST))
                .add(key(ModItems.NIHILITH_SHARD))
                .add(key(ModItems.ENDERITE_INGOT))
                .add(key(Items.GLOW_INK_SAC))
                .add(key(Items.GLOWSTONE_DUST));

        builder(ItemTags.TRIMMABLE_ARMOR)
                .add(key(ModItems.ENDERITE_HELMET))
                .add(key(ModItems.ENDERITE_CHESTPLATE))
                .add(key(ModItems.ENDERITE_LEGGINGS))
                .add(key(ModItems.ENDERITE_BOOTS));

        addVoidProtected();
    }

    /**
     * Befuellt {@link ModTags.Items#VOID_PROTECTED} deterministisch aus der Item-Registry statt
     * aus einer handgepflegten Liste: alles, was
     * {@link ModTags.Items#isVoidProtectedByRule(Identifier)} akzeptiert. Neue Enderite-Items
     * sind damit automatisch gegen den Void geschuetzt, ohne dass jemand daran denken muss --
     * und der Mixin braucht keinen sprachabhaengigen Check auf den Anzeigenamen mehr.
     */
    private void addVoidProtected() {
        // Sortiert, damit die erzeugte JSON unabhaengig von der Registrierungsreihenfolge ist.
        Set<Identifier> ids = new TreeSet<>(Comparator.comparing(Identifier::toString));
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (ModTags.Items.isVoidProtectedByRule(id)) {
                ids.add(id);
            }
        }

        var voidProtected = builder(ModTags.Items.VOID_PROTECTED);
        for (Identifier id : ids) {
            voidProtected.add(ResourceKey.create(Registries.ITEM, id));
        }
    }
}
