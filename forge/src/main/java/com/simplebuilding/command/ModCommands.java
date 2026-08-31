package com.simplebuilding.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Simplebuilding.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
