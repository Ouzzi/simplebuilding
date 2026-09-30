from pathlib import Path
import json
M=Path(__file__).resolve().parents[1]
def w(p,s):p=M/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf-8')
def r(p,a,b):p=M/p;s=p.read_text();assert a in s,(p,a);p.write_text(s.replace(a,b))
r('fabric/src/main/java/com/simplefun/SimplefunFabric.java','net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents','net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents')
r('fabric/src/main/java/com/simplefun/SimplefunFabric.java','ItemGroupEvents.modifyEntriesEvent','CreativeModeTabEvents.modifyOutputEvent')
r('fabric/src/main/java/com/simplefun/SimplefunFabricClient.java','LivingEntityFeatureRendererRegistrationCallback','LivingEntityRenderLayerRegistrationCallback')
r('shared/java/com/simplefun/entity/BrickProjectileEntity.java','net.minecraft.world.level.block.GlassBlock','net.minecraft.world.level.block.TransparentBlock')
# Shared config screen reads a copy, writes only disk for the next server restart. No client packets.
w('shared/java/com/simplefun/client/FunConfigScreen.java','''package com.simplefun.client;
import com.simplefun.*;import com.simplefun.config.*;import net.minecraft.client.gui.screens.Screen;import net.minecraft.network.chat.Component;import me.shedaniel.clothconfig2.api.ConfigBuilder;
public final class FunConfigScreen {
 public static Screen build(Screen parent){var gson=new com.google.gson.GsonBuilder().setPrettyPrinting().create();var c=gson.fromJson(gson.toJson(SimplefunCommon.getConfig()),SimplefunConfig.class);var defaults=new SimplefunConfig();var b=ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("text.autoconfig.simplefun.title"));var eb=b.entryBuilder();
 for(var field:SimplefunConfig.Fun.class.getFields()){String key=field.getName();String prefix="text.autoconfig.simplefun.option.fun."+key;var cat=b.getOrCreateCategory(Component.translatable("simplefun.config.tab."+(key.endsWith("Greeting")||key.equals("enableAnimalHeads")?"heads":key.endsWith("Crumbs")||java.util.Set.of("flowerSniff","appleSparkle","carrotCrunch","melonSplash","honeyBubbles","berryBlush").contains(key)?"delights":"gameplay")));
 try{if(field.getType()==boolean.class)cat.addEntry(eb.startBooleanToggle(Component.translatable(prefix),field.getBoolean(c.fun)).setDefaultValue(field.getBoolean(defaults.fun)).setTooltip(Component.translatable(prefix+".@Tooltip")).setSaveConsumer(v->{try{field.setBoolean(c.fun,v);}catch(Exception e){throw new IllegalStateException(e);}}).build());else cat.addEntry(eb.startFloatField(Component.translatable(prefix),field.getFloat(c.fun)).setDefaultValue(field.getFloat(defaults.fun)).setMin(key.equals("yeetStrength")?.1f:0).setMax(key.equals("yeetStrength")?3:4).setTooltip(Component.translatable(prefix+".@Tooltip")).setSaveConsumer(v->{try{field.setFloat(c.fun,v);}catch(Exception e){throw new IllegalStateException(e);}}).build());}catch(Exception e){throw new IllegalStateException(e);}}
 b.setSavingRunnable(()->{c.normalize();try{java.nio.file.Files.writeString(java.nio.file.Path.of("config/simplefun.json"),gson.toJson(c));}catch(Exception e){throw new IllegalStateException(e);}});return b.build();}
}''')
r('fabric/src/main/java/com/simplefun/compat/ModMenuIntegration.java','AutoConfig.getConfigScreen(SimplefunConfig.class, parent).get()','com.simplefun.client.FunConfigScreen.build(parent)')
w('neoforge/src/main/java/com/simplefun/SimplefunNeoForgeConfigScreen.java','package com.simplefun; public final class SimplefunNeoForgeConfigScreen { public static net.minecraft.client.gui.screens.Screen build(net.minecraft.client.gui.screens.Screen p){return com.simplefun.client.FunConfigScreen.build(p);} }')
# Animal heads use the existing Vanilla skull contracts; no SimpleBuilding internals.
w('shared/java/com/simplefun/heads/AnimalHead.java','''package com.simplefun.heads;
import net.minecraft.world.level.block.SkullBlock;import net.minecraft.world.entity.*;import net.minecraft.sounds.*;
public enum AnimalHead implements SkullBlock.Type {
 PIG(EntityTypes.PIG,SoundEvents.PIG_AMBIENT,"pig/temperate_pig"), COW(EntityTypes.COW,SoundEvents.COW_AMBIENT,"cow/temperate_cow"), CHICKEN(EntityTypes.CHICKEN,SoundEvents.CHICKEN_AMBIENT,"chicken/temperate_chicken"), SHEEP(EntityTypes.SHEEP,SoundEvents.SHEEP_AMBIENT,"sheep/sheep");
 public final EntityType<?> source;public final SoundEvent sound;public final String texture;
 AnimalHead(EntityType<?> e,SoundEvent s,String t){source=e;sound=s;texture=t;SkullBlock.Type.TYPES.put(getSerializedName(),this);}
 public String getSerializedName(){return "simplefun:"+name().toLowerCase(java.util.Locale.ROOT);}public String path(){return name().toLowerCase(java.util.Locale.ROOT)+"_head";}public String wall(){return name().toLowerCase(java.util.Locale.ROOT)+"_wall_head";}
}''')
w('shared/java/com/simplefun/heads/AnimalHeads.java','''package com.simplefun.heads;
import java.util.*;import net.minecraft.core.*;import net.minecraft.core.component.DataComponents;import net.minecraft.core.registries.*;import net.minecraft.resources.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.*;import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;import net.minecraft.world.item.*;import net.minecraft.world.entity.EquipmentSlot;
public final class AnimalHeads {
 public static final Map<AnimalHead,Block> STANDING=new EnumMap<>(AnimalHead.class),WALL=new EnumMap<>(AnimalHead.class);public static final Map<AnimalHead,Item> ITEMS=new EnumMap<>(AnimalHead.class);
 public static Identifier id(String s){return Identifier.fromNamespaceAndPath("simplefun",s);}
 public static void blocks(){for(var t:AnimalHead.values()){var key=ResourceKey.create(Registries.BLOCK,id(t.path()));var b=Registry.register(BuiltInRegistries.BLOCK,key,new SkullBlock(t,BlockBehaviour.Properties.of().setId(key).strength(1).noOcclusion().instrument(NoteBlockInstrument.CUSTOM_HEAD)));STANDING.put(t,b);var wk=ResourceKey.create(Registries.BLOCK,id(t.wall()));WALL.put(t,Registry.register(BuiltInRegistries.BLOCK,wk,new WallSkullBlock(t,BlockBehaviour.Properties.of().setId(wk).strength(1).noOcclusion().overrideLootTable(b.getLootTable()).overrideDescription(b.getDescriptionId()))));}}
 public static void items(){for(var t:AnimalHead.values()){var k=ResourceKey.create(Registries.ITEM,id(t.path()));ITEMS.put(t,Registry.register(BuiltInRegistries.ITEM,k,new StandingAndWallBlockItem(STANDING.get(t),WALL.get(t),Direction.DOWN,new Item.Properties().setId(k).useBlockDescriptionPrefix().equippableUnswappable(EquipmentSlot.HEAD).component(DataComponents.NOTE_BLOCK_SOUND,t.sound.location()))));}}
 public static void tab(){Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,id("fun"),CreativeModeTab.builder(CreativeModeTab.Row.TOP,0).title(net.minecraft.network.chat.Component.translatable("itemGroup.simplefun.fun")).icon(()->new ItemStack(com.simplefun.registry.ModItems.BRICK_SNOWBALL)).displayItems((p,out)->{out.accept(com.simplefun.registry.ModItems.BRICK_SNOWBALL);for(var t:AnimalHead.values())out.accept(ITEMS.get(t));}).build());}
}''')
w('shared/java/com/simplefun/mixin/AnimalSkullValidityMixin.java','''package com.simplefun.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;import net.minecraft.world.level.block.entity.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;
@Mixin(BlockEntityType.class)public class AnimalSkullValidityMixin {@Inject(method="isValid",at=@At("HEAD"),cancellable=true)private void fun$valid(BlockState s,CallbackInfoReturnable<Boolean> c){if((Object)this==BlockEntityTypes.SKULL&&s.getBlock() instanceof AbstractSkullBlock skull&&skull.getType() instanceof com.simplefun.heads.AnimalHead)c.setReturnValue(true);}}
''')
for p in ['fabric/src/main/java/com/simplefun/SimplefunFabric.java','neoforge/src/main/java/com/simplefun/SimplefunNeoForge.java']:
 if p.startswith('fabric'):
  r(p,'SimplefunRegistry.registerItems();','com.simplefun.heads.AnimalHeads.blocks(); SimplefunRegistry.registerItems(); com.simplefun.heads.AnimalHeads.items(); com.simplefun.heads.AnimalHeads.tab(); FunFabricData.register();')
 else:r(p,'event.register(Registries.ITEM, h -> h.register(ModItems.BRICK_SNOWBALL_KEY, ModItems.BRICK_SNOWBALL));','''if(event.getRegistryKey().equals(Registries.BLOCK))com.simplefun.heads.AnimalHeads.blocks();
            if(event.getRegistryKey().equals(Registries.ITEM)){event.register(Registries.ITEM, h -> h.register(ModItems.BRICK_SNOWBALL_KEY, ModItems.BRICK_SNOWBALL));com.simplefun.heads.AnimalHeads.items();}
            if(event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB))com.simplefun.heads.AnimalHeads.tab();
            if(event.getRegistryKey().equals(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS))event.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS,com.simplefun.heads.AnimalHeads.id("trades_enabled"),()->FunNeoData.CODEC);''')
