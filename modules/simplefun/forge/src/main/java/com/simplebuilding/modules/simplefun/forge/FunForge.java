package com.simplebuilding.modules.simplefun.forge;
import com.simplefun.*;
import com.simplefun.registry.*;
import net.minecraft.core.registries.*;
@net.minecraftforge.fml.common.Mod("simplefun")
public final class FunForge {
 public FunForge(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context){
  var bus=context.getModBusGroup();SimplefunCommon.init();SimplefunCommon.registerConfig();ModuleForgeTests.register(bus);
  net.minecraftforge.registries.RegisterEvent.getBus(bus).addListener(e->{
   if(e.getRegistryKey().equals(Registries.BLOCK))com.simplefun.heads.AnimalHeads.blocks();
   if(e.getRegistryKey().equals(Registries.ITEM)){e.register(Registries.ITEM,r->r.register(ModItems.BRICK_SNOWBALL_KEY,ModItems.BRICK_SNOWBALL));com.simplefun.heads.AnimalHeads.items();}
   if(e.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB))com.simplefun.heads.AnimalHeads.tab();
   e.register(Registries.ENTITY_TYPE,r->r.register(ModEntities.BRICK_PROJECTILE_KEY,ModEntities.BRICK_PROJECTILE));
   e.register(Registries.MOB_EFFECT,r->r.register(ModEffects.PIGGY_KEY,ModEffects.PIGGY_EFFECT));
   e.register(net.minecraftforge.registries.ForgeRegistries.Keys.CONDITION_SERIALIZERS,com.simplefun.heads.AnimalHeads.id("trades_enabled"),()->FunCondition.CODEC);
  });
  // Beside the vanilla models, so the search tab lists them there (AnimalHeads.SNOWBALL_ANCHOR).
  net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(e->{
   java.util.List<net.minecraft.world.item.ItemStack> stacks;net.minecraft.world.item.ItemStack previous;
   if(e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.COMBAT)){previous=new net.minecraft.world.item.ItemStack(com.simplefun.heads.AnimalHeads.SNOWBALL_ANCHOR);stacks=java.util.List.of(new net.minecraft.world.item.ItemStack(ModItems.BRICK_SNOWBALL));}
   else if(e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS)){previous=new net.minecraft.world.item.ItemStack(com.simplefun.heads.AnimalHeads.HEAD_ANCHOR);stacks=com.simplefun.heads.AnimalHeads.headStacks();}
   else return;
   var entries=e.getEntries();if(!entries.contains(previous))return;
   for(var stack:stacks){entries.putAfter(previous,stack,net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);previous=stack;}
  });
  if(net.minecraftforge.fml.loading.FMLEnvironment.dist==net.minecraftforge.api.distmarker.Dist.CLIENT)FunForgeClient.init(bus);
  net.minecraftforge.event.entity.living.LivingDeathEvent.BUS.addListener(e->{com.simplefun.event.PlayerHeadDrop.onDeath(e.getEntity(),e.getSource());});
  net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e->com.simplefun.command.SimplefunCommands.register(e.getDispatcher()));
 }
}
