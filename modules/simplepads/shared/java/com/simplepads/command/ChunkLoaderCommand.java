package com.simplepads.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simplepads.block.entity.ChunkLoaderBlockEntity;
import com.simplepads.block.entity.ChunkLoaderRegistry;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * {@code /simplebuilding chunkloaders list} und {@code ... remove <dimension> <pos>} (Operatoren,
 * Besitzer-Entscheidung 2026-09-28): alle Chunk-Loader des Servers mit Position, Dimension, Besitzer,
 * ob der Besitzer online ist, Bereich und ob der Loader gerade Chunks haelt - aus der
 * {@link ChunkLoaderRegistry}, ohne einen Chunk zu laden. {@code remove} baut den Loader ab (das Item
 * faellt an seiner Stelle) oder streicht einen Eintrag, an dessen Stelle kein Loader mehr steht.
 */
public final class ChunkLoaderCommand {

    private ChunkLoaderCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("chunkloaders")
                .requires(com.simplepads.command.PadsCommands.OPERATOR_ONLY)
                .then(Commands.literal("list").executes(ChunkLoaderCommand::list))
                .then(Commands.literal("remove")
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ChunkLoaderCommand::remove))));
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        List<ChunkLoaderRegistry.Entry> entries = ChunkLoaderRegistry.all(server);
        if (entries.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.simplepads.chunkloaders.none")
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        for (ChunkLoaderRegistry.Entry entry : entries) {
            Component line = describe(server, entry);
            context.getSource().sendSuccess(() -> line, false);
        }
        int count = entries.size();
        context.getSource().sendSuccess(() -> Component.translatable("commands.simplepads.chunkloaders.count", count)
                .withStyle(ChatFormatting.YELLOW), false);
        return count;
    }

    /** Eine Zeile: Position, Dimension, Besitzer (online/offline), Bereich, Zustand. */
    public static Component describe(MinecraftServer server, ChunkLoaderRegistry.Entry entry) {
        BlockPos pos = entry.pos();
        String owner;
        boolean online;
        if (entry.owner().isPresent()) {
            UUID id = entry.owner().get();
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            online = player != null;
            owner = online ? player.getName().getString() : id.toString();
        } else {
            online = true;
            owner = "-";
        }
        return Component.translatable("commands.simplepads.chunkloaders.entry",
                        pos.getX() + " " + pos.getY() + " " + pos.getZ(),
                        entry.dimension(),
                        owner,
                        Component.translatable(online ? "commands.simplepads.chunkloaders.online"
                                : "commands.simplepads.chunkloaders.offline"),
                        Component.translatable("commands.simplepads.chunkloaders.area." + Math.max(1, Math.min(3, entry.tier()))),
                        Component.translatable(entry.active() ? "commands.simplepads.chunkloaders.active"
                                : "commands.simplepads.chunkloaders.inactive"))
                .withStyle(entry.active() ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel level = DimensionArgument.getDimension(context, "dimension");
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        ChunkLoaderRegistry.Entry entry = ChunkLoaderRegistry.find(context.getSource().getServer(), level.dimension(), pos);
        // Laedt den Chunk, falls noetig - der Abbau muss die eigenen Tickets freigeben koennen.
        level.getChunk(pos);
        boolean loader = level.getBlockEntity(pos) instanceof ChunkLoaderBlockEntity;
        if (entry == null && !loader) {
            context.getSource().sendFailure(Component.translatable("commands.simplepads.chunkloaders.not_found",
                    pos.getX() + " " + pos.getY() + " " + pos.getZ(), level.dimension().identifier().toString()));
            return 0;
        }
        if (loader) {
            // destroyBlock: Item faellt, preRemoveSideEffects gibt die Chunks frei und streicht den Eintrag.
            level.destroyBlock(pos, true);
            if (level.getBlockEntity(pos) instanceof ChunkLoaderBlockEntity) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        ChunkLoaderRegistry.remove(level, pos);
        context.getSource().sendSuccess(() -> Component.translatable("commands.simplepads.chunkloaders.removed",
                pos.getX() + " " + pos.getY() + " " + pos.getZ(), level.dimension().identifier().toString())
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}
