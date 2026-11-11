package com.simplequalityoflife;
import com.simplequalityoflife.config.SimplequalityoflifeConfig;
import org.slf4j.*;
public final class Simplequalityoflife {
 public static final String MOD_ID="simplequalityoflife";
 public static final Logger LOGGER=LoggerFactory.getLogger(MOD_ID);
 private static SimplequalityoflifeConfig serverConfig=new SimplequalityoflifeConfig();
 /** Q-B1: the config file is plain JSON (framework ConfigFiles), so the mod and a dedicated server run without Cloth Config. */
 static final java.nio.file.Path FILE=java.nio.file.Path.of("config",MOD_ID+".json");
 private static SimplequalityoflifeConfig localConfig;
 public static void init(){adoptRenamedSwitches(FILE);localConfig=load(FILE);serverConfig=localConfig;serverConfig.normalize();subMods(serverConfig);}
 /** Reads the file (a broken one is quarantined); a missing file is written with the defaults. */
 static SimplequalityoflifeConfig load(java.nio.file.Path file){
  var c=com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file,t->new com.google.gson.Gson().fromJson(t,SimplequalityoflifeConfig.class),LOGGER::warn);
  if(c==null){c=new SimplequalityoflifeConfig();if(!java.nio.file.Files.exists(file))write(file,c);}
  return c;
 }
 static void write(java.nio.file.Path file,SimplequalityoflifeConfig c){
  try{java.nio.file.Files.createDirectories(file.toAbsolutePath().getParent());com.simplebuilding.framework.api.ConfigFiles.writeAtomic(file,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(c));}
  catch(java.io.IOException e){LOGGER.warn("Cannot save {}",file,e);}
 }
 /** The client config screen: stores the edited local config and applies the sub-mod switches. */
 public static void saveLocal(SimplequalityoflifeConfig c){c.normalize();localConfig=c;write(FILE,c);subMods(c);}
 /** Switches of renamed sub-mods (Simple Creatures is now Simple Mob Manners): {old key, new key}. */
 static final String[][] RENAMED_SWITCHES={{"enableSimpleCreatures","enableSimpleMobManners"}};
 /** Renames old switch keys in the config file once, before the config is read (a value set under the new key wins). */
 public static boolean adoptRenamedSwitches(java.nio.file.Path file){
  try{if(!java.nio.file.Files.isRegularFile(file)||java.nio.file.Files.size(file)>1<<20)return false;
   var parsed=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(file));if(!parsed.isJsonObject())return false;
   var root=parsed.getAsJsonObject();boolean changed=false;
   for(String[] r:RENAMED_SWITCHES)if(root.has(r[0])){var value=root.remove(r[0]);if(!root.has(r[1]))root.add(r[1],value);changed=true;}
   if(changed){com.simplebuilding.framework.api.ConfigFiles.writeAtomic(file,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root));LOGGER.info("Renamed old sub-mod switches in {}",file.getFileName());}
   return changed;
  }catch(java.io.IOException|RuntimeException e){LOGGER.warn("Cannot adopt renamed switches in {}",file,e);return false;}
 }
 /** Gameplay never reads the client's synchronized cache or editable holder. */
 public static SimplequalityoflifeConfig getConfig(){serverConfig.normalizeNumbers();return serverConfig;}
 public static SimplequalityoflifeConfig getLocalConfig(){if(localConfig==null)localConfig=load(FILE);return localConfig;}
 public static void serverStarted(){serverConfig=getLocalConfig();serverConfig.normalize();subMods(serverConfig);}
 /** Super-mod switches "Enable Simple XY": the sub-mods read them through framework SubMods (no class import). */
 public static void subMods(SimplequalityoflifeConfig c){com.simplebuilding.framework.api.SubMods.set("simpleweather",c.enableSimpleWeather);com.simplebuilding.framework.api.SubMods.set("simpleinterfaces",c.enableSimpleInterfaces);com.simplebuilding.framework.api.SubMods.set("simpleloot",c.enableSimpleLoot);com.simplebuilding.framework.api.SubMods.set("simplemobmanners",c.enableSimpleMobManners);com.simplebuilding.framework.api.SubMods.set("simplefarming",c.enableSimpleFarming);com.simplebuilding.framework.api.SubMods.set("simpletools",c.enableSimpleTools);com.simplebuilding.framework.api.SubMods.set("simplemovement",c.enableSimpleMovement);}
 /** The config command (CMD24): a new, normalised server config; {@link #save} writes it. */
 public static void replace(SimplequalityoflifeConfig c){c.normalize();serverConfig=c;}
 public static void save(){serverConfig.normalize();localConfig=serverConfig;write(FILE,serverConfig);subMods(serverConfig);}
}
