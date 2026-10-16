package com.simplebuilding.modules.simpletrims;

import com.simpletrims.SimpleTrims;
import com.simpletrims.TrimsRegistry;
import com.simpletrims.placed.PlacedTrimTemplateBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration order as Fabric/NeoForge. Id moves become Forge aliases in SimpleLib's registry events. */
@Mod(SimpleTrims.MOD_ID)
public final class TrimsForge {
    public TrimsForge(FMLJavaModLoadingContext context) {
        com.simplebuilding.modules.simpletrims.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleTrims.init(FMLPaths.CONFIGDIR.get());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.DATA_COMPONENT_TYPE)) TrimsRegistry.registerComponents();
            if (key.equals(Registries.BLOCK)) TrimsRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) TrimsRegistry.registerItems();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) TrimsRegistry.registerTab();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                TrimsRegistry.registerBlockEntity(new BlockEntityType<>(PlacedTrimTemplateBlockEntity::new, Set.of(TrimsRegistry.PLACED_TEMPLATE)));
            }
            if (key.equals(Registries.RECIPE_SERIALIZER)) TrimsRegistry.registerRecipeSerializers();
        });
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)
                    && com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleTrims.MOD_ID)) {
                TrimsRegistry.tabStacks().forEach(s -> event.getEntries().put(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleTrims.on()) {
                event.getEntries().put(com.simpletrims.guide.TrimsGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simpletrims.guide.TrimsGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) TrimsForgeClient.init();
    }
}
