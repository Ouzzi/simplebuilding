package com.simplebuilding.modules.simplechallenges.core;

import com.simplebuilding.modules.simplechallenges.config.ChallengeServerConfig;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Credits from advancements (concept 3, variant "Erfolge"): every 40 ticks each player's finished advancements with a display
 * and a parent (no recipe or root advancements) pay credits by frame (task / goal / challenge, from the preset), once per
 * advancement for the whole team. A player's advancements that were already done when they first appear count as the
 * baseline and pay nothing, so switching a challenge on in a played world gives no windfall.
 */
public final class Credits {
    private Credits() {}

    /** The credits this player can spend: their own pot when the server gives every player one, else the team pot. */
    public static int of(Active a, ServerPlayer player) {
        return ChallengeServerConfig.perPlayerCredits && player != null ? a.state.playerCredits.getOrDefault(player.getStringUUID(), 0) : a.state.credits;
    }

    public static void add(Active a, ServerPlayer player, int amount) {
        if (ChallengeServerConfig.perPlayerCredits && player != null) {
            a.state.playerCredits.put(player.getStringUUID(), Math.max(0, of(a, player) + amount));
        } else {
            a.state.credits = Math.max(0, a.state.credits + amount);
        }
        a.state.setDirty();
    }

    public static void poll(Active a) {
        poll(a, a.server.getPlayerList().getPlayers());
    }

    public static void poll(Active a, java.util.Collection<ServerPlayer> players) {
        if (a.mode == null || !a.mode.earnsCredits()) return;
        for (ServerPlayer player : players) {
            boolean baseline = a.state.baselined.add(player.getStringUUID());
            int gained = 0;
            for (AdvancementHolder holder : a.server.getAdvancements().getAllAdvancements()) {
                DisplayInfo display = holder.value().display().orElse(null);
                if (display == null || holder.value().parent().isEmpty()) continue;
                // a team pays each advancement once; with a pot per player each player earns their own
                String id = (ChallengeServerConfig.perPlayerCredits ? player.getStringUUID() + "|" : "") + holder.id();
                if (a.state.rewarded.contains(id) || !player.getAdvancements().getOrStartProgress(holder).isDone()) continue;
                a.state.rewarded.add(id);
                if (!baseline) gained += a.mode.creditsFor(display.type().getSerializedName());
            }
            if (baseline) a.state.setDirty();
            if (gained > 0) {
                add(a, player, gained);
                player.sendSystemMessage(Component.translatable("simplechallenges.msg.credits_gained", gained, of(a, player)));
            }
        }
    }
}
