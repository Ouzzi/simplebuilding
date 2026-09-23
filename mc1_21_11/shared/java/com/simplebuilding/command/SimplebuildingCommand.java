package com.simplebuilding.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.simplebuilding.config.SimplebuildingConfig;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Der Befehlsbaum von {@code /simplebuilding}, einmal pro Minecraft-Linie. Die Loader haengen ihn
 * nur noch ein ({@code ModCommands} je Loader: Fabric-Callback, NeoForge- und Forge-Event); bis
 * 2026-09 baute jeder der fuenf Loader-Staende den ganzen Baum selbst.
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
        dispatcher.register(Commands.literal("simplebuilding")
                .requires(OPERATOR_ONLY)
                .then(Commands.literal("config")
                        .then(Commands.literal("setTrimMultiplier")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, SimplebuildingConfig.maxMultiplierLimit))
                                        .executes(context -> {
                                            double newValue = DoubleArgumentType.getDouble(context, "value");
                                            SimplebuildingConfig.trimBenefitBaseMultiplier = newValue;
                                            context.getSource().sendSuccess(() -> Component.translatable("commands.simplebuilding.trim_multiplier.set", newValue).withStyle(ChatFormatting.GREEN), true);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("getTrimMultiplier")
                                .executes(context -> {
                                    context.getSource().sendSuccess(() -> Component.translatable("commands.simplebuilding.trim_multiplier.get", SimplebuildingConfig.trimBenefitBaseMultiplier).withStyle(ChatFormatting.YELLOW), false);
                                    return 1;
                                })
                        )
                )
        );
    }
}
