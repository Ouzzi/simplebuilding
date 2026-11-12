package com.simplebuilding.modules.simplelib;

import com.simplelib.api.LibFeatures;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.CustomValue;

/** simplelib-3: reads {@value LibFeatures#KEY} from every loaded mod's fabric.mod.json and decides {@link LibFeatures} once. */
final class LibFeaturesFabric {
    private LibFeaturesFabric() {}

    static void decide() {
        Map<String, List<String>> declared = new LinkedHashMap<>();
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            CustomValue custom = mod.getMetadata().getCustomValue(LibFeatures.KEY);
            if (custom == null || custom.getType() != CustomValue.CvType.ARRAY) continue;
            List<String> features = new ArrayList<>();
            for (CustomValue v : custom.getAsArray()) {
                if (v.getType() == CustomValue.CvType.STRING) features.add(v.getAsString());
            }
            declared.put(mod.getMetadata().getId(), features);
        }
        LibFeatures.decide(declared);
        for (String feature : LibFeatures.ALL) {
            com.simplelib.SimpleLib.LOG.info("SimpleLib feature {}: {}", feature,
                    LibFeatures.requested(feature) ? "on, asked for by " + LibFeatures.requesters(feature) : "off (no mod asked for it)");
        }
    }
}
