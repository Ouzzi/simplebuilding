package com.simplefun.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.simplefun.SimplefunCommon;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Shared command tree (vanilla brigadier - loader-agnostic). Each loader registers it via its own
 * command-registration event.
 */
public class SimplefunCommands {

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("simplefun")
            // Level 4 (admins) for config changes.
            .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
            .then(
                Commands.literal("pvp")
                    .then(
                        Commands.literal("headDrops")
                            .then(
                                Commands.argument("enabled", BoolArgumentType.bool())
                                    .executes(
                                        ctx -> {
                                          boolean val = BoolArgumentType.getBool(ctx, "enabled");
                                          SimplefunCommon.getConfig().fun.playerHeadDrops = val;
                                          SimplefunCommon.saveConfig();
                                          ctx.getSource()
                                              .sendSuccess(
                                                  () ->
                                                      Component.translatable(
                                                          "commands.simplefun.pvp.headDrops", val),
                                                  true);
                                          return 1;
                                        }))))
            .then(
                Commands.literal("tweaks")
                    .then(
                        Commands.literal("yeet")
                            .then(
                                Commands.literal("toggle")
                                    .then(
                                        Commands.argument("enabled", BoolArgumentType.bool())
                                            .executes(
                                                ctx -> {
                                                  boolean val =
                                                      BoolArgumentType.getBool(ctx, "enabled");
                                                  SimplefunCommon.getConfig().fun.enableYeet = val;
                                                  SimplefunCommon.saveConfig();
                                                  ctx.getSource()
                                                      .sendSuccess(
                                                          () ->
                                                              Component.translatable(
                                                                  "commands.simplefun.yeet.enabled",
                                                                  val),
                                                          true);
                                                  return 1;
                                                })))
                            .then(
                                Commands.literal("strength")
                                    .then(
                                        Commands.argument(
                                                "value",
                                                FloatArgumentType.floatArg(
                                                    0.1f,
                                                    com.simplefun.config.SimplefunConfig
                                                        .MAX_YEET_STRENGTH))
                                            .executes(
                                                ctx -> {
                                                  float val =
                                                      FloatArgumentType.getFloat(ctx, "value");
                                                  SimplefunCommon.getConfig().fun.yeetStrength =
                                                      val;
                                                  SimplefunCommon.saveConfig();
                                                  ctx.getSource()
                                                      .sendSuccess(
                                                          () ->
                                                              Component.translatable(
                                                                  "commands.simplefun.yeet.strength",
                                                                  val),
                                                          true);
                                                  return 1;
                                                }))))
                    .then(
                        Commands.literal("bricks")
                            .then(
                                Commands.literal("enable")
                                    .then(
                                        Commands.argument("enabled", BoolArgumentType.bool())
                                            .executes(
                                                ctx -> {
                                                  boolean val =
                                                      BoolArgumentType.getBool(ctx, "enabled");
                                                  SimplefunCommon.getConfig()
                                                          .fun
                                                          .enableThrowableBricks =
                                                      val;
                                                  SimplefunCommon.saveConfig();
                                                  ctx.getSource()
                                                      .sendSuccess(
                                                          () ->
                                                              Component.translatable(
                                                                  "commands.simplefun.bricks.enabled",
                                                                  val),
                                                          true);
                                                  return 1;
                                                })))
                            .then(
                                Commands.literal("breakGlass")
                                    .then(
                                        Commands.argument("enabled", BoolArgumentType.bool())
                                            .executes(
                                                ctx -> {
                                                  boolean val =
                                                      BoolArgumentType.getBool(ctx, "enabled");
                                                  SimplefunCommon.getConfig()
                                                          .fun
                                                          .throwableBricksBreakBlocks =
                                                      val;
                                                  SimplefunCommon.saveConfig();
                                                  ctx.getSource()
                                                      .sendSuccess(
                                                          () ->
                                                              Component.translatable(
                                                                  "commands.simplefun.bricks.breakGlass",
                                                                  val),
                                                          true);
                                                  return 1;
                                                })))
                            .then(
                                Commands.literal("damage")
                                    .then(
                                        Commands.argument(
                                                "value",
                                                FloatArgumentType.floatArg(
                                                    0.0f,
                                                    com.simplefun.config.SimplefunConfig
                                                        .MAX_DAMAGE))
                                            .executes(
                                                ctx -> {
                                                  float val =
                                                      FloatArgumentType.getFloat(ctx, "value");
                                                  SimplefunCommon.getConfig().fun.brickDamage = val;
                                                  SimplefunCommon.saveConfig();
                                                  ctx.getSource()
                                                      .sendSuccess(
                                                          () ->
                                                              Component.translatable(
                                                                  "commands.simplefun.bricks.damage",
                                                                  val),
                                                          true);
                                                  return 1;
                                                })))
                            .then(
                                Commands.literal("snowballDamage")
                                    .then(
                                        Commands.argument(
                                                "value",
                                                FloatArgumentType.floatArg(
                                                    0.0f,
                                                    com.simplefun.config.SimplefunConfig
                                                        .MAX_DAMAGE))
                                            .executes(
                                                ctx -> {
                                                  float val =
                                                      FloatArgumentType.getFloat(ctx, "value");
                                                  SimplefunCommon.getConfig()
                                                          .fun
                                                          .brickSnowballDamage =
                                                      val;
                                                  SimplefunCommon.saveConfig();
                                                  ctx.getSource()
                                                      .sendSuccess(
                                                          () ->
                                                              Component.translatable(
                                                                  "commands.simplefun.bricks.snowballDamage",
                                                                  val),
                                                          true);
                                                  return 1;
                                                }))))));
  }
}
