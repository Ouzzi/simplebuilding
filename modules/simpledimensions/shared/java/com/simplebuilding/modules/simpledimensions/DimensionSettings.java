package com.simplebuilding.modules.simpledimensions;
import com.google.gson.*;
import java.nio.file.*;
public final class DimensionSettings {
 public boolean accessEnabled=true, automaticDestination=true, nonPlayerTravel=false;
 public int portalDelayTicks=0, teleportCooldownTicks=60;
 public void normalize(){portalDelayTicks=ConfigLimits.clamp(portalDelayTicks,0,200);teleportCooldownTicks=ConfigLimits.clamp(teleportCooldownTicks,20,1200); nonPlayerTravel=false;}
 public static DimensionSettings load(Path root){
  var file=root.resolve("server.json"); var gson=new GsonBuilder().setPrettyPrinting().create();
  try{Files.createDirectories(root);if(!Files.exists(file)){var c=new DimensionSettings();Files.writeString(file,gson.toJson(c));return c;}
   if(Files.isSymbolicLink(file)||Files.size(file)>ConfigLimits.MAX_BYTES)throw new IllegalArgumentException("Settings file size");
   var c=gson.fromJson(Files.readString(file),DimensionSettings.class);if(c==null)throw new IllegalArgumentException("Empty settings");c.normalize();return c;
  }catch(Exception e){throw new IllegalStateException("Invalid server settings",e);}
 }
}
