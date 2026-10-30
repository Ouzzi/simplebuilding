package com.simplebuilding.modules.simpledinos;

import com.simpledinos.DinosRegistry;
import com.simpledinos.SimpleDinos;
import com.simpledinos.block.DinoEggBlockEntity;
import com.simpledinos.block.FossilBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. The fossil block's worldgen comes from the
 * biome modifier file in data/simpledinos/neoforge/biome_modifier.
 */
@Mod(SimpleDinos.MOD_ID)
public final class DinosNeoForge {
    public DinosNeoForge(IEventBus bus, Dist dist) {
        SimpleDinos.init(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) event.register(Registries.BLOCK, h -> DinosRegistry.blocks(h::register));
            if (key.equals(Registries.ITEM)) event.register(Registries.ITEM, h -> DinosRegistry.items(h::register));
            if (key.equals(Registries.ENTITY_TYPE)) event.register(Registries.ENTITY_TYPE, h -> DinosRegistry.entityTypes(h::register));
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                DinosRegistry.registerBlockEntityTypes(new BlockEntityType<>(FossilBlockEntity::new, DinosRegistry.fossilBlocks(), false),
                        new BlockEntityType<>(DinoEggBlockEntity::new, DinosRegistry.eggBlocks(), false));
            }
        });
        bus.addListener((EntityAttributeCreationEvent event) -> event.put(DinosRegistry.TREX, DinosRegistry.trexAttributes().build()));
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (!SimpleDinos.on()) return;
            var stacks = DinosRegistry.tabStacks();
            var vis = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
            if (event.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS)) stacks.subList(0, 1 + com.simpledinos.DinoKind.values().length).forEach(s -> event.accept(s, vis));
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                event.accept(new net.minecraft.world.item.ItemStack(DinosRegistry.LARGE_SADDLE), vis);
                event.accept(com.simpledinos.guide.DinosGuide.stack(), vis);
            }
            if (event.getTabKey().equals(CreativeModeTabs.COMBAT)) {
                event.accept(new net.minecraft.world.item.ItemStack(DinosRegistry.TREX_ARMOR_LEATHER), vis);
                event.accept(new net.minecraft.world.item.ItemStack(DinosRegistry.TREX_ARMOR_IRON), vis);
                event.accept(new net.minecraft.world.item.ItemStack(DinosRegistry.TREX_ARMOR_DIAMOND), vis);
            }
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simpledinos.guide.DinosGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (dist == Dist.CLIENT) DinosNeoClient.init(bus);
    }
}
