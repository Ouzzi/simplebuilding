package com.simplebuilding.modules.simpleaesthetics;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
/** Super-mod entry: no content, only the sub-mod switches (config/simpleaesthetics.json). */
@Mod("simpleaesthetics")
public final class ForgeExample {
    public ForgeExample(FMLJavaModLoadingContext context) {
        SimpleAesthetics.load(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
    }
}
