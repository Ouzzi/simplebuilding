package com.simplebuilding.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Schreibt die tatsächlichen Item-Eigenschaften aus der Registry nach
 * {@code src/main/generated/wiki/items.json}, damit {@code wiki/generate.py}
 * Haltbarkeit, Stapelgröße, Verzauberbarkeit, Angriffswerte,
 * Zauberstab-Durchmesser, Meißel-Abklingzeit und Bündel-Kapazität zeigen kann.
 *
 * <p>Warum aus der Registry und nicht per Textsuche in {@code ModItems.java}:
 * Die Registry ist die eine Wahrheit. Wer dort eine Konstante ändert, ändert Mod
 * und Wiki mit einem einzigen Datagen-Lauf. Ein Parser über den Java-Text bräche
 * bei jeder Umformatierung und würde außerdem die Fälle verfehlen, in denen
 * Minecraft einen übergebenen Wert wieder überschreibt – etwa bei den
 * Vorschlaghämmern, deren {@code enchantable(...)} von
 * {@code Item.Properties.pickaxe(...)} auf den Wert des ToolMaterial
 * zurückgesetzt wird.
 *
 * <p><b>Diese Datei gilt nur für die 1.21.11-Linie.</b> Hier trägt jedes Item
 * seine Komponenten noch selbst, {@code item.components()} liest also direkt ein
 * Feld. Ab 26.2 geht das nicht mehr: dort legt der Item-Konstruktor nur einen
 * Eintrag in {@code DataComponentInitializers} ab, die Map wird erst später
 * gegen eine {@code HolderLookup.Provider} gebaut und an den Registry-Holder
 * gebunden – im Datagen wirft {@code components()} deshalb
 * "Components not bound yet". Die 26.2-Fassung unter {@code src/main/java/}
 * baut die Maps darum selbst; sie kann nicht dieselbe Datei sein, weil
 * {@code DataComponentInitializers} auf dieser Linie gar nicht existiert.
 */
public class WikiDataProvider implements DataProvider {

    /**
     * Grundwerte des Spielers, gegen die Minecraft die Item-Modifier verrechnet:
     * Angriffsschaden 1.0 aus {@code Player.createAttributes()}, Angriffstempo
     * aus {@code Attributes.DEFAULT_ATTACK_SPEED} (4.0). Die Komponente eines
     * Items trägt nur den Modifier, nicht die im Tooltip gezeigte Zahl.
     */
    private static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0D;

    /** Vanilla-Standard; wird weggelassen, damit reine Bausteine keine leere Tabelle bekommen. */
    private static final int DEFAULT_MAX_STACK_SIZE = 64;

    private final PackOutput output;

    public WikiDataProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public String getName() {
        return "Wiki item properties";
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<Map.Entry<Identifier, Item>> ours = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null && Simplebuilding.MOD_ID.equals(id.getNamespace())) {
                ours.add(Map.entry(id, item));
            }
        }
        // Nach Id sortieren, damit der committete Stand unabhängig von der
        // Registry-Reihenfolge ist und ein Diff nur echte Änderungen zeigt.
        ours.sort(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)));

        JsonArray items = new JsonArray();
        for (Map.Entry<Identifier, Item> entry : ours) {
            items.add(describe(entry.getKey(), entry.getValue(), entry.getValue().components()));
        }

        // Auch die registrierten Bloecke: das Wiki leitet seine Blockliste aus den
        // Sprachschluesseln ab und zeigte dadurch Bloecke, die es gar nicht gibt
        // (auskommentierte TODOs mit vorhandenem block.*-Schluessel).
        JsonArray blocks = new JsonArray();
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            Identifier id = entry.getKey().identifier();
            if (Simplebuilding.MOD_ID.equals(id.getNamespace())) {
                blocks.add(id.toString());
            }
        }

        JsonObject root = new JsonObject();
        root.addProperty("schema", 1);
        root.addProperty("generator", getClass().getName());
        root.addProperty("note", "Aus der Item-Registry erzeugt (gradlew runDatagen) - nicht von Hand aendern.");
        root.add("items", items);
        root.add("blocks", blocks);

        Path path = this.output.getOutputFolder().resolve("wiki").resolve("items.json");
        return DataProvider.saveStable(cache, root, path);
    }

    private JsonObject describe(Identifier id, Item item, DataComponentMap components) {
        JsonObject o = new JsonObject();
        o.addProperty("id", id.toString());

        if (components != null) {
            int stack = components.getOrDefault(DataComponents.MAX_STACK_SIZE, DEFAULT_MAX_STACK_SIZE);
            if (stack != DEFAULT_MAX_STACK_SIZE) {
                o.addProperty("maxStackSize", stack);
            }

            Integer maxDamage = components.get(DataComponents.MAX_DAMAGE);
            if (maxDamage != null && maxDamage > 0) {
                o.addProperty("durability", maxDamage);
            }

            Enchantable enchantable = components.get(DataComponents.ENCHANTABLE);
            if (enchantable != null) {
                o.addProperty("enchantability", enchantable.value());
            }

            addAttackValues(o, components);
        }

        if (item instanceof BuildingWandItem wand) {
            o.addProperty("wandSquareDiameter", wand.getWandSquareDiameter());
        }
        if (item instanceof ChiselItem chisel) {
            o.addProperty("cooldownTicks", chisel.getCooldownTicks());
            o.addProperty("dedicatedSpatula", chisel.isDedicatedSpatula());
        }
        if (item instanceof ReinforcedBundleItem bundle) {
            // getBaseCapacityItems() kommt ohne ItemStack aus - im Datagen
            // liesse sich keiner bauen, weil dessen Konstruktor die noch nicht
            // gebundenen Komponenten liest. QuiverItem ueberschreibt die
            // Methode mit seiner eigenen Staffelung.
            o.addProperty("bundleCapacityItems", bundle.getBaseCapacityItems());
        }
        return o;
    }

    /**
     * Angriffsschaden und -tempo so, wie der Tooltip sie zeigt: Grundwert des
     * Spielers plus die Modifier der Haupthand. {@code compute} rechnet alle
     * Operationen durch, nicht nur ADD_VALUE.
     */
    private void addAttackValues(JsonObject o, DataComponentMap components) {
        ItemAttributeModifiers mods = components.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (mods == null || mods.modifiers().isEmpty()) {
            return;
        }
        boolean hasDamage = false;
        boolean hasSpeed = false;
        for (ItemAttributeModifiers.Entry entry : mods.modifiers()) {
            if (Attributes.ATTACK_DAMAGE.equals(entry.attribute())) hasDamage = true;
            if (Attributes.ATTACK_SPEED.equals(entry.attribute())) hasSpeed = true;
        }
        if (hasDamage) {
            o.addProperty("attackDamage",
                    round(mods.compute(Attributes.ATTACK_DAMAGE, PLAYER_BASE_ATTACK_DAMAGE, EquipmentSlot.MAINHAND)));
        }
        if (hasSpeed) {
            o.addProperty("attackSpeed",
                    round(mods.compute(Attributes.ATTACK_SPEED, Attributes.DEFAULT_ATTACK_SPEED, EquipmentSlot.MAINHAND)));
        }
    }

    /** Zwei Nachkommastellen - mehr ist Fließkommarauschen und ließe den Diff flattern. */
    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }
}
