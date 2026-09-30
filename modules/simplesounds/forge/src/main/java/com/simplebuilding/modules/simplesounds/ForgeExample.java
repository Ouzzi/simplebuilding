package com.simplebuilding.modules.simplesounds;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
@Mod("simplesounds")
public final class ForgeExample {
    public ForgeExample(FMLJavaModLoadingContext context) {
        com.simplebuilding.modules.simplesounds.forge.ModuleForgeTests.register(context.getModBusGroup());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) ExampleItems.register();
        });
    }
}
