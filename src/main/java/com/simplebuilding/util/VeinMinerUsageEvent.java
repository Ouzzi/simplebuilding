package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class VeinMinerUsageEvent implements PlayerBlockBreakEvents.Before {

    private static final Set<BlockPos> MINED_BLOCKS = new HashSet<>();

    @Override
    public boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        ItemStack stack = player.getMainHandStack();

        if (!(player instanceof ServerPlayerEntity serverPlayer)) return true;
        if (MINED_BLOCKS.contains(pos)) return true;
        if (!stack.getItem().isCorrectForDrops(stack, state)) return true;

        boolean isPickaxe = stack.isIn(ItemTags.PICKAXES);
        boolean isAxe = stack.isIn(ItemTags.AXES);
        if (!isPickaxe && !isAxe) return true;

        var registry = world.getRegistryManager();
        var enchantLookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var veinMinerKey = enchantLookup.getOptional(ModEnchantments.VEIN_MINER);

        if (veinMinerKey.isEmpty()) return true;

        int level = EnchantmentHelper.getLevel(veinMinerKey.get(), stack);
        if (level <= 0) return true;

        // Logik: Nur Erze (Pickaxe) oder Logs (Axt)
        if (isPickaxe && !isOre(state)) return true;
        if (isAxe && !state.isIn(BlockTags.LOGS)) return true;

        int maxBlocks = switch (level) {
            case 1 -> 3;
            case 2 -> 6;
            case 3 -> 9;
            case 4 -> 12;
            case 5 -> 18;
            default -> 18;
        };

        List<BlockPos> blocksToMine = findConnectedBlocks(world, pos, state, maxBlocks);

        for (BlockPos targetPos : blocksToMine) {
            if (targetPos.equals(pos)) continue;

            if (stack.isEmpty()) break;

            MINED_BLOCKS.add(targetPos);
            boolean broken = serverPlayer.interactionManager.tryBreakBlock(targetPos);
            MINED_BLOCKS.remove(targetPos);
        }

        return true;
    }

    private List<BlockPos> findConnectedBlocks(World world, BlockPos startPos, BlockState targetState, int maxCount) {
        List<BlockPos> found = new ArrayList<>();
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(startPos);
        visited.add(startPos);
        int foundCount = 0;

        while (!queue.isEmpty() && foundCount < (maxCount - 1)) {
            BlockPos current = queue.poll();

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;

                        BlockPos neighbor = current.add(x, y, z);
                        if (!visited.contains(neighbor)) {
                            BlockState neighborState = world.getBlockState(neighbor);
                            if (neighborState.getBlock() == targetState.getBlock()) {
                                visited.add(neighbor);
                                queue.add(neighbor);
                                found.add(neighbor);
                                foundCount++;
                                if (foundCount >= (maxCount - 1)) break;
                            }
                        }
                    }
                    if (foundCount >= (maxCount - 1)) break;
                }
                if (foundCount >= (maxCount - 1)) break;
            }
        }
        return found;
    }

    private boolean isOre(BlockState state) {
        return state.isIn(BlockTags.COAL_ORES) ||
                state.isIn(BlockTags.IRON_ORES) ||
                state.isIn(BlockTags.COPPER_ORES) ||
                state.isIn(BlockTags.GOLD_ORES) ||
                state.isIn(BlockTags.REDSTONE_ORES) ||
                state.isIn(BlockTags.LAPIS_ORES) ||
                state.isIn(BlockTags.DIAMOND_ORES) ||
                state.isIn(BlockTags.EMERALD_ORES);
    }
}