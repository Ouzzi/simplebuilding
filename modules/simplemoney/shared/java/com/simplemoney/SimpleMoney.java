package com.simplemoney;
import com.google.gson.Gson;
import java.nio.file.*;
public final class SimpleMoney {
 public static final String MOD_ID="simplemoney";
 public static Config config=new Config();
 /** Super-mod switch "Enable Simple Money" (framework SubMods): off = items stay registered but inert (no tab entries, loot, trades, link offers). */
 public static boolean on() { return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID); }
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
 public static final class Trades { public boolean enableVillagerTrades=true; public boolean enableWanderingTrades=true;
  /** Price-list links of the Village Wanderer (Simple Wanderers, a-small22): the 14 heads with chance 0.1. */
  public boolean enableWandererTrades=true; }
 private static Path configFile;
 private static java.nio.file.attribute.FileTime lastRead;
 private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("SimpleMoney");
 public static void loadConfig(Path dir) {
  Path file=dir.resolve("simplemoney.json"); Gson gson=new Gson();
  Config read=com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file,t->gson.fromJson(t,Config.class),LOG::warn);
  if(read!=null&&read.trades==null) { LOG.warn("simplemoney.json has no trades section, old file kept as {}",com.simplebuilding.framework.api.ConfigFiles.quarantine(file)); read=null; }
  config=read!=null?read:new Config();
  if(config.links==null) config.links=new Links(); config.links.normalize();
  configFile=file;
  // REV2-B: a file that cannot be written or read keeps the values in use (resource conditions call this, they must not throw).
  try { if(read==null) { Files.createDirectories(dir); com.simplebuilding.framework.api.ConfigFiles.writeAtomic(file,gson.toJson(config)); }
   lastRead=Files.getLastModifiedTime(file);
  } catch(Exception e) { LOG.warn("Cannot write {}: {}",file,e.toString()); lastRead=null; }
 }
 /** The directory of the config file in use (tests restore it), null before {@link #loadConfig}. */
 public static Path configDir() { return configFile==null?null:configFile.getParent(); }
 public static synchronized boolean enabled(String flag) {
  // Resource conditions run at world load and /reload. Observe disk changes there,
  // keeping a remote server's settings independent of any client config screen.
  if(configFile!=null) {
   try {
    if(!Files.getLastModifiedTime(configFile).equals(lastRead)) loadConfig(configFile.getParent());
   } catch(java.io.IOException e) {
    // deleted or unreadable while running: keep the values in use, warn once
    if(lastRead!=null) LOG.warn("Cannot inspect {} ({}), keeping the loaded settings",configFile,e.toString());
    lastRead=null;
   }
  }
  if(!on()) return false;
  if(flag.startsWith("links:")) return MoneyLinks.enabled(flag.substring(6));
  return switch(flag) {case "enableVillagerTrades" -> config.trades.enableVillagerTrades; case "enableWanderingTrades" -> config.trades.enableWanderingTrades; case "enableWandererTrades" -> config.trades.enableWandererTrades; default -> false;};
 }
 /** The config switch of a price-list merchant (tools/generate_links.py FLAGS): every profession is a villager. */
 public static String merchantFlag(String merchant) {
  return switch(merchant) {case "wandering_trader" -> "enableWanderingTrades"; case "village_wanderer" -> "enableWandererTrades"; default -> "enableVillagerTrades";};
 }
}
