package com.simplebuilding.modules.simpledimensions;
import com.google.gson.*;
import java.nio.file.*;
public final class DimensionSettings {
 public boolean accessEnabled=true, automaticDestination=true, nonPlayerTravel=false;
 public boolean skyblockEnabled=true, miningEnabled=true, travelEnabled=true;
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
 public void save(Path root){
  normalize();
  try{Files.createDirectories(root);var file=root.resolve("server.json");
   if(Files.isSymbolicLink(file))throw new IllegalArgumentException("Settings symlink");
   var temp=Files.createTempFile(root,"server-",".tmp");
   Files.writeString(temp,new GsonBuilder().setPrettyPrinting().create().toJson(this));
   Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
  }catch(Exception e){throw new IllegalStateException("Cannot save server settings",e);}
 }
 public static DimensionSettings load(Path root){
  var file=root.resolve("server.json"); var gson=new GsonBuilder().setPrettyPrinting().create();
  try{Files.createDirectories(root);if(!Files.exists(file)){var c=new DimensionSettings();Files.writeString(file,gson.toJson(c));return c;}
   if(Files.isSymbolicLink(file)||Files.size(file)>ConfigLimits.MAX_BYTES)throw new IllegalArgumentException("Settings file size");
   var c=gson.fromJson(Files.readString(file),DimensionSettings.class);if(c==null)throw new IllegalArgumentException("Empty settings");c.normalize();return c;
  }catch(Exception e){throw new IllegalStateException("Invalid server settings",e);}
 }
}
