package com.simplebuilding.trim;

import com.simplebuilding.items.ModArmorMaterials;
import com.simplebuilding.version.McVersion;

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
import net.minecraft.world.item.equipment.trim.TrimMaterial;

public class ModTrimMaterials {

    public static final ResourceKey<TrimMaterial> ASTRALIT = of("astralit");
    public static final ResourceKey<TrimMaterial> NIHILITH = of("nihilith");
    public static final ResourceKey<TrimMaterial> ENDERITE = of("enderite");

    /**
     * Materials whose trim is drawn in the "_darker" palette on armour of the same material, as
     * vanilla does for iron on iron: Enderite trim on Enderite armour. 26.2 stores this in the
     * material (asset group override), 26.3 in the equipment asset's trim_overrides
     * (mc26_3/overlay/resources/assets/simplebuilding/equipment/enderite.json).
     */
    private static final java.util.Map<ResourceKey<TrimMaterial>, ResourceKey<EquipmentAsset>> DARKER_ON =
            java.util.Map.of(ENDERITE, ModArmorMaterials.ENDERITE_ASSET_KEY);

    public static final Holder<TrimMaterial> NIHILITH_HOLDER = holder(NIHILITH, Style.EMPTY.withColor(TextColor.fromRgb(0xAA00AA)));
    public static final Holder<TrimMaterial> ASTRALIT_HOLDER = holder(ASTRALIT, Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55)));
    public static final Holder<TrimMaterial> ENDERITE_HOLDER = holder(ENDERITE, Style.EMPTY.withColor(TextColor.fromRgb(0x111111)));

    public static void bootstrap(BootstrapContext<TrimMaterial> context) {
        // itemModelIndex (der float Wert) wurde entfernt, wir übergeben nur noch Style/Farbe
        register(context, ASTRALIT, Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55)));
        register(context, NIHILITH, Style.EMPTY.withColor(TextColor.fromRgb(0xAA00AA)));
        register(context, ENDERITE, Style.EMPTY.withColor(TextColor.fromRgb(0x111111)));
    }

    private static void register(BootstrapContext<TrimMaterial> context, ResourceKey<TrimMaterial> key, Style style) {
        // FIX: Nutze die statische Factory-Methode 'of', die den String automatisch in eine AssetId umwandelt
        // 26.2: Asset-Gruppe, 26.3: Paletten-Id - beides aus dem Pfad, siehe McVersion.trimMaterial.
        TrimMaterial trimMaterial = material(key, style);

        context.register(key, trimMaterial);
    }

    private static Holder<TrimMaterial> holder(ResourceKey<TrimMaterial> key, Style style) {
        return Holder.direct(material(key, style));
    }

    private static TrimMaterial material(ResourceKey<TrimMaterial> key, Style style) {
        String palette = key.identifier().getPath();
        Component description = Component.translatable(Util.makeDescriptionId("trim_material", key.identifier())).setStyle(style);
        ResourceKey<EquipmentAsset> darkerOn = DARKER_ON.get(key);
        return darkerOn == null ? McVersion.trimMaterial(palette, description)
                : McVersion.trimMaterial(palette, description, darkerOn);
    }

    private static ResourceKey<TrimMaterial> of(String name) {
        return ResourceKey.create(Registries.TRIM_MATERIAL, Identifier.fromNamespaceAndPath("simplebuilding", name));
    }
}