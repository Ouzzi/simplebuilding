package com.simplebuilding.blocks.custom;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.ModToolMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dimensions-Schrott (Besitzer 2026-10-01): kommt in jeder Dimension vor und bleibt bis SimpleBuilding v2
 * ein geheimnisvoller Block ohne Rezept. Abbaubar nur mit Enderit-Spitzhacke oder -Vorschlaghammer; jedes
 * andere Werkzeug (und die Hand) kommt nicht voran. Die Haerte ist so gewaehlt, dass eine unverzauberte
 * Enderit-Spitzhacke {@link #BREAK_SECONDS} Sekunden braucht - so lange wie Obsidian mit der Hand. Effizienz,
 * Eile und Wasser/Luft wirken wie immer (Effizienz V: rund 69 s).
 */
public class DimensionalScrapBlock extends Block {
    /** Abbauzeit mit unverzauberter Enderit-Spitzhacke am Boden, in Sekunden. */
    public static final int BREAK_SECONDS = 250;
    /** Vanilla: Fortschritt je Tick = Werkzeugtempo / Haerte / 30 mit passendem Werkzeug. */
    public static final float HARDNESS = BREAK_SECONDS * 20F * ModToolMaterials.ENDERITE.speed() / 30F;

    public DimensionalScrapBlock(Properties properties) {
        super(properties);
    }

    public static boolean isEnderiteTool(ItemStack stack) {
        return stack.is(ModItems.ENDERITE_PICKAXE) || stack.is(ModItems.ENDERITE_SLEDGEHAMMER);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return isEnderiteTool(player.getMainHandItem()) ? super.getDestroyProgress(state, player, level, pos) : 0.0F;
    }
}
