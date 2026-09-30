package com.simplebuilding.modules.simpledimensions;
import dev.simpledimension.common.portal.*;
import net.minecraft.resources.Identifier;
import java.util.*;
/** Untrusted JSON is validated before scanners or world-generation allocate data. */
public final class ConfigLimits {
 public static final int MAX_DEFINITIONS=16, MAX_RECIPES=8, MAX_INTERIOR=21, MAX_MATRIX=23, MAX_ORIGINS=8, MAX_BYTES=65536;
 public static final int MAX_DELAY=200, MIN_COOLDOWN=20, MAX_COOLDOWN=1200, MAX_PORTALS=1024, MAX_BUILDS_PER_TICK=1;
 public static final double MIN_SCALE=.5, MAX_SCALE=10;
 public static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
 public static double scale(double v){if(!Double.isFinite(v))throw new IllegalArgumentException("Non-finite coordinate scale");return Math.max(MIN_SCALE,Math.min(MAX_SCALE,v));}
 private static void id(String v){if(v==null||v.length()>128||Identifier.tryParse(v)==null)throw new IllegalArgumentException("Invalid resource id");}
 public static DimensionPortalConfig validate(DimensionPortalConfig c) {
  if(c==null||c.id==null||!c.id.matches("[a-z0-9_]{1,48}"))throw new IllegalArgumentException("Invalid definition id");
  id(c.sourceDimensionId); id(c.targetDimensionId);
  if(!c.targetDimensionId.equals("simpledimension:"+c.id)||c.sourceDimensionId.equals(c.targetDimensionId))throw new IllegalArgumentException("Target must be isolated under simpledimension");
  c.portalDelayTicks=clamp(c.portalDelayTicks,0,MAX_DELAY);c.teleportCooldownTicks=clamp(c.teleportCooldownTicks,MIN_COOLDOWN,MAX_COOLDOWN);
  c.travelCoordinateScale=scale(c.travelCoordinateScale);
  c.minPortalWidth=clamp(c.minPortalWidth,1,MAX_INTERIOR); c.maxPortalWidth=clamp(c.maxPortalWidth,c.minPortalWidth,MAX_INTERIOR);
  c.minPortalHeight=clamp(c.minPortalHeight,2,MAX_INTERIOR); c.maxPortalHeight=clamp(c.maxPortalHeight,c.minPortalHeight,MAX_INTERIOR);
  id(c.frameBlock); if(c.frameBlock.equals("minecraft:obsidian"))throw new IllegalArgumentException("Vanilla portal frame reserved");
  if(c.openFromDimensions==null||c.openFromDimensions.size()>MAX_ORIGINS)throw new IllegalArgumentException("Origin limit");
  c.openFromDimensions.forEach(ConfigLimits::id);
  if(c.portalRecipes==null||c.portalRecipes.size()>MAX_RECIPES)throw new IllegalArgumentException("Recipe limit");
  for(var r:c.portalRecipes){
   if(r==null||r.rows==null||r.rows.isEmpty()||r.rows.size()>MAX_MATRIX||r.legend==null||r.legend.size()>32)throw new IllegalArgumentException("Invalid matrix");
   int w=r.rows.getFirst().length();if(w<1||w>MAX_MATRIX)throw new IllegalArgumentException("Matrix width");
   if(r.interior==null||r.interior.length()!=1||r.ignore==null||r.ignore.length()!=1||r.interior.equals(r.ignore))throw new IllegalArgumentException("Recipe symbols");
   for(var v:r.legend.values()){id(v);if(v.equals("minecraft:obsidian"))throw new IllegalArgumentException("Vanilla portal reserved");}
   for(String row:r.rows){if(row==null||row.length()!=w)throw new IllegalArgumentException("Ragged matrix");
    for(char ch:row.toCharArray())if(ch!=r.interior.charAt(0)&&ch!=r.ignore.charAt(0)&&!r.legend.containsKey(""+ch))throw new IllegalArgumentException("Unknown symbol");}
   var parsed=PortalRecipe.parse(r.rows,r.legend,r.interior.charAt(0),r.ignore.charAt(0));
   if(!parsed.hasInterior()||parsed.interiorCellsRel().size()>MAX_INTERIOR*MAX_INTERIOR)throw new IllegalArgumentException("Interior limit");
  }
  var g=c.worldGeneration; if(g==null)throw new IllegalArgumentException("Missing generation");
  g.minY=Math.floorDiv(clamp(g.minY,-64,0),16)*16;g.height=Math.floorDiv(clamp(g.height,16,384),16)*16;
  g.logicalHeight=clamp(g.logicalHeight,1,g.height);g.coordinateScale=scale(g.coordinateScale);id(g.baseBiome);
  if(!List.of("flat","noise").contains(g.generatorType)||g.biomeSelection==null||g.biomeSelection.size()>16||g.flatLayers==null||g.flatLayers.size()>16)throw new IllegalArgumentException("Generation limits");
  g.biomeSelection.forEach(ConfigLimits::id);int layers=0;
  for(var layer:g.flatLayers){if(layer==null||layer.height<1||layer.height>384)throw new IllegalArgumentException("Layer height");id(layer.block);layers+=layer.height;}
  if(layers>g.height)throw new IllegalArgumentException("Layers exceed height");
  return c;
 }
}
