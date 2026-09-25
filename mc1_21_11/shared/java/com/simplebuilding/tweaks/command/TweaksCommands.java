package com.simplebuilding.tweaks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.simplebuilding.command.SimplebuildingCommand;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksConfig;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.phys.AABB;

/**
 * Befehle aus Simple Tweaks: {@code /killboats}, {@code /killcarts} und der Config-Baum, der dort
 * {@code /simpletweaks ...} hiess und hier unter {@code /simplebuilding tweaks ...} haengt (gleicher
 * Unterbaum). Nur fuer Operatoren, wie der Rest von {@code /simplebuilding}. Die Claim-Befehle
 * bleiben in Simple Tweaks (docs/SIMPLETWEAKS-UEBERNAHME.md).
 */
public final class TweaksCommands {
    public static final List<String> MODES = List.of("standard", "empty", "all");
    public static final double KILL_RADIUS = 100.0;

    private TweaksCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("killboats")
                .requires(SimplebuildingCommand.OPERATOR_ONLY)
                .executes(ctx -> executeKill(ctx, "standard", true))
                .then(Commands.argument("mode", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(MODES, b))
                        .executes(ctx -> executeKill(ctx, StringArgumentType.getString(ctx, "mode"), true))));
        dispatcher.register(Commands.literal("killcarts")
                .requires(SimplebuildingCommand.OPERATOR_ONLY)
                .executes(ctx -> executeKill(ctx, "standard", false))
                .then(Commands.argument("mode", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(MODES, b))
                        .executes(ctx -> executeKill(ctx, StringArgumentType.getString(ctx, "mode"), false))));

        dispatcher.register(Commands.literal("simplebuilding")
                .requires(SimplebuildingCommand.OPERATOR_ONLY)
                .then(Commands.literal("tweaks")
                        .then(Commands.literal("balancing")
                                .then(intSetting("rocketStackSize", 1, 64, (c, v) -> c.balancing.rocketStackSize = v)))
                        .then(Commands.literal("dimension")
                                .then(boolSetting("nether", (c, v) -> c.dimensions.allowNether = v))
                                .then(boolSetting("end", (c, v) -> c.dimensions.allowEnd = v)))
                        .then(Commands.literal("spawn")
                                .then(intSetting("teleporterCount", 0, 64, (c, v) -> c.spawn.spawnTeleporterCount = v))
                                .then(Commands.literal("elytra")
                                        .then(boolSetting("toggle", (c, v) -> c.spawn.giveElytraOnSpawn = v))
                                        .then(intSetting("radius", 1, Integer.MAX_VALUE, (c, v) -> c.spawn.spawnElytraRadius = v))
                                        .then(intSetting("flightTime", 1, Integer.MAX_VALUE, (c, v) -> c.spawn.flightTimeSeconds = v))
                                        .then(intSetting("maxBoosts", 1, Integer.MAX_VALUE, (c, v) -> c.spawn.maxBoosts = v))
                                        .then(Commands.literal("boostStrength")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.1f))
                                                        .executes(ctx -> apply(ctx, "boostStrength", FloatArgumentType.getFloat(ctx, "value"),
                                                                c -> c.spawn.boostStrength = FloatArgumentType.getFloat(ctx, "value")))))
                                        .then(Commands.literal("center")
                                                .then(Commands.literal("worldspawn").executes(ctx ->
                                                        apply(ctx, "elytraCenter", "worldspawn", c -> c.spawn.useWorldSpawnAsCenter = true)))
                                                .then(Commands.literal("set")
                                                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                                        .executes(ctx -> setElytraCenter(ctx,
                                                                                IntegerArgumentType.getInteger(ctx, "x"),
                                                                                IntegerArgumentType.getInteger(ctx, "z"))))))
                                                .then(Commands.literal("here").executes(ctx -> {
                                                    BlockPos pos = ctx.getSource().getEntityOrException().blockPosition();
                                                    return setElytraCenter(ctx, pos.getX(), pos.getZ());
                                                })))))
                        .then(Commands.literal("worldspawn")
                                .then(Commands.literal("setspawn1").executes(ctx -> setTeleporterSpawn(ctx, 1)))
                                .then(Commands.literal("setspawn2").executes(ctx -> setTeleporterSpawn(ctx, 2)))
                                .then(Commands.literal("setspawn3").executes(ctx -> setTeleporterSpawn(ctx, 3)))
                                .then(Commands.literal("setspawn4").executes(ctx -> setTeleporterSpawn(ctx, 4)))
                                .then(boolSetting("forceExact", (c, v) -> c.spawn.forceExactSpawn = v))
                                .then(boolSetting("custom", (c, v) -> c.spawn.useCustomWorldSpawn = v))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                                .then(Commands.argument("y", IntegerArgumentType.integer(-1, 320))
                                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                                .executes(ctx -> setWorldSpawn(ctx,
                                                                        IntegerArgumentType.getInteger(ctx, "x"),
                                                                        IntegerArgumentType.getInteger(ctx, "y"),
                                                                        IntegerArgumentType.getInteger(ctx, "z")))))))
                                .then(Commands.literal("here").executes(ctx -> {
                                    BlockPos pos = ctx.getSource().getEntityOrException().blockPosition();
                                    return setWorldSpawn(ctx, pos.getX(), pos.getY(), pos.getZ());
                                })))
                        .then(Commands.literal("commands")
                                .then(boolSetting("enableKillBoats", (c, v) -> c.commands.enableKillBoatsCommand = v))
                                .then(boolSetting("enableKillCarts", (c, v) -> c.commands.enableKillCartsCommand = v)))
                        .then(Commands.literal("pads")
                                .then(boolSetting("chunkLoaders", (c, v) -> c.pads.enableChunkLoaders = v))
                                .then(boolSetting("elytraPads", (c, v) -> c.pads.enableElytraPads = v))
                                .then(boolSetting("flypads", (c, v) -> c.pads.enableFlypads = v))
                                .then(boolSetting("spawnTeleporters", (c, v) -> c.pads.enableSpawnTeleporters = v))
                                .then(boolSetting("launchpads", (c, v) -> c.pads.enableLaunchpads = v))
                                .then(boolSetting("timedCopperPlates", (c, v) -> c.pads.enableTimedCopperPlates = v))
                                .then(boolSetting("filterPlates", (c, v) -> c.pads.enableFilterPlates = v)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> boolSetting(String name, BiConsumer<TweaksConfig, Boolean> setter) {
        return Commands.literal(name).then(Commands.argument("enabled", BoolArgumentType.bool())
                .executes(ctx -> {
                    boolean value = BoolArgumentType.getBool(ctx, "enabled");
                    return apply(ctx, name, value, c -> setter.accept(c, value));
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> intSetting(String name, int min, int max, BiConsumer<TweaksConfig, Integer> setter) {
        return Commands.literal(name).then(Commands.argument("value", IntegerArgumentType.integer(min, max))
                .executes(ctx -> {
                    int value = IntegerArgumentType.getInteger(ctx, "value");
                    return apply(ctx, name, value, c -> setter.accept(c, value));
                }));
    }

    private static int apply(CommandContext<CommandSourceStack> ctx, String name, Object value, Consumer<TweaksConfig> change) {
        change.accept(SimpleTweaks.config());
        SimpleTweaks.saveConfig();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.simplebuilding.tweaks.set", name, String.valueOf(value)), true);
        return 1;
    }

    private static int setElytraCenter(CommandContext<CommandSourceStack> ctx, int x, int z) {
        return apply(ctx, "elytraCenter", x + " " + z, c -> {
            c.spawn.useWorldSpawnAsCenter = false;
            c.spawn.customSpawnElytraX = x;
            c.spawn.customSpawnElytraZ = z;
        });
    }

    private static int setWorldSpawn(CommandContext<CommandSourceStack> ctx, int x, int y, int z) {
        return apply(ctx, "worldSpawn", x + " " + y + " " + z, c -> {
            c.spawn.useCustomWorldSpawn = true;
            c.spawn.xCoordSpawnPoint = x;
            c.spawn.yCoordSpawnPoint = y;
            c.spawn.zCoordSpawnPoint = z;
        });
    }

    private static int setTeleporterSpawn(CommandContext<CommandSourceStack> ctx, int tier) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        BlockPos pos = player.blockPosition();
        setTeleporterSpawn(tier, pos);
        SimpleTweaks.saveConfig();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.simplebuilding.tweaks.teleporter_spawn", tier, pos.toShortString())
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    public static void setTeleporterSpawn(int tier, BlockPos pos) {
        TweaksConfig.Spawn spawn = SimpleTweaks.config().spawn;
        switch (tier) {
            case 2 -> { spawn.spawn2X = pos.getX(); spawn.spawn2Y = pos.getY(); spawn.spawn2Z = pos.getZ(); }
            case 3 -> { spawn.spawn3X = pos.getX(); spawn.spawn3Y = pos.getY(); spawn.spawn3Z = pos.getZ(); }
            case 4 -> { spawn.spawn4X = pos.getX(); spawn.spawn4Y = pos.getY(); spawn.spawn4Z = pos.getZ(); }
            default -> { spawn.spawn1X = pos.getX(); spawn.spawn1Y = pos.getY(); spawn.spawn1Z = pos.getZ(); }
        }
    }

    private static int executeKill(CommandContext<CommandSourceStack> ctx, String mode, boolean boats) {
        TweaksConfig.Commands config = SimpleTweaks.config().commands;
        if (boats ? !config.enableKillBoatsCommand : !config.enableKillCartsCommand) {
            ctx.getSource().sendFailure(Component.translatable("commands.simplebuilding.tweaks.disabled"));
            return 0;
        }
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.translatable("commands.simplebuilding.tweaks.players_only"));
            return 0;
        }
        if (!MODES.contains(mode)) {
            ctx.getSource().sendFailure(Component.translatable("commands.simplebuilding.tweaks.invalid_mode", mode));
            return 0;
        }
        AABB box = player.getBoundingBox().inflate(KILL_RADIUS);
        int count = boats ? killBoats(player.level(), box, mode) : killCarts(player.level(), box, mode);
        String key = boats ? "commands.simplebuilding.killboats.success" : "commands.simplebuilding.killcarts.success";
        ctx.getSource().sendSuccess(() -> Component.translatable(key, count, mode), true);
        return count;
    }

    /** standard = nur Boote ohne Kiste, empty = dazu leere Kistenboote, all = alle unbesetzten Boote. */
    public static int killBoats(ServerLevel level, AABB box, String mode) {
        int count = 0;
        for (AbstractBoat boat : level.getEntitiesOfClass(AbstractBoat.class, box, b -> !b.isVehicle())) {
            boolean storage = boat instanceof AbstractChestBoat;
            boolean remove = switch (mode) {
                case "empty" -> !storage || ((Container) boat).isEmpty();
                case "all" -> true;
                default -> !storage;
            };
            if (remove) {
                boat.discard();
                count++;
            }
        }
        return count;
    }

    /** standard = nur normale Loren, empty = dazu leere Lagerloren, all = alle unbesetzten Loren. */
    public static int killCarts(ServerLevel level, AABB box, String mode) {
        int count = 0;
        for (AbstractMinecart cart : level.getEntitiesOfClass(AbstractMinecart.class, box, c -> !c.isVehicle())) {
            boolean standard = cart instanceof Minecart;
            boolean storage = cart instanceof AbstractMinecartContainer;
            boolean remove = switch (mode) {
                case "empty" -> standard || (storage && ((AbstractMinecartContainer) cart).isEmpty());
                case "all" -> true;
                default -> standard;
            };
            if (remove) {
                cart.discard();
                count++;
            }
        }
        return count;
    }
}
