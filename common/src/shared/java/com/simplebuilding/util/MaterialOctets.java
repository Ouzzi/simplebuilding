package com.simplebuilding.util;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.MaterialOctetBlock;
import com.simplebuilding.items.custom.MaterialOctetItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/**
 * Material-Achtel (Queue Nachtrag 24, docs/ai/PLAN-Q-HAMMER-OCTETS-2026-10-09.md): welcher volle Block in welche
 * Achtelzelle zerfaellt (Holz-Bretter, Melone), das Setzen einer Melonenscheibe als Achtel und die essbare glitzernde
 * Melonenscheibe.
 */
public final class MaterialOctets {
    /** Glitzernde Melonenscheibe: so viel wie eine Goldene Karotte (gleicher Goldpreis: 8 Goldnuggets). */
    public static final FoodProperties GLISTERING_MELON_SLICE_FOOD = new FoodProperties.Builder()
            .nutrition(6).saturationModifier(1.2F).build();

    private MaterialOctets() {
    }

    private static java.util.Map<Block, Block> cells;

    /**
     * Voller Block -> Achtelzelle fuer Holz, alle Material-Achtel (N19/N15) und die Melone, beim ersten Gebrauch gebaut.
     * Auf 26.3 meldet {@link LibServices#init} genau diese Paare bei SimpleLibs {@code OctetCells} an.
     */
    public static java.util.Map<Block, Block> ownCells() {
        if (cells == null) {
            java.util.Map<Block, Block> map = new java.util.LinkedHashMap<>();
            for (Block cell : ModBlocks.WOOD_OCTETS) {
                map.put(((MaterialOctetBlock) cell).source(), cell);
            }
            for (Block cell : ModBlocks.MATERIAL_OCTETS) {
                map.put(((MaterialOctetBlock) cell).source(), cell);
            }
            if (ModBlocks.MELON_OCTET != null) {
                map.put(Blocks.MELON, ModBlocks.MELON_OCTET);
            }
            cells = java.util.Collections.unmodifiableMap(map);
        }
        return cells;
    }

    /** Die Achtelzelle des Materials, dessen voller Block {@code full} ist; null ohne Achtel (26.3: SimpleLib {@code OctetCells}). */
    public static @Nullable Block cellFor(@Nullable Block full) {
        return LibServices.octetCellFor(full);
    }

    /**
     * Schleichen + Rechtsklick mit einer Melonenscheibe auf einen Block setzt sie als Melonen-Achtel; ohne Schleichen,
     * oder wo kein Achtel hinpasst, bleibt es beim Essen (null = nicht zustaendig).
     */
    public static @Nullable InteractionResult tryPlaceSlice(UseOnContext context) {
        Player player = context.getPlayer();
        if (ModBlocks.MELON_OCTET == null || player == null || !player.isSecondaryUseActive()
                || !context.getItemInHand().is(Items.MELON_SLICE)) {
            return null;
        }
        InteractionResult result = MaterialOctetItem.place(context, ModBlocks.MELON_OCTET);
        return result.consumesAction() ? result : null;
    }
}
