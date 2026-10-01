package com.simplebuilding.modules.simplequalityoflife.forge;
import com.simplequalityoflife.config.SimplequalityoflifeConfig;
import java.nio.file.*;
/** Module-owned AutoConfig platform equivalent; never bundles shared shim packages. */
public final class QolForgeConfig {
 public static final QolForgeConfig HOLDER=new QolForgeConfig();
 private SimplequalityoflifeConfig config=new SimplequalityoflifeConfig();
 private static Path path(){return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("simplequalityoflife.json");}
 public static void load(){try{if(Files.isRegularFile(path())&&Files.size(path())<=32768){var c=new com.google.gson.Gson().fromJson(Files.readString(path()),SimplequalityoflifeConfig.class);if(c!=null)HOLDER.config=c;}}catch(java.io.IOException|RuntimeException e){com.simplequalityoflife.Simplequalityoflife.LOGGER.warn("Cannot load Forge configuration",e);}HOLDER.config.normalize();}
 public SimplequalityoflifeConfig getConfig(){return config;}
 public void setConfig(SimplequalityoflifeConfig c){c.normalize();config=c;}
 public void save(){
  config.normalize();
  try{
   Files.createDirectories(path().getParent());
   var temporary=Files.createTempFile(path().getParent(),"simplequalityoflife-",".tmp");
   try{
    Files.writeString(temporary,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(config));
    Files.move(temporary,path(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
   }finally{Files.deleteIfExists(temporary);}
  }catch(java.io.IOException e){throw new IllegalStateException("Cannot save Forge configuration",e);}
 }
}
