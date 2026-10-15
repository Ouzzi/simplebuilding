package com.simplebuilding.modules.simplesandwiches;

import com.simplesandwiches.Sandwiches;
import com.simplesandwiches.block.CuttingBoardBlockEntity;
import com.simplesandwiches.registry.ModBlockEntities;
import com.simplesandwiches.registry.ModBlocks;
import com.simplesandwiches.registry.ModComponents;
import com.simplesandwiches.registry.ModItems;
import com.simplesandwiches.registry.SandwichRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class SandwichesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Sandwiches.init(FabricLoader.getInstance().getConfigDir());
        Sandwiches.simpleEnd = FabricLoader.getInstance().isModLoaded("simpleend");
        ModComponents.register();
        com.simplesandwiches.registry.ModSounds.register();
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register(FabricBlockEntityTypeBuilder.create(CuttingBoardBlockEntity::new, ModBlocks.boards()).build());
        SandwichRegistry.tab();
        SandwichRegistry.vanillaTabStacks().forEach((tab, stacks) -> net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(tab).register(out -> {
            if (!Sandwiches.on() || !com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), "simplesandwiches")) return;
            stacks.forEach(s -> out.accept(s, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        }));
        SandwichRegistry.setup();
        SandwichesEnabledCondition.register();
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplesandwiches.guide.SandwichGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
