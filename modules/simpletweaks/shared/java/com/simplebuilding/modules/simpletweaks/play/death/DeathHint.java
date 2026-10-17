package com.simplebuilding.modules.simpletweaks.play.death;

import com.simplebuilding.modules.simpletweaks.play.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * T5: after a real death (end of {@code ServerPlayer.die}, the player is dead) only the dead player gets a line with
 * place (copy on click), dimension, cause and how long the dropped items stay (5 minutes while the area is loaded).
 * With Simple Respawn a downed player is not dead yet, so the hint comes with the real death.
 */
public final class DeathHint {
    /** Item lifetime of Vanilla {@code ItemEntity} (6000 ticks). */
    public static final int ITEM_SECONDS = 300;

    /** The last hint sent (tests). */
    public static Component last;

    public static boolean active() { return Play.on() && Play.server() != null && Play.config().deathHint().enabled(); }

    public static void died(ServerPlayer p) {
        if (!active() || !p.isDeadOrDying()) return;
        last = hint(p);
        p.sendSystemMessage(last);
    }

    public static Component hint(ServerPlayer p) {
        var pos = p.blockPosition();
        var out = PlayText.t("death.where", PlayText.coords(pos.getX(), pos.getY(), pos.getZ()), PlayText.dimension(p.level().dimension())).withStyle(ChatFormatting.GOLD);
        out.append(Component.literal("\n")).append(PlayText.t("death.cause", p.getCombatTracker().getDeathMessage()).withStyle(ChatFormatting.GRAY));
        boolean kept = p.level().getGameRules().get(GameRules.KEEP_INVENTORY);
        out.append(Component.literal("\n")).append((kept ? PlayText.t("death.kept") : PlayText.t("death.items", ITEM_SECONDS / 60)).withStyle(ChatFormatting.GRAY));
        return out;
    }
    private DeathHint() {}
}
