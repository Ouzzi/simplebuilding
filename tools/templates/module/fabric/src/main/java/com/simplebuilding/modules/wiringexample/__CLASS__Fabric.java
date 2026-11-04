package com.simplebuilding.modules.__MODID__;
import java.util.List;
import net.fabricmc.api.ModInitializer;
public final class __CLASS__Fabric implements ModInitializer {
    @Override public void onInitialize() {
        ExampleItems.register();
        // Test centre station (docs/TESTZENTRALE.md): the module's items in the shared hub; title key in the lang files.
        com.simplebuilding.framework.api.TestCentreStations.register("__MODID__", "main", "__MODID__.testcentre.station",
                List.of("__MODID__:token"));
    }
}
