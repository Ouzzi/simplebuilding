package com.simplebuilding.util;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class MiningUtils {

    public static List<BlockPos> getStripMinerBlocks(Level world, BlockPos startPos, Player player, ItemStack stack, int level) {
        List<BlockPos> found = new ArrayList<>();
        int depth = (level == 3) ? 4 : level;
        Direction miningDirection = getMiningDirection(player);

        for (int i = 1; i <= depth; i++) {
            BlockPos targetPos = startPos.relative(miningDirection, i);
            BlockState targetState = world.getBlockState(targetPos);

            if (targetState.isAir() || targetState.getDestroySpeed(world, targetPos) < 0) break;
            if (!stack.getItem().isCorrectToolForDrops(stack, targetState)) break;
            found.add(targetPos);
        }
        return found;
    }

    public static List<BlockPos> getVeinMinerBlocks(Level world, BlockPos startPos, BlockState targetState, int level, ItemStack stack) {
        boolean isPickaxe = stack.is(ItemTags.PICKAXES);
        boolean isAxe = stack.is(ItemTags.AXES);

        // Validierung: Nur Erze bei Spitzhacken, nur Holz bei Äxten
        if (isPickaxe && !isOre(targetState)) return Collections.emptyList();
        if (isAxe && !targetState.is(BlockTags.LOGS)) return Collections.emptyList();

        int maxBlocks = switch (level) {
            case 1 -> 3;
            case 2 -> 6;
            case 3 -> 9;
            case 4 -> 12;
            case 5 -> 18;
            default -> 18;
        };

        List<BlockPos> found = new ArrayList<>();
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        // Startblock zur Queue hinzufügen, aber nicht zur 'found'-Liste (das macht die Logik unten)
        // Normalerweise zählt VeinMiner den abgebauten Block mit.
        queue.add(startPos);
        visited.add(startPos);
        found.add(startPos);

        while (!queue.isEmpty() && found.size() < maxBlocks) {
            BlockPos current = queue.poll();

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;

                        BlockPos neighbor = current.offset(x, y, z);
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor); // Sofort markieren
                            BlockState neighborState = world.getBlockState(neighbor);

                            // Check: Ist es der gleiche Block? (z.B. Coal Ore == Coal Ore)
                            if (neighborState.getBlock() == targetState.getBlock()) {
                                queue.add(neighbor);
                                found.add(neighbor);
                                if (found.size() >= maxBlocks) break;
                            }
                            // Optional: Deepslate-Varianten erkennen (z.B. Coal Ore und Deepslate Coal Ore)
                            // Das erfordert komplexere Logik oder Tags. Fürs erste reicht der Block-Vergleich.
                        }
                    }
                    if (found.size() >= maxBlocks) break;
                }
                if (found.size() >= maxBlocks) break;
            }
        }

        // Entferne den Startblock aus der Rückgabeliste, da WorldRendererMixin nur *zusätzliche* Blöcke rendern will?
        // Im Mixin iterierst du über die Liste und renderst Damage.
        // Der Spieler baut den Startblock bereits ab (Vanilla Damage Render).
        // Also entfernen wir den Startblock aus der Liste für das Rendering / Logic.
        found.remove(startPos);

        return found;
    }

    public static Direction getMiningDirection(Player player) {
        float pitch = player.getXRot();
        if (pitch < -60) return Direction.UP;
        if (pitch > 60) return Direction.DOWN;
        return player.getDirection();
    }

    public static boolean isOre(BlockState state) {
        // Vanilla Tags nutzen. Hinweis: GOLD_ORES beinhaltet in Vanilla auch Nether Gold Ore.
        // MC 1.21.11: Die Erz-Tags stehen noch direkt in BlockTags (minecraft:coal_ores usw.);
        // die gepaarten Block/Item-Tags gibt es erst ab 26.2. Tag-Daten unveraendert.
        return state.is(BlockTags.COAL_ORES) ||
                state.is(BlockTags.IRON_ORES) ||
                state.is(BlockTags.COPPER_ORES) ||
                state.is(BlockTags.GOLD_ORES) ||
                state.is(BlockTags.REDSTONE_ORES) ||
                state.is(BlockTags.LAPIS_ORES) ||
                state.is(BlockTags.DIAMOND_ORES) ||
                state.is(BlockTags.EMERALD_ORES) ||
                state.is(Blocks.NETHER_QUARTZ_ORE) || // Manueller Check für Quarz
                state.is(Blocks.ANCIENT_DEBRIS);       // Optional: Antiker Schutt als Erz zählen
    }
}