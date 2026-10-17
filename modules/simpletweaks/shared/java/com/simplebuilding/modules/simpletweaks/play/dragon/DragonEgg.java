package com.simplebuilding.modules.simpletweaks.play.dragon;

import com.simplebuilding.modules.simpletweaks.play.Play;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * T28: every dragon fight leaves a dragon egg. Vanilla places it only after the first fight
 * ({@code EnderDragonFight#setDragonKilled}); for every later kill this puts it on top of the exit portal pillar, or
 * drops it as an item when an egg already lies there. Simple End Dragon Fight changes the fight itself, not the egg, and calls
 * the same Vanilla method, so it works with it unchanged.
 */
public final class DragonEgg {
    public static boolean active() { return Play.on() && Play.server() != null && Play.config().dragonEgg().enabled(); }

    /** Places (or drops) one egg on the podium at {@code podium}; true when it was placed as a block. */
    public static boolean give(ServerLevel level, BlockPos podium) {
        var pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, podium);
        if (level.getBlockState(pos).isAir() && !level.getBlockState(pos.below()).is(Blocks.DRAGON_EGG)) { level.setBlockAndUpdate(pos, Blocks.DRAGON_EGG.defaultBlockState()); return true; }
        var e = new ItemEntity(level, pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, new ItemStack(Items.DRAGON_EGG));
        e.setUnlimitedLifetime();
        level.addFreshEntity(e);
        return false;
    }
    private DragonEgg() {}
}
