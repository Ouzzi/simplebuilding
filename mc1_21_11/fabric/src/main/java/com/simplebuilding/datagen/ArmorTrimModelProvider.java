package com.simplebuilding.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.ModArmorMaterials;
import com.simplebuilding.items.ModItems;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * Sichtbare Ruestungsbesaetze: das Icon jedes besetzbaren Ruestungsteils zeigt das angebrachte
 * MUSTER in der Farbe des Materials, nicht nur einen Farbfleck.
 *
 * <p>Vanilla waehlt das Besatz-Modell nur nach dem Material ({@code minecraft:trim_material}).
 * Hier ist die Item-Definition ein {@code minecraft:composite} aus zwei Ebenen, die beide nach der
 * ganzen {@code minecraft:trim}-Komponente waehlen ({@code minecraft:select} mit
 * {@code minecraft:component}, also Material UND Muster): unten das unbesetzte Teil (bei Leder
 * mit Faerbung), oben je Muster x Material eine Muster-Ebene
 * {@code simplebuilding:item/trim_overlay/<slot>_<muster>_<farbe>}.
 * Deren Textur {@code simplebuilding:trims/items/<slot>_<muster>_<farbe>} entsteht im Atlas
 * {@code minecraft:items} per {@code paletted_permutations} aus den Graustufen-Ebenen von
 * {@code tools/textures/generate_trim_overlays.py} - dasselbe Verfahren wie Vanillas eigene
 * Besatz-Ebenen, daher auch dieselben {@code _darker}-Stufen, wenn Material und Ruestung gleich
 * sind (aus {@code MaterialAssetGroup#assetId}).
 *
 * <p>Ohne Besatz, und bei Mustern oder Materialien, die diese Datei nicht kennt (andere Mods),
 * zeigt die untere Ebene ihren {@code fallback} - Vanillas eigene Auswahl nach Material, Bit fuer
 * Bit wie {@code ItemModelGenerators#generateTrimmableItem} - und die obere nichts
 * ({@code minecraft:empty}): das Icon sieht dann genau wie in Vanilla aus. Ruestungsstaender und Rahmen benutzen
 * dieselbe Item-Definition und zeigen das Muster deshalb ebenfalls.
 *
 * <p>Muster und Materialien kommen aus der Registry (Vanilla + {@code ModTrimMaterials}); die
 * Holder im Fallwert loest das Spiel beim Laden ueber den Registry-Tauscher des Clients auf.
 * Geprueft von {@code DataIntegrityTests#everyTrimmableArmourShowsEveryTrimPatternOnItsIcon}.
 */
public class ArmorTrimModelProvider implements DataProvider {

    /** Standardfarbe ungefaerbten Leders, wie in Vanillas Leder-Definitionen. */
    private static final int LEATHER_DEFAULT = -6265536;

    private record Armour(Item item, String slot, ResourceKey<EquipmentAsset> asset, boolean dyed) {
        Identifier id() {
            return BuiltInRegistries.ITEM.getKey(item);
        }
    }

    private static List<Armour> armour() {
        List<Armour> list = new ArrayList<>();
        addSet(list, EquipmentAssets.LEATHER, true, Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS);
        addSet(list, EquipmentAssets.CHAINMAIL, false, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS);
        addSet(list, EquipmentAssets.IRON, false, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        addSet(list, EquipmentAssets.GOLD, false, Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);
        addSet(list, EquipmentAssets.DIAMOND, false, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
        addSet(list, EquipmentAssets.NETHERITE, false, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);
        addSet(list, EquipmentAssets.COPPER, false, Items.COPPER_HELMET, Items.COPPER_CHESTPLATE, Items.COPPER_LEGGINGS, Items.COPPER_BOOTS);
        list.add(new Armour(Items.TURTLE_HELMET, "helmet", EquipmentAssets.TURTLE_SCUTE, false));
        addSet(list, ModArmorMaterials.ENDERITE_ASSET_KEY, false, ModItems.ENDERITE_HELMET, ModItems.ENDERITE_CHESTPLATE, ModItems.ENDERITE_LEGGINGS, ModItems.ENDERITE_BOOTS);
        return list;
    }

    private static void addSet(List<Armour> list, ResourceKey<EquipmentAsset> asset, boolean dyed, Item helmet, Item chest, Item legs, Item boots) {
        list.add(new Armour(helmet, "helmet", asset, dyed));
        list.add(new Armour(chest, "chestplate", asset, dyed));
        list.add(new Armour(legs, "leggings", asset, dyed));
        list.add(new Armour(boots, "boots", asset, dyed));
    }

    private final PackOutput.PathProvider items;
    private final PackOutput.PathProvider models;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public ArmorTrimModelProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.items = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
        this.models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        this.registries = registries;
    }

    @Override
    public String getName() {
        return "Armour trim pattern item models";
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registries.thenCompose(lookup -> {
            List<Holder.Reference<TrimPattern>> patterns = lookup.lookupOrThrow(Registries.TRIM_PATTERN).listElements()
                    .sorted(Comparator.comparing(h -> h.key().identifier().toString())).toList();
            List<Holder.Reference<TrimMaterial>> materials = lookup.lookupOrThrow(Registries.TRIM_MATERIAL).listElements()
                    .sorted(Comparator.comparing(h -> h.key().identifier().toString())).toList();

            Map<Identifier, JsonObject> overlayModels = new TreeMap<>();
            List<CompletableFuture<?>> writes = new ArrayList<>();
            for (Armour armour : armour()) {
                // Zwei Ebenen: unten das Teil selbst, oben die Muster-Ebene. Das untere Teil ist fuer
                // jeden bekannten Besatz das unbesetzte Modell (sonst laege Vanillas Farbfleck unter dem
                // Muster) und sonst Vanillas Auswahl nach Material; oben liegt fuer jeden bekannten
                // Besatz dessen Muster-Ebene und sonst nichts.
                JsonArray known = new JsonArray();
                JsonArray overlayCases = new JsonArray();
                for (Holder.Reference<TrimPattern> pattern : patterns) {
                    String patternName = pattern.value().assetId().getPath();
                    for (Holder.Reference<TrimMaterial> material : materials) {
                        String colour = material.value().assets().assetId(armour.asset()).suffix();
                        String sprite = armour.slot() + "_" + patternName + "_" + colour;
                        Identifier overlay = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "item/trim_overlay/" + sprite);
                        overlayModels.computeIfAbsent(overlay, id -> overlayModel(sprite));

                        JsonObject when = new JsonObject();
                        when.addProperty("material", material.key().identifier().toString());
                        when.addProperty("pattern", pattern.key().identifier().toString());
                        known.add(when);
                        JsonObject entry = new JsonObject();
                        entry.add("model", plain(overlay));
                        entry.add("when", when.deepCopy());
                        overlayCases.add(entry);
                    }
                }
                JsonObject baseCase = new JsonObject();
                baseCase.add("model", baseModel(armour, armour.id().withPrefix("item/")));
                baseCase.add("when", known);
                JsonArray baseCases = new JsonArray();
                baseCases.add(baseCase);
                JsonObject empty = new JsonObject();
                empty.addProperty("type", "minecraft:empty");

                JsonArray layers = new JsonArray();
                layers.add(trimSelect(baseCases, vanillaTrimSelect(armour)));
                layers.add(trimSelect(overlayCases, empty));
                JsonObject composite = new JsonObject();
                composite.addProperty("type", "minecraft:composite");
                composite.add("models", layers);
                JsonObject definition = new JsonObject();
                definition.add("model", composite);
                writes.add(DataProvider.saveStable(cache, definition, items.json(armour.id())));
            }
            overlayModels.forEach((id, json) -> writes.add(DataProvider.saveStable(cache, json, models.json(id))));
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        });
    }

    /** {@code minecraft:select} ueber die ganze {@code minecraft:trim}-Komponente (Material UND Muster). */
    private static JsonObject trimSelect(JsonArray cases, JsonObject fallback) {
        JsonObject select = new JsonObject();
        select.addProperty("type", "minecraft:select");
        select.addProperty("property", "minecraft:component");
        select.addProperty("component", "minecraft:trim");
        select.add("cases", cases);
        select.add("fallback", fallback);
        return select;
    }

    private static JsonObject overlayModel(String sprite) {
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", Simplebuilding.MOD_ID + ":trims/items/" + sprite);
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:item/generated");
        model.add("textures", textures);
        return model;
    }

    /** Vanillas Auswahl nach Material, wie {@code ItemModelGenerators#generateTrimmableItem}. */
    private static JsonObject vanillaTrimSelect(Armour armour) {
        Identifier modelLocation = armour.id().withPrefix("item/");
        JsonArray cases = new JsonArray();
        for (ItemModelGenerators.TrimMaterialData material : ItemModelGenerators.TRIM_MATERIAL_MODELS) {
            JsonObject entry = new JsonObject();
            entry.add("model", baseModel(armour, modelLocation.withSuffix("_" + material.assets().base().suffix() + "_trim")));
            entry.addProperty("when", material.materialKey().identifier().toString());
            cases.add(entry);
        }
        JsonObject select = new JsonObject();
        select.addProperty("type", "minecraft:select");
        select.add("cases", cases);
        select.add("fallback", baseModel(armour, modelLocation));
        select.addProperty("property", "minecraft:trim_material");
        return select;
    }

    private static JsonObject baseModel(Armour armour, Identifier model) {
        JsonObject json = plain(model);
        if (armour.dyed()) {
            JsonObject dye = new JsonObject();
            dye.addProperty("type", "minecraft:dye");
            dye.addProperty("default", LEATHER_DEFAULT);
            JsonArray tints = new JsonArray();
            tints.add(dye);
            json.add("tints", tints);
        }
        return json;
    }

    private static JsonObject plain(Identifier model) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:model");
        json.addProperty("model", model.toString());
        return json;
    }
}
