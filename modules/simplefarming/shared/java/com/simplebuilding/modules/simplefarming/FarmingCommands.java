package com.simplebuilding.modules.simplefarming;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.function.BiConsumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** {@code /simplefarming} (game masters, level 2): the switches formerly under {@code /simplequalityoflife tweaks}. */
public final class FarmingCommands {
    private FarmingCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(SimpleFarming.MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(toggle("farmlandProtect", (c, v) -> c.preventFarmlandTrampleWithFeatherFalling = v))
                .then(toggle("hoeHarvest", (c, v) -> c.enableHoeHarvest = v))
                .then(toggle("sharpnessCut", (c, v) -> c.sharpnessCutsGrass = v)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> toggle(String name, BiConsumer<FarmingConfig, Boolean> setter) {
        return Commands.literal(name).then(Commands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
            boolean on = BoolArgumentType.getBool(ctx, "enabled");
            setter.accept(SimpleFarming.config(), on);
            SimpleFarming.save();
            ctx.getSource().sendSuccess(() -> Component.translatable("command.simplefarming.changed", name, on), true);
            return 1;
        }));
    }
}
