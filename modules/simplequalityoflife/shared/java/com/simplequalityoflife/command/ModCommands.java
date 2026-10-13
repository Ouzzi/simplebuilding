package com.simplequalityoflife.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.simplequalityoflife.Simplequalityoflife;
import com.simplequalityoflife.config.SimplequalityoflifeConfig;
import java.util.function.BiConsumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * {@code /simplequalityoflife submod <name> <true|false>} (owners): the super-mod switches. The former tweak commands
 * moved with their features into the sub-mods ({@code /simpleloot}, {@code /simplemobmanners}, {@code /simplefarming},
 * {@code /simpletools}, {@code /simplemovement}, {@code /simpleinterfaces}; {@code /crawl} belongs to Simple Movement).
 */
public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var submod = Commands.literal("submod");
        submod.then(toggle("weather", (c, v) -> c.enableSimpleWeather = v));
        submod.then(toggle("interfaces", (c, v) -> c.enableSimpleInterfaces = v));
        submod.then(toggle("loot", (c, v) -> c.enableSimpleLoot = v));
        submod.then(toggle("mobmanners", (c, v) -> c.enableSimpleMobManners = v));
        // Name before the rename (Simple Creatures), kept so old scripts keep working.
        submod.then(toggle("creatures", (c, v) -> c.enableSimpleMobManners = v));
        submod.then(toggle("farming", (c, v) -> c.enableSimpleFarming = v));
        submod.then(toggle("tools", (c, v) -> c.enableSimpleTools = v));
        submod.then(toggle("movement", (c, v) -> c.enableSimpleMovement = v));
        dispatcher.register(Commands.literal("simplequalityoflife")
                .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                .then(submod));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> toggle(String name,
            BiConsumer<SimplequalityoflifeConfig, Boolean> setter) {
        return Commands.literal(name)
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean val = BoolArgumentType.getBool(ctx, "enabled");
                            setter.accept(Simplequalityoflife.getConfig(), val);
                            Simplequalityoflife.save();
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.simplequalityoflife.changed", "submod " + name, val), true);
                            return 1;
                        }));
    }
}
