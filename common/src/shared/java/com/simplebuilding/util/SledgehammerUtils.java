package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.SledgehammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class SledgehammerUtils {

    private SledgehammerUtils() {
    }

    /**
     * Prüft, ob der Ursprungsblock überhaupt mit dem Vorschlaghammer bearbeitet werden kann.
     */
    public static boolean canMineOrigin(Level world, BlockPos originPos, ItemStack stack) {
        return shouldBreak(world, originPos, originPos, stack);
    }

    /**
     * Prüft, ob ein Block basierend auf Override-Stufe abgebaut werden soll.
     * <ul>
     *   <li>Stufe 0: gleicher Blocktyp + Spitzhacke</li>
     *   <li>Stufe 1: gemischte Spitzhacke-Blöcke</li>
     *   <li>Stufe 2+: beliebige abbau bare Blöcke</li>
     * </ul>
     */
    public static boolean shouldBreak(Level world, BlockPos pos, BlockPos originPos, ItemStack stack) {
        BlockState targetState = world.getBlockState(pos);
        BlockState originState = world.getBlockState(originPos);

        if (targetState.isAir() || targetState.getDestroySpeed(world, pos) < 0.0F) {
            return false;
        }

        if (!(stack.getItem() instanceof SledgehammerItem hammer)) {
            return false;
        }

        int overrideLevel = EnchantmentHelper.getEnchantmentLevel(stack, world, ModEnchantments.OVERRIDE);
        boolean sameBlock = targetState.getBlock() == originState.getBlock();
        boolean pickaxeBlock = targetState.is(BlockTags.MINEABLE_WITH_PICKAXE);

        if (overrideLevel <= 0) {
            return sameBlock && pickaxeBlock && hammer.isCorrectToolForDrops(stack, targetState);
        }
        if (overrideLevel == 1) {
            return pickaxeBlock && hammer.isCorrectToolForDrops(stack, targetState);
        }
        return true;
    }
}
