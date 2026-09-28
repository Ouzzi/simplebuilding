package com.simplebuilding.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.ConfigOptions;
import com.simplebuilding.config.ConfigSaving;
import com.simplebuilding.config.SimplebuildingConfig;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Der Befehlsbaum von {@code /simplebuilding}, einmal pro Minecraft-Linie. Die Loader haengen ihn
 * nur noch ein ({@code ModCommands} je Loader: Fabric-Callback, NeoForge- und Forge-Event); bis
 * 2026-09 baute jeder der fuenf Loader-Staende den ganzen Baum selbst.
 *
 * <p><b>{@code /simplebuilding config}</b> (Config-Umbau 2026-09-28): {@code list [filter]},
 * {@code get <option>}, {@code set <option> <wert>} und {@code reset <option>} fuer JEDE Option der
 * Config, mit ihrem Pfad wie in {@code simplebuilding.json} ({@code tweaks.pads.enableFlypads}).
 * Die Liste kommt aus {@link ConfigOptions}, also aus den Feldern selbst - ein neues Feld ist sofort
 * per Befehl erreichbar. {@code set}/{@code reset} begrenzen wie beim Laden
 * ({@code validatePostLoad}), speichern die Datei und schicken den Clients, was sie vom Server
 * brauchen ({@link #afterConfigChange}). {@code setTrimMultiplier}/{@code getTrimMultiplier} bleiben.
 */
public final class SimplebuildingCommand {

    /** Nur Operatoren; die Konsole und Befehlsbloecke haben keinen Spieler und bleiben draussen. */
    public static final Predicate<CommandSourceStack> OPERATOR_ONLY = source -> {
        try {
            return source.getServer().getPlayerList().isOp(source.getPlayerOrException().nameAndId());
        } catch (Exception e) {
            return false;
        }
    };

    private SimplebuildingCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Eigene Wurzel /sbtestcentre (Befehlsrechte statt nur Operator-Spieler, damit Befehlsbloecke ihn nutzen).
        com.simplebuilding.dev.testcentre.TestCentreCommand.register(dispatcher);
        dispatcher.register(Commands.literal("simplebuilding")
                .requires(OPERATOR_ONLY)
                .then(Commands.literal("config")
                        .then(Commands.literal("setTrimMultiplier")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, SimplebuildingConfig.maxMultiplierLimit))
                                        .executes(context -> {
                                            double newValue = DoubleArgumentType.getDouble(context, "value");
                                            com.simplebuilding.Simplebuilding.getConfig().trimBenefitBaseMultiplier = newValue;
                                            // Gespeichert und sofort an alle Clients (TrimStatsPanel).
                                            com.simplebuilding.config.ConfigSaving.save();
                                            for (net.minecraft.server.level.ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                                                if (player instanceof com.simplebuilding.util.SurvivalTracerAccessor accessor) {
                                                    accessor.simplebuilding$syncTrimData();
                                                }
                                            }
                                            context.getSource().sendSuccess(() -> Component.translatable("commands.simplebuilding.trim_multiplier.set", newValue).withStyle(ChatFormatting.GREEN), true);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("getTrimMultiplier")
                                .executes(context -> {
                                    context.getSource().sendSuccess(() -> Component.translatable("commands.simplebuilding.trim_multiplier.get", com.simplebuilding.Simplebuilding.getConfig().trimBenefitBaseMultiplier).withStyle(ChatFormatting.YELLOW), false);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("list")
                                .executes(context -> list(context, ""))
                                .then(Commands.argument("filter", StringArgumentType.word())
                                        .suggests(GROUPS)
                                        .executes(context -> list(context, StringArgumentType.getString(context, "filter")))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("option", StringArgumentType.word())
                                        .suggests(OPTIONS)
                                        .executes(context -> get(context, StringArgumentType.getString(context, "option")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("option", StringArgumentType.word())
                                        .suggests(OPTIONS)
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .suggests(VALUES)
                                                .executes(context -> set(context, StringArgumentType.getString(context, "option"),
                                                        StringArgumentType.getString(context, "value"))))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("option", StringArgumentType.word())
                                        .suggests(OPTIONS)
                                        .executes(context -> reset(context, StringArgumentType.getString(context, "option")))))
                )
        );
    }

    private static final SuggestionProvider<CommandSourceStack> OPTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(ConfigOptions.all().stream().map(ConfigOptions.Option::path), builder);

    /** Group prefixes for {@code list}: {@code root}, {@code tools}, {@code tweaks.pads}, ... */
    private static final SuggestionProvider<CommandSourceStack> GROUPS = (context, builder) ->
            SharedSuggestionProvider.suggest(ConfigOptions.all().stream()
                    .map(o -> o.path().contains(".") ? o.path().substring(0, o.path().lastIndexOf('.')) : "root")
                    .distinct(), builder);

    /** true/false for switches, the default for numbers. */
    private static final SuggestionProvider<CommandSourceStack> VALUES = (context, builder) -> {
        ConfigOptions.Option option = ConfigOptions.byPath(StringArgumentType.getString(context, "option"));
        if (option == null) {
            return builder.buildFuture();
        }
        if (option.type() == boolean.class) {
            return SharedSuggestionProvider.suggest(List.of("true", "false"), builder);
        }
        return SharedSuggestionProvider.suggest(List.of(String.valueOf(option.defaultValue())), builder);
    };

    /** {@code filter}: empty = all, {@code root} = the options outside any group, else a path prefix. */
    public static int list(CommandContext<CommandSourceStack> context, String filter) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        int count = 0;
        for (ConfigOptions.Option option : ConfigOptions.all()) {
            boolean match = "root".equals(filter) ? !option.path().contains(".") : option.path().startsWith(filter);
            if (!match) {
                continue;
            }
            Object value = option.get(config);
            boolean changed = !String.valueOf(value).equals(String.valueOf(option.defaultValue()));
            context.getSource().sendSuccess(() -> Component.literal(option.path() + " = " + value)
                    .withStyle(changed ? ChatFormatting.GOLD : ChatFormatting.GRAY), false);
            count++;
        }
        if (count == 0) {
            context.getSource().sendFailure(Component.translatable("commands.simplebuilding.config.unknown", filter));
            return 0;
        }
        int total = count;
        context.getSource().sendSuccess(() -> Component.translatable("commands.simplebuilding.config.list", total)
                .withStyle(ChatFormatting.YELLOW), false);
        return count;
    }

    private static int get(CommandContext<CommandSourceStack> context, String path) {
        ConfigOptions.Option option = ConfigOptions.byPath(path);
        if (option == null) {
            context.getSource().sendFailure(Component.translatable("commands.simplebuilding.config.unknown", path));
            return 0;
        }
        Object value = option.get(Simplebuilding.getConfig());
        context.getSource().sendSuccess(() -> Component.translatable("commands.simplebuilding.config.get",
                path, String.valueOf(value), String.valueOf(option.defaultValue()), option.typeName())
                .withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> context, String path, String text) {
        ConfigOptions.Option option = ConfigOptions.byPath(path);
        if (option == null) {
            context.getSource().sendFailure(Component.translatable("commands.simplebuilding.config.unknown", path));
            return 0;
        }
        Object value = option.parse(text);
        if (value == null) {
            context.getSource().sendFailure(Component.translatable("commands.simplebuilding.config.invalid",
                    text, option.typeName(), path));
            return 0;
        }
        return write(context, option, value, "commands.simplebuilding.config.set");
    }

    private static int reset(CommandContext<CommandSourceStack> context, String path) {
        ConfigOptions.Option option = ConfigOptions.byPath(path);
        if (option == null) {
            context.getSource().sendFailure(Component.translatable("commands.simplebuilding.config.unknown", path));
            return 0;
        }
        return write(context, option, option.defaultValue(), "commands.simplebuilding.config.reset");
    }

    private static int write(CommandContext<CommandSourceStack> context, ConfigOptions.Option option, Object value, String key) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        if (!option.set(config, value)) {
            context.getSource().sendFailure(Component.translatable("commands.simplebuilding.config.unknown", option.path()));
            return 0;
        }
        // Dieselben Grenzen wie beim Laden einer handeditierten Datei.
        config.validatePostLoad();
        ConfigSaving.save();
        afterConfigChange(context.getSource().getServer(), option.path());
        Object stored = option.get(config);
        context.getSource().sendSuccess(() -> Component.translatable(key, option.path(), String.valueOf(stored))
                .withStyle(ChatFormatting.GREEN), true);
        if (option.clientSide() && context.getSource().getServer().isDedicatedServer()) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.simplebuilding.config.client_side")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        if (option.appliesOnReload()) {
            context.getSource().sendSuccess(() -> Component.translatable("commands.simplebuilding.config.on_reload")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    /**
     * Nach einer Aenderung per Befehl: allen Spielern schicken, was ihre Clients vom Server brauchen
     * (Tweaks-Werte samt Luftsprung-Abklingzeit, Kolben-Regeln, Besatz-Multiplikator), und nach einer
     * Weltspawn-Option den eigenen Weltspawn sofort setzen (wie {@code tweaks worldspawn set}).
     */
    public static void afterConfigChange(MinecraftServer server, String path) {
        boolean worldSpawn = path.equals("tweaks.spawn.useCustomWorldSpawn") || path.equals("tweaks.spawn.xCoordSpawnPoint")
                || path.equals("tweaks.spawn.yCoordSpawnPoint") || path.equals("tweaks.spawn.zCoordSpawnPoint");
        com.simplebuilding.tweaks.command.TweaksCommands.afterChange(server, worldSpawn);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            com.simplebuilding.networking.PistonConfigPayload.sendTo(player);
            if (player instanceof com.simplebuilding.util.SurvivalTracerAccessor accessor) {
                accessor.simplebuilding$syncTrimData();
            }
        }
    }
}
