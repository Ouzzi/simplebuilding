package com.simplebuilding.modules.simpletools;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.function.BiConsumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** {@code /simpletools} (owners): the switches formerly under {@code /simplequalityoflife tweaks}. */
public final class ToolsCommands {
    private ToolsCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(SimpleTools.MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                .then(toggle("fullDurabilityBonus", (c, v) -> c.enableFullDurabilityBonus = v))
                .then(toggle("anvilRepairKeepsCost", (c, v) -> c.anvilRepairKeepsCost = v))
                .then(toggle("furnaceLava", (c, v) -> c.enableFurnaceLavaFill = v))
                .then(toggle("soulbound", (c, v) -> c.enableSoulbound = v))
                .then(toggle("deathVault", (c, v) -> c.enableDeathVault = v))
                .then(toggle("deathVaultRedeem", (c, v) -> c.deathVaultRedeem = v)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> toggle(String name, BiConsumer<ToolsConfig, Boolean> setter) {
        return Commands.literal(name).then(Commands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
            boolean on = BoolArgumentType.getBool(ctx, "enabled");
            setter.accept(SimpleTools.config(), on);
            SimpleTools.save();
            ctx.getSource().sendSuccess(() -> Component.translatable("command.simpletools.changed", name, on), true);
            return 1;
        }));
    }
}
