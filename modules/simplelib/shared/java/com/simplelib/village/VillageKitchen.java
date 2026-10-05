package com.simplelib.village;

import com.mojang.datafixers.util.Pair;
import com.simplelib.SimpleLib;
import com.simplelib.config.LibConfig;
import com.simplelib.mixin.TemplatePoolAccessor;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

/**
 * Village field kitchen (owner, plan section 11; answers 44-46): a small piece in the houses pool of
 * all five village types - an iron crucible on an unlit campfire holding cooked food, a barrel with
 * raw meat beside it and a seat. Weight 3 by default (about every third village); 0 switches it off.
 */
public final class VillageKitchen {
    public static final List<String> TYPES = List.of("plains", "desert", "savanna", "snowy", "taiga");

    /** Appends the kitchen to every village houses pool; returns how many pools changed. Called once per server start. */
    public static int inject(RegistryAccess access) {
        int weight = LibConfig.villageKitchenWeight;
        if (weight <= 0) return 0;
        Registry<StructureTemplatePool> pools = access.lookupOrThrow(Registries.TEMPLATE_POOL);
        int changed = 0;
        for (String type : TYPES) {
            var pool = pools.getValue(Identifier.withDefaultNamespace("village/" + type + "/houses"));
            if (pool == null) continue;
            String location = SimpleLib.MOD_ID + ":village/" + type + "/field_kitchen";
            TemplatePoolAccessor accessor = (TemplatePoolAccessor) pool;
            if (accessor.simplelib$templates().stream().anyMatch(e -> e.toString().contains(location))) continue;
            StructurePoolElement element = StructurePoolElement.legacy(location).apply(StructureTemplatePool.Projection.RIGID);
            for (int i = 0; i < weight; i++) accessor.simplelib$templates().add(element);
            try {
                accessor.simplelib$rawTemplates().add(Pair.of(element, weight));
            } catch (UnsupportedOperationException immutable) {
                // the raw list is only informative; selection uses the expanded template list
            }
            changed++;
        }
        return changed;
    }

    private VillageKitchen() {}
}
