package com.simplevisuals;
import java.nio.file.*;import com.google.gson.*;import com.simplevisuals.config.SimplevisualsConfig;
public final class Visuals {
 public static final String MOD_ID="simplevisuals";
 private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
 public static final Path CONFIG_PATH=Path.of("config/simplevisuals.json");
 public static SimplevisualsConfig CONFIG=new SimplevisualsConfig();
 private static volatile boolean serverFormatting=true;
 public static boolean serverFormatting(){return serverFormatting;}
 /** Called by the authorized server command, never by the cosmetic config screen. */
 public static void setServerFormatting(boolean value){serverFormatting=value;}
 public static void initialize(){
  try{if(Files.exists(CONFIG_PATH)){try(var r=Files.newBufferedReader(CONFIG_PATH)){var c=GSON.fromJson(r,SimplevisualsConfig.class);if(c!=null)CONFIG=c;}}ConfigOptions.normalize(CONFIG);}
  catch(Exception e){org.slf4j.LoggerFactory.getLogger(MOD_ID).warn("Invalid Simple Visuals config; using safe defaults",e);CONFIG=new SimplevisualsConfig();}
  serverFormatting=CONFIG.visuals.enableAnvilFormatting;
 }
 public static void save(){ConfigOptions.normalize(CONFIG);try{Files.createDirectories(CONFIG_PATH.getParent());Files.writeString(CONFIG_PATH,GSON.toJson(CONFIG));}catch(Exception e){throw new IllegalStateException(e);}}
 public static String formatName(String name,boolean enabled){
  if(name==null)return null;
  if(name.length()>50)return null;
  StringBuilder out=new StringBuilder();
  for(int i=0;i<name.length();i++){char ch=name.charAt(i);if(ch<32||ch==127)continue;
   if((ch=='&'||ch=='§')&&i+1<name.length()&&"0123456789abcdefklmnor".indexOf(Character.toLowerCase(name.charAt(i+1)))>=0){char code=Character.toLowerCase(name.charAt(++i));if(enabled)out.append('§').append(code);continue;}
   if(ch!='§')out.append(ch);
  }return out.toString();
 }
}
