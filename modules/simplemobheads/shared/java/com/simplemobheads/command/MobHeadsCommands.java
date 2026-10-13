package com.simplemobheads.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.config.MobHeadsConfig;
import java.util.function.BiConsumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * {@code /simplemobheads} (admins): the server switches of {@link MobHeadsConfig}. {@code /simplefun pvp headDrops} stays as
 * an alias since the PvP head moved here from Simple Fun (concept §3.3): it sets the same switch and names the new command.
 */
public final class MobHeadsCommands {
    private MobHeadsCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("simplemobheads")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("pvp").then(toggle("headDrops", (c, v) -> c.playerHeadDrops = v)))
                .then(Commands.literal("drops")
                        .then(toggle("creeper", (c, v) -> c.creeperDrops = v))
                        .then(toggle("wither", (c, v) -> c.witherHead = v))
                        .then(toggle("warden", (c, v) -> c.wardenHead = v))
                        .then(toggle("playerKill", (c, v) -> c.playerKillDrops = v))
                        .then(Commands.literal("playerKillChance").then(Commands.argument("value",
                                FloatArgumentType.floatArg(0.0F, MobHeadsConfig.MAX_PLAYER_KILL_CHANCE)).executes(ctx -> {
                                    float value = FloatArgumentType.getFloat(ctx, "value");
                                    return set(ctx.getSource(), "drops.playerKillChance", value, c -> c.playerKillChance = value);
                                })))
                        .then(Commands.literal("lootingBonus").then(Commands.argument("value",
                                FloatArgumentType.floatArg(0.0F, MobHeadsConfig.MAX_LOOTING_BONUS)).executes(ctx -> {
                                    float value = FloatArgumentType.getFloat(ctx, "value");
                                    return set(ctx.getSource(), "drops.lootingBonus", value, c -> c.playerKillLootingBonus = value);
                                }))))
                .then(toggle("greeting", (c, v) -> c.greeting = v))
                .then(toggle("enabled", (c, v) -> c.enabled = v))
                .then(Commands.literal("ability").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SimpleMobHeads.abilityIds(), builder))
                        .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            if (!SimpleMobHeads.abilityIds().contains(id)) {
                                ctx.getSource().sendFailure(Component.translatable("commands.simplemobheads.unknown_ability", id));
                                return 0;
                            }
                            boolean value = BoolArgumentType.getBool(ctx, "enabled");
                            return set(ctx.getSource(), "ability", id + " = " + value, c -> c.abilities.put(id, value));
                        })))));
        // Alias of the moved PvP switch (Simple Fun 1.3 and older): same switch, plus a hint to the new command.
        dispatcher.register(Commands.literal("simplefun")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("pvp").then(Commands.literal("headDrops").then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean value = BoolArgumentType.getBool(ctx, "enabled");
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.simplemobheads.moved", "/simplemobheads pvp headDrops"), false);
                            return set(ctx.getSource(), "pvp.headDrops", value, c -> c.playerHeadDrops = value);
                        })))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> toggle(String name, BiConsumer<MobHeadsConfig, Boolean> setter) {
        return Commands.literal(name).then(Commands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
            boolean value = BoolArgumentType.getBool(ctx, "enabled");
            return set(ctx.getSource(), name, value, c -> setter.accept(c, value));
        }));
    }

    private static int set(CommandSourceStack source, String key, Object value, java.util.function.Consumer<MobHeadsConfig> change) {
        change.accept(SimpleMobHeads.config());
        SimpleMobHeads.saveConfig();
        source.sendSuccess(() -> Component.translatable("commands.simplemobheads.set", key, String.valueOf(value)), true);
        return 1;
    }
}
