package com.simplebuilding.modules.simpleloot;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** {@code /simpleloot} (game masters, level 2): the vault settings, formerly {@code /simplequalityoflife vaults}. */
public final class LootCommands {
    private LootCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(SimpleLoot.MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("vaults")
                        .then(Commands.literal("cooldown")
                                .then(Commands.argument("days", IntegerArgumentType.integer(1, 36500))
                                        .executes(ctx -> {
                                            int days = IntegerArgumentType.getInteger(ctx, "days");
                                            SimpleLoot.config().vaultCooldownDays = days;
                                            SimpleLoot.save();
                                            ctx.getSource().sendSuccess(() -> Component.translatable("command.simpleloot.days", days), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("repeat")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean on = BoolArgumentType.getBool(ctx, "enabled");
                                            SimpleLoot.config().enableVaultCooldown = on;
                                            SimpleLoot.save();
                                            ctx.getSource().sendSuccess(() -> Component.translatable("command.simpleloot.changed", "enableVaultCooldown", on), true);
                                            return 1;
                                        })))));
    }
}
