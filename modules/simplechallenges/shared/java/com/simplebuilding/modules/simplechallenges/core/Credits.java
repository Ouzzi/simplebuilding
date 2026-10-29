package com.simplebuilding.modules.simplechallenges.core;

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

    public static void poll(Active a) {
        poll(a, a.server.getPlayerList().getPlayers());
    }

    public static void poll(Active a, java.util.Collection<ServerPlayer> players) {
        if (!a.mode.earnsCredits()) return;
        for (ServerPlayer player : players) {
            boolean baseline = a.state.baselined.add(player.getStringUUID());
            int gained = 0;
            for (AdvancementHolder holder : a.server.getAdvancements().getAllAdvancements()) {
                DisplayInfo display = holder.value().display().orElse(null);
                if (display == null || holder.value().parent().isEmpty()) continue;
                String id = holder.id().toString();
                if (a.state.rewarded.contains(id) || !player.getAdvancements().getOrStartProgress(holder).isDone()) continue;
                a.state.rewarded.add(id);
                if (!baseline) gained += a.mode.creditsFor(display.type().getSerializedName());
            }
            if (baseline) a.state.setDirty();
            if (gained > 0) {
                a.state.credits += gained;
                a.state.setDirty();
                player.sendSystemMessage(Component.translatable("simplechallenges.msg.credits_gained", gained, a.state.credits));
            }
        }
    }
}
