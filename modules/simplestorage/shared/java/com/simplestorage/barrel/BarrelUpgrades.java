package com.simplestorage.barrel;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The in-world upgrade of a barrel (copper -&gt; reinforced -&gt; netherite -&gt; enderite, {@code in_world_upgrades/barrels.json},
 * run by SimpleLib's axe and hammer ways): the contents, the lid direction and the docking state go to the new block.
 */
public final class BarrelUpgrades {
    private BarrelUpgrades() {}

    /** Replaces the barrel by {@code to}, keeping facing, lid direction, docking state and contents (owner 11). */
    public static int upgradeInPlace(ServerLevel level, BlockPos pos, Block to) {
        List<ItemStack> kept = new ArrayList<>();
        if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
            for (int i = 0; i < barrel.getContainerSize(); i++) kept.add(barrel.removeItemNoUpdate(i));
        }
        BlockState from = level.getBlockState(pos);
        BlockState next = to.defaultBlockState();
        if (from.hasProperty(BarrelBlock.FACING) && next.hasProperty(BarrelBlock.FACING)) {
            next = next.setValue(BarrelBlock.FACING, from.getValue(BarrelBlock.FACING))
                    .setValue(BarrelBlock.ATTACHED, from.getValue(BarrelBlock.ATTACHED))
                    .setValue(BarrelBlock.TOP, from.getValue(BarrelBlock.TOP));
        }
        level.setBlock(pos, next, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof BarrelBlockEntity fresh) {
            for (int i = 0; i < kept.size() && i < fresh.getContainerSize(); i++) fresh.setItem(i, kept.get(i));
        }
        return 1;
    }
}
