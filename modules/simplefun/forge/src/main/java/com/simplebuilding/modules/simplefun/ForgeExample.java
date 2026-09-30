package com.simplebuilding.modules.simplefun;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
@Mod("simplefun")
public final class ForgeExample {
    public ForgeExample(FMLJavaModLoadingContext context) {
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) ExampleItems.register();
        });
    }
}
