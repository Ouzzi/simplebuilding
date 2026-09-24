package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.SledgehammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class SledgehammerUtils {

    /** Ab so vielen Bloecken wird der Hammer nicht mehr langsamer (5x5). */
    public static final int SPEED_BLOCK_CAP = 25;

    private SledgehammerUtils() {
    }

    /**
     * Wie viele Bloecke ein Schlag auf {@code origin} wirklich abbaut: der Ursprung plus jede
     * Position aus {@link SledgehammerItem#getBlocksToBeDestroyed}, die {@link #shouldBreak}
     * durchlaesst - dieselbe Auswahl, die {@code SledgehammerUsageEvent} abbaut. 0, wenn der Hammer
     * den Ursprung gar nicht bearbeitet (oder keiner in der Haupthand liegt).
     */
    public static int countBlocksBroken(Player player, BlockPos origin) {
        ItemStack stack = player.getMainHandItem();
        Level world = player.level();
        int count = 0;
        for (BlockPos pos : SledgehammerItem.getBlocksToBeDestroyed(1, origin, player)) {
            if (pos.equals(origin) || shouldBreak(world, pos, origin, stack)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Teiler fuer das Abbautempo: {@code sqrt(min(n, 25))} bei {@code n} wirklich abgebauten
     * Bloecken. 3x3 braucht damit etwa dreimal, 5x5 fuenfmal so lange wie ein einzelner Block;
     * bricht nur der Ursprung (auch beim Schleichen), ist der Hammer eine normale Spitzhacke
     * seines Materials. Client und Server rechnen dasselbe, sonst ruckelt der Abbau.
     */
    public static float miningSpeedDivisor(Player player, BlockPos origin) {
        if (!(player.getMainHandItem().getItem() instanceof SledgehammerItem)) {
            return 1.0F;
        }
        int blocks = countBlocksBroken(player, origin);
        if (blocks <= 1) {
            return 1.0F;
        }
        return (float) Math.sqrt(Math.min(blocks, SPEED_BLOCK_CAP));
    }

    /**
     * Prüft, ob der Ursprungsblock überhaupt mit dem Vorschlaghammer bearbeitet werden kann.
     */
    public static boolean canMineOrigin(Level world, BlockPos originPos, ItemStack stack) {
        return shouldBreak(world, originPos, originPos, stack);
    }

    /**
     * Prüft, ob ein Block basierend auf Override-Stufe abgebaut werden soll.
     * <ul>
     *   <li>Stufe 0: gleicher Blocktyp + Spitzhacke</li>
     *   <li>Stufe 1: gemischte Spitzhacke-Blöcke</li>
     *   <li>Stufe 2+: beliebige abbau bare Blöcke</li>
     * </ul>
     */
    public static boolean shouldBreak(Level world, BlockPos pos, BlockPos originPos, ItemStack stack) {
        BlockState targetState = world.getBlockState(pos);
        BlockState originState = world.getBlockState(originPos);

        if (targetState.isAir() || targetState.getDestroySpeed(world, pos) < 0.0F) {
            return false;
        }

        if (!(stack.getItem() instanceof SledgehammerItem hammer)) {
            return false;
        }

        int overrideLevel = EnchantmentHelper.getEnchantmentLevel(stack, world, ModEnchantments.OVERRIDE);
        boolean sameBlock = targetState.getBlock() == originState.getBlock();
        boolean pickaxeBlock = targetState.is(BlockTags.MINEABLE_WITH_PICKAXE);

        if (overrideLevel <= 0) {
            return sameBlock && pickaxeBlock && hammer.isCorrectToolForDrops(stack, targetState);
        }
        if (overrideLevel == 1) {
            return pickaxeBlock && hammer.isCorrectToolForDrops(stack, targetState);
        }
        return true;
    }
}
