package com.simplebuilding.modules.simplemobs;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;

/**
 * Boss bars of the Simple Mobs bosses and minibosses (Nachtrag 68). The bar shows the life-based phases: the overlay
 * gets notches exactly on the phase borders ({@link #overlayFor}), the color and the name change with the phase.
 */
public final class BossBars {
    private BossBars() {}

    /**
     * The overlay whose notches sit on every given phase border (health shares 0..1): the smallest of 6, 10, 12 and
     * 20 segments that hits all of them, else the plain progress bar.
     */
    public static BossEvent.BossBarOverlay overlayFor(float... borders) {
        if (borders.length == 0) return BossEvent.BossBarOverlay.PROGRESS;
        int[] counts = {6, 10, 12, 20};
        BossEvent.BossBarOverlay[] overlays = {BossEvent.BossBarOverlay.NOTCHED_6, BossEvent.BossBarOverlay.NOTCHED_10,
                BossEvent.BossBarOverlay.NOTCHED_12, BossEvent.BossBarOverlay.NOTCHED_20};
        for (int i = 0; i < counts.length; i++) {
            boolean all = true;
            for (float b : borders) {
                float n = b * counts[i];
                if (Math.abs(n - Math.round(n)) > 1.0e-3f) { all = false; break; }
            }
            if (all) return overlays[i];
        }
        return BossEvent.BossBarOverlay.PROGRESS;
    }

    /** Bar title: "[Miniboss: ]Name[ - Phase]". */
    public static Component title(Component mob, boolean miniboss, Component phaseName) {
        Component name = miniboss ? Component.translatable("simplemobs.bossbar.miniboss", mob) : mob;
        return phaseName == null ? name : Component.translatable("simplemobs.bossbar.title", name, phaseName);
    }

    /** Phase label, e.g. "Phase 2: Hunger"; the lang key is {@code simplemobs.bossbar.<mob>.<phase>}. */
    public static Component phaseName(String mob, int phase) {
        return Component.translatable("simplemobs.bossbar." + mob + "." + phase);
    }

    /** Sets name, color, overlay and progress (each only broadcasts on change). */
    public static void apply(ServerBossEvent bar, Component name, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay, float progress) {
        bar.setName(name);
        bar.setColor(color);
        bar.setOverlay(overlay);
        bar.setProgress(progress);
    }

    /** Shows the bar to every player within {@code range} blocks of the boss and hides it for the rest. */
    public static void track(ServerBossEvent bar, Entity boss, ServerLevel level, double range) {
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.distanceToSqr(boss) <= range * range) bar.addPlayer(p);
            else bar.removePlayer(p);
        }
    }
}
