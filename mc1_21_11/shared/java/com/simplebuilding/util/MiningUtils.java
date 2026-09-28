package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MiningUtils {

    /**
     * Teiler fuer das Abbautempo, den Strip Miner kostet: 2, 3 oder 4 fuer Stufe I-III, sonst 1.
     * Gilt nur fuer eine Spitzhacke (Tag {@code minecraft:pickaxes}) auf einem Block, den sie
     * wirklich abbaut. Vorschlaghaemmer stehen nicht in dem Tag - dieser Teiler und
     * {@link SledgehammerUtils#miningSpeedDivisor} schliessen sich also aus.
     *
     * <p>Angewandt in {@code BlockStateBaseMixin} auf {@code getDestroyProgress}, nicht mehr auf
     * {@code Player#getDestroySpeed(BlockState)}: NeoForge und Forge rufen beim Abbau ihre
     * Variante mit Position und umgingen die alte Injektion.
     */
    public static float stripMinerSpeedDivisor(Player player, BlockState state) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ItemTags.PICKAXES) || !stack.getItem().isCorrectToolForDrops(stack, state)) {
            return 1.0F;
        }
        return switch (EnchantmentHelper.getEnchantmentLevel(stack, player.level(), ModEnchantments.STRIP_MINER)) {
            case 1 -> 2.0F;
            case 2 -> 3.0F;
            case 3 -> 4.0F;
            default -> 1.0F;
        };
    }

    public static List<BlockPos> getStripMinerBlocks(Level world, BlockPos startPos, Player player, ItemStack stack, int level) {
        List<BlockPos> found = new ArrayList<>();
        int depth = getStripMinerDepth(level);
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

    /**
     * Die eine Tiefentabelle des Streifenabbaus. Die Riss-Vorschau (ueber
     * {@link #getStripMinerBlocks}) und der Abbau-Hook (StripMinerUsageEvent) fragen beide genau
     * diese Methode - die Vorschau kann also keine Tiefe mehr anzeigen, die der Server danach
     * nicht abbaut.
     *
     * <p>Stufe III ist die Hoechststufe der Verzauberung (max_level 3) und bekommt mit 4 statt 3
     * einen Extrablock; I und II graben so tief, wie sie hoch sind. Das ist eine Balance-Zusage
     * des Handbuchs, kein Rechenfehler.
     */
    public static int getStripMinerDepth(int level) {
        return (level == 3) ? 4 : level;
    }

    /**
     * Die eine Abbaurichtung. Riss-Vorschau und Abbau-Hook fragen dieselbe Methode; eine zweite
     * Kopie im Hook koennte invertiert werden, ohne dass die Vorschau davon etwas merkte - der
     * Spieler saehe dann Risse in der einen und Loecher in der anderen Richtung.
     */
    public static Direction getMiningDirection(Player player) {
        float pitch = player.getXRot();
        if (pitch < -60) return Direction.UP;
        if (pitch > 60) return Direction.DOWN;
        return player.getDirection();
    }

    /**
     * Die eine Erzliste des Aderabbaus. Die Riss-Vorschau (MultiBlockBreakingSupport) und der
     * Abbau-Hook (VeinMinerUsageEvent) fragen beide genau diese Methode - die Vorschau kann also
     * nichts mehr anzeigen, was der Server danach stehen laesst.
     *
     * <p>Der Block-Tag {@code simplebuilding:vein_miner_ores} (Besitzer 2026-09-28): {@code #c:ores}
     * des Loaders, die acht Vanilla-Erz-Tags, Netherquarzerz, Nethergolderz, Antiker Schutt und die
     * Mod-Erze. Netherquarzerz und Antiker Schutt standen frueher nur in der Vorschau-Kopie; jetzt
     * gehoeren sie bewusst dazu (Handbuch und {@code VeinAndStripMinerTests} ziehen mit).
     */
    public static boolean isOre(BlockState state) {
        // Seit 2026-09-28 ein Tag statt einer festen Liste: simplebuilding:vein_miner_ores enthaelt
        // #c:ores des Loaders (jedes Mod-Erz nach Konvention), die Vanilla-Erz-Tags, Netherquarzerz,
        // Nethergolderz, Antiken Schutt und die Mod-Erze; Modpacks koennen ihn per Datapack erweitern.
        return state.is(ModTags.Blocks.VEIN_MINER_ORES);
    }
}