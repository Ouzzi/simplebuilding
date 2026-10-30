package com.simplebuilding.modules.simpleanimals;

import com.simpleanimals.AnimalsRegistry;
import com.simpleanimals.SimpleAnimals;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry: same registration as Fabric/NeoForge (spawns and tests follow with the Forge port, not requested yet). */
@Mod(SimpleAnimals.MOD_ID)
public final class AnimalsForge {
    public AnimalsForge(FMLJavaModLoadingContext context) {
        SimpleAnimals.init(FMLPaths.CONFIGDIR.get());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            event.register(Registries.ENTITY_TYPE, r -> AnimalsRegistry.entityTypes(r::register));
            event.register(Registries.ITEM, r -> AnimalsRegistry.items(r::register));
        });
        EntityAttributeCreationEvent.BUS.addListener(event -> {
            event.put(AnimalsRegistry.DEER, AnimalsRegistry.deerAttributes().build());
            event.put(AnimalsRegistry.BOAR, AnimalsRegistry.boarAttributes().build());
            event.put(AnimalsRegistry.HEDGEHOG, AnimalsRegistry.hedgehogAttributes().build());
            event.put(AnimalsRegistry.CRAB, AnimalsRegistry.crabAttributes().build());
            event.put(AnimalsRegistry.ZEBRA, AnimalsRegistry.horseAttributes().build());
            event.put(AnimalsRegistry.ZORSE, AnimalsRegistry.horseAttributes().build());
        });
    }
}
