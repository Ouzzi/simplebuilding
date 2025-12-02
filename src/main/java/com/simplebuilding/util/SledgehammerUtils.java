package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.SledgehammerItem;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SledgehammerUtils {

    /**
     * Prüft, ob ein Block basierend auf den Enchantments des Hammers abgebaut werden soll.
     */
    public static boolean shouldBreak(World world, BlockPos pos, BlockPos originPos, ItemStack stack) {
        BlockState targetState = world.getBlockState(pos);
        BlockState originState = world.getBlockState(originPos);

        if (targetState.isAir() || targetState.getHardness(world, pos) < 0) {return false;}

        RegistryWrapper.WrapperLookup registry = world.getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var ignoreTypeKey = enchantments.getOptional(ModEnchantments.IGNORE_BLOCK_TYPE);

        int ignoreLevel = 0;
        if (ignoreTypeKey.isPresent()) {ignoreLevel = EnchantmentHelper.getLevel(ignoreTypeKey.get(), stack);}
        if (ignoreLevel >= 2) {return true;}
        if (ignoreLevel == 1) {return targetState.getBlock() == originState.getBlock() || stack.getItem().isCorrectForDrops(stack, targetState);}

        return targetState.getBlock() == originState.getBlock();
    }
}