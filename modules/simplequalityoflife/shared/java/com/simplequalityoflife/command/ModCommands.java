package com.simplequalityoflife.command;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.simplequalityoflife.Simplequalityoflife;
import com.simplequalityoflife.config.SimplequalityoflifeConfig;
import com.simplequalityoflife.util.CrawlAccessor;
import com.mojang.brigadier.CommandDispatcher;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

            dispatcher.register(Commands.literal("crawl")
                    .executes(ctx -> {
                        // Both loaders use Vanilla synchronized pose; gameplay decisions stay on the server.
                        if (ctx.getSource().getEntity() instanceof Player player && player instanceof CrawlAccessor crawler) {
                            if (!com.simplequalityoflife.util.CrawlLimiter.allow(player)) return 0;
                            boolean newState = !crawler.simpleQualityOfLife$isCrawling();
                            crawler.simpleQualityOfLife$setCrawling(newState);
                            // Kein Feedback im Chat, um Spam beim Drücken der Taste zu vermeiden
                        }
                        return 1;
                    }));

            // --- CONFIG COMMANDS (simplequalityoflife) ---
            dispatcher.register(Commands.literal("simplequalityoflife")
                    // Level 4 (OWNERS) für Admin-Befehle (Config Änderungen).
                    .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))

                    // --- Vaults ---
                    .then(Commands.literal("vaults")
                            .then(Commands.literal("cooldown")
                                    .then(Commands.argument("days", IntegerArgumentType.integer(1, 36500))
                                            .executes(ctx -> {
                                                int val = IntegerArgumentType.getInteger(ctx, "days");
                                                Simplequalityoflife.getConfig().qOL.vaultCooldownDays = val;
                                                saveConfig();
                                                ctx.getSource().sendSuccess(() -> Component.translatable("command.simplequalityoflife.days", val), true);
                                                return 1;
                                            }))))

                    // --- Tweaks (Alle Features) ---
                    .then(Commands.literal("tweaks")
                            .then(boolTweak("fullDurabilityBonus", (c, v) -> { c.qOL.enableFullDurabilityBonus = v; }, "Full Durability Bonus enabled"))
                            .then(boolTweak("autowalk", (c, v) -> { c.qOL.enableAutowalk = v; }, "Auto-Walk enabled"))
                            .then(boolTweak("farmlandProtect", (c, v) -> { c.qOL.preventFarmlandTrampleWithFeatherFalling = v; }, "Farmland Feather Falling Protection"))
                            .then(boolTweak("frostWalkerSnow", (c, v) -> { c.frostWalkerWalkOnPowderSnow = v; }, "Frost Walker on Powder Snow"))
                            .then(boolTweak("hoeHarvest", (c, v) -> { c.qOL.enableHoeHarvest = v; }, "Hoe Harvest & Replant"))
                            .then(boolTweak("furnaceLava", (c, v) -> { c.qOL.enableFurnaceLavaFill = v; }, "Furnace Lava Fill"))
                            .then(boolTweak("sharpnessCut", (c, v) -> { c.qOL.sharpnessCutsGrass = v; }, "Sharpness Cuts Grass"))
                            .then(Commands.literal("ladderSpeed")
                                    .then(Commands.argument("speed", DoubleArgumentType.doubleArg(0.2, 0.4))
                                            .executes(ctx -> {
                                                double val = DoubleArgumentType.getDouble(ctx, "speed");
                                                Simplequalityoflife.getConfig().qOL.ladderClimbingSpeed = val;
                                                saveConfig();
                                                ctx.getSource().sendSuccess(() -> Component.translatable("command.simplequalityoflife.speed", val), true);
                                                return 1;
                                            })))
                            .then(suffixCommands("muteSuffixes",
                                    () -> Simplequalityoflife.getConfig().qOL.nametagMuteSuffixes,
                                    "mute suffix", "Mute Suffixes"))
                            .then(suffixCommands("babySuffixes",
                                    () -> Simplequalityoflife.getConfig().qOL.nametagBabySuffixes,
                                    "baby suffix", "Baby Suffixes"))
                    )
            );
    }

    // Baut einen einfachen An/Aus-Schalter für ein Boolean-Feature.
    private static LiteralArgumentBuilder<CommandSourceStack> boolTweak(String name,
                                                                       BiConsumer<SimplequalityoflifeConfig, Boolean> setter,
                                                                       String label) {
        return Commands.literal(name)
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean val = BoolArgumentType.getBool(ctx, "enabled");
                            setter.accept(Simplequalityoflife.getConfig(), val);
                            saveConfig();
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.simplequalityoflife.changed", name, val), true);
                            return 1;
                        }));
    }

    // Baut den list/add/remove/clear-Unterbaum für eine Suffix-Liste.
    private static LiteralArgumentBuilder<CommandSourceStack> suffixCommands(String name,
                                                                            Supplier<List<String>> listSupplier,
                                                                            String labelSingular,
                                                                            String labelPlural) {
        return Commands.literal(name)
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            List<String> list = listSupplier.get();
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.simplequalityoflife.list", list).withStyle(ChatFormatting.YELLOW), false);
                            return 1;
                        }))
                .then(Commands.literal("add")
                        .then(Commands.argument("suffix", StringArgumentType.string())
                                .executes(ctx -> {
                                    String suffix = StringArgumentType.getString(ctx, "suffix");
                                    List<String> list = listSupplier.get();
                                    if (list.contains(suffix)) {
                                        ctx.getSource().sendFailure(Component.translatable("command.simplequalityoflife.exists"));
                                        return 0;
                                    }
                                    list.add(suffix);
                                    saveConfig();
                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.simplequalityoflife.added", suffix).withStyle(ChatFormatting.GREEN), true);
                                    return 1;
                                })))
                .then(Commands.literal("remove")
                        .then(Commands.argument("suffix", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(listSupplier.get(), builder))
                                .executes(ctx -> {
                                    String suffix = StringArgumentType.getString(ctx, "suffix");
                                    if (listSupplier.get().remove(suffix)) {
                                        saveConfig();
                                        ctx.getSource().sendSuccess(() -> Component.translatable("command.simplequalityoflife.removed", suffix).withStyle(ChatFormatting.GREEN), true);
                                        return 1;
                                    }
                                    ctx.getSource().sendFailure(Component.translatable("command.simplequalityoflife.missing"));
                                    return 0;
                                })))
                .then(Commands.literal("clear")
                        .executes(ctx -> {
                            listSupplier.get().clear();
                            saveConfig();
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.simplequalityoflife.cleared").withStyle(ChatFormatting.RED), true);
                            return 1;
                        }));
    }

    private static void saveConfig() {
        Simplequalityoflife.save();
    }
}
