package com.simplequalityoflife.event;

import com.simplequalityoflife.Simplequalityoflife;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Iterator;
import java.util.List;

public class HoeHarvestHandler {



    public static InteractionResult onRightClickBlock(Player player, InteractionHand hand, BlockPos pos, Direction face) {
        if (!Simplequalityoflife.getConfig().qOL.enableHoeHarvest) return InteractionResult.PASS;

        Level level = player.level();
        if (level.isClientSide()) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ItemTags.HOES)) return InteractionResult.PASS;
        if (!InteractionGuard.allow(player, pos)) return InteractionResult.PASS;

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (isMatureCrop(state, block) && InteractionGuard.action(player) && InteractionGuard.mayChange(player, pos)) {
            return harvest(player, (ServerLevel) level, pos, state, stack);
        }

        return InteractionResult.PASS;
    }

    private static boolean isMatureCrop(BlockState state, Block block) {
        if (block instanceof CropBlock cropBlock) {
            return cropBlock.isMaxAge(state);
        } else if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) == 3;
        } else if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) == 2;
        }
        return false;
    }

    private static InteractionResult harvest(Player player, ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
        // Block.getDrops builds the loot context internally (ORIGIN, TOOL, THIS_ENTITY, BLOCK_STATE).
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, tool);
        Item seedItem = getSeedItem(state.getBlock());

        boolean paidSeed = false;

        Iterator<ItemStack> iterator = drops.iterator();
        while (iterator.hasNext()) {
            ItemStack drop = iterator.next();

            if (drop.getItem() == seedItem) {
                drop.shrink(1);
                paidSeed = true;

                if (drop.isEmpty()) {
                    iterator.remove();
                }
                break;
            }
        }

        if (!paidSeed && !player.isCreative()) {
            return InteractionResult.PASS;
        }

        BlockState newState = state;
        if (state.getBlock() instanceof CropBlock crop) {
            newState = crop.getStateForAge(0);
        } else if (state.getBlock() instanceof NetherWartBlock) {
            newState = state.setValue(NetherWartBlock.AGE, 0);
        } else if (state.getBlock() instanceof CocoaBlock) {
            newState = state.setValue(CocoaBlock.AGE, 0);
        }

        if (!level.setBlockAndUpdate(pos, newState)) return InteractionResult.PASS;
        for (ItemStack drop : drops) Block.popResource(level, pos, drop);

        level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
        // A few crop crumbs like a vanilla break (not levelEvent 2001: that would play the break sound twice).
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state),
                pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 8, 0.25, 0.2, 0.25, 0.05);

        if (!player.isCreative()) {
            tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }

        player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);

        return InteractionResult.SUCCESS;
    }

    private static Item getSeedItem(Block block) {
        if (block == Blocks.WHEAT) return Items.WHEAT_SEEDS;
        if (block == Blocks.POTATOES) return Items.POTATO;
        if (block == Blocks.CARROTS) return Items.CARROT;
        if (block == Blocks.BEETROOTS) return Items.BEETROOT_SEEDS;
        if (block == Blocks.NETHER_WART) return Items.NETHER_WART;
        if (block == Blocks.COCOA) return Items.COCOA_BEANS;
        return Items.AIR;
    }
}
