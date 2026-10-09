package com.simplebuilding.woodwork;

import com.simplebuilding.items.custom.ChiselItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Carving a sherd motif into wood (docs/ai/PLAN-HOLZWERK-2026-10-09.md): chisel in the main hand, a pottery sherd in
 * the off hand, right-click the side of a standing stripped log (or of carved wood, to change the motif). The sherd
 * is a stencil and stays; the chisel takes one point of damage and its usual cooldown.
 */
public final class PotteryCarving {
    private PotteryCarving() {
    }

    /** The carved state for {@code target} clicked on {@code face} with {@code motif}, or null if it cannot be carved. */
    public static @Nullable BlockState carve(BlockState target, Direction face, SherdMotif motif) {
        if (!face.getAxis().isHorizontal()) {
            return null;
        }
        CarvedLogBlock carved = null;
        if (target.getBlock() instanceof CarvedLogBlock already) {
            carved = already;
        } else if (target.hasProperty(RotatedPillarBlock.AXIS) && target.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y) {
            carved = WoodBlocks.carvedFor(target.getBlock());
        }
        return carved == null ? null : carved.defaultBlockState().setValue(CarvedLogBlock.FACING, face).setValue(CarvedLogBlock.MOTIF, motif);
    }

    /**
     * The chisel's use on a block when a sherd is in the off hand: null if the sherd rule does not apply (the chisel
     * carries on as usual), otherwise the result - also PASS for a stripped log clicked on its top, so the sherd
     * never hollows the log by accident.
     */
    public static @Nullable InteractionResult useOn(ChiselItem chisel, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND || chisel.isDedicatedSpatula()) {
            return null;
        }
        SherdMotif motif = SherdMotif.of(player.getOffhandItem());
        if (motif == null) {
            return null;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState target = level.getBlockState(pos);
        if (!(target.getBlock() instanceof CarvedLogBlock) && WoodBlocks.carvedFor(target.getBlock()) == null) {
            return null;
        }
        BlockState carved = carve(target, context.getClickedFace(), motif);
        if (carved == null || carved == target) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        level.setBlockAndUpdate(pos, carved);
        level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 0.8F, 1.1F + level.getRandom().nextFloat() * 0.2F);
        ((ServerLevel) level).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, target),
                pos.getX() + 0.5 + context.getClickedFace().getStepX() * 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5 + context.getClickedFace().getStepZ() * 0.5, 10, 0.2, 0.2, 0.2, 0.05);
        if (!player.getAbilities().instabuild) {
            player.getCooldowns().addCooldown(stack, chisel.effectiveCooldownTicks());
            stack.hurtAndBreak(ChiselItem.TRANSFORM_DAMAGE, player, EquipmentSlot.MAINHAND);
        }
        com.simplebuilding.stats.ModStats.award(player, com.simplebuilding.stats.ModStats.CHISEL_USES);
        return InteractionResult.SUCCESS_SERVER;
    }
}
