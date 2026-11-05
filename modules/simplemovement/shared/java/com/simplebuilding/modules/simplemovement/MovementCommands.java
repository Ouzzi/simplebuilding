package com.simplebuilding.modules.simplemovement;

import com.simplelib.api.CommandAliases;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.function.BiConsumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** {@code /simplemovement crawl} for players and {@code /simplemovement} (game masters, level 2), formerly in Simple QoL. */
public final class MovementCommands {
    private static final java.util.function.Predicate<CommandSourceStack> GM = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);

    private MovementCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandAliases.nested(dispatcher, SimpleMovement.MOD_ID, Commands.literal("crawl").executes(ctx -> {
            // Vanilla synchronized pose; the decision stays on the server. No chat feedback (key spam).
            if (ctx.getSource().getEntity() instanceof Player player && player instanceof CrawlAccessor crawler) {
                if (!CrawlLimiter.allow(player)) return 0;
                crawler.simpleQualityOfLife$setCrawling(!crawler.simpleQualityOfLife$isCrawling());
            }
            return 1;
        }));
        // The root is open for /simplemovement crawl (players); the game-master gate sits on each setting below.
        dispatcher.register(Commands.literal(SimpleMovement.MOD_ID)
                .then(toggle("autowalk", (c, v) -> c.enableAutowalk = v).requires(GM))
                .then(toggle("frostWalkerSnow", (c, v) -> c.frostWalkerWalkOnPowderSnow = v).requires(GM))
                .then(toggle("jumpBoostStep", (c, v) -> c.jumpBoostStepUp = v).requires(GM))
                .then(Commands.literal("ladderSpeed").requires(GM).then(Commands.argument("speed", DoubleArgumentType.doubleArg(0.2, 0.4)).executes(ctx -> {
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
