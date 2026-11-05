package com.simplepads.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.simplepads.PadsConfig;
import com.simplepads.SimplePads;
import com.simplepads.network.PadsNetwork;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /simplepads} (until 04.11. parts of {@code /simplebuilding tweaks}): {@code chunkloaders list|remove},
 * {@code teleporterspawn here} (target of every Spawn Teleporter), {@code elytracenter here|worldspawn|set x z}. Every option of
 * {@code simplepads-server.json} is set with SimpleLib's {@code /simplepads config}.
 */
public final class PadsCommands {
    /** Operators only; console and command blocks have no player and stay out (as SimpleBuilding's command). */
    public static final Predicate<CommandSourceStack> OPERATOR_ONLY = source -> {
        try {
            return source.getServer().getPlayerList().isOp(source.getPlayerOrException().nameAndId());
        } catch (Exception e) {
            return false;
        }
    };

    private PadsCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(SimplePads.MOD_ID)
                .requires(OPERATOR_ONLY)
                .then(ChunkLoaderCommand.node())
                .then(Commands.literal("teleporterspawn")
                        .then(Commands.literal("here").executes(PadsCommands::setTeleporterSpawnHere)))
                .then(Commands.literal("elytracenter")
                        .then(Commands.literal("worldspawn").executes(ctx -> change(ctx, "worldspawn", c -> c.spawn.useWorldSpawnAsCenter = true)))
                        .then(Commands.literal("here").executes(ctx -> {
                            BlockPos pos = ctx.getSource().getEntityOrException().blockPosition();
                            return setElytraCenter(ctx, pos.getX(), pos.getZ());
                        }))
                        .then(Commands.literal("set")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(ctx -> setElytraCenter(ctx, IntegerArgumentType.getInteger(ctx, "x"),
                                                        IntegerArgumentType.getInteger(ctx, "z"))))))));
    }

    private static int setElytraCenter(CommandContext<CommandSourceStack> ctx, int x, int z) {
        return change(ctx, x + " " + z, c -> {
            c.spawn.useWorldSpawnAsCenter = false;
            c.spawn.customSpawnElytraX = x;
            c.spawn.customSpawnElytraZ = z;
        });
    }

    private static int setTeleporterSpawnHere(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        BlockPos pos = player.blockPosition();
        return change(ctx, pos.toShortString(), c -> setTeleporterSpawn(c, pos));
    }

    /** Sets the target of every Spawn Teleporter. */
    public static void setTeleporterSpawn(PadsConfig config, BlockPos pos) {
        config.spawn.spawn1X = pos.getX();
        config.spawn.spawn1Y = pos.getY();
        config.spawn.spawn1Z = pos.getZ();
    }

    private static int change(CommandContext<CommandSourceStack> ctx, String value, java.util.function.Consumer<PadsConfig> edit) {
        edit.accept(SimplePads.localConfig());
        SimplePads.localConfig().validate();
        SimplePads.saveConfig();
        PadsNetwork.sendConfigToAll(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.simplepads.set", value).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}
