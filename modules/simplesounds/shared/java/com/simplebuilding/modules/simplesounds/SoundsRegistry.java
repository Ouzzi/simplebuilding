package com.simplebuilding.modules.simplesounds;
import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class SoundsRegistry {
 public record Effect(String id, String category, String sound, int interval, float volume, float pitch) {}
 public static final List<Effect> ALL=load();
 public static List<Effect> load() {
  try(var stream=SoundsRegistry.class.getResourceAsStream("/assets/simplesounds/effects.json")) {
   if(stream==null) throw new IllegalStateException("Missing sound mappings");
   var list=List.of(new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),Effect[].class));
   var ids=new HashSet<String>();
   for(var e:list) if(!ids.add(e.id()) || e.interval()<20 || !Float.isFinite(e.volume()) || e.volume()<0 || e.volume()>.25f || !Float.isFinite(e.pitch()) || e.pitch()<.5f || e.pitch()>2 || !e.sound().startsWith("minecraft:")) throw new IllegalStateException("Unsafe sound mapping "+e.id());
   return list;
  } catch(IOException e) {throw new IllegalStateException(e);}
 }
}
