package com.simplebuilding.client.render;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.EnchantmentHelper;
import com.simplebuilding.util.MiningUtils;
import com.simplebuilding.util.SledgehammerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Erweitert die Vanilla-Abbau-Riss-Extraktion um alle verbundenen Blöcke bei
 * Sledgehammer, Strip Miner (Pickaxe + Sneak) und Vein Miner (Pickaxe/Axt + Sneak).
 * Die zusätzlichen {@link BlockBreakingRenderState}-Einträge werden von Vanilla
 * anschließend ganz normal als Riss-Overlay gerendert.
 *
 * <p>Gezeichnet wird, solange Vanilla selbst am anvisierten Block einen Riss extrahiert hat - nicht
 * solange {@code MultiPlayerGameMode#isDestroying()} gilt. Vanilla baut einen Block auch ohne dieses
 * Flag weiter ab: {@code continueDestroyBlock} nimmt fuer denselben Block mit demselben Werkzeug den
 * {@code sameDestroyTarget}-Zweig, der nur Fortschritt sammelt (etwa nachdem das Fadenkreuz fuer
 * einen Tick vom Block gerutscht ist), und zeichnet dabei seinen Riss. Bis 2026-09 fehlten in genau
 * diesem Fall die Risse der Nachbarbloecke. Der reine Stufen-Check allein reicht nicht: nach einem
 * solchen Abbau bleibt {@code getDestroyStage()} stehen, bis der naechste Klick kommt, und die Risse
 * wuerden um jeden anvisierten Block gezeichnet. Der Vanilla-Riss an {@code mainPos} schliesst das
 * aus - er steht nur am Block, den Vanilla wirklich abbaut.
 * Loader-neutral: Fabric ruft dies über LevelRenderEvents.END_EXTRACTION auf,
 * NeoForge über ExtractLevelRenderStateEvent — beide feuern nach der
 * Vanilla-Extraktion der Breaking-States.
 */
public final class MultiBlockBreakingSupport {

    // Cache: verbundene Blöcke nur neu berechnen, wenn sich Zielblock, Werkzeug oder Sneak ändert
    private static BlockPos lastMainPos = null;
    private static ItemStack lastToolStack = ItemStack.EMPTY;
    private static boolean lastSneaking = false;
    private static List<BlockPos> cachedConnectedBlocks = Collections.emptyList();

    private MultiBlockBreakingSupport() {
    }

    public static void extractExtraBreakingStates(LevelRenderState renderState, ClientLevel level) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;

        if (player == null || level == null || gameMode == null) {
            resetCache();
            return;
        }

        // Dieselbe Grenze wie ClientLevel#destroyBlockProgress. Jeder fertige Abbau setzt den
        // Fortschritt auf 0 (Stufe -1), der Cache wird also genau dort geleert, wo es frueher der
        // isDestroying-Check tat.
        int stage = gameMode.getDestroyStage();
        if (stage < 0 || stage > 9) {
            resetCache();
            return;
        }

        ItemStack stack = player.getMainHandItem();
        boolean isSledgehammer = stack.getItem() instanceof SledgehammerItem;
        int stripLevel = EnchantmentHelper.getEnchantmentLevel(stack, level, ModEnchantments.STRIP_MINER);
        int veinLevel = EnchantmentHelper.getEnchantmentLevel(stack, level, ModEnchantments.VEIN_MINER);

        if (!isSledgehammer && stripLevel <= 0 && veinLevel <= 0) {
            resetCache();
            return;
        }

        // Der lokale Spieler baut immer den Block ab, den er gerade anvisiert
        if (!(client.hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos mainPos = blockHit.getBlockPos();
        if (!vanillaCracks(renderState, mainPos)) {
            return;
        }
        BlockState mainState = level.getBlockState(mainPos);
        boolean sneaking = player.isShiftKeyDown();

        List<BlockPos> connectedBlocks;
        boolean cacheValid = mainPos.equals(lastMainPos)
                && ItemStack.isSameItem(stack, lastToolStack)
                && sneaking == lastSneaking;

        if (cacheValid) {
            connectedBlocks = cachedConnectedBlocks;
        } else {
            connectedBlocks = new ArrayList<>();

            if (isSledgehammer) {
                connectedBlocks.addAll(SledgehammerItem.getBlocksToBeDestroyed(1, mainPos, player));
            } else if (stack.getItem().isCorrectToolForDrops(stack, mainState) && sneaking) {
                if (stripLevel > 0 && stack.is(ItemTags.PICKAXES)) {
                    connectedBlocks.addAll(MiningUtils.getStripMinerBlocks(level, mainPos, player, stack, stripLevel));
                } else if (veinLevel > 0 && (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES))) {
                    connectedBlocks.addAll(MiningUtils.getVeinMinerBlocks(level, mainPos, mainState, veinLevel, stack));
                }
            }

            lastMainPos = mainPos;
            lastToolStack = stack;
            lastSneaking = sneaking;
            cachedConnectedBlocks = connectedBlocks;
        }

        for (BlockPos targetPos : connectedBlocks) {
            if (targetPos.equals(mainPos)) {
                continue;
            }
            if (isSledgehammer && !SledgehammerUtils.shouldBreak(level, targetPos, mainPos, stack)) {
                continue;
            }
            BlockState state = level.getBlockState(targetPos);
            if (state.isAir()) {
                continue;
            }
            renderState.blockBreakingRenderStates.add(new BlockBreakingRenderState(targetPos, state, stage));
        }
    }

    /** Ob Vanilla in diesem Durchlauf schon einen Riss an {@code pos} extrahiert hat. */
    private static boolean vanillaCracks(LevelRenderState renderState, BlockPos pos) {
        for (BlockBreakingRenderState state : renderState.blockBreakingRenderStates) {
            if (state.blockPos().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    private static void resetCache() {
        lastMainPos = null;
        lastToolStack = ItemStack.EMPTY;
        lastSneaking = false;
        cachedConnectedBlocks = Collections.emptyList();
    }
}
