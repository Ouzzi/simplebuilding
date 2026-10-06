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

/**
 * Hammer strikes break a block or one placed fire charge into small chips, on 26.3 only. Since 2026-10-06 (owner) step by
 * step: one chip per strike ({@link com.simplebuilding.util.InWorldStrikes#strikeYield}), as many strikes as chips; the
 * last strike removes the block and frees all chips. Durability and cooldown once, at the end.
 */
public final class SledgehammerChips {
    public static final int DAMAGE = 1;
    public static final int COOLDOWN_TICKS = 10;
    public static final int ICE_CHIPS = 4;
    public static final int PACKED_ICE_CHIPS = 9;
    public static final int OBSIDIAN_CHIPS = 9;
    public static final int FIRE_CHIPS = 4;

    /** One crushing for JEI and the wiki: input item, result, count = strikes (one per strike). */
    public record Crush(net.minecraft.world.item.Item input, net.minecraft.world.item.Item result, int count) {
    }

    /** Every hammer crushing besides the diamond block (which has its own section), as the game does it. */
    public static java.util.List<Crush> crushes() {
        java.util.List<Crush> out = new ArrayList<>();
        if (McVersion.SMALL_PLACEABLES) {
            out.add(new Crush(Items.ICE, ModItems.ICE_CHIP, ICE_CHIPS));
            out.add(new Crush(Items.PACKED_ICE, ModItems.ICE_CHIP, PACKED_ICE_CHIPS));
            out.add(new Crush(Items.OBSIDIAN, ModItems.OBSIDIAN_CHIP, OBSIDIAN_CHIPS));
            out.add(new Crush(Items.FIRE_CHARGE, ModItems.FIRE_CHIP, FIRE_CHIPS));
        }
        if (McVersion.CRUCIBLE) {
            out.add(new Crush(Items.QUARTZ_BLOCK, Items.QUARTZ, com.simplebuilding.items.custom.SledgehammerItem.QUARTZ_BLOCK_QUARTZ));
        }
        return out;
    }

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
            result = new ItemStack(ModItems.ICE_CHIP, ICE_CHIPS);
        } else if (state.is(Blocks.PACKED_ICE)) {
            result = new ItemStack(ModItems.ICE_CHIP, PACKED_ICE_CHIPS);
        } else if (state.is(Blocks.OBSIDIAN)) {
            result = new ItemStack(ModItems.OBSIDIAN_CHIP, OBSIDIAN_CHIPS);
        } else if (entity instanceof PlacedSmallPartsBlockEntity pile) {
            // Like disc flipping, select the most recently placed matching item.
            for (int i = pile.parts().size() - 1; i >= 0; i--) {
                if (pile.parts().get(i).is(Items.FIRE_CHARGE)) {
                    fireIndex = i;
                    break;
                }
            }
            if (fireIndex < 0) return InteractionResult.PASS;
            result = new ItemStack(ModItems.FIRE_CHIP, FIRE_CHIPS);
        } else if (entity instanceof PlacedTemplateBlockEntity placed && placed.getTemplate().is(Items.FIRE_CHARGE)) {
            result = new ItemStack(ModItems.FIRE_CHIP, FIRE_CHIPS);
        } else {
            return InteractionResult.PASS;
        }
        if (level.isClientSide() || player.getCooldowns().isOnCooldown(context.getItemInHand())) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel server = (ServerLevel) level;
        var strike = InWorldStrikes.strikeYield(server, pos, "chips", state, result, result.getCount(), server.getGameTime());
        if (strike == null) {
            return InteractionResult.SUCCESS;
        }
        McVersion.swing(player, context.getHand(), true);
        if (!strike.finished()) {
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
        InWorldStrikes.release(server, pos, result);
        level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
        ((ServerLevel) level).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, result.getItem()),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.05);
        context.getItemInHand().hurtAndBreak(DAMAGE, player, context.getHand());
        player.getCooldowns().addCooldown(context.getItemInHand(), COOLDOWN_TICKS);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.SUCCESS;
    }
}
