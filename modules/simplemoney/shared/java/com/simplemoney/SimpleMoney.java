package com.simplemoney;
import com.google.gson.Gson;
import java.nio.file.*;
public final class SimpleMoney {
 public static final String MOD_ID="simplemoney";
 public static Config config=new Config();
 public static final class Config { public Trades trades=new Trades(); public Links links=new Links(); }
 public static final class Links {
  public static final int DEFAULT_BILLS_PER_HOUR=1;
  public static final int DEFAULT_RARITY_COST=3;
  public static final int DEFAULT_CRAFT_COST=1;
  public static final int DEFAULT_STOCK_LIMIT=2;
  public static final int DEFAULT_DAILY_LIMIT=8;
  public static final int DEFAULT_COOLDOWN_TICKS=100;
  public boolean enabled=true;
  public int billsPerHour=DEFAULT_BILLS_PER_HOUR, rarityStep=DEFAULT_RARITY_COST, craftWeight=DEFAULT_CRAFT_COST;
  public int stock=DEFAULT_STOCK_LIMIT, dailyLimit=DEFAULT_DAILY_LIMIT, cooldownTicks=DEFAULT_COOLDOWN_TICKS;
  public void normalize() {
   billsPerHour=Math.clamp(billsPerHour,1,2); rarityStep=Math.clamp(rarityStep,2,8); craftWeight=Math.clamp(craftWeight,1,4);
   stock=Math.clamp(stock,1,4); dailyLimit=Math.clamp(dailyLimit,1,16); cooldownTicks=Math.clamp(cooldownTicks,20,1200);
  }
 }
 public static final class Trades { public boolean enableVillagerTrades=true; public boolean enableWanderingTrades=true; }
 private static Path configFile;
 private static java.nio.file.attribute.FileTime lastRead;
 public static void loadConfig(Path dir) {
  Path file=dir.resolve("simplemoney.json"); Gson gson=new Gson();
  Config read=com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file,Config.class,gson,org.slf4j.LoggerFactory.getLogger("SimpleMoney")::warn);
  if(read!=null&&read.trades==null) { org.slf4j.LoggerFactory.getLogger("SimpleMoney").warn("simplemoney.json has no trades section, old file kept as {}",com.simplebuilding.framework.api.ConfigFiles.quarantine(file)); read=null; }
  config=read!=null?read:new Config();
  try { if(read==null) { Files.createDirectories(dir); Files.writeString(file,gson.toJson(config)); }
   if(config.links==null) config.links=new Links(); config.links.normalize();
   configFile=file; lastRead=Files.getLastModifiedTime(file);
  } catch(Exception e) { throw new IllegalStateException("Cannot load "+file,e); }
 }
 public static synchronized boolean enabled(String flag) {
  // Resource conditions run at world load and /reload. Observe disk changes there,
  // keeping a remote server's settings independent of any client config screen.
  try {
   if(configFile!=null && !Files.getLastModifiedTime(configFile).equals(lastRead)) loadConfig(configFile.getParent());
  } catch(java.io.IOException e) { throw new IllegalStateException("Cannot inspect "+configFile,e); }
  if(flag.startsWith("links:")) return MoneyLinks.enabled(flag.substring(6));
  return switch(flag) {case "enableVillagerTrades" -> config.trades.enableVillagerTrades; case "enableWanderingTrades" -> config.trades.enableWanderingTrades; default -> false;};
 }
}
