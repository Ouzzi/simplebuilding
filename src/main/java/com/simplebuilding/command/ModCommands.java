package com.simplebuilding.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.simplebuilding.config.SimplebuildingConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

public class ModCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("simplebuilding")
                    .requires(source -> {
                        try {
                            return source.getServer().getPlayerManager().isOperator(source.getPlayerOrThrow().getPlayerConfigEntry());
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .then(CommandManager.literal("config")
                            .then(CommandManager.literal("setTrimMultiplier")
                                    .then(CommandManager.argument("value", DoubleArgumentType.doubleArg(0.0, SimplebuildingConfig.maxMultiplierLimit))
                                            .executes(context -> {
                                                double newValue = DoubleArgumentType.getDouble(context, "value");
                                                SimplebuildingConfig.trimBenefitBaseMultiplier = newValue;
                                                context.getSource().sendFeedback(() -> Text.literal("§aTrim Multiplier gesetzt auf: " + newValue), true);
                                                return 1;
                                            })
                                    )
                            )
                            .then(CommandManager.literal("getTrimMultiplier")
                                    .executes(context -> {
                                        context.getSource().sendFeedback(() -> Text.literal("§eAktueller Trim Multiplier: " + SimplebuildingConfig.trimBenefitBaseMultiplier), false);
                                        return 1;
                                    })
                            )
                    )
            );
        });
    }

}