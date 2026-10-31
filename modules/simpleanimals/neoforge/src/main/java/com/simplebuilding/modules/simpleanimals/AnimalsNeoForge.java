package com.simplebuilding.modules.simpleanimals;

import com.simpleanimals.AnimalsRegistry;
import com.simpleanimals.SimpleAnimals;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. Natural spawns come from the
 * biome modifier files in data/simpleanimals/neoforge/biome_modifier.
 */
@Mod(SimpleAnimals.MOD_ID)
public final class AnimalsNeoForge {
    public AnimalsNeoForge(IEventBus bus, Dist dist) {
        SimpleAnimals.init(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            event.register(Registries.ENTITY_TYPE, h -> AnimalsRegistry.entityTypes(h::register));
            event.register(Registries.ITEM, h -> AnimalsRegistry.items(h::register));
        });
        bus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(AnimalsRegistry.DEER, AnimalsRegistry.deerAttributes().build());
            event.put(AnimalsRegistry.BOAR, AnimalsRegistry.boarAttributes().build());
            event.put(AnimalsRegistry.HEDGEHOG, AnimalsRegistry.hedgehogAttributes().build());
            event.put(AnimalsRegistry.CRAB, AnimalsRegistry.crabAttributes().build());
            event.put(AnimalsRegistry.ZEBRA, AnimalsRegistry.horseAttributes().build());
            event.put(AnimalsRegistry.ZORSE, AnimalsRegistry.horseAttributes().build());
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (!SimpleAnimals.on()) return;
            if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
                AnimalsRegistry.eggStacks().forEach(s -> event.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
            if (event.getTabKey().equals(CreativeModeTabs.FOOD_AND_DRINKS)) {
                event.accept(new ItemStack(AnimalsRegistry.RAW_CRAB), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.accept(new ItemStack(AnimalsRegistry.COOKED_CRAB), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.accept(new ItemStack(AnimalsRegistry.TRUFFLE), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.accept(new ItemStack(AnimalsRegistry.TRUFFLE_SOUP), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                event.accept(com.simpleanimals.guide.AnimalsGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simpleanimals.guide.AnimalsGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (dist == Dist.CLIENT) AnimalsNeoClient.init(bus);
    }
}
