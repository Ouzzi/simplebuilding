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
        requestContent();
        for (String feature : LibFeatures.ALL) {
            com.simplelib.SimpleLib.LOG.info("SimpleLib feature {}: {}", feature,
                    LibFeatures.requested(feature) ? "on, asked for by " + LibFeatures.requesters(feature) : "off (no mod asked for it)");
        }
    }

    /** Content groups ({@code LibMaterials.GROUPS}) a mod requests by fabric.mod.json, without calling {@code requestMaterial}. */
    static final String CONTENT_KEY = "simplelib:content";

    /**
     * N48 (b-circ46): {@code "custom": {"simplelib:content": ["nether_bookshelves", ...]}} requests those SimpleLib material
     * groups (recipes, creative entries). Same key as VANILLA36 (a-vanilla36 d555bc109, not in this base): there a non-empty
     * list asks for SimpleLib's content as a whole, here each known group name also requests its group; unknown names are
     * ignored, so both readings stay compatible.
     */
    static void requestContent() {
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            CustomValue custom = mod.getMetadata().getCustomValue(CONTENT_KEY);
            if (custom == null || custom.getType() != CustomValue.CvType.ARRAY) continue;
            for (CustomValue v : custom.getAsArray()) {
                if (v.getType() == CustomValue.CvType.STRING && com.simplelib.registry.LibMaterials.GROUPS.containsKey(v.getAsString())) {
                    com.simplelib.registry.LibMaterials.request(v.getAsString());
                }
            }
        }
    }
}
