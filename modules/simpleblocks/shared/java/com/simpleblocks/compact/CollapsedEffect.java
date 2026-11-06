package com.simpleblocks.compact;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Collapsed blocks are so dense that they slow you down (owner 06.11., docs/ai/PLAN-A-COMPACT-2026-11-06.md): Slowness I with at
 * least one stack in the inventory, II from 2 stacks, III from 9 stacks. The normal Vanilla effect, refreshed every second for a few
 * seconds like a beacon (ambient, no particles), so it is gone a moment after the last stack leaves the inventory. Switch:
 * {@link CollapsedConfig#enabled}. The red slot beam on the client ({@code SlotAlert}) shows while this effect really acts.
 */
public final class CollapsedEffect {
    /** Refresh interval and effect length in ticks (a bit more than the interval, so it never flickers). */
    public static final int INTERVAL = 20, DURATION = 60;
    public static final int STACKS_II = 2, STACKS_III = 9;

    private CollapsedEffect() {}

    public static boolean isCollapsed(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BlockItem bi && CompactBlocks.COLLAPSED.contains(bi.getBlock());
    }

    /** Number of Collapsed stacks in the whole inventory (main, armour, off hand). */
    public static int stacks(Inventory inv) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isCollapsed(inv.getItem(i))) n++;
        }
        return n;
    }

    /** Slowness amplifier for this many stacks, -1 for none. */
    public static int amplifier(int stacks) {
        return stacks >= STACKS_III ? 2 : stacks >= STACKS_II ? 1 : stacks >= 1 ? 0 : -1;
    }

    /** Server tick: once per {@link #INTERVAL} ticks every player gets the effect for the stacks they carry. */
    public static void tick(MinecraftServer server) {
        if (!CollapsedConfig.enabled || server.getTickCount() % INTERVAL != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) apply(player);
    }

    public static void apply(ServerPlayer player) {
        if (!CollapsedConfig.enabled || player.isSpectator()) return;
        int amp = amplifier(stacks(player.getInventory()));
        if (amp >= 0) player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DURATION, amp, true, false, true));
    }
}
