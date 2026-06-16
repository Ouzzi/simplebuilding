package com.simplebuilding.command;

import com.simplebuilding.config.SimplebuildingConfig;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = com.simplebuilding.Simplebuilding.MOD_ID)
public final class ModCommands {

    private ModCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("simplebuilding")
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
                                            context.getSource().sendSuccess(() -> Component.literal("Trim Multiplier gesetzt auf: " + newValue), true);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("getTrimMultiplier")
                                .executes(context -> {
                                    context.getSource().sendSuccess(() -> Component.literal("Aktueller Trim Multiplier: " + SimplebuildingConfig.trimBenefitBaseMultiplier), false);
                                    return 1;
                                })
                        )
                )
        );
    }
}
