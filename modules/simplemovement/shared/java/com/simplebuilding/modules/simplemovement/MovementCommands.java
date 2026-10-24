package com.simplebuilding.modules.simplemovement;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.function.BiConsumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** {@code /crawl} for players and {@code /simplemovement} (game masters, level 2), formerly in Simple QoL. */
public final class MovementCommands {
    private MovementCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("crawl").executes(ctx -> {
            // Vanilla synchronized pose; the decision stays on the server. No chat feedback (key spam).
            if (ctx.getSource().getEntity() instanceof Player player && player instanceof CrawlAccessor crawler) {
                if (!CrawlLimiter.allow(player)) return 0;
                crawler.simpleQualityOfLife$setCrawling(!crawler.simpleQualityOfLife$isCrawling());
            }
            return 1;
        }));
        dispatcher.register(Commands.literal(SimpleMovement.MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(toggle("autowalk", (c, v) -> c.enableAutowalk = v))
                .then(toggle("frostWalkerSnow", (c, v) -> c.frostWalkerWalkOnPowderSnow = v))
                .then(toggle("jumpBoostStep", (c, v) -> c.jumpBoostStepUp = v))
                .then(Commands.literal("ladderSpeed").then(Commands.argument("speed", DoubleArgumentType.doubleArg(0.2, 0.4)).executes(ctx -> {
                    double speed = DoubleArgumentType.getDouble(ctx, "speed");
                    SimpleMovement.config().ladderClimbingSpeed = speed;
                    SimpleMovement.save();
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.simplemovement.speed", speed), true);
                    return 1;
                }))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> toggle(String name, BiConsumer<MovementConfig, Boolean> setter) {
        return Commands.literal(name).then(Commands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
            boolean on = BoolArgumentType.getBool(ctx, "enabled");
            setter.accept(SimpleMovement.config(), on);
            SimpleMovement.save();
            ctx.getSource().sendSuccess(() -> Component.translatable("command.simplemovement.changed", name, on), true);
            return 1;
        }));
    }
}
