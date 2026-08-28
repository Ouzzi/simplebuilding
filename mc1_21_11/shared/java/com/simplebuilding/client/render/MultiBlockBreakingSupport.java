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
import net.minecraft.client.renderer.state.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
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
 * Loader-neutral: Fabric ruft dies über WorldRenderEvents.END_EXTRACTION auf,
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

        if (player == null || level == null || gameMode == null || !gameMode.isDestroying()) {
            resetCache();
            return;
        }

        int stage = gameMode.getDestroyStage();
        if (stage < 0 || stage > 9) {
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
            renderState.blockBreakingRenderStates.add(new BlockBreakingRenderState(level, targetPos, stage));
        }
    }

    private static void resetCache() {
        lastMainPos = null;
        lastToolStack = ItemStack.EMPTY;
        lastSneaking = false;
        cachedConnectedBlocks = Collections.emptyList();
    }
}
