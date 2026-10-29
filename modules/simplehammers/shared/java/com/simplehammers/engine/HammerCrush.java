package com.simplehammers.engine;

import com.simplelib.api.HammerActions;
import com.simplelib.api.InWorldStrikes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Crushing with the hammer (HS29-3, owner 29.10.): five right-click strikes turn Stone into Cobblestone, Cobblestone into
 * Gravel and Gravel into Sand - the same for every sledgehammer, strike gap by hammer tier ({@link SledgehammerUpgrades#hitInterval}).
 * Sneaking is the reshape / carve stance and leaves these blocks alone; the 3x3 area breaking and the corner carving of the
 * hammer are untouched. Each strike cracks the block and grows the next stage over it (shared {@link InWorldStrikes});
 * the last strike swaps the block and costs {@value #DAMAGE} durability. Gravel and Sand fall like in Vanilla.
 */
public final class HammerCrush {
    public static final int STRIKES = 5;
    public static final int DAMAGE = 1;
    /** {@link InWorldStrikes} kind of the crushing. */
    public static final String KIND = "hammer_crush";

    private HammerCrush() {}

    /** The block {@code block} is crushed into, or null for any other block. */
    public static @Nullable Block next(Block block) {
        if (block == Blocks.STONE) return Blocks.COBBLESTONE;
        if (block == Blocks.COBBLESTONE) return Blocks.GRAVEL;
        if (block == Blocks.GRAVEL) return Blocks.SAND;
        return null;
    }

    /** Whether a plain right click (no sneaking) of {@code player} on {@code state} is a crushing strike. */
    public static boolean crushes(BlockState state, Player player) {
        return !player.isShiftKeyDown() && next(state.getBlock()) != null;
    }

    /** One strike at game time {@code now}; the number of counted strikes (0 = too soon). */
    public static int strike(ServerLevel level, BlockPos pos, Player player, ItemStack hammer) {
        return strike(level, pos, player, hammer, level.getGameTime());
    }

    public static int strike(ServerLevel level, BlockPos pos, Player player, ItemStack hammer, long now) {
        BlockState state = level.getBlockState(pos);
        Block next = next(state.getBlock());
        if (next == null) return 0;
        int done = InWorldStrikes.count(level, pos, KIND, state, STRIKES, now, SledgehammerUpgrades.hitInterval(hammer));
        if (done == 0) return 0;
        SledgehammerUpgrades.swing(player);
        InWorldStrikes.feedback(level, pos, state, done, STRIKES);
        if (done < STRIKES) {
            InWorldStrikes.preview(level, pos, next.defaultBlockState(), done, STRIKES);
            return done;
        }
        BlockState crushed = next.defaultBlockState();
        level.setBlock(pos, crushed, Block.UPDATE_ALL);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, crushed));
        level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.9F, 0.9F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                16, 0.3, 0.3, 0.3, 0.05);
        if (!player.isCreative() && player instanceof ServerPlayer serverPlayer) {
            EquipmentSlot slot = player.getOffhandItem() == hammer ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
            hammer.hurtAndBreak(DAMAGE, level, serverPlayer, item -> player.onEquippedItemBroken(item, slot));
        }
        HammerActions.fire(HammerActions.CRUSH, player);
        return done;
    }
}
