package com.simplebuilding.trim;

import com.simplebuilding.items.ModArmorMaterials;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.MaterialAssetGroup;
import net.minecraft.world.item.equipment.trim.TrimMaterial;

public class ModTrimMaterials {

    public static final ResourceKey<TrimMaterial> ASTRALIT = of("astralit");
    public static final ResourceKey<TrimMaterial> NIHILITH = of("nihilith");
    public static final ResourceKey<TrimMaterial> ENDERITE = of("enderite");

    /**
     * Materials whose trim is drawn in the "_darker" palette on armour of the same material, as
     * vanilla does for iron on iron: Enderite trim on Enderite armour (asset group override).
     */
    private static final java.util.Map<ResourceKey<TrimMaterial>, ResourceKey<EquipmentAsset>> DARKER_ON =
            java.util.Map.of(ENDERITE, ModArmorMaterials.ENDERITE_ASSET_KEY);

    private static MaterialAssetGroup assets(ResourceKey<TrimMaterial> key) {
        String palette = key.identifier().getPath();
        ResourceKey<EquipmentAsset> darkerOn = DARKER_ON.get(key);
        return darkerOn == null ? MaterialAssetGroup.create(palette)
                : MaterialAssetGroup.create(palette, java.util.Map.of(darkerOn, palette + "_darker"));
    }

    public static final Holder<TrimMaterial> NIHILITH_HOLDER = holder(NIHILITH, Style.EMPTY.withColor(TextColor.fromRgb(0xAA00AA)));
    public static final Holder<TrimMaterial> ASTRALIT_HOLDER = holder(ASTRALIT, Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55)));
    public static final Holder<TrimMaterial> ENDERITE_HOLDER = holder(ENDERITE, Style.EMPTY.withColor(TextColor.fromRgb(0x9A7BD8)));

    public static void bootstrap(BootstrapContext<TrimMaterial> context) {
        // itemModelIndex (der float Wert) wurde entfernt, wir übergeben nur noch Style/Farbe
        register(context, ASTRALIT, Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55)));
        register(context, NIHILITH, Style.EMPTY.withColor(TextColor.fromRgb(0xAA00AA)));
        register(context, ENDERITE, Style.EMPTY.withColor(TextColor.fromRgb(0x9A7BD8)));
    }

    private static void register(BootstrapContext<TrimMaterial> context, ResourceKey<TrimMaterial> key, Style style) {
        // FIX: Nutze die statische Factory-Methode 'of', die den String automatisch in eine AssetId umwandelt
        MaterialAssetGroup assets = assets(key);

        // Erstelle das Material nur mit Assets und Beschreibung
        TrimMaterial trimMaterial = new TrimMaterial(
                assets,
                Component.translatable(Util.makeDescriptionId("trim_material", key.identifier())).setStyle(style)
        );

        context.register(key, trimMaterial);
    }

    private static Holder<TrimMaterial> holder(ResourceKey<TrimMaterial> key, Style style) {
        MaterialAssetGroup assets = assets(key);
        return Holder.direct(new TrimMaterial(
                assets,
                Component.translatable(Util.makeDescriptionId("trim_material", key.identifier())).setStyle(style)
        ));
    }

    private static ResourceKey<TrimMaterial> of(String name) {
        return ResourceKey.create(Registries.TRIM_MATERIAL, Identifier.fromNamespaceAndPath("simplebuilding", name));
    }
}