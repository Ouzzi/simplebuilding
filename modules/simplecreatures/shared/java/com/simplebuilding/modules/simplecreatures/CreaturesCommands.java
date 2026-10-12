package com.simplebuilding.modules.simplecreatures;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/** {@code /simplecreatures} (owners): suffix lists and piglin switches, formerly {@code /simplequalityoflife tweaks}. */
public final class CreaturesCommands {
    private CreaturesCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(SimpleCreatures.MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                .then(suffixes("muteSuffixes", c -> c.nametagMuteSuffixes))
                .then(suffixes("babySuffixes", c -> c.nametagBabySuffixes))
                .then(suffixes("mutedEntities", c -> c.mutedEntities))
                .then(toggle("piglinsIgnoreGoldTrims", (c, v) -> c.piglinsIgnoreGoldTrims = v))
                .then(toggle("piglinsIgnoreGoldTools", (c, v) -> c.piglinsIgnoreGoldTools = v)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> toggle(String name, BiConsumer<CreaturesConfig, Boolean> setter) {
        return Commands.literal(name).then(Commands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
            boolean on = BoolArgumentType.getBool(ctx, "enabled");
            setter.accept(SimpleCreatures.config(), on);
            SimpleCreatures.save();
            ctx.getSource().sendSuccess(() -> Component.translatable("command.simplecreatures.changed", name, on), true);
            return 1;
        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> suffixes(String name, Function<CreaturesConfig, List<String>> list) {
        return Commands.literal(name)
                .then(Commands.literal("list").executes(ctx -> {
                    var values = list.apply(SimpleCreatures.config());
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.simplecreatures.list", values).withStyle(ChatFormatting.YELLOW), false);
                    return 1;
                }))
                .then(Commands.literal("add").then(Commands.argument("suffix", StringArgumentType.string()).executes(ctx -> {
                    String value = StringArgumentType.getString(ctx, "suffix");
                    var values = list.apply(SimpleCreatures.config());
                    if (values.contains(value)) {
                        ctx.getSource().sendFailure(Component.translatable("command.simplecreatures.exists"));
                        return 0;
                    }
                    values.add(value);
                    SimpleCreatures.save();
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.simplecreatures.added", value).withStyle(ChatFormatting.GREEN), true);
                    return 1;
                })))
                .then(Commands.literal("remove").then(Commands.argument("suffix", StringArgumentType.string())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(list.apply(SimpleCreatures.config()), builder))
                        .executes(ctx -> {
                            String value = StringArgumentType.getString(ctx, "suffix");
                            if (list.apply(SimpleCreatures.config()).remove(value)) {
                                SimpleCreatures.save();
                                ctx.getSource().sendSuccess(() -> Component.translatable("command.simplecreatures.removed", value).withStyle(ChatFormatting.GREEN), true);
                                return 1;
                            }
                            ctx.getSource().sendFailure(Component.translatable("command.simplecreatures.missing"));
                            return 0;
                        })))
                .then(Commands.literal("clear").executes(ctx -> {
                    list.apply(SimpleCreatures.config()).clear();
                    SimpleCreatures.save();
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.simplecreatures.cleared").withStyle(ChatFormatting.RED), true);
                    return 1;
                }));
    }
}
