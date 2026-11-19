package com.simplebuilding.modules.simpleslabs;
import java.util.List;
import net.fabricmc.api.ModInitializer;
public final class SlabsFabric implements ModInitializer {
    @Override public void onInitialize() {
        ExampleItems.register();
        // Test centre station (docs/TESTZENTRALE.md): the module's items in the shared hub; title key in the lang files.
        com.simplebuilding.framework.api.TestCentreStations.register("simpleslabs", "main", "simpleslabs.testcentre.station",
                List.of("simpleslabs:token"));
    }
}
