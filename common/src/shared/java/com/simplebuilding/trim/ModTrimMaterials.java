package com.simplebuilding.trim;

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
import net.minecraft.world.item.equipment.trim.TrimMaterial;

public class ModTrimMaterials {

    public static final ResourceKey<TrimMaterial> ASTRALIT = of("astralit");
    public static final ResourceKey<TrimMaterial> NIHILITH = of("nihilith");
    public static final ResourceKey<TrimMaterial> ENDERITE = of("enderite");

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
        TrimMaterial trimMaterial = McVersion.trimMaterial(key.identifier().getPath(),
                Component.translatable(Util.makeDescriptionId("trim_material", key.identifier())).setStyle(style));

        context.register(key, trimMaterial);
    }

    private static Holder<TrimMaterial> holder(ResourceKey<TrimMaterial> key, Style style) {
        return Holder.direct(McVersion.trimMaterial(key.identifier().getPath(),
                Component.translatable(Util.makeDescriptionId("trim_material", key.identifier())).setStyle(style)));
    }

    private static ResourceKey<TrimMaterial> of(String name) {
        return ResourceKey.create(Registries.TRIM_MATERIAL, Identifier.fromNamespaceAndPath("simplebuilding", name));
    }
}