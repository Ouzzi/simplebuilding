package com.simplebuilding.dev.testcentre;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.ModItems;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;

/**
 * {@code /sbtestcentre} - baut die Testzentrale. Braucht Befehlsrechte (Stufe 2), damit auch die
 * Befehlsbloecke der Steuerwand ihn ausfuehren koennen.
 *
 * <ul>
 *   <li>{@code build} - am gespeicherten Ursprung (sonst ueber 0/0), {@code build here} an den Fuessen
 *       des Spielers, {@code build <x y z>} an einem festen Punkt.</li>
 *   <li>{@code section <id> [x y z]} - nur einen Abschnitt neu bauen.</li>
 *   <li>{@code kit [Spieler]} - die wichtigsten Werkzeuge (hoechste Stufe) ins Inventar.</li>
 *   <li>{@code give <station> [Spieler]} - das Kit einer Station ({@link TestCentreKits}): ersetzt das
 *       Inventar, Haupt-/Nebenhand fuer die Interaktionstests, dazu alles, was die Station zeigt. So ruft
 *       der Ausgabe-Knopf vorn an jeder Station.</li>
 *   <li>{@code tp} - zum Eingang; {@code coverage} - was noch keinen Abschnitt hat.</li>
 * </ul>
 * Ursprung und Fingerabdruck der Planung ({@link TestCentreLayout.Plan#fingerprint}) stehen in
 * {@code simplebuilding_testcentre.txt} im Weltordner ({@code x y z fingerabdruck}).
 */
public final class TestCentreCommand {

    /** Welt mit diesem Namen baut sich in einer Entwicklungsumgebung beim ersten Betreten selbst. */
    public static final String WORLD_NAME = "SB-Testzentrale";
    static final String ORIGIN_FILE = "simplebuilding_testcentre.txt";

