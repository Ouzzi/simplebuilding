package com.simplequalityoflife;
import com.simplequalityoflife.config.SimplequalityoflifeConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import org.slf4j.*;
public final class Simplequalityoflife {
 public static final String MOD_ID="simplequalityoflife";
 public static final Logger LOGGER=LoggerFactory.getLogger(MOD_ID);
 public static java.util.function.BiConsumer<net.minecraft.world.entity.player.Player,Boolean> crawlSync=(p,c)->{};
 private static java.util.function.Supplier<SimplequalityoflifeConfig> clientConfig=()->null;
 public static void clientConfig(java.util.function.Supplier<SimplequalityoflifeConfig> s){clientConfig=s;}
 public static SimplequalityoflifeConfig configFor(net.minecraft.world.level.Level level){var c=level.isClientSide()?clientConfig.get():null;return c==null?getConfig():c;}
 private static SimplequalityoflifeConfig serverConfig=new SimplequalityoflifeConfig();
 public static void init(){AutoConfig.register(SimplequalityoflifeConfig.class,GsonConfigSerializer::new); serverConfig=getLocalConfig();serverConfig.normalize();}
 /** Gameplay never reads the client's synchronized cache or editable holder. */
 public static SimplequalityoflifeConfig getConfig(){serverConfig.normalizeNumbers();return serverConfig;}
 public static SimplequalityoflifeConfig getLocalConfig(){return AutoConfig.getConfigHolder(SimplequalityoflifeConfig.class).getConfig();}
 public static void serverStarted(){serverConfig=getLocalConfig();serverConfig.normalize();}
 public static Runnable onChange=()->{};
 public static void save(){serverConfig.normalize();AutoConfig.getConfigHolder(SimplequalityoflifeConfig.class).setConfig(serverConfig);AutoConfig.getConfigHolder(SimplequalityoflifeConfig.class).save();onChange.run();}
}
