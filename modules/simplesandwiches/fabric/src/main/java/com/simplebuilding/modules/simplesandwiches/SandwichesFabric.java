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
        // CMD24: /simplesandwiches config for every option (Fabric only so far: the NeoForge/Forge builds do not compile against SimpleLib).
        com.simplelib.api.config.ConfigCommands.server(Sandwiches.MOD_ID).json(com.simplesandwiches.config.SandwichConfig::toJson,
                com.simplesandwiches.config.SandwichConfig::apply).file(FabricLoader.getInstance().getConfigDir(), com.simplesandwiches.config.SandwichConfig.FILE).register();
        Sandwiches.simpleEnd = FabricLoader.getInstance().isModLoaded("simpleend");
        ModComponents.register();
        com.simplesandwiches.registry.ModSounds.register();
        com.simplesandwiches.registry.ModEffects.register();
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register(FabricBlockEntityTypeBuilder.create(CuttingBoardBlockEntity::new, ModBlocks.boards()).build());
        SandwichRegistry.tab();
        SandwichRegistry.VANILLA_TABS.forEach(tab -> net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(tab).register(out -> {
            if (!Sandwiches.on() || !com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), "simplesandwiches")) return;
            SandwichRegistry.vanillaTabStacks().getOrDefault(tab, java.util.List.of()).forEach(s -> out.accept(s, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        }));
        SandwichRegistry.setup();
        SandwichesEnabledCondition.register();
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplesandwiches.guide.SandwichGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
