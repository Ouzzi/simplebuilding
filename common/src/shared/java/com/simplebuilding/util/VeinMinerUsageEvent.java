package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class VeinMinerUsageEvent {

    private static final Set<BlockPos> MINED_BLOCKS = new HashSet<>();

    private VeinMinerUsageEvent() {
    }

    public static boolean handleBeforeBlockBreak(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        // --- ÄNDERUNG: Nur ausführen, wenn Spieler sneakt ---
        if (!player.isShiftKeyDown()) {
            return true;
        }

        ItemStack stack = player.getMainHandItem();

        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        if (MINED_BLOCKS.contains(pos)) return true;
        if (!stack.getItem().isCorrectToolForDrops(stack, state)) return true;

        boolean isPickaxe = stack.is(ItemTags.PICKAXES);
        boolean isAxe = stack.is(ItemTags.AXES);
        if (!isPickaxe && !isAxe) return true;

        var registry = world.registryAccess();
        var enchantLookup = registry.lookupOrThrow(Registries.ENCHANTMENT);
        var veinMinerKey = enchantLookup.get(ModEnchantments.VEIN_MINER);

        if (veinMinerKey.isEmpty()) return true;

        int level = EnchantmentHelper.getItemEnchantmentLevel(veinMinerKey.get(), stack);
        if (level <= 0) return true;

        // Logik: Nur Erze (Pickaxe) oder Logs (Axt)
        // Die Erzliste steht nur noch in MiningUtils.isOre; die Riss-Vorschau fragt dieselbe
        // Methode. Eine eigene Kopie hier hatte genau eine Wirkung: sie ist auseinandergelaufen
        // (die Vorschau zaehlte Netherquarzerz und Antiken Schutt mit, der Abbau nicht).
        if (isPickaxe && !MiningUtils.isOre(state)) return true;
        if (isAxe && !state.is(BlockTags.LOGS)) return true;

        // Budget und Suche stehen nur noch in MiningUtils.getVeinMinerBlocks; die Riss-Vorschau
        // fragt dieselbe Methode. Bis zum 2026-09-10 trug dieser Hook eine eigene Kopie der
        // Stufentabelle und der Breitensuche - und eine Mutation der Tabelle in MiningUtils
        // (Stufe I: 6 statt 3) liess jeden Servertest gruen, weil der Abbau sie nie las. Genau
        // die Drift, die der Kommentar oben fuer die Erzliste als beseitigt meldete, sass eine
        // Zeile tiefer noch. Die Liste kommt ohne den Startblock zurueck, so wie der Abbau ihn
        // will: den bricht Vanilla selbst.
        List<BlockPos> blocksToMine = MiningUtils.getVeinMinerBlocks(world, pos, state, level, stack);

        for (BlockPos targetPos : blocksToMine) {
            if (targetPos.equals(pos)) continue;

            if (stack.isEmpty()) break;

            MINED_BLOCKS.add(targetPos);
            try {
                serverPlayer.gameMode.destroyBlock(targetPos);
            } finally {
                // MINED_BLOCKS ist statisch und wird nie geleert. Ohne finally bliebe targetPos
                // nach einer Ausnahme aus destroyBlock (Blockentity, Loot, ein anderer Mod im
                // Break-Event) fuer immer drin, und der Rekursionsschutz oben wuerde jeden
                // spaeteren Abbau an genau dieser Weltposition den Rest der Sitzung lang
                // stillschweigend verschlucken. SledgehammerUsageEvent sichert sich genauso ab.
                MINED_BLOCKS.remove(targetPos);
            }
        }

        return true;
    }
}