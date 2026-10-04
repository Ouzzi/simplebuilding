package com.simplebuilding.modules.simplesandwiches;

import com.simplesandwiches.Sandwiches;
import com.simplesandwiches.block.CuttingBoardBlockEntity;
import com.simplesandwiches.registry.ModBlockEntities;
import com.simplesandwiches.registry.ModBlocks;
import com.simplesandwiches.registry.ModComponents;
import com.simplesandwiches.registry.ModItems;
import com.simplesandwiches.registry.SandwichRegistry;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration order as Fabric/NeoForge. */
@Mod(Sandwiches.MOD_ID)
public final class SandwichesForge {
    public SandwichesForge(FMLJavaModLoadingContext context) {
        Sandwiches.init(FMLPaths.CONFIGDIR.get());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.DATA_COMPONENT_TYPE)) ModComponents.register();
            if (key.equals(Registries.BLOCK)) ModBlocks.register();
            if (key.equals(Registries.ITEM)) ModItems.register();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                ModBlockEntities.register(new BlockEntityType<>(CuttingBoardBlockEntity::new, Set.of(ModBlocks.boards())));
            }
            if (key.equals(Registries.CREATIVE_MODE_TAB)) SandwichRegistry.tab();
        });
        SandwichRegistry.setup();
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) SandwichesForgeClient.init();
    }
}
