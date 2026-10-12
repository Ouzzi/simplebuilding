package com.simplebuilding.modules.simplerespawn;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /simplerespawn giveup} (the "Give up" button of the downed screen, every player, only while downed) and the
 * game master commands {@code down} / {@code revive} for tests and the test centre.
 */
public final class RespawnCommands {
    private RespawnCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(SimpleRespawn.ID)
                .then(Commands.literal("giveup").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!RespawnServer.isDowned(player)) {
                        ctx.getSource().sendFailure(Component.translatable("commands.simplerespawn.not_downed"));
                        return 0;
                    }
                    RespawnServer.die(player);
                    return 1;
                }))
                .then(Commands.literal("down").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("targets", EntityArgument.players()).executes(ctx -> down(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                .then(Commands.literal("revive").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("targets", EntityArgument.players()).executes(ctx -> revive(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"))))));
    }

    private static int down(CommandSourceStack source, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        int n = 0;
        for (ServerPlayer player : targets) {
            if (RespawnServer.isDowned(player) || !player.isAlive() || player.isCreative() || player.isSpectator()) continue;
            RespawnServer.knockDown(player, player.damageSources().generic());
            n++;
        }
        int count = n;
        source.sendSuccess(() -> Component.translatable("commands.simplerespawn.down", count), true);
        return n;
    }

    private static int revive(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int n = 0;
        for (ServerPlayer player : targets) {
            if (!RespawnServer.isDowned(player)) continue;
            RespawnServer.revive(player, null);
            n++;
        }
        int count = n;
        source.sendSuccess(() -> Component.translatable("commands.simplerespawn.revive", count), true);
        return n;
    }
}
