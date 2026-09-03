package com.simplebuilding.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.simplebuilding.config.SimplebuildingConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class ModCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("simplebuilding")
                    .requires(source -> {
                        try {
                            return source.getServer().getPlayerList().isOp(source.getPlayerOrException().nameAndId());
                        } catch (Exception e) {
                            return false;
                        }
                    })
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
        });
    }

}