package com.simplebuilding.modules.simpleoptserver.fabric;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.Config;

/**
 * Fabric side of the target check (concept §4.5): the target classes of every prepared mixin config, by the mod that
 * registered it (Fabric decorates each config with its mod id). Our own family (author Ouzzi), the loader and Fabric API
 * never count as a conflict. Same helper in each Simple Optimization sub-mod, since sub-mods share no classes.
 */
final class OptTargetScan {
    private OptTargetScan() {}

    static Map<String, Set<String>> targetsByMod() {
        Map<String, Set<String>> out = new HashMap<>();
        for (Config c : Mixins.getConfigs()) {
            IMixinConfig config = c.getConfig();
            if (config == null) continue;
            out.computeIfAbsent(FabricUtil.getModId(config, "unknown"), k -> new HashSet<>()).addAll(config.getTargets());
        }
        return out;
    }

    static boolean ignored(String modId) {
        if (modId.startsWith("fabric") || modId.equals("minecraft") || modId.equals("java") || modId.equals("mixinextras")) return true;
        return FabricLoader.getInstance().getModContainer(modId)
                .map(m -> m.getMetadata().getAuthors().stream().anyMatch(p -> "Ouzzi".equals(p.getName()))).orElse(false);
    }
}
