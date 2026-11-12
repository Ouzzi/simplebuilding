package com.simplebuilding.modules.simpledimensions;
import com.google.gson.*;
import java.nio.file.*;
public final class DimensionSettings {
 public boolean accessEnabled=true, automaticDestination=true, nonPlayerTravel=false;
 public boolean skyblockEnabled=true, miningEnabled=true, travelEnabled=true;
 /** W5: the first journey into a Simple Dimensions dimension hands out the guide (once per player). */
 public boolean giveGuideOnFirstTravel=true;
 /** N33 (owner F3=A): obsidian frames may also be arches (1 × 2 to 21 × 21, glow 20 % darker); rectangles stay Vanilla. */
 public boolean shapedNetherPortals=true;
 public int portalDelayTicks=0, teleportCooldownTicks=60;
 /** Built-in access is a server setting, never a second switch in the definition JSON. */
 public boolean allowsAccess(dev.simpledimension.common.portal.DimensionPortalConfig definition){
  return accessEnabled&&definition!=null&&switch(definition.id){
   case "skyblock" -> skyblockEnabled;
   case "mining" -> miningEnabled;
   case "travel" -> travelEnabled;
   default -> definition.enabled;
  };
 }
 public void normalize(){portalDelayTicks=ConfigLimits.clamp(portalDelayTicks,0,200);teleportCooldownTicks=ConfigLimits.clamp(teleportCooldownTicks,20,1200); nonPlayerTravel=false;}
 /** Takes over every value of {@code o} (a live settings object stays the same instance). */
 public void copyFrom(DimensionSettings o){
  accessEnabled=o.accessEnabled;automaticDestination=o.automaticDestination;nonPlayerTravel=o.nonPlayerTravel;
  giveGuideOnFirstTravel=o.giveGuideOnFirstTravel;shapedNetherPortals=o.shapedNetherPortals;skyblockEnabled=o.skyblockEnabled;miningEnabled=o.miningEnabled;travelEnabled=o.travelEnabled;
  portalDelayTicks=o.portalDelayTicks;teleportCooldownTicks=o.teleportCooldownTicks;
 }
 public void save(Path root){
  normalize();
  try{Files.createDirectories(root);var file=root.resolve("server.json");
   if(Files.isSymbolicLink(file))throw new IllegalArgumentException("Settings symlink");
   var temp=Files.createTempFile(root,"server-",".tmp");
   Files.writeString(temp,new GsonBuilder().setPrettyPrinting().create().toJson(this));
   Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
  }catch(Exception e){throw new IllegalStateException("Cannot save server settings",e);}
 }
 /**
  * Project policy of the framework {@code ConfigFiles} (audit simpledimensions-P2-2): a server.json that is a link, too big, unreadable or
  * empty never crashes the server - it is kept as {@code server.json.broken-<time>}, a warning names it, and the defaults are written.
  */
 public static DimensionSettings load(Path root){
  var file=root.resolve("server.json"); var gson=new GsonBuilder().setPrettyPrinting().create();
  var log=org.slf4j.LoggerFactory.getLogger("simpledimensions");
  try{Files.createDirectories(root);
   if(Files.isSymbolicLink(file)||Files.isRegularFile(file)&&Files.size(file)>ConfigLimits.MAX_BYTES)
    log.warn("Unsafe server.json (link or too big), using defaults; old file kept as {}",com.simplebuilding.framework.api.ConfigFiles.quarantine(file));
   var c=com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(file,text->gson.fromJson(text,DimensionSettings.class),log::warn);
   if(c==null){c=new DimensionSettings();Files.writeString(file,gson.toJson(c));}
   c.normalize();return c;
  }catch(Exception e){throw new IllegalStateException("Invalid server settings",e);}
 }
}
