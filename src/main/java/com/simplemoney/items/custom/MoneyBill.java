package com.simplemoney.items.custom;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Map;

public class MoneyBill extends Item {
    private static final Map<Block, Block> MONEY_USE_MAP =
            Map.of(
                    Blocks.CARTOGRAPHY_TABLE,
                    Blocks.FLETCHING_TABLE,
                    Blocks.LOOM,
                    Blocks.GRINDSTONE,
                    Blocks.SMITHING_TABLE,
                    Blocks.STONECUTTER,
                    Blocks.ANVIL,
                    Blocks.CHIPPED_ANVIL
            );

    public MoneyBill(Settings settings) {super(settings);}

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        Block clickedBlock = world.getBlockState(context.getBlockPos()).getBlock();

        if (MONEY_USE_MAP.containsKey(clickedBlock)) {
            if (!world.isClient()) {
                world.setBlockState(context.getBlockPos(), MONEY_USE_MAP.get(clickedBlock).getDefaultState());
                context.getStack().decrement(1);

                world.playSound(null, context.getBlockPos(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.BLOCKS);
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        World world = user.getEntityWorld();

        if (!world.isClient()) {
            entity.heal(4.0F);
            stack.decrement(1);
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.0f);
        }


        return ActionResult.SUCCESS;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}