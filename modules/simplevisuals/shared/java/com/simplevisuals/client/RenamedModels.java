package com.simplevisuals.client;

import com.google.gson.*;
import com.simplevisuals.Visuals;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.world.item.*;
import java.nio.file.*;
import java.util.*;

/** Compatibility bridge for old CIT JSON and local PNGs. Never changes the actual stack. */
public final class RenamedModels {
    public static final int MAX_RULES=com.simplevisuals.AssetBounds.MAX_RULES, MAX_TEXTURE_BYTES=com.simplevisuals.AssetBounds.MAX_TEXTURE_BYTES, MAX_TEXTURE_SIZE=com.simplevisuals.AssetBounds.MAX_TEXTURE_SIZE;
    public record Entry(Identifier id, Item item, String name, Identifier model, List<String> tags, float weight, Identifier renderedModel) {}
    private static List<Entry> entries=List.of();
    private static String fingerprint="";
    private static boolean installed;
    private static final Path GENERATED=Path.of("config/simplevisuals/generated-pack");
    public static boolean modelsLoaded;
    public static List<Entry> entries(Item item){return entries.stream().filter(e->e.item==item).toList();}
    public static ItemStack renderStack(ItemStack stack){
        if(!Visuals.CONFIG.visuals.enableRenamedItemTextures||modelsLoaded||!stack.has(DataComponents.CUSTOM_NAME))return stack;
        for(var entry:entries)if(entry.item==stack.getItem()&&entry.name.equals(stack.getHoverName().getString())){
            var copy=stack.copy();copy.set(DataComponents.ITEM_MODEL,entry.renderedModel);return copy;
        }return stack;
    }
    public static void reload(Minecraft mc){
        if(!Visuals.CONFIG.visuals.enableRenamedItemTextures||modelsLoaded){entries=List.of();return;}
        try{
            var parsed=new ArrayList<Entry>();var files=new TreeMap<String,String>();
            for(var resource:mc.getResourceManager().listResources("cit",id->id.getPath().endsWith(".json")).entrySet()){
                if(parsed.size()>=MAX_RULES)break;
                try(var input=resource.getValue().open()){
                    var json=JsonParser.parseString(com.simplevisuals.AssetBounds.readRule(input)).getAsJsonObject();
                    add(parsed,files,resource.getKey(),json);
                }catch(Exception error){org.slf4j.LoggerFactory.getLogger("simplevisuals").warn("Ignoring invalid CIT rule {}",resource.getKey(),error);}
            }
            var textures=Path.of("config/simplevisuals/textures");
            if(Files.isDirectory(textures))try(var stream=Files.list(textures)){
                for(var path:stream.sorted().limit(MAX_RULES).toList()){
                    if(parsed.size()>=MAX_RULES)break;
                    var filename=path.getFileName().toString().toLowerCase(Locale.ROOT);
                    if(!filename.matches("[a-z0-9_-]+\\.png")||Files.isSymbolicLink(path)||!Files.isRegularFile(path)||Files.size(path)>MAX_TEXTURE_BYTES)continue;
                    try(var input=Files.newInputStream(path)){if(!com.simplevisuals.AssetBounds.pngHeader(input.readNBytes(24)))continue;}
                    try(var input=Files.newInputStream(path);var image=com.mojang.blaze3d.platform.NativeImage.read(input)){
                        if(image.getWidth()>MAX_TEXTURE_SIZE||image.getHeight()>MAX_TEXTURE_SIZE)continue;
                    }
                    String stem=filename.substring(0,filename.length()-4);
                    String texture="simplevisuals:item/"+stem;
                    files.put("assets/simplevisuals/models/item/"+stem+".json",new Gson().toJson(Map.of("parent","minecraft:item/generated","textures",Map.of("layer0",texture))));
                    var target=GENERATED.resolve("assets/simplevisuals/textures/item/"+filename);Files.createDirectories(target.getParent());Files.copy(path,target,StandardCopyOption.REPLACE_EXISTING);
                    var rule=new JsonObject();rule.addProperty("item","minecraft:stick");rule.addProperty("name",stem.startsWith("stick_")?stem.substring(6):stem);rule.addProperty("model","simplevisuals:item/"+stem);
                    add(parsed,files,Identifier.fromNamespaceAndPath("simplevisuals","local/"+stem),rule);
                }
            }
            parsed.sort(Comparator.comparingDouble(Entry::weight).reversed().thenComparing(e->e.id.toString()));entries=List.copyOf(parsed);
            String signature=files.toString()+parsed.toString();
            if(signature.equals(fingerprint))return;fingerprint=signature;
            int major=net.minecraft.SharedConstants.RESOURCE_PACK_FORMAT_MAJOR,minor=net.minecraft.SharedConstants.RESOURCE_PACK_FORMAT_MINOR;
            files.put("pack.mcmeta",new Gson().toJson(Map.of("pack",Map.of("description","Simple Visuals generated models","min_format",List.of(major,minor),"max_format",List.of(major,minor)))));
            for(var file:files.entrySet()){Path target=GENERATED.resolve(file.getKey());Files.createDirectories(target.getParent());Files.writeString(target,file.getValue());}
            if(!installed){
                installed=true;var accessor=(com.simplevisuals.mixin.PackRepositoryAccessor)mc.getResourcePackRepository();
                var sources=new HashSet<>(accessor.visuals$sources());
                sources.add(consumer->{
                    var info=new PackLocationInfo("simplevisuals-generated",Component.literal("Simple Visuals"),PackSource.BUILT_IN,Optional.empty());
                    var pack=Pack.readMetaAndCreate(info,new PathPackResources.PathResourcesSupplier(GENERATED),PackType.CLIENT_RESOURCES,new PackSelectionConfig(true,Pack.Position.TOP,true));
                    if(pack!=null)consumer.accept(pack);
                });accessor.visuals$sources(sources);
            }
            mc.getResourcePackRepository().reload();mc.execute(mc::reloadResourcePacks);
        }catch(Exception error){entries=List.of();org.slf4j.LoggerFactory.getLogger("simplevisuals").warn("Renamed model reload failed safely",error);}
    }
    private static byte[] digest(String value){try{return java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}
    private static void add(List<Entry> entries,Map<String,String> files,Identifier fileId,JsonObject json){
        var itemId=Identifier.parse(json.get("item").getAsString());if(!BuiltInRegistries.ITEM.containsKey(itemId))return;
        String name=json.get("name").getAsString();if(name.length()>50)return;
        String model=json.get("model").getAsString();if(!model.contains(":"))model="minecraft:"+(model.startsWith("item/")?model:"item/"+model);
        var modelId=Identifier.parse(model);
        if(!modelId.getPath().contains("/"))modelId=Identifier.fromNamespaceAndPath(modelId.getNamespace(),"item/"+modelId.getPath());
        var id=json.has("id")?Identifier.parse(json.get("id").getAsString()):fileId;
        var tags=new ArrayList<String>();if(json.has("tags"))for(var tag:json.getAsJsonArray("tags")){if(tags.size()==16)break;tags.add(tag.getAsString().substring(0,Math.min(64,tag.getAsString().length())));}
        float weight=json.has("weight")?json.get("weight").getAsFloat():0;if(!Float.isFinite(weight))weight=0;
        var render=Identifier.fromNamespaceAndPath("simplevisuals","cit_"+java.util.HexFormat.of().formatHex(digest(id.toString())));
        files.put("assets/simplevisuals/items/"+render.getPath()+".json",new Gson().toJson(Map.of("model",Map.of("type","minecraft:model","model",modelId.toString()))));
        entries.add(new Entry(id,BuiltInRegistries.ITEM.getValue(itemId),name,modelId,List.copyOf(tags),weight,render));
    }
}
