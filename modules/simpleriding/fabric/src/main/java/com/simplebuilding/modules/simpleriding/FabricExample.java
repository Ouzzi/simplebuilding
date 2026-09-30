package com.simplebuilding.modules.simpleriding;
import com.simpleriding.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
public final class FabricExample implements ModInitializer {
 public void onInitialize(){Riding.CONFIG=RidingConfig.load(FabricLoader.getInstance().getConfigDir());Riding.components();Riding.tab();RidingFabricData.register();}
}
