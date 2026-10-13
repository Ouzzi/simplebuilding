package com.simplebuilding.modules.simpletrims;

import com.simpletrims.SimpleTrims;
import com.simpletrims.TrimsRegistry;
import com.simpletrims.placed.PlacedTrimTemplateBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleTrims.MOD_ID)
public final class TrimsNeoForge {
    public TrimsNeoForge(IEventBus bus, Dist dist) {
        SimpleTrims.init(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.DATA_COMPONENT_TYPE)) TrimsRegistry.registerComponents();
            if (key.equals(Registries.BLOCK)) TrimsRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) TrimsRegistry.registerItems();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                TrimsRegistry.registerBlockEntity(new BlockEntityType<>(PlacedTrimTemplateBlockEntity::new, Set.of(TrimsRegistry.PLACED_TEMPLATE), false));
            }
            if (key.equals(Registries.RECIPE_SERIALIZER)) TrimsRegistry.registerRecipeSerializers();
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)
                    && com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleTrims.MOD_ID)) {
                TrimsRegistry.tabStacks().forEach(s -> event.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleTrims.on()) {
                event.accept(com.simpletrims.guide.TrimsGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simpletrims.guide.TrimsGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (dist == Dist.CLIENT) TrimsNeoClient.init(bus);
    }
}