# Module trade condition and charged-creeper additive pools.
for loader in ['fabric','neoforge']:
 src=M.parents[1]/f'modules/simpleriding/{loader}/src/main/java/com/simplebuilding/modules/simpleriding/Riding{ "Fabric" if loader=="fabric" else "Neo"}Data.java'
 s=src.read_text().replace('package com.simplebuilding.modules.simpleriding;','package com.simplefun;').replace('import com.simpleriding.*;','import com.simplefun.heads.*;').replace('RidingFabricData','FunFabricData').replace('RidingNeoData','FunNeoData').replace('Riding.id','AnimalHeads.id').replace('Riding.CONFIG.worldGen.enableVillagerTrades','SimplefunCommon.getConfig().fun.enableNoDamageTrades').replace('modid="simpleriding"','modid="simplefun"').replace('RidingLoot.apply','FunLoot.apply')
 s=s.replace(' Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE,AnimalHeads.id("weighted_enchant"),WeightedEnchantFunction.MAP_CODEC);','')
 w(f'{loader}/src/main/java/com/simplefun/Fun{"Fabric" if loader=="fabric" else "Neo"}Data.java',s)
p=M.parents[1]/'common/src/shared/java/com/simplebuilding/loot/ModLootTableModifications.java';s=p.read_text();start=s.index('    public static LootPool.Builder headPool');end=s.index('\n    }',start)+6;print(s[start:end])
d=json.loads((M/'shared/resources/simplefun.mixins.json').read_text());d['mixins']+=['AnimalSkullValidityMixin'];w('shared/resources/simplefun.mixins.json',json.dumps(d,indent=2))
