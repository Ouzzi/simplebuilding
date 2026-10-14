package com.simplebuilding.mixin;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.ShriekerSilence;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Right-click a Sculk Shrieker with a Silent Dandelion: sounds off / on, for every player (owner N57). Same
 * feedback and cost as on mobs: the Golden Dandelion sounds and growth particles, one flower per use except in
 * Creative. The Vanilla block has no item interaction of its own, so this adds one (like {@code ShulkerBoxBlockMixin}).
 */
@Mixin(SculkShriekerBlock.class)
public abstract class SculkShriekerBlockMixin {
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!McVersion.SILENT_DANDELION || !stack.is(ModItems.SILENT_DANDELION) || !player.getAbilities().mayBuild) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel server && server.getBlockEntity(pos) instanceof ShriekerSilence shrieker) {
            boolean silenced = !shrieker.simplebuilding$isSilenced();
            shrieker.simplebuilding$setSilenced(silenced);
            stack.consume(1, player);
            level.playSound(null, pos, silenced ? SoundEvents.GOLDEN_DANDELION_USE : SoundEvents.GOLDEN_DANDELION_UNUSE,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            server.sendParticles(silenced ? ParticleTypes.PAUSE_MOB_GROWTH : ParticleTypes.RESET_MOB_GROWTH,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.0);
        }
        return InteractionResult.SUCCESS;
    }
}
