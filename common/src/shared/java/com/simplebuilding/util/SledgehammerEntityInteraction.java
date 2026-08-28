package com.simplebuilding.util;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.SledgehammerItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class SledgehammerEntityInteraction {
    private SledgehammerEntityInteraction() {
    }

    public static InteractionResult handleAttackEntity(Player player, Level world, InteractionHand hand, Entity entity) {
        if (world.isClientSide()) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        ItemStack mainStack = player.getMainHandItem();
        ItemStack offStack = player.getOffhandItem();

        if (mainStack.getItem() instanceof SledgehammerItem && offStack.is(Items.GLOW_INK_SAC)) {
            if (entity instanceof ItemFrame itemFrame) {
                ItemStack frameStack = itemFrame.getItem();

                if (isTrimTemplate(frameStack)) {
                    itemFrame.setItem(new ItemStack(ModItems.GLOWING_TRIM_TEMPLATE), true);

                    if (!player.isCreative()) {
                        offStack.shrink(1);
                        mainStack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    }

                    world.playSound(null, itemFrame.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1.0f, 1.5f);
                    world.playSound(null, itemFrame.blockPosition(), SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 1.0f, 1.0f);

                    world.addParticle(ParticleTypes.GLOW, itemFrame.getX(), itemFrame.getY(), itemFrame.getZ(), 0.0, 0.1, 0.0);
                    world.addParticle(ParticleTypes.GLOW_SQUID_INK, itemFrame.getX(), itemFrame.getY(), itemFrame.getZ(), 0.0, 0.1, 0.0);

                    return InteractionResult.SUCCESS;
                }
            }
        }

        if (mainStack.getItem() instanceof SledgehammerItem && offStack.is(Items.GLOWSTONE_DUST)) {
            if (entity instanceof ItemFrame itemFrame) {
                ItemStack frameStack = itemFrame.getItem();

                if (isTrimTemplate(frameStack)) {
                    itemFrame.setItem(new ItemStack(ModItems.EMITTING_TRIM_TEMPLATE), true);

                    if (!player.isCreative()) {
                        offStack.shrink(1);
                        mainStack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    }

                    world.playSound(null, itemFrame.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1.0f, 1.5f);
                    world.playSound(null, itemFrame.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.0f, 1.0f);

                    world.addParticle(ParticleTypes.GLOW, itemFrame.getX(), itemFrame.getY(), itemFrame.getZ(), 0.0, 0.1, 0.0);
                    world.addParticle(ParticleTypes.LARGE_SMOKE, itemFrame.getX(), itemFrame.getY(), itemFrame.getZ(), 0.0, 0.1, 0.0);

                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.PASS;
    }

    private static boolean isTrimTemplate(ItemStack stack) {
        String path = stack.getItem().toString();
        return path.contains("trim_smithing_template");
    }
}
