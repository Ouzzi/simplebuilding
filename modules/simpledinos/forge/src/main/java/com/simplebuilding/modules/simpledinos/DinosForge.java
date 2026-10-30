package com.simplebuilding.modules.simpledinos;

import com.simpledinos.DinosRegistry;
import com.simpledinos.SimpleDinos;
import com.simpledinos.block.DinoEggBlockEntity;
import com.simpledinos.block.FossilBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry: same registration as Fabric/NeoForge (worldgen, tests and client follow with the Forge port, not requested yet). */
@Mod(SimpleDinos.MOD_ID)
public final class DinosForge {
    public DinosForge(FMLJavaModLoadingContext context) {
        SimpleDinos.init(FMLPaths.CONFIGDIR.get());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) event.register(Registries.BLOCK, r -> DinosRegistry.blocks(r::register));
            if (key.equals(Registries.ITEM)) event.register(Registries.ITEM, r -> DinosRegistry.items(r::register));
            if (key.equals(Registries.ENTITY_TYPE)) event.register(Registries.ENTITY_TYPE, r -> DinosRegistry.entityTypes(r::register));
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                DinosRegistry.registerBlockEntityTypes(new BlockEntityType<>(FossilBlockEntity::new, DinosRegistry.fossilBlocks()),
                        new BlockEntityType<>(DinoEggBlockEntity::new, DinosRegistry.eggBlocks()));
            }
        });
        EntityAttributeCreationEvent.BUS.addListener(event -> event.put(DinosRegistry.TREX, DinosRegistry.trexAttributes().build()));
    }
}
