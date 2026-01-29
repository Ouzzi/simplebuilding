package com.simplebuilding.trim;

import com.simplebuilding.items.ModItems;
import net.minecraft.item.equipment.trim.ArmorTrimAssets;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Map;

public class ModTrimMaterials {

    public static final RegistryKey<ArmorTrimMaterial> ASTRALIT = of("astralit");
    public static final RegistryKey<ArmorTrimMaterial> NIHILITH = of("nihilith");
    public static final RegistryKey<ArmorTrimMaterial> ENDERITE = of("enderite");

    public static void bootstrap(Registerable<ArmorTrimMaterial> context) {
        // itemModelIndex (der float Wert) wurde entfernt, wir übergeben nur noch Style/Farbe
        register(context, ASTRALIT, Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55)));
        register(context, NIHILITH, Style.EMPTY.withColor(TextColor.fromRgb(0xAA00AA)));
        register(context, ENDERITE, Style.EMPTY.withColor(TextColor.fromRgb(0x111111)));
    }

    private static void register(Registerable<ArmorTrimMaterial> context, RegistryKey<ArmorTrimMaterial> key, Style style) {
        // FIX: Nutze die statische Factory-Methode 'of', die den String automatisch in eine AssetId umwandelt
        ArmorTrimAssets assets = ArmorTrimAssets.of(key.getValue().getPath());

        // Erstelle das Material nur mit Assets und Beschreibung
        ArmorTrimMaterial trimMaterial = new ArmorTrimMaterial(
                assets,
                Text.translatable(Util.createTranslationKey("trim_material", key.getValue())).setStyle(style)
        );

        context.register(key, trimMaterial);
    }

    private static RegistryKey<ArmorTrimMaterial> of(String name) {
        return RegistryKey.of(RegistryKeys.TRIM_MATERIAL, Identifier.of("simplebuilding", name));
    }
}