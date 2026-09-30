package com.simplebuilding.modules.simplevisuals;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
@Mod("simplevisuals")
public final class ForgeExample {
    public ForgeExample(FMLJavaModLoadingContext context) {
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) ExampleItems.register();
        });
    }
}
