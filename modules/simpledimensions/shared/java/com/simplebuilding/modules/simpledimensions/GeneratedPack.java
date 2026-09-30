package com.simplebuilding.modules.simpledimensions;
import dev.simpledimension.common.config.DimensionConfigStore;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.validation.DirectoryValidator;
import java.util.*;
import java.util.function.Consumer;
/** The same mandatory config pack is discovered on both loaders before world registries load. */
public final class GeneratedPack {
 public static void load(Consumer<Pack> result){
  DimensionConfigStore.loadAndGenerate(DimensionRuntime.CONFIG_ROOT,SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA).major());
  try {FolderRepositorySource.discoverPacks(DimensionConfigStore.datapacksFolder(DimensionRuntime.CONFIG_ROOT),new DirectoryValidator(p->false),(path,resources)->{
   if(!path.getFileName().toString().equals("simpledimension_generated"))return;
   var info=new PackLocationInfo("simpledimension_generated",Component.literal("Simple Dimensions"),PackSource.BUILT_IN,Optional.empty());
   var pack=Pack.readMetaAndCreate(info,resources,PackType.SERVER_DATA,new PackSelectionConfig(true,Pack.Position.TOP,true));
   if(pack==null)throw new IllegalStateException("Generated dimension pack failed validation");result.accept(pack);
  });}catch(java.io.IOException e){throw new IllegalStateException("Cannot load generated dimension pack",e);}
 }
}
