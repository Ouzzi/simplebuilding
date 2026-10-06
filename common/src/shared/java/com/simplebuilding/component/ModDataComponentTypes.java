package com.simplebuilding.component;

import com.mojang.serialization.Codec;
import com.simplebuilding.Simplebuilding;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class ModDataComponentTypes {
    public static final DataComponentType<Integer> GUIDE_CHAPTERS = register("guide_chapters", builder -> builder
            .persistent(Codec.intRange(0, (1 << 20) - 1))
            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
    public static final DataComponentType<Integer> OFFSET = register("offset", builder -> builder.persistent(Codec.INT));

    /** Teile eines Pfeils vom Befiederungstisch (2026-10-01). */
    public static final DataComponentType<com.simplebuilding.fletching.ArrowParts.Parts> ARROW_PARTS = register("arrow_parts", builder -> builder
            .persistent(com.simplebuilding.fletching.ArrowParts.Parts.CODEC)
            .networkSynchronized(com.simplebuilding.fletching.ArrowParts.Parts.STREAM_CODEC));

    // Glowing hat nur noch eine Stufe (Besitzer 2026-09-29): der Codec liest alte Stufe-2-Ruestung als 1,
    // beim naechsten Speichern steht dann 1 da (GlowingTrimUtils.normalizeGlowLevel). Ohne eigenen
    // Netzwerk-Codec leitet Vanilla ihn aus diesem ab, der Client sieht also ebenfalls nur 1.
    public static final DataComponentType<Integer> GLOW_LEVEL = register("glow_level", builder -> builder.persistent(
            Codec.INT.xmap(com.simplebuilding.util.GlowingTrimUtils::normalizeGlowLevel, level -> level)));

    /** Kupfer-Eimer (Crucible P5, 26.3): Oxidationsstufe 0-3 (Optik) und gewachst. */
    public static final DataComponentType<Integer> OXIDATION = com.simplebuilding.version.McVersion.CRUCIBLE ? register("oxidation", builder -> builder
            .persistent(Codec.intRange(0, 3)).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT)) : null;
    public static final DataComponentType<Boolean> WAXED = com.simplebuilding.version.McVersion.CRUCIBLE ? register("waxed", builder -> builder
            .persistent(Codec.BOOL).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL)) : null;
    /** Ceramic bucket (owner N12b): fills and pours so far (0..31); the wear stage is the item itself. */
    public static final DataComponentType<Integer> CERAMIC_USES = com.simplebuilding.version.McVersion.CRUCIBLE ? register("ceramic_uses", builder -> builder
            .persistent(Codec.intRange(0, 31)).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT)) : null;

    // NEU: Visueller Glow (RGB Effekt)
    public static final DataComponentType<Boolean> VISUAL_GLOW = register("visual_glow", builder -> builder.persistent(Codec.BOOL));

    // NEU: Lichtquelle (Fackel-Effekt)
    public static final DataComponentType<Boolean> LIGHT_SOURCE = register("light_source", builder -> builder.persistent(Codec.BOOL));

    // Pulsierender Besatz (Besitzer 2026-09-28): der Besatz blendet im Takt nach Schwarz und zurueck,
    // mit Glowing leuchtend. Gesetzt am Schmiedetisch (Pulsating Armor Trim + Echoscherbe), gelesen
    // nur vom Client (EquipmentRendererMixin) - deshalb auch zum Client synchronisiert.
    public static final DataComponentType<Boolean> PULSATING = register("pulsating", builder -> builder
            .persistent(Codec.BOOL)
            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

    public static final DataComponentType<BlockPos> COORDINATES =
            register("coordinates", builder -> builder.persistent(BlockPos.CODEC));

    // Rucksack-Inhalt (getragen, im Inventar und als Block-Drop). Eigener Codec statt
    // minecraft:container, weil Tiefe Taschen Stapel ueber 99 zulaesst; siehe BackpackContents.
    public static final DataComponentType<BackpackContents> BACKPACK_CONTENTS =
            register("backpack_contents", builder -> builder
                    .persistent(BackpackContents.CODEC)
                    .networkSynchronized(BackpackContents.STREAM_CODEC)
                    .cacheEncoding());

    // Blaupause: Bau-Code + Titel/Autor/signiert (docs/BLUEPRINT.md).
    public static final DataComponentType<com.simplebuilding.blueprint.BlueprintContent> BLUEPRINT =
            register("blueprint", builder -> builder
                    .persistent(com.simplebuilding.blueprint.BlueprintContent.CODEC)
                    .networkSynchronized(com.simplebuilding.blueprint.BlueprintContent.STREAM_CODEC)
                    .cacheEncoding());

    // Drehung der Blaupause im Baumodus (Viertelumdrehungen im Uhrzeigersinn, 0..3; Strg+Mausrad).
    public static final DataComponentType<Integer> BLUEPRINT_ROTATION =
            register("blueprint_rotation", builder -> builder
                    .persistent(Codec.intRange(0, 3))
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    // Amethystlinse mit Beruehrung des Konstrukteurs: letzte gemessene Entfernung (Tooltip).
    public static final DataComponentType<LensMeasurement> LENS_MEASUREMENT =
            register("lens_measurement", builder -> builder
                    .persistent(LensMeasurement.CODEC)
                    .networkSynchronized(LensMeasurement.STREAM_CODEC)
                    // Die Messung wird beim Strahlen jede Sekunde neu geschrieben; ohne das senkte
                    // Vanilla den Stab in der Hand jedes Mal ab und hob ihn wieder (Besitzer 2026-09-29:
                    // "das Item darf sich nicht bewegen"). Wie DAMAGE.
                    .ignoreSwapAnimation());

    // Gestufte Shulkerkisten (Netherit x2, Enderit x4): die echten Anzahlen der Plaetze ueber 99, die
    // minecraft:container nicht speichert (dort steht eine lesbare Kopie mit 99). Zum Client
    // synchronisiert, sonst verloere das Kreativinventar sie beim Verschieben.
    public static final DataComponentType<ContainerCounts> CONTAINER_COUNTS =
            register("container_counts", builder -> builder
                    .persistent(ContainerCounts.CODEC)
                    .networkSynchronized(ContainerCounts.STREAM_CODEC));


    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name), (builderOperator.apply(DataComponentType.builder())).build());
    }

    public static void registerDataComponentTypes() {
        Simplebuilding.LOGGER.info("Registering Data Component Types for " + Simplebuilding.MOD_ID);
    }
}
