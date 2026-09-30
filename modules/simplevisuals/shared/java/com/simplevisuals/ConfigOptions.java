package com.simplevisuals;
import java.lang.reflect.*;import java.util.*;
import com.simplevisuals.config.SimplevisualsConfig;
public final class ConfigOptions {
 public record Option(String path,Object owner,Field field,Object defaultValue,String tab,double min,double max) {
  public Object get(){try{return field.get(owner);}catch(Exception e){throw new IllegalStateException(e);}}
  public void set(Object value){try{field.set(owner,value);}catch(Exception e){throw new IllegalArgumentException(e);}}
 }
 public static List<Option> all(SimplevisualsConfig c){var list=new ArrayList<Option>();walk("",c,new SimplevisualsConfig(),list);return list;}
 private static void walk(String prefix,Object obj,Object defaults,List<Option> out){
  for(Field f:obj.getClass().getFields())try{
   if(Modifier.isStatic(f.getModifiers())||Map.class.isAssignableFrom(f.getType()))continue;
   String p=prefix+f.getName();Object v=f.get(obj),d=f.get(defaults);
   if(f.getType().isPrimitive()||f.getType().isEnum()){
    double[] bounds=bounds(f.getName());String tab=p.startsWith("particles")?"particles":p.contains("speedLines")?"speed":p.contains("pickupNotifier")?"pickup":p.contains("damageIndicators")?"damage":p.contains("heldItemTooltips")?"tooltips":p.contains("biomeInfo")?"biome":"general";
    out.add(new Option(p,obj,f,d,tab,bounds[0],bounds[1]));
   }else{if(v==null){v=f.getType().getConstructor().newInstance();f.set(obj,v);}walk(p+".",v,d,out);}
  }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
 }
 private static double[] bounds(String n){
  if(n.toLowerCase(Locale.ROOT).contains("color"))return new double[]{0,0xffffff};
  if(n.contains("Offset"))return new double[]{0,4096};
  return switch(n){
   case "elytraTargetAngleUp","elytraTargetAngleDown"->new double[]{-90,90};
   case "elytraPitchTolerance"->new double[]{0,45};
   case "elytraSensitivity"->new double[]{0,10};
   case "speedLinesAlpha","pickupBackgroundOpacity"->new double[]{0,1};
   case "speedLinesAmount"->new double[]{0,2};
   case "speedLinesRadius"->new double[]{.2,1};
   case "speedLinesWidth"->new double[]{.25,8};
   case "speedLinesSpeed","speedLinesScale"->new double[]{0,4};
   case "speedThreshold"->new double[]{.05,4};
   case "scale","pickupNotifierScale"->new double[]{.5,2};
   case "maxEnchantments"->new double[]{0,8};
   case "displayDuration","pickupNotifierDuration"->new double[]{20,600};
   case "cooldownSeconds"->new double[]{0,3600};
   default->new double[]{0,1};
  };
 }
 public static void normalize(SimplevisualsConfig c){
  for(var o:all(c)){
   if(o.get() instanceof Number n){double v=n.doubleValue();if(!Double.isFinite(v))v=((Number)o.defaultValue()).doubleValue();v=Math.max(o.min(),Math.min(o.max(),v));if(o.field().getType()==int.class)o.set((int)v);else o.set((float)v);}
   else if(o.get()==null)o.set(o.defaultValue());
  }
  if(c.particles.overrides==null)c.particles.overrides=new LinkedHashMap<>();
  var ids=com.simplevisuals.effects.EffectRegistry.ALL.stream().map(com.simplevisuals.effects.EffectRegistry.Effect::id).toList();
  c.particles.overrides.entrySet().removeIf(e->!ids.contains(e.getKey())||e.getValue()==null);
 }
}
