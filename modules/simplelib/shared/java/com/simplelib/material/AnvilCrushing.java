package com.simplelib.material;

import com.simplebuilding.framework.api.Protection;
import com.simplelib.api.SimpleLibApi.AutomationCheck;
import com.simplelib.registry.LibMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Anvil crushing (shared material {@code cracked_diamond}, split stage 3d): an anvil that falls at least one block onto
 * a diamond block consumes it and drops 72 diamond pebbles - nine pebbles craft a cracked diamond, which blasts into one
 * diamond, so automation always loses one diamond's value. Runs only while a loaded mod requested the material.
 */
public final class AnvilCrushing {
    public static final int PEBBLES = 72;
    public static final int MINIMUM_FALL_BLOCKS = 1;

    private static volatile AutomationCheck check = AnvilCrushing::protectionAllows;

    private AnvilCrushing() {}

    public static void setAutomationCheck(AutomationCheck newCheck) {
        check = newCheck == null ? AnvilCrushing::protectionAllows : newCheck;
    }

    public static void onLand(Level level, BlockPos landed, FallingBlockEntity anvil) {
        if (!LibMaterials.requested("cracked_diamond") || !(level instanceof ServerLevel server)
                || !anvil.getBlockState().is(BlockTags.ANVIL)
                || anvil.getStartPos().getY() - landed.getY() < MINIMUM_FALL_BLOCKS) return;
        BlockPos target = landed.below();
        if (!server.getBlockState(target).is(Blocks.DIAMOND_BLOCK)
                || !check.allows(server, anvil.getStartPos(), target)
                || !server.destroyBlock(target, false, anvil)) return;

        // Consume first, then drop legal stacks. Repeated falls cannot reuse the same input.
        for (int remaining = PEBBLES; remaining > 0;) {
            ItemStack pebbles = new ItemStack(LibMaterials.item("diamond_pebble"));
            int count = Math.min(remaining, pebbles.getMaxStackSize());
            pebbles.setCount(count);
            Block.popResource(server, target, pebbles);
            remaining -= count;
        }
    }

    private static boolean protectionAllows(ServerLevel level, BlockPos source, BlockPos target) {
        var server = level.getServer();
        if (!Protection.active(server)) return true;
        String dimension = level.dimension().identifier().toString();
        return Protection.allowsAutomation(server, new Protection.Target(null, dimension, source.getX(), source.getY(), source.getZ(), false),
                new Protection.Target(null, dimension, target.getX(), target.getY(), target.getZ(), false));
    }
}
