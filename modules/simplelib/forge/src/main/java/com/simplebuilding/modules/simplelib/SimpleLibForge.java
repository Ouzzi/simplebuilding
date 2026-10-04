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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration order as Fabric/NeoForge. */
@Mod(SimpleLib.MOD_ID)
public final class SimpleLibForge {
    public SimpleLibForge(FMLJavaModLoadingContext context) {
        SimpleLib.init(FMLPaths.CONFIGDIR.get());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.DATA_COMPONENT_TYPE)) LibComponents.register();
            if (key.equals(Registries.BLOCK)) LibBlocks.register();
            if (key.equals(Registries.ITEM)) LibItems.register();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                LibBlockEntities.register(new BlockEntityType<>(CrucibleBlockEntity::new, Set.of(LibBlocks.crucibles())));
            }
            if (key.equals(Registries.MENU)) LibMenus.register();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) LibRegistry.tab();
        });
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            SimpleLibForgeClient.init(context);
        }
    }
}
