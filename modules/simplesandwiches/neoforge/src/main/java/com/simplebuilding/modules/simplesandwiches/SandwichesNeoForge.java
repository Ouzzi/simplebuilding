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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(Sandwiches.MOD_ID)
public final class SandwichesNeoForge {
    public SandwichesNeoForge(IEventBus bus, Dist dist) {
        Sandwiches.init(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.DATA_COMPONENT_TYPE)) ModComponents.register();
            if (key.equals(Registries.BLOCK)) ModBlocks.register();
            if (key.equals(Registries.ITEM)) ModItems.register();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                ModBlockEntities.register(new BlockEntityType<>(CuttingBoardBlockEntity::new, Set.of(ModBlocks.boards()), false));
            }
            if (key.equals(Registries.CREATIVE_MODE_TAB)) SandwichRegistry.tab();
        });
        SandwichRegistry.setup();
        if (dist == Dist.CLIENT) SandwichesNeoForgeClient.init(bus);
    }
}
