package com.simplebuilding.modules.simplemodels;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Stage 0 of the model editor (b-models12): six example models shipped in the module jar. Their item definitions and
 * models live in {@code assets/simplemodels/items|models/item/example/} (always loaded with the mod, no pack needed,
 * only {@code minecraft:block/...} texture references), their catalogue entries in {@code assets/simplemodels/examples/}.
 * Fixed name list, no classpath scan; every entry runs through the same strict {@link ModelDefinition#parse}.
 */
public final class ModelExamples {
    public static final List<String> NAMES = List.of("stool", "table_lantern", "mailbox", "birdhouse", "weather_vane", "crystal_cluster");
    private static volatile List<ModelDefinition> cache;

    public static String resource(String path) throws IOException {
        try (InputStream in = ModelExamples.class.getResourceAsStream("/assets/simplemodels/" + path)) {
            if (in == null) throw new IOException("Missing resource " + path);
            byte[] bytes = in.readNBytes(ModelPolicy.MAX_FILE_BYTES + 1);
            if (bytes.length > ModelPolicy.MAX_FILE_BYTES) throw new IOException("Oversized resource " + path);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    /** The built-in catalogue entries in {@link #NAMES} order; a broken entry is logged and skipped. */
    public static List<ModelDefinition> all() {
        var list = cache;
        if (list != null) return list;
        var out = new ArrayList<ModelDefinition>();
        for (String name : NAMES) {
            try { out.add(ModelDefinition.parse(JsonParser.parseString(resource("examples/" + name + ".json")).getAsJsonObject())); }
            catch (IOException | RuntimeException e) {
                org.slf4j.LoggerFactory.getLogger("simplemodels").warn("Rejected built-in example {}: {}", name, e.getMessage());
            }
        }
        return cache = List.copyOf(out);
    }

    /** Appends the examples (policy {@code builtinExamples}); folder entries with the same id win, {@code maxModels} still caps. */
    public static ModelCatalogue.Snapshot merge(ModelCatalogue.Snapshot loaded) {
        if (!loaded.policy().builtinExamples) return loaded;
        var models = new ArrayList<>(loaded.models());
        var ids = new HashSet<String>();
        models.forEach(d -> ids.add(d.id()));
        for (var example : all()) {
            if (models.size() >= loaded.policy().maxModels) break;
            if (ids.add(example.id())) models.add(example);
        }
        return new ModelCatalogue.Snapshot(loaded.policy(), List.copyOf(models));
    }

    private ModelExamples() {}
}
