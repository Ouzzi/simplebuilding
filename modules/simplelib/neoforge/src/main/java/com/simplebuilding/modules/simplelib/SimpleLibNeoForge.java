package com.simplebuilding.modules.simplelib;

import com.simplelib.SimpleLib;
import com.simplelib.crucible.CrucibleBlockEntity;
import com.simplelib.registry.LibBlockEntities;
import com.simplelib.registry.LibBlocks;
import com.simplelib.registry.LibComponents;
import com.simplelib.registry.LibItems;
import com.simplelib.registry.LibMenus;
import com.simplelib.registry.LibRegistry;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleLib.MOD_ID)
public final class SimpleLibNeoForge {
    public SimpleLibNeoForge(IEventBus bus, Dist dist) {
        SimpleLib.init(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.DATA_COMPONENT_TYPE)) LibComponents.register();
            if (key.equals(Registries.BLOCK)) LibBlocks.register();
            if (key.equals(Registries.ITEM)) LibItems.register();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                LibBlockEntities.register(new BlockEntityType<>(CrucibleBlockEntity::new, Set.of(LibBlocks.crucibles()), false));
            }
            if (key.equals(Registries.MENU)) LibMenus.register();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) LibRegistry.tab();
        });
        if (dist == Dist.CLIENT) SimpleLibNeoForgeClient.init(bus);
    }
}
