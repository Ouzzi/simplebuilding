package com.simplebuilding.modules.simplestats;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.simplebuilding.modules.simplestats.data.StatData;
import com.simplebuilding.modules.simplestats.data.StatFormat;
import com.simplebuilding.modules.simplestats.data.StatKeys;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** {@code /simplestats reset} (the only way to clear statistics), {@code /simplestats show} (raw values) and {@code /simplestats reload}; game masters. */
public final class StatsCommands {
    private StatsCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(SimpleStats.MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("reset").executes(StatsCommands::reset))
                .then(Commands.literal("show").executes(StatsCommands::show))
                .then(Commands.literal("reload").executes(ctx -> {
                    SimpleStats.load();
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.simplestats.reloaded"), true);
                    return 1;
                })));
    }

    private static ItemStack held(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return ctx.getSource().getPlayerOrException().getItemBySlot(EquipmentSlot.MAINHAND);
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ItemStack stack = held(ctx);
        if (stack.isEmpty() || !stack.has(StatsComponents.ITEM_STATS)) {
            ctx.getSource().sendFailure(Component.translatable("command.simplestats.none"));
            return 0;
        }
        StatsRuntime.discard(stack);
        stack.remove(StatsComponents.ITEM_STATS);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.simplestats.reset", stack.getHoverName()), true);
        return 1;
    }

    private static int show(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ItemStack stack = held(ctx);
        StatsRuntime.flush();
        StatData data = StatsComponents.of(stack);
        if (stack.isEmpty() || data.isBlank()) {
            ctx.getSource().sendFailure(Component.translatable("command.simplestats.none"));
            return 0;
        }
        Locale locale = Locale.US;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.simplestats.header", stack.getHoverName(), data.version(), data.bytes()), false);
        for (String key : StatKeys.TOTAL_ORDER) {
            if (data.total(key) > 0) {
                ctx.getSource().sendSuccess(() -> Component.translatable("command.simplestats.line", key, StatFormat.value(StatKeys.unit(key), data.total(key), locale, false)), false);
            }
        }
        for (var list : data.details().entrySet()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.simplestats.list", list.getKey(), list.getValue().entries().size(), list.getValue().other()), false);
        }
        return 1;
    }
}
