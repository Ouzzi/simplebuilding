package com.simplequalityoflife;
import com.simplequalityoflife.config.SimplequalityoflifeConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import org.slf4j.*;
public final class Simplequalityoflife {
 public static final String MOD_ID="simplequalityoflife";
 public static final Logger LOGGER=LoggerFactory.getLogger(MOD_ID);
 private static SimplequalityoflifeConfig serverConfig=new SimplequalityoflifeConfig();
 public static void init(){adoptRenamedSwitches(me.shedaniel.autoconfig.util.Utils.getConfigFolder().resolve(MOD_ID+".json"));AutoConfig.register(SimplequalityoflifeConfig.class,GsonConfigSerializer::new); serverConfig=getLocalConfig();serverConfig.normalize();subMods(serverConfig);}
 /** Switches of renamed sub-mods (Simple Creatures is now Simple Mob Manners): {old key, new key}. */
 static final String[][] RENAMED_SWITCHES={{"enableSimpleCreatures","enableSimpleMobManners"}};
 /** Renames old switch keys in the config file once, before AutoConfig reads it (a value set under the new key wins). */
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
 public static SimplequalityoflifeConfig getLocalConfig(){return AutoConfig.getConfigHolder(SimplequalityoflifeConfig.class).getConfig();}
 public static void serverStarted(){serverConfig=getLocalConfig();serverConfig.normalize();subMods(serverConfig);}
 /** Super-mod switches "Enable Simple XY": the sub-mods read them through framework SubMods (no class import). */
 public static void subMods(SimplequalityoflifeConfig c){com.simplebuilding.framework.api.SubMods.set("simpleweather",c.enableSimpleWeather);com.simplebuilding.framework.api.SubMods.set("simpleinterfaces",c.enableSimpleInterfaces);com.simplebuilding.framework.api.SubMods.set("simpleloot",c.enableSimpleLoot);com.simplebuilding.framework.api.SubMods.set("simplemobmanners",c.enableSimpleMobManners);com.simplebuilding.framework.api.SubMods.set("simplefarming",c.enableSimpleFarming);com.simplebuilding.framework.api.SubMods.set("simpletools",c.enableSimpleTools);com.simplebuilding.framework.api.SubMods.set("simplemovement",c.enableSimpleMovement);}
 public static void save(){serverConfig.normalize();AutoConfig.getConfigHolder(SimplequalityoflifeConfig.class).setConfig(serverConfig);AutoConfig.getConfigHolder(SimplequalityoflifeConfig.class).save();subMods(serverConfig);}
}
