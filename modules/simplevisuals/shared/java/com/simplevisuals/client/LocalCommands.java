package com.simplevisuals.client;
import com.simplevisuals.*;import com.mojang.brigadier.CommandDispatcher;import com.mojang.brigadier.builder.*;import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.*;import net.minecraft.network.chat.Component;
public final class LocalCommands {
 public static <S> void register(CommandDispatcher<S> dispatcher,java.util.function.BiConsumer<S,Component> feedback){
  var root=LiteralArgumentBuilder.<S>literal("simplevisuals");
  for(var option:ConfigOptions.all(Visuals.CONFIG)){
   if(option.path().endsWith("enableAnvilFormatting"))continue;
   add(root,"config."+option.path(),option,feedback);
   String alias=switch(option.path()){
    case "visuals.enablePlayerLocator"->"locator";case "visuals.enableStatusEffectBars"->"statusBars";case "visuals.enableChatHeads"->"chatHeads";case "visuals.enableElytraPitchHelper"->"elytraHelper.enable";
    case "visuals.elytraPitchTolerance"->"elytraHelper.tolerance";case "visuals.elytraSensitivity"->"elytraHelper.sensitivity";
    default->option.path().startsWith("visuals.speedLines.")?"visuals.speedLines."+switch(option.field().getName()){case "enableSpeedLines"->"enable";case "speedLinesColor"->"color";case "speedLinesAlpha"->"alpha";case "speedLinesAmount"->"amount";case "speedThreshold"->"threshold";default->option.field().getName();}:option.path();
   };add(root,alias,option,feedback);
   if(option.path().startsWith("visuals.pickupNotifier.")){
    String pickup=switch(option.field().getName()){
     case "enablePickupNotifier"->"enable";case "pickupNotifierScale"->"scale";case "pickupNotifierDuration"->"duration";
     case "pickupNotifierShowXp"->"showXp";case "pickupVanillaStyle"->"vanillaStyle";case "pickupBackgroundOpacity"->"opacity";
     case "pickupNotifierSide"->"side";case "pickupNotifierLayout"->"layout";case "pickupShowItem"->"elements.item";
     case "pickupShowName"->"elements.name";case "pickupShowCount"->"elements.count";default->null;};
    if(pickup!=null)add(root,"visuals.pickupNotifier."+pickup,option,feedback);
   }
  }
  root.then(LiteralArgumentBuilder.<S>literal("elytraHelper").then(LiteralArgumentBuilder.<S>literal("angles")
   .then(RequiredArgumentBuilder.<S,Float>argument("up",com.mojang.brigadier.arguments.FloatArgumentType.floatArg())
    .then(RequiredArgumentBuilder.<S,Float>argument("down",com.mojang.brigadier.arguments.FloatArgumentType.floatArg()).executes(ctx->{
     Visuals.CONFIG.visuals.elytraTargetAngleUp=com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx,"up");
     Visuals.CONFIG.visuals.elytraTargetAngleDown=com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx,"down");Visuals.save();return 1;})))));
  root.then(LiteralArgumentBuilder.<S>literal("visuals").then(LiteralArgumentBuilder.<S>literal("pickupNotifier").then(LiteralArgumentBuilder.<S>literal("offset")
   .then(RequiredArgumentBuilder.<S,Integer>argument("x",com.mojang.brigadier.arguments.IntegerArgumentType.integer())
    .then(RequiredArgumentBuilder.<S,Integer>argument("y",com.mojang.brigadier.arguments.IntegerArgumentType.integer()).executes(ctx->{
     Visuals.CONFIG.visuals.pickupNotifier.pickupNotifierOffsetX=com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx,"x");
     Visuals.CONFIG.visuals.pickupNotifier.pickupNotifierOffsetY=com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx,"y");Visuals.save();return 1;}))))));
  dispatcher.register(root);
 }
 private static <S> void add(LiteralArgumentBuilder<S> root,String path,ConfigOptions.Option option,java.util.function.BiConsumer<S,Component> feedback){
  var node=LiteralArgumentBuilder.<S>literal(path.substring(path.lastIndexOf('.')+1));
  node.executes(ctx->{var fresh=ConfigOptions.all(Visuals.CONFIG).stream().filter(o->o.path().equals(option.path())).findFirst().orElseThrow();feedback.accept(ctx.getSource(),Component.literal(String.valueOf(fresh.get())));return 1;});
  node.then(RequiredArgumentBuilder.<S,String>argument("value",StringArgumentType.word()).executes(ctx->{
   try{String value=StringArgumentType.getString(ctx,"value");Object parsed;
    Class<?> type=option.field().getType();if(type==boolean.class){if(!value.equals("true")&&!value.equals("false"))return 0;parsed=Boolean.parseBoolean(value);}else if(type==int.class)parsed=Integer.parseInt(value);else if(type==float.class)parsed=Float.parseFloat(value);else parsed=Enum.valueOf((Class)type,value.toUpperCase(Locale.ROOT));
    // Resolve fresh config after a GUI save replaces its instance.
    var fresh=ConfigOptions.all(Visuals.CONFIG).stream().filter(o->o.path().equals(option.path())).findFirst().orElseThrow();fresh.set(parsed);Visuals.save();return 1;
   }catch(IllegalArgumentException error){return 0;}
  }));
  String[] parts=path.split("\\.");for(int i=parts.length-2;i>=0;i--)node=LiteralArgumentBuilder.<S>literal(parts[i]).then(node);root.then(node);
 }
}
