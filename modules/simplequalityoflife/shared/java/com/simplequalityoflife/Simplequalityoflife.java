package com.simplequalityoflife;
import com.simplequalityoflife.config.SimplequalityoflifeConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import org.slf4j.*;
public final class Simplequalityoflife {
 public static final String MOD_ID="simplequalityoflife";
 public static final Logger LOGGER=LoggerFactory.getLogger(MOD_ID);
 private static SimplequalityoflifeConfig serverConfig=new SimplequalityoflifeConfig();
 public static void init(){AutoConfig.register(SimplequalityoflifeConfig.class,GsonConfigSerializer::new); serverConfig=getLocalConfig();serverConfig.normalize();subMods(serverConfig);}
 /** Gameplay never reads the client's synchronized cache or editable holder. */
 public static SimplequalityoflifeConfig getConfig(){serverConfig.normalizeNumbers();return serverConfig;}
 public static SimplequalityoflifeConfig getLocalConfig(){return AutoConfig.getConfigHolder(SimplequalityoflifeConfig.class).getConfig();}
 public static void serverStarted(){serverConfig=getLocalConfig();serverConfig.normalize();subMods(serverConfig);}
 /** Super-mod switches "Enable Simple XY": the sub-mods read them through framework SubMods (no class import). */
 public static void subMods(SimplequalityoflifeConfig c){com.simplebuilding.framework.api.SubMods.set("simpleweather",c.enableSimpleWeather);com.simplebuilding.framework.api.SubMods.set("simpleinterfaces",c.enableSimpleInterfaces);com.simplebuilding.framework.api.SubMods.set("simpleloot",c.enableSimpleLoot);com.simplebuilding.framework.api.SubMods.set("simplecreatures",c.enableSimpleCreatures);com.simplebuilding.framework.api.SubMods.set("simplefarming",c.enableSimpleFarming);com.simplebuilding.framework.api.SubMods.set("simpletools",c.enableSimpleTools);com.simplebuilding.framework.api.SubMods.set("simplemovement",c.enableSimpleMovement);}
 public static void save(){serverConfig.normalize();AutoConfig.getConfigHolder(SimplequalityoflifeConfig.class).setConfig(serverConfig);AutoConfig.getConfigHolder(SimplequalityoflifeConfig.class).save();subMods(serverConfig);}
}
