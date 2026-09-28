package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class StripMinerUsageEvent {

    // Verhindert, dass das Event durch sich selbst (tryBreakBlock) rekursiv ausgelöst wird
    private static final Set<BlockPos> MINING_BLOCKS = new HashSet<>();

    private StripMinerUsageEvent() {
    }

    public static boolean handleBeforeBlockBreak(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        // --- ÄNDERUNG: Nur ausführen, wenn Spieler sneakt ---
        if (!player.isShiftKeyDown()) {
            return true;
        }

        ItemStack stack = player.getMainHandItem();

        if (!(player instanceof ServerPlayer serverPlayer) || !stack.is(ItemTags.PICKAXES)) {return true;}

        if (MINING_BLOCKS.contains(pos)) {return true;}

        if (!stack.getItem().isCorrectToolForDrops(stack, state)) {return true;}

        var registry = world.registryAccess();
        var enchantLookup = registry.lookupOrThrow(Registries.ENCHANTMENT);
        var stripMinerKey = enchantLookup.get(ModEnchantments.STRIP_MINER);

        if (stripMinerKey.isEmpty()) return true;

        int level = EnchantmentHelper.getItemEnchantmentLevel(stripMinerKey.get(), stack);
        if (level <= 0) return true;

        // --- Logik Start ---
        // Tiefentabelle und Abbaurichtung stehen nur noch in MiningUtils; die Riss-Vorschau fragt
        // dieselben Methoden. Eigene Kopien hier haetten genau eine Wirkung: sie koennten
        // auseinanderlaufen, und der Spieler saehe Risse dort, wo der Server nichts abbaut.
        int depth = MiningUtils.getStripMinerDepth(level);

        Direction miningDirection = MiningUtils.getMiningDirection(player);

        int brokenBlocks = 0;
        int damageBefore = stack.getDamageValue();
        for (int i = 1; i <= depth; i++) {
            BlockPos targetPos = pos.relative(miningDirection, i);
            BlockState targetState = world.getBlockState(targetPos);

            if (targetState.isAir() || targetState.getDestroySpeed(world, targetPos) < 0) {break;}
            if (!stack.getItem().isCorrectToolForDrops(stack, targetState)) {break;}
            // Vanilla-Spawnschutz und Weltgrenze: destroyBlock prueft beides nicht (das tut nur der
            // Paket-Handler fuer den angeklickten Block). Geschuetzte Stellen werden uebersprungen.
            if (!world.mayInteract(player, targetPos)) {continue;}

            MINING_BLOCKS.add(targetPos);
            boolean broken;
            try {
                broken = serverPlayer.gameMode.destroyBlock(targetPos);
            } finally {
                // MINING_BLOCKS ist statisch und wird nie geleert. Ohne finally bliebe targetPos
                // nach einer Ausnahme aus destroyBlock (Blockentity, Loot, ein anderer Mod im
                // Break-Event) fuer immer drin, und der Rekursionsschutz oben wuerde jeden
                // spaeteren Abbau an genau dieser Weltposition den Rest der Sitzung lang
                // stillschweigend verschlucken. SledgehammerUsageEvent sichert sich genauso ab.
                MINING_BLOCKS.remove(targetPos);
            }

            if (broken) {
                brokenBlocks++;
                if (stack.isEmpty()) {break;}
            }
        }

        if (brokenBlocks > 0) {
            com.simplebuilding.advancement.ModTriggers.feature(serverPlayer, com.simplebuilding.advancement.ModTriggers.STRIP_MINE);
        }
        if (brokenBlocks > 0 && !stack.isEmpty()) {
            // Rabatt: ein Drittel des Schadens, den die Zusatzbloecke WIRKLICH gekostet haben
            // (gerundet). Frueher hing er an der Blockzahl ((n + 1) / 3); mit Unbreaking kosten
            // die Bloecke oft gar nichts, und der Rabatt reparierte die Spitzhacke dann netto
            // (Audit 2026-09-26 #26). Ohne Unbreaking ist gemessen == Blockzahl x Schaden je Block.
            int measured = stack.getDamageValue() - damageBefore;
            int damageRefund = (measured + 1) / 3;
            if (damageRefund > 0) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - damageRefund));
            }
        }

        return true;
    }
}