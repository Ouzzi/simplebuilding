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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Carving wood with the chisel (docs/ai/PLAN-HOLZWERK-2026-10-09.md, N34, N40): chisel in the main hand,
 * right-click a side of a standing stripped log (or of carved wood) and all four sides are carved at once (N40).
 * Without a sherd they get the wood's own motif, with a pottery sherd in the off hand the sherd's pattern (the sherd
 * is a stencil and stays). Sneaking with the chisel takes the whole carving off again (the stripped log comes back).
 * The chisel takes one point of damage and its usual cooldown. Top and bottom clicks
 * keep the chisel's old job (without sherd it hollows the log).
 */
public final class PotteryCarving {
    private PotteryCarving() {
    }

    /**
     * The carved state for {@code target} clicked on a side: {@code motif} null = the wood's own motif, else the
     * sherd's pattern - always on all four sides at once (N40). With {@code remove} the whole carving comes off (the
     * stripped log comes back). Null if it cannot be carved or nothing would change.
     */
    public static @Nullable BlockState carve(BlockState target, @Nullable SherdMotif motif, boolean remove) {
        if (target.getBlock() instanceof CarvedLogBlock block) {
            BlockState next = remove ? strippedOf(block).defaultBlockState() : block.carvedWith(motif);
            return next == target ? null : next;
        }
        if (remove || !target.hasProperty(RotatedPillarBlock.AXIS) || target.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y) {
            return null;
        }
        CarvedLogBlock carved = WoodBlocks.carvedFor(target.getBlock());
        return carved == null ? null : carved.carvedWith(motif);
    }

    private static Block strippedOf(CarvedLogBlock block) {
        return block.wood().strippedBlock();
    }

    /**
     * The chisel's use on a block: null if carving does not apply (the chisel carries on as usual), otherwise the
     * result - also PASS for a sherd on a stripped log's top, so the sherd never hollows the log by accident.
     */
    public static @Nullable InteractionResult useOn(ChiselItem chisel, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND || chisel.isDedicatedSpatula()) {
            return null;
        }
        SherdMotif motif = SherdMotif.of(player.getOffhandItem());
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState target = level.getBlockState(pos);
        boolean alreadyCarved = target.getBlock() instanceof CarvedLogBlock;
        if (!alreadyCarved && WoodBlocks.carvedFor(target.getBlock()) == null) {
            return null;
        }
        Direction face = context.getClickedFace();
        boolean remove = player.isShiftKeyDown();
        if (!face.getAxis().isHorizontal()) {
            // Top/bottom: a sherd must not hollow the log; without one the chisel's table runs (hollowing).
            return motif != null ? InteractionResult.PASS : null;
        }
        if (remove && !alreadyCarved) {
            return null; // sneaking on a plain stripped log: the chisel's own reverse action
        }
        BlockState carved = carve(target, motif, remove);
        if (carved == null) {
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
        level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.8F, 1.1F + level.getRandom().nextFloat() * 0.2F);
        ((ServerLevel) level).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, target),
                pos.getX() + 0.5 + face.getStepX() * 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5 + face.getStepZ() * 0.5, 10, 0.2, 0.2, 0.2, 0.05);
        if (!player.getAbilities().instabuild) {
            player.getCooldowns().addCooldown(stack, chisel.effectiveCooldownTicks());
            stack.hurtAndBreak(ChiselItem.TRANSFORM_DAMAGE, player, EquipmentSlot.MAINHAND);
        }
        com.simplebuilding.stats.ModStats.award(player, com.simplebuilding.stats.ModStats.CHISEL_USES);
        return InteractionResult.SUCCESS_SERVER;
    }
}