    private TestCentreCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sbtestcentre")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("build")
                        .executes(ctx -> build(ctx.getSource(), savedOrDefaultOrigin(ctx.getSource().getLevel())))
                        .then(Commands.literal("here")
                                .executes(ctx -> build(ctx.getSource(), BlockPos.containing(ctx.getSource().getPosition()))))
                        .then(Commands.argument("origin", BlockPosArgument.blockPos())
                                .executes(ctx -> build(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "origin")))))
                .then(Commands.literal("section")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(TestCentreLayout.SECTION_IDS, builder))
                                .executes(ctx -> section(ctx, savedOrDefaultOrigin(ctx.getSource().getLevel())))
                                .then(Commands.argument("origin", BlockPosArgument.blockPos())
                                        .executes(ctx -> section(ctx, BlockPosArgument.getBlockPos(ctx, "origin"))))))
                .then(Commands.literal("kit")
                        .executes(ctx -> kit(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> kit(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                .then(Commands.literal("give")
                        .then(Commands.argument("station", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(TestCentreLayout.SECTION_IDS, builder))
                                .executes(ctx -> give(ctx, List.of(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> give(ctx, EntityArgument.getPlayers(ctx, "targets"))))))
                .then(Commands.literal("tp")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            BlockPos entrance = plan(ctx.getSource().getLevel(), savedOrDefaultOrigin(ctx.getSource().getLevel())).entrance();
                            player.teleportTo(entrance.getX() + 0.5, entrance.getY(), entrance.getZ() + 0.5);
                            return 1;
                        }))
                .then(Commands.literal("coverage")
                        .executes(ctx -> {
                            TestCentreLayout.Plan plan = plan(ctx.getSource().getLevel(), BlockPos.ZERO);
                            List<String> names = new ArrayList<>();
                            plan.leftovers().forEach(item -> names.add(TcContext.id(item).toString()));
                            ctx.getSource().sendSuccess(() -> names.isEmpty()
                                    ? TcText.t("command.coverage.ok", "Every mod item has a place in the test centre.").withStyle(ChatFormatting.GREEN)
                                    : TcText.t("command.coverage.missing", "Not in any section yet: %s", String.join(", ", names)).withStyle(ChatFormatting.RED), false);
                            return names.size();
                        })));
    }

    public static TestCentreLayout.Plan plan(ServerLevel level, BlockPos origin) {
        return TestCentreLayout.plan(level.registryAccess(), origin);
    }

    private static int build(CommandSourceStack source, BlockPos origin) {
        ServerLevel level = source.getLevel();
        TestCentreLayout.Plan plan = plan(level, origin);
        TestCentreBuilder.Result result = TestCentreBuilder.build(level, plan);
        saveOrigin(level.getServer(), origin, plan.fingerprint());
        source.sendSuccess(() -> TcText.t("command.built", "Test centre built at %s %s %s (%s steps, %s ms)",
                origin.getX(), origin.getY(), origin.getZ(), result.ops(), result.millis()).withStyle(ChatFormatting.GREEN), true);
        if (!plan.leftovers().isEmpty()) {
            source.sendSuccess(() -> TcText.t("command.unsorted", "%s mod items are not in a section yet (see 'Unsorted')",
                    plan.leftovers().size()).withStyle(ChatFormatting.GOLD), false);
        }
        return 1;
    }

    private static int section(CommandContext<CommandSourceStack> ctx, BlockPos origin) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "id");
        if (!TestCentreLayout.SECTION_IDS.contains(id)) {
            ctx.getSource().sendFailure(TcText.t("command.unknown_section", "Unknown section %s", id));
            return 0;
        }
        ServerLevel level = ctx.getSource().getLevel();
        TestCentreBuilder.Result result = TestCentreBuilder.buildSection(level, plan(level, origin), id);
        ctx.getSource().sendSuccess(() -> TcText.t("command.section_built", "Section %s rebuilt (%s ms)", id, result.millis())
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** Die wichtigsten Werkzeuge: je Familie die hoechste Stufe, dazu die Geraete und eine Blaupause. */
    static List<ItemStack> kitItems(TcContext ctx) {
        List<ItemStack> out = new ArrayList<>();
        for (String family : List.of("chisels", "building_wands", "sledgehammers", "pickaxes")) {
            List<Item> items = ctx.rowItems(family);
            if (!items.isEmpty()) {
                out.add(ctx.maxEnchanted(new ItemStack(items.getLast())));
            }
        }
        out.addAll(ctx.row("gadgets"));
        out.add(new ItemStack(ModItems.BLUEPRINT));
        out.add(new ItemStack(Items.STONE_BRICKS, 64));
        return out;
    }

    private static int give(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        String id = StringArgumentType.getString(ctx, "station");
        TestCentreKits.Kit kit = plan(ctx.getSource().getLevel(), BlockPos.ZERO).kits().get(id);
        if (kit == null) {
            ctx.getSource().sendFailure(TcText.t("command.give.none", "Station %s has nothing to give", id));
            return 0;
        }
        for (ServerPlayer player : players) {
            giveKit(player, kit);
        }
        ctx.getSource().sendSuccess(() -> TcText.t("command.give", "Items of %s given to %s player(s)", id, players.size()), true);
        if (kit.leftOut() > 0) {
            ctx.getSource().sendSuccess(() -> TcText.t("command.give.left_out", "%s more items did not fit - see the frames",
                    kit.leftOut()).withStyle(ChatFormatting.GOLD), false);
        }
        return players.size();
    }

    /** Ersetzt das Inventar durch das Kit: erst leeren, dann Haende, dann der Rest. */
    public static void giveKit(ServerPlayer player, TestCentreKits.Kit kit) {
        player.getInventory().clearContent();
        player.setItemSlot(EquipmentSlot.MAINHAND, kit.mainHand().copy());
        player.setItemSlot(EquipmentSlot.OFFHAND, kit.offHand().copy());
        for (ItemStack stack : kit.inventory()) {
            com.simplebuilding.version.McVersion.placeItemBackInInventory(player, stack.copy());
        }
        player.containerMenu.broadcastChanges();
    }

    private static int kit(CommandSourceStack source, Collection<ServerPlayer> players) {
        List<ItemStack> kit = kitItems(new TcContext(source.getLevel().registryAccess()));
        for (ServerPlayer player : players) {
            for (ItemStack stack : kit) {
                com.simplebuilding.version.McVersion.placeItemBackInInventory(player, stack.copy());
            }
        }
        source.sendSuccess(() -> TcText.t("command.kit", "Kit given to %s player(s)", players.size()), true);
        return players.size();
    }

    // ------------------------------------------------------------------ Ursprung merken

    static Path originFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(ORIGIN_FILE);
    }

    /** Der gespeicherte Fingerabdruck, oder leer (Datei fehlt oder stammt von vor 2026-09-28). */
    static String savedFingerprint(MinecraftServer server) {
        Path file = originFile(server);
        try {
            String[] parts = Files.readString(file, StandardCharsets.UTF_8).trim().split("\\s+");
            return parts.length > 3 ? parts[3] : "";
        } catch (IOException | RuntimeException e) {
            return "";
        }
    }

    static BlockPos savedOrDefaultOrigin(ServerLevel level) {
        Path file = originFile(level.getServer());
        if (Files.isRegularFile(file)) {
            try {
                String[] parts = Files.readString(file, StandardCharsets.UTF_8).trim().split("\\s+");
                return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            } catch (IOException | RuntimeException e) {
                Simplebuilding.LOGGER.warn("Unreadable test centre origin in {}, using the default", file, e);
            }
        }
        return new BlockPos(0, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0), 0);
    }

    static void saveOrigin(MinecraftServer server, BlockPos origin, String fingerprint) {
        try {
            Files.writeString(originFile(server), origin.getX() + " " + origin.getY() + " " + origin.getZ() + " " + fingerprint + "\n",
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            Simplebuilding.LOGGER.warn("Could not store the test centre origin", e);
        }
    }

    /** Wartezeit vor dem Neubau einer veralteten Zentrale: die Rahmen der alten muessen erst geladen sein. */
    static final int REBUILD_DELAY_TICKS = 60;

    /** Ein geplanter Neubau (Server, faellig ab Tick, Spieler fuer die Meldung). */
    private record PendingRebuild(MinecraftServer server, int dueTick, java.util.UUID player) {
    }

    private static PendingRebuild pending;

    /**
     * Beim Betreten: in einer Entwicklungsumgebung baut sich eine Welt namens {@link #WORLD_NAME} beim
     * ersten Mal selbst (erkennbar an der fehlenden Ursprungsdatei) und setzt den Spieler an den Eingang.
     *
     * <p>Seit 2026-09-28 auch, wenn die Welt aelter ist als der Code: weicht der gespeicherte
     * Fingerabdruck von der heutigen Planung ab (oder fehlt er), kommt der Spieler an den Eingang und die
     * Zentrale wird nach {@link #REBUILD_DELAY_TICKS} Ticks neu gebaut - so lange brauchen die Rahmen der
     * alten Zentrale, bis sie geladen sind und das Leeren sie sieht (sonst stuenden sie doppelt). Vorher
     * blieb eine einmal gebaute Zentrale fuer immer auf ihrem alten Stand, auch mit laengst behobenen
     * Fehlern wie den dicht gepackten Befehlsbloecken.
     */
    public static void onPlayerJoin(ServerPlayer player, boolean developmentEnvironment) {
        if (!developmentEnvironment) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null || !WORLD_NAME.equalsIgnoreCase(server.getWorldData().getLevelName())) {
            return;
        }
        ServerLevel level = server.overworld();
        boolean built = Files.exists(originFile(server));
        BlockPos origin = savedOrDefaultOrigin(level);
        TestCentreLayout.Plan plan = plan(level, origin);
        if (built && plan.fingerprint().equals(savedFingerprint(server))) {
            return;
        }
        if (built) {
            if (player.level() == level) {
                BlockPos entrance = plan.entrance();
                player.teleportTo(entrance.getX() + 0.5, entrance.getY(), entrance.getZ() + 0.5);
            }
            pending = new PendingRebuild(server, server.getTickCount() + REBUILD_DELAY_TICKS, player.getUUID());
            player.sendSystemMessage(TcText.t("command.outdated", "The test centre is older than the code - rebuilding in 3 s")
                    .withStyle(ChatFormatting.GOLD));
            return;
        }
        TestCentreBuilder.Result result = TestCentreBuilder.build(level, plan);
        saveOrigin(server, origin, plan.fingerprint());
        Simplebuilding.LOGGER.info("Built the test centre at {} ({} steps, {} ms)", origin, result.ops(), result.millis());
        if (player.level() == level) {
            BlockPos entrance = plan.entrance();
            player.teleportTo(entrance.getX() + 0.5, entrance.getY(), entrance.getZ() + 0.5);
        }
        player.sendSystemMessage(TcText.t("command.autobuilt", "Test centre built - /sbtestcentre for more")
                .withStyle(ChatFormatting.GREEN));
    }

    /** Jeden Server-Tick (ueber TweaksContent#onServerTick auf allen Loadern): faelligen Neubau ausfuehren. */
    public static void serverTick(MinecraftServer server) {
        PendingRebuild job = pending;
        if (job == null) {
            return;
        }
        if (job.server() != server) {
            pending = null;
            return;
        }
        if (server.getTickCount() < job.dueTick()) {
            return;
        }
        pending = null;
        ServerLevel level = server.overworld();
        BlockPos origin = savedOrDefaultOrigin(level);
        TestCentreLayout.Plan plan = plan(level, origin);
        TestCentreBuilder.Result result = TestCentreBuilder.build(level, plan);
        saveOrigin(server, origin, plan.fingerprint());
        Simplebuilding.LOGGER.info("Rebuilt the outdated test centre at {} ({} steps, {} ms)", origin, result.ops(), result.millis());
        ServerPlayer player = server.getPlayerList().getPlayer(job.player());
        if (player != null) {
            player.sendSystemMessage(TcText.t("command.rebuilt", "Test centre rebuilt to match the code")
                    .withStyle(ChatFormatting.GREEN));
        }
    }
}
