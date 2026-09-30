package com.simplevisuals;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
/** The sole server-owned option is formatting. Cosmetic choices are local client settings. */
public final class VisualsCommands {
 public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
  dispatcher.register(Commands.literal("simplevisuals")
   .then(Commands.literal("config").requires(source->source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN))
    .then(Commands.literal("visuals").then(Commands.literal("enableAnvilFormatting")
     .executes(ctx->{ctx.getSource().sendSuccess(()->Component.translatable("simplevisuals.server_formatting",Visuals.serverFormatting()),false);return 1;})
     .then(Commands.argument("enabled",BoolArgumentType.bool()).executes(ctx->{boolean value=BoolArgumentType.getBool(ctx,"enabled");Visuals.setServerFormatting(value);Visuals.CONFIG.visuals.enableAnvilFormatting=value;Visuals.save();return 1;}))))));
 }
}
