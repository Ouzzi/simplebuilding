package com.simplebuilding.util;

import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;

/** One hammer strike breaks a block or one placed fire charge into small chips, on 26.3 only. */
public final class SledgehammerChips {
    public static final int DAMAGE = 1;
    public static final int COOLDOWN_TICKS = 10;

    private SledgehammerChips() {
    }

    /** Shared by the hammer and placed piles, so another pile interaction cannot consume this strike. */
    public static InteractionResult tryCrush(UseOnContext context) {
        if (!McVersion.SMALL_PLACEABLES) return InteractionResult.PASS;
        var level = context.getLevel();
        var player = context.getPlayer();
        var pos = context.getClickedPos();
        if (player == null || !(context.getItemInHand().getItem() instanceof com.simplebuilding.items.custom.SledgehammerItem)
                || !TransformTargets.mayTransform(level, player, pos, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        var state = level.getBlockState(pos);
        var entity = level.getBlockEntity(pos);
        ItemStack result;
        int fireIndex = -1;
        if (state.is(Blocks.ICE)) {
            result = new ItemStack(ModItems.ICE_CHIP, 4);
        } else if (state.is(Blocks.PACKED_ICE)) {
            result = new ItemStack(ModItems.ICE_CHIP, 9);
        } else if (state.is(Blocks.OBSIDIAN)) {
            result = new ItemStack(ModItems.OBSIDIAN_CHIP, 9);
        } else if (entity instanceof PlacedSmallPartsBlockEntity pile) {
            // Like disc flipping, select the most recently placed matching item.
            for (int i = pile.parts().size() - 1; i >= 0; i--) {
                if (pile.parts().get(i).is(Items.FIRE_CHARGE)) {
                    fireIndex = i;
                    break;
                }
            }
            if (fireIndex < 0) return InteractionResult.PASS;
            result = new ItemStack(ModItems.FIRE_CHIP, 4);
        } else if (entity instanceof PlacedTemplateBlockEntity placed && placed.getTemplate().is(Items.FIRE_CHARGE)) {
            result = new ItemStack(ModItems.FIRE_CHIP, 4);
        } else {
            return InteractionResult.PASS;
        }
        if (level.isClientSide() || player.getCooldowns().isOnCooldown(context.getItemInHand())) {
            return InteractionResult.SUCCESS;
        }
        if (entity instanceof PlacedSmallPartsBlockEntity pile) {
            var remaining = new ArrayList<>(pile.parts());
            remaining.remove(fireIndex);
            pile.setParts(remaining);
            if (remaining.isEmpty()) level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
        } else {
            // Clear the stored item first, so removing its block cannot drop a second fire charge.
            if (entity instanceof PlacedTemplateBlockEntity placed) placed.setTemplate(ItemStack.EMPTY);
            level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
        }
        Block.popResource(level, pos, result);
        level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
        ((ServerLevel) level).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, result.getItem()),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.05);
        McVersion.swing(player, context.getHand(), true);
        context.getItemInHand().hurtAndBreak(DAMAGE, player, context.getHand());
        player.getCooldowns().addCooldown(context.getItemInHand(), COOLDOWN_TICKS);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.SUCCESS;
    }
}
