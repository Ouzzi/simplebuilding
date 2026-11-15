package com.simplevisuals;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
/** The server-owned options are anvil formatting and the obfuscation ({@code &k}) lock (W3B). Cosmetic choices are local client settings. */
public final class VisualsCommands {
 public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
  dispatcher.register(Commands.literal("simplevisuals")
   .then(Commands.literal("config").requires(source->source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
    .then(Commands.literal("visuals")
     .then(policy("enableAnvilFormatting","simplevisuals.server_formatting",Visuals::serverFormatting,value->{Visuals.setServerFormatting(value);Visuals.CONFIG.visuals.enableAnvilFormatting=value;}))
     .then(policy("allowAnvilObfuscation","simplevisuals.server_obfuscation",Visuals::serverObfuscation,value->{Visuals.setServerObfuscation(value);Visuals.CONFIG.visuals.allowAnvilObfuscation=value;})))));
 }
 private static LiteralArgumentBuilder<CommandSourceStack> policy(String name,String key,java.util.function.BooleanSupplier get,java.util.function.Consumer<Boolean> set){
  return Commands.literal(name)
   .executes(ctx->{boolean value=get.getAsBoolean();ctx.getSource().sendSuccess(()->Component.translatable(key,value),false);return 1;})
   .then(Commands.argument("enabled",BoolArgumentType.bool()).executes(ctx->{boolean value=BoolArgumentType.getBool(ctx,"enabled");set.accept(value);Visuals.save();ctx.getSource().sendSuccess(()->Component.translatable(key,value),true);return 1;}));
 }
}
