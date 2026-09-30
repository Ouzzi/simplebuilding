package com.simplemoney;
import com.google.gson.Gson;
import java.nio.file.*;
public final class SimpleMoney {
 public static final String MOD_ID="simplemoney";
 public static Config config=new Config();
 public static final class Config { public Trades trades=new Trades(); }
 public static final class Trades { public boolean enableVillagerTrades=true; public boolean enableWanderingTrades=true; }
 private static Path configFile;
 private static java.nio.file.attribute.FileTime lastRead;
 public static void loadConfig(Path dir) {
  Path file=dir.resolve("simplemoney.json"); Gson gson=new Gson();
  try { if(Files.exists(file)) { config=gson.fromJson(Files.readString(file),Config.class); if(config==null||config.trades==null) throw new IllegalArgumentException("Missing trades configuration"); }
   else { Files.createDirectories(dir); Files.writeString(file,gson.toJson(config)); }
   configFile=file; lastRead=Files.getLastModifiedTime(file);
  } catch(Exception e) { throw new IllegalStateException("Cannot load "+file,e); }
 }
 public static synchronized boolean enabled(String flag) {
  // Resource conditions run at world load and /reload. Observe disk changes there,
  // keeping a remote server's settings independent of any client config screen.
  try {
   if(configFile!=null && !Files.getLastModifiedTime(configFile).equals(lastRead)) loadConfig(configFile.getParent());
  } catch(java.io.IOException e) { throw new IllegalStateException("Cannot inspect "+configFile,e); }
  return switch(flag) {case "enableVillagerTrades" -> config.trades.enableVillagerTrades; case "enableWanderingTrades" -> config.trades.enableWanderingTrades; default -> false;};
 }
}
