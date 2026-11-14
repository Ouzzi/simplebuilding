package com.simplebuilding.modules.simplemobmanners;

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

/**
 * {@code /simplemobmanners} (game masters, level 2): suffix lists and piglin switches, formerly {@code /simplequalityoflife tweaks};
 * {@code /simplecreatures} (name before the rename) stays as an alias with a hint.
 */
public final class MobMannersCommands {
    private MobMannersCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(tree(SimpleMobManners.MOD_ID, false));
        // The name before the rename (Simple Creatures) stays as an alias; every answer starts with a hint to the new name.
        dispatcher.register(tree(SimpleMobManners.LEGACY_ID, true).executes(ctx -> {
            hint(ctx.getSource(), true);
            return 1;
        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tree(String name, boolean legacy) {
        var root = Commands.literal(name)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(suffixes("muteSuffixes", c -> c.nametagMuteSuffixes, legacy));
        // simplemobmanners-2: on 26.3+ Vanilla's golden dandelion replaces baby suffixes, so the command is only offered on the older ports.
        if (SimpleMobManners.BABY_NAMETAG_LEGACY) root.then(suffixes("babySuffixes", c -> c.nametagBabySuffixes, legacy));
        return root
                .then(suffixes("mutedEntities", c -> c.mutedEntities, legacy))
                .then(toggle("piglinsIgnoreGoldTrims", (c, v) -> c.piglinsIgnoreGoldTrims = v, legacy))
                .then(toggle("piglinsIgnoreGoldTools", (c, v) -> c.piglinsIgnoreGoldTools = v, legacy));
    }

    private static void hint(CommandSourceStack source, boolean legacy) {
        if (legacy) source.sendSystemMessage(Component.translatable("command.simplemobmanners.renamed", SimpleMobManners.LEGACY_ID, SimpleMobManners.MOD_ID).withStyle(ChatFormatting.GOLD));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> toggle(String name, BiConsumer<MobMannersConfig, Boolean> setter, boolean legacy) {
        return Commands.literal(name).then(Commands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
            hint(ctx.getSource(), legacy);
            boolean on = BoolArgumentType.getBool(ctx, "enabled");
            setter.accept(SimpleMobManners.config(), on);
            SimpleMobManners.save();
            ctx.getSource().sendSuccess(() -> Component.translatable("command.simplemobmanners.changed", name, on), true);
            return 1;
        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> suffixes(String name, Function<MobMannersConfig, List<String>> list, boolean legacy) {
        return Commands.literal(name)
                .then(Commands.literal("list").executes(ctx -> {
            hint(ctx.getSource(), legacy);
                    var values = list.apply(SimpleMobManners.config());
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.simplemobmanners.list", values).withStyle(ChatFormatting.YELLOW), false);
                    return 1;
                }))
                .then(Commands.literal("add").then(Commands.argument("suffix", StringArgumentType.string()).executes(ctx -> {
            hint(ctx.getSource(), legacy);
                    String value = StringArgumentType.getString(ctx, "suffix");
                    var values = list.apply(SimpleMobManners.config());
                    if (values.contains(value)) {
                        ctx.getSource().sendFailure(Component.translatable("command.simplemobmanners.exists"));
                        return 0;
                    }
                    // simplemobmanners-2: the config drops blank values, values over 64 characters and anything past 64 entries; say so instead of "Added".
                    if (value.isBlank() || value.length() > MobMannersConfig.MAX_LENGTH || values.size() >= MobMannersConfig.MAX_ENTRIES) {
                        ctx.getSource().sendFailure(Component.translatable("command.simplemobmanners.rejected", MobMannersConfig.MAX_LENGTH, MobMannersConfig.MAX_ENTRIES));
                        return 0;
                    }
                    values.add(value);
                    SimpleMobManners.save();
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.simplemobmanners.added", value).withStyle(ChatFormatting.GREEN), true);
                    return 1;
                })))
                .then(Commands.literal("remove").then(Commands.argument("suffix", StringArgumentType.string())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(list.apply(SimpleMobManners.config()), builder))
                        .executes(ctx -> {
            hint(ctx.getSource(), legacy);
                            String value = StringArgumentType.getString(ctx, "suffix");
                            if (list.apply(SimpleMobManners.config()).remove(value)) {
                                SimpleMobManners.save();
                                ctx.getSource().sendSuccess(() -> Component.translatable("command.simplemobmanners.removed", value).withStyle(ChatFormatting.GREEN), true);
                                return 1;
                            }
                            ctx.getSource().sendFailure(Component.translatable("command.simplemobmanners.missing"));
                            return 0;
                        })))
                .then(Commands.literal("clear").executes(ctx -> {
            hint(ctx.getSource(), legacy);
                    list.apply(SimpleMobManners.config()).clear();
                    SimpleMobManners.save();
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.simplemobmanners.cleared").withStyle(ChatFormatting.RED), true);
                    return 1;
                }));
    }
}
