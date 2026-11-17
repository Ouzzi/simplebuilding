package com.simpleores.client;

import com.simpleores.SimpleOres;
import com.simpleores.config.OresClientConfig;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * N78b (docs/ai/PLAN-N78B-2026-11-17.md): the client option "3D grates" chooses the grate models while the block models load
 * ({@code mixin.ModelManagerGrateMixin}). The mod ships the 3D models under the usual ids ({@code simpleores:block/<metal>_grate} and
 * Vanilla's {@code minecraft:block/<age>copper_grate}); with the option off each of them gets the content of its flat twin
 * {@code simpleores:block/<path>_flat} (a plain cube_all). Blockstates, items and resource packs stay untouched. No client classes here,
 * so the GameTests check {@link #apply(Map, boolean)}.
 */
public final class GrateModels {
    /** The option the loaded models were built with (null before the first load). */
    private static volatile Boolean loadedWith;

    private GrateModels() {}

    public static <T> Map<Identifier, T> apply(Map<Identifier, T> models) {
        return apply(models, OresClientConfig.grates3d());
    }

    public static <T> Map<Identifier, T> apply(Map<Identifier, T> models, boolean threeD) {
        loadedWith = threeD;
        if (threeD) return models;
        Map<Identifier, T> out = new HashMap<>(models);
        int swapped = 0;
        for (Map.Entry<Identifier, T> e : models.entrySet()) {
            Identifier flat = flatOf(e.getKey());
            T model = flat == null ? null : models.get(flat);
            if (model != null) {
                out.put(e.getKey(), model);
                swapped++;
            }
        }
        SimpleOres.LOG.debug("3D grates off: {} grate models flat", swapped);
        return out;
    }

    /** The flat twin of a grate model id, or null (only Simple Ores' and Vanilla's grates). */
    public static Identifier flatOf(Identifier id) {
        String ns = id.getNamespace();
        String path = id.getPath();
        if (!(ns.equals(SimpleOres.MOD_ID) || ns.equals("minecraft")) || !path.startsWith("block/") || !path.endsWith("_grate")) return null;
        return SimpleOres.id(path + "_flat");
    }

    /** True when the option differs from the loaded models (the caller reloads the resources). */
    public static boolean stale() {
        Boolean with = loadedWith;
        return with != null && with != OresClientConfig.grates3d();
    }
}
